package com.vayu.weather.data.billing

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** One-shot user-facing billing message (shown by the paywall, then cleared). */
data class BillingMessage(val text: String, val isError: Boolean)

/**
 * UI-facing billing state. The paywall renders from this; [entitlement] is the
 * only source of truth for Premium (propagated to Settings/ad gating by
 * `SettingsViewModel`, never written from button handlers directly).
 */
data class BillingUiState(
    val connection: BillingConnection = BillingConnection.IDLE,
    val productsConfigured: Boolean = false,
    val offers: List<BillingOffer> = emptyList(),
    val entitlement: PremiumEntitlement = PremiumEntitlement.NONE,
    val purchaseInProgress: Boolean = false,
    val message: BillingMessage? = null
)

/**
 * Genuine Google Play Billing orchestration.
 *
 * Lifecycle contract:
 * - [refresh] connects, loads offers, queries owned purchases, acknowledges
 *   newly completed PURCHASED records exactly once per token, and resolves the
 *   entitlement. Call on app start and on resume so expiry, cancellation and
 *   revocation take effect, and so a reinstall restores an eligible purchase
 *   for the same Google Play account.
 * - Entitlement is revoked only on a *successful* Play query that returns no
 *   eligible purchase — never on a connection/query failure (which keeps the
 *   previous state and reports the connection instead).
 * - [launchPurchase] opens the genuine Play sheet; PENDING/cancelled/failed
 *   outcomes never grant Premium.
 */
