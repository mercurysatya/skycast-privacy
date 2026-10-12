package com.vayu.weather.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * [BillingGateway] backed by the genuine Google Play Billing Library.
 *
 * Threading: all BillingClient calls happen on the calling coroutine;
 * purchase-sheet outcomes are bridged from [PurchasesUpdatedListener] via a
 * guarded [CompletableDeferred] so duplicate Play callbacks can never grant,
 * double-count, or crash (extra completions are ignored).
 *
 * No server-side verification exists in this project (Firebase Analytics /
 * Remote Config / App Check are not purchase-verification backends), so this
 * implements the supported client-side query flow. Limitation is documented in
 * `reports/VAAYU_BILLING_IMPLEMENTATION.md`.
 */
@Singleton
class PlayBillingGateway @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val products: BillingProductConfig
) : BillingGateway, PurchasesUpdatedListener {

    private companion object {
        const val TAG = "PlayBilling"
        const val MAX_CONNECT_ATTEMPTS = 3
        const val QUERY_TIMEOUT_MS = 15_000L
        const val LAUNCH_TIMEOUT_MS = 120_000L
    }

    private val lock = Mutex()
    private var client: BillingClient? = null

    /** Completes with the terminal outcome of the in-flight purchase sheet. */
    private var pendingOutcome: CompletableDeferred<PurchaseOutcome>? = null

    /** ProductDetails cache so launch can build params without re-querying. */
    private val detailsCache = mutableMapOf<String, ProductDetails>()

    // ------------------------------------------------------------------ setup

    private fun getOrBuildClient(): BillingClient {
        return client ?: BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
            .also { client = it }
    }

    override suspend fun ensureConnected(): BillingConnection {
        val c = getOrBuildClient()
        if (c.isReady) return BillingConnection.CONNECTED
        var attempt = 0
        var delayMs = 500L
        while (attempt < MAX_CONNECT_ATTEMPTS) {
            attempt++
            val code = suspendCancellableCoroutine<Int> { cont ->
                try {
                    c.startConnection(object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            if (cont.isActive) cont.resume(result.responseCode)
                        }

                        override fun onBillingServiceDisconnected() {
                            if (cont.isActive) {
                                cont.resume(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "startConnection threw", e)
                    if (cont.isActive) cont.resume(BillingClient.BillingResponseCode.ERROR)
                }
            }
            when (code) {
                BillingClient.BillingResponseCode.OK -> return BillingConnection.CONNECTED
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                    return BillingConnection.UNAVAILABLE
                else -> {
                    Log.w(TAG, "Billing connect attempt $attempt failed: $code")
                    delay(delayMs)
                    delayMs *= 2
                }
            }
        }
        return BillingConnection.DISCONNECTED
    }

    // ------------------------------------------------------------------ offers

    override suspend fun queryOffers(): List<BillingOffer> {
        if (!products.isConfigured) return emptyList()
        if (ensureConnected() != BillingConnection.CONNECTED) return emptyList()
        val c = getOrBuildClient()
        val offers = mutableListOf<BillingOffer>()
        suspend fun queryType(
            ids: List<String>,
            type: String
        ) {
            if (ids.isEmpty()) return
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ids.map { id ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(id)
                            .setProductType(type)
                            .build()
                    }
                )
                .build()
            try {
                withTimeout(QUERY_TIMEOUT_MS) {
                    // Captured on Play's callback thread; merged into shared
                    // state under lock after resume (two concurrent refreshes
                    // must never race on detailsCache).
                    var fetched: List<ProductDetails> = emptyList()
                    suspendCancellableCoroutine<Unit> { cont ->
                        c.queryProductDetailsAsync(params) { result, queryResult ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                fetched = queryResult.productDetailsList
                                // Non-empty means Play Console lacks these ids
                                // (wrong product id, draft app, or unsigned test
                                // build) — logged so the owner can fix setup.
                                queryResult.unfetchedProductList.forEach { unfetched ->
                                    Log.w(
                                        TAG,
                                        "Play returned no details for " +
                                            "${unfetched.productId} (${unfetched.statusCode})"
                                    )
                                }
                            } else {
                                Log.w(TAG, "queryProductDetails($type) -> ${result.responseCode}")
                            }
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                    lock.withLock {
                        fetched.forEach { d ->
                            detailsCache[d.productId] = d
                            offers += d.toOffers()
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "queryProductDetails($type) timed out", e)
            } catch (e: Exception) {
                Log.w(TAG, "queryProductDetails($type) threw", e)
            }
        }
        queryType(products.subscriptionIds, BillingClient.ProductType.SUBS)
        queryType(products.oneTimeIds, BillingClient.ProductType.INAPP)
        return offers
    }

    private fun ProductDetails.toOffers(): List<BillingOffer> {
        val cleanTitle = title.substringBefore(" (").ifBlank { title }
        val subsOffers = subscriptionOfferDetails
        if (subsOffers.isNullOrEmpty()) {
            // One-time product.
            val otp = oneTimePurchaseOfferDetails ?: return emptyList()
            return listOf(
                BillingOffer(
                    productId = productId,
                    basePlanId = null,
                    offerToken = "",
                    title = cleanTitle,
                    displayPrice = otp.formattedPrice,
                    displayPeriod = "one-time",
                    isSubscription = false
                )
            )
        }
        return subsOffers.mapNotNull { offer ->
            val token = offer.offerToken ?: return@mapNotNull null
            val phase = offer.pricingPhases.pricingPhaseList.firstOrNull()
            BillingOffer(
                productId = productId,
                basePlanId = offer.basePlanId,
                offerToken = token,
                title = cleanTitle,
                displayPrice = phase?.formattedPrice ?: "",
                displayPeriod = phase?.billingPeriod?.toDisplayPeriod() ?: "",
                isSubscription = true
            )
        }
    }

    private fun String.toDisplayPeriod(): String = when (this) {
        "P1W" -> "/ week"
        "P1M" -> "/ month"
        "P3M" -> "/ 3 months"
        "P6M" -> "/ 6 months"
        "P1Y" -> "/ year"
        else -> this
    }

    // ---------------------------------------------------------------- purchases

    override suspend fun queryPurchases(): List<PurchaseRecord> =
        queryPurchasesResult().records

    override suspend fun queryPurchasesResult(): PurchaseQueryResult {
        if (ensureConnected() != BillingConnection.CONNECTED) {
            return PurchaseQueryResult(emptyList(), succeeded = false)
        }
        val c = getOrBuildClient()
        val out = mutableListOf<PurchaseRecord>()
        var ok = true
        suspend fun queryType(type: String) {
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            try {
                withTimeout(QUERY_TIMEOUT_MS) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        c.queryPurchasesAsync(params) { result, purchases ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                out += purchases.map { it.toRecord() }
                            } else {
                                Log.w(TAG, "queryPurchases($type) -> ${result.responseCode}")
                                ok = false
                            }
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "queryPurchases($type) timed out", e)
                ok = false
            } catch (e: Exception) {
                Log.w(TAG, "queryPurchases($type) threw", e)
                ok = false
            }
        }
        queryType(BillingClient.ProductType.SUBS)
        queryType(BillingClient.ProductType.INAPP)
        return PurchaseQueryResult(out, succeeded = ok)
    }

    private fun Purchase.toRecord(): PurchaseRecord = PurchaseRecord(
        purchaseToken = purchaseToken,
        orderId = orderId,
        productIds = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> BillingPurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> BillingPurchaseState.PENDING
            else -> BillingPurchaseState.UNSPECIFIED
        },
        acknowledged = isAcknowledged,
        autoRenewing = isAutoRenewing
    )

    override suspend fun launchPurchase(activity: Activity, offer: BillingOffer): PurchaseOutcome {
        if (ensureConnected() != BillingConnection.CONNECTED) return PurchaseOutcome.FAILED
        val details = lock.withLock { detailsCache[offer.productId] }
            ?: return PurchaseOutcome.FAILED
        val paramsList = if (offer.isSubscription) {
            if (offer.offerToken.isBlank()) return PurchaseOutcome.FAILED
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offer.offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()
        val deferred = CompletableDeferred<PurchaseOutcome>()
        lock.withLock {
            // Only one sheet at a time; a second tap while the sheet is open
            // is ignored rather than launching a duplicate flow.
            if (pendingOutcome != null) return PurchaseOutcome.FAILED
            pendingOutcome = deferred
        }
        val launchResult = getOrBuildClient().launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            lock.withLock { pendingOutcome = null }
            // USER_CANCELED here means the sheet never opened; map the rest.
            return if (launchResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                PurchaseOutcome.CANCELLED
            } else {
                Log.w(TAG, "launchBillingFlow -> ${launchResult.responseCode}")
                PurchaseOutcome.FAILED
            }
        }
        return try {
            withTimeout(LAUNCH_TIMEOUT_MS) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Purchase sheet outcome timed out", e)
            lock.withLock { if (pendingOutcome === deferred) pendingOutcome = null }
            PurchaseOutcome.FAILED
        } catch (e: CancellationException) {
            // Caller went away (e.g. paywall torn down): release the gate so a
            // later purchase can proceed instead of failing forever.
            withContext(NonCancellable) {
                lock.withLock { if (pendingOutcome === deferred) pendingOutcome = null }
            }
            throw e
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val outcome = when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val states = purchases.orEmpty().map { it.purchaseState }.toSet()
                when {
                    Purchase.PurchaseState.PURCHASED in states -> PurchaseOutcome.PURCHASED
                    Purchase.PurchaseState.PENDING in states -> PurchaseOutcome.PENDING
                    else -> PurchaseOutcome.FAILED
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseOutcome.CANCELLED
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseOutcome.PURCHASED
            else -> {
                Log.w(TAG, "onPurchasesUpdated -> ${result.responseCode}")
                PurchaseOutcome.FAILED
            }
        }
        // complete() ignores duplicates — Play may redeliver callbacks
        // across rotation or process events; never crash or double-handle here.
        pendingOutcome?.complete(outcome)
    }

    override suspend fun acknowledge(record: PurchaseRecord): Boolean {
        if (ensureConnected() != BillingConnection.CONNECTED) return false
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(record.purchaseToken)
            .build()
        return try {
            withTimeout(QUERY_TIMEOUT_MS) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    getOrBuildClient().acknowledgePurchase(params) { result ->
                        if (cont.isActive) cont.resume(
                            result.responseCode == BillingClient.BillingResponseCode.OK
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "acknowledge threw", e)
            false
        }
    }
}
