package com.vayu.weather.data.billing

/**
 * Play-agnostic billing models.
 *
 * These types deliberately avoid importing `com.android.billingclient` so the
 * entitlement logic ([BillingEntitlementResolver]) and orchestration
 * ([BillingManager]) stay unit-testable on plain JUnit. The only class that
 * touches the Play Billing Library is [PlayBillingGateway], which translates
 * PBL types into these models at the boundary.
 */

/** Purchase lifecycle state, mirrored from Play's Purchase.PurchaseState. */
enum class BillingPurchaseState {
    PURCHASED,
    PENDING,
    UNSPECIFIED
}

/** Where the current Premium entitlement comes from. */
enum class EntitlementSource {
    /** No verified purchase. Ads are shown. */
    NONE,

    /** Verified by a current Google Play purchase query. Ads are suppressed. */
    GOOGLE_PLAY
}

/**
 * The single entitlement decision consumed by the UI and [com.vayu.weather.presentation.ads.AdManager].
 * `isPremium == true` is only ever produced from a verified Play purchase
 * (see [BillingEntitlementResolver]); the DataStore flag is a display cache.
 */
data class PremiumEntitlement(
    val isPremium: Boolean,
    val source: EntitlementSource,
    /** Play product id that granted the entitlement, if any. */
    val productId: String? = null
) {
    companion object {
        val NONE = PremiumEntitlement(isPremium = false, source = EntitlementSource.NONE)
    }
}

/**
 * One purchasable offer, translated from Play `ProductDetails`.
 * `displayPrice`/`displayPeriod` are the localized strings returned by
 * Google Play — never invented in the app.
 */
data class BillingOffer(
    /** Play product id (e.g. a subscription id configured in Play Console). */
    val productId: String,
    /** Subscription base-plan id, null for one-time products. */
    val basePlanId: String?,
    /** Opaque token Play requires to launch this exact offer. */
    val offerToken: String,
    val title: String,
    /** Localized price from Play, e.g. "₹299.00". */
    val displayPrice: String,
    /** Localized period derived from Play's ISO-8601 billing period. */
    val displayPeriod: String,
    /** True for subscriptions (auto-renewing), false for one-time purchases. */
    val isSubscription: Boolean
)

/** One owned purchase, translated from Play `Purchase`. */
data class PurchaseRecord(
    val purchaseToken: String,
    val orderId: String?,
    val productIds: List<String>,
    val state: BillingPurchaseState,
    val acknowledged: Boolean,
    val autoRenewing: Boolean
)

/** Connection state of the Play Billing service. */
enum class BillingConnection {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    /** Play Billing unavailable on this device/account (e.g. no Play Store). */
    UNAVAILABLE
}

/** One purchase query: records plus whether Play actually answered. */
data class PurchaseQueryResult(
    val records: List<PurchaseRecord>,
    val succeeded: Boolean
)

/** Terminal outcome of one purchase-sheet attempt. */
enum class PurchaseOutcome {
    /** Play reported a purchase; caller must still verify state + acknowledge. */
    PURCHASED,

    /** Payment is pending; no entitlement may be granted. */
    PENDING,

    /** User dismissed the Play sheet. */
    CANCELLED,

    /** Play returned an error (network, service, item unavailable, …). */
    FAILED,

    /** Tried to buy but no product is configured — never reaches Play. */
    NOT_CONFIGURED
}