@Singleton
class BillingManager @Inject constructor(
    private val gateway: BillingGateway,
    private val products: BillingProductConfig
) {
    private companion object {
        const val TAG = "BillingManager"
    }

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private val lock = Mutex()

    /** Tokens already acknowledged in this process — never ack twice. */
    private val acknowledgedTokens = mutableSetOf<String>()

    fun configuredIds(): Set<String> = products.allIds()

    /** Connect + reload offers + re-verify entitlement. Safe to call often. */
    suspend fun refresh() {
        update { copy(connection = BillingConnection.CONNECTING, message = null) }
        if (!products.isConfigured) {
            update {
                copy(
                    connection = BillingConnection.DISCONNECTED,
                    productsConfigured = false,
                    offers = emptyList(),
                    entitlement = PremiumEntitlement.NONE
                )
            }
            return
        }
        val connection = gateway.ensureConnected()
        if (connection != BillingConnection.CONNECTED) {
            // Keep the previous entitlement on transport failure; revocation
            // requires positive evidence (a successful empty query).
            update { copy(connection = connection) }
            return
        }
        val offers = gateway.queryOffers()
        val result = gateway.queryPurchasesResult()
        if (!result.succeeded) {
            update { copy(connection = BillingConnection.DISCONNECTED, offers = offers) }
            return
        }
        acknowledgeNewPurchases(result.records)
        val entitlement = BillingEntitlementResolver.resolve(result.records, configuredIds())
        update {
            copy(
                connection = BillingConnection.CONNECTED,
                productsConfigured = true,
                offers = preferConfiguredOffers(offers),
                entitlement = entitlement
            )
        }
    }

    /**
     * Opens the Play purchase sheet for [offer]. No-ops with an honest message
     * when products are not configured — this path can never simulate success.
     */
    suspend fun launchPurchase(activity: Activity, offer: BillingOffer) {
        if (!products.isConfigured) {
            update {
                copy(
                    message = BillingMessage(
                        "Premium purchases aren't set up yet. Please check back later.",
                        isError = true
                    )
                )
            }
            return
        }
        update { copy(purchaseInProgress = true, message = null) }
        try {
            when (gateway.launchPurchase(activity, offer)) {
                PurchaseOutcome.PURCHASED -> {
                    // Mandatory verification: re-query, acknowledge, resolve.
                    // Entitlement comes from the query result, not the callback.
                    val result = gateway.queryPurchasesResult()
                    if (!result.succeeded) {
                        update {
                            copy(
                                message = BillingMessage(
                                    "Purchase completed but couldn't be verified. " +
                                        "It will be restored automatically — or tap Restore purchase.",
                                    isError = true
                                )
                            )
                        }
                        return
                    }
                    acknowledgeNewPurchases(result.records)
                    val entitlement =
                        BillingEntitlementResolver.resolve(result.records, configuredIds())
                    update {
                        copy(
                            entitlement = entitlement,
                            message = if (entitlement.isPremium) {
                                BillingMessage("Welcome to SkyCast Premium.", isError = false)
                            } else {
                                BillingMessage(
                                    "No eligible purchase found for this account.",
                                    isError = true
                                )
                            }
                        )
                    }
                }
                PurchaseOutcome.PENDING -> update {
                    copy(
                        message = BillingMessage(
                            "Your payment is pending. Premium unlocks once Google Play confirms it.",
                            isError = false
                        )
                    )
                }
                PurchaseOutcome.CANCELLED -> update {
                    copy(message = BillingMessage("Purchase cancelled.", isError = false))
                }
                PurchaseOutcome.FAILED -> update {
                    copy(
                        message = BillingMessage(
                            "Purchase couldn't start. Check your connection and try again.",
                            isError = true
                        )
                    )
                }
                PurchaseOutcome.NOT_CONFIGURED -> update {
                    copy(
                        message = BillingMessage(
                            "Premium purchases aren't set up yet. Please check back later.",
                            isError = true
                        )
                    )
                }
            }
        } finally {
            update { copy(purchaseInProgress = false) }
        }
    }

    /** Re-queries Play and reports the result — the real "restore purchase". */
    suspend fun restorePurchases() {
        update { copy(message = null) }
        if (!products.isConfigured) {
            update {
                copy(
                    message = BillingMessage(
                        "Premium purchases aren't set up yet. Please check back later.",
                        isError = true
                    )
                )
            }
            return
        }
        refresh()
        val current = _state.value
        if (current.connection != BillingConnection.CONNECTED) {
            update {
                copy(
                    message = BillingMessage(
                        "Couldn't reach Google Play. Check your connection and try again.",
                        isError = true
                    )
                )
            }
            return
        }
        update {
            copy(
                message = if (current.entitlement.isPremium) {
                    BillingMessage(
                        "Premium restored for this Google Play account.",
                        isError = false
                    )
                } else {
                    BillingMessage(
                        "No Premium purchase found for this Google Play account.",
                        isError = true
                    )
                }
            )
        }
    }

    fun clearMessage() {
        update { copy(message = null) }
    }

    // --------------------------------------------------------------- internals

    private fun update(transform: BillingUiState.() -> BillingUiState) {
        _state.value = _state.value.transform()
    }

    /** Acknowledges newly completed purchases once per token, within Play's deadline. */
    private suspend fun acknowledgeNewPurchases(records: List<PurchaseRecord>) {
        val ids = configuredIds()
        records
            .filter { BillingEntitlementResolver.needsAcknowledgement(it) }
            .filter { it.productIds.any { id -> id in ids } }
            .forEach { record ->
                val already = lock.withLock { !acknowledgedTokens.add(record.purchaseToken) }
                if (already) return@forEach
                val ok = try {
                    gateway.acknowledge(record)
                } catch (e: Exception) {
                    Log.w(TAG, "acknowledge threw", e)
                    false
                }
                if (!ok) {
                    // Let a later refresh retry — Play refunds only after its
                    // multi-day deadline, so a retry is safe and required.
                    lock.withLock { acknowledgedTokens.remove(record.purchaseToken) }
                    Log.w(TAG, "acknowledge failed, will retry on next refresh")
                }
            }
    }

    /**
     * Orders offers so the owner's preferred base plan (when configured) comes
     * first per product; otherwise keeps Play's order.
     */
    private fun preferConfiguredOffers(offers: List<BillingOffer>): List<BillingOffer> {
        if (products.preferredBasePlans.isEmpty()) return offers
        return offers.sortedBy { offer ->
            val preferred = products.preferredBasePlans[offer.productId]
            if (preferred != null && offer.basePlanId == preferred) 0 else 1
        }
    }
}
