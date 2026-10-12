package com.vayu.weather.data.billing

import android.app.Activity

/**
 * Seam between [BillingManager] and Google Play.
 *
 * Production code uses [PlayBillingGateway]; unit tests inject a fake, so every
 * purchase/restore scenario in `reports/VAAYU_BILLING_IMPLEMENTATION.md` is
 * covered on plain JUnit without the Play Store. No method here may grant an
 * entitlement — they only transport Play data and outcomes.
 */
interface BillingGateway {

    /** Connect (or reconnect) to the Play Billing service. */
    suspend fun ensureConnected(): BillingConnection

    /**
     * Offers for the configured products, with Play-localized pricing and the
     * offer token required to launch each one. Empty when nothing is
     * configured or Play returns no details. Never throws — failures are
     * reported through [BillingManager] state.
     */
    suspend fun queryOffers(): List<BillingOffer>

    /** Currently owned purchases (active subscriptions + unconsumed one-times). */
    suspend fun queryPurchases(): List<PurchaseRecord>

    /**
     * Same as [queryPurchases] but reports whether Play actually answered.
     * The manager must only revoke an entitlement on a *successful* empty
     * result — never on a network/service failure.
     */
    suspend fun queryPurchasesResult(): PurchaseQueryResult

    /**
     * Launches the genuine Play purchase sheet for [offer].
     * Returns the terminal outcome; a [PurchaseOutcome.PURCHASED] result still
     * requires state verification + acknowledgement by the caller.
     */
    suspend fun launchPurchase(activity: Activity, offer: BillingOffer): PurchaseOutcome

    /**
     * Acknowledges a PURCHASED record. Returns true if Play accepted it or it
     * was already acknowledged. Must be called within Play's deadline
     * (3 days) or the purchase is refunded — [BillingManager] acknowledges
     * immediately, never repeatedly for the same token.
     */
    suspend fun acknowledge(record: PurchaseRecord): Boolean
}
