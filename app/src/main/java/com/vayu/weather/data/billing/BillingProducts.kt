package com.vayu.weather.data.billing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play product configuration.
 *
 * No product ids are configured: none were found anywhere in the project, and
 * inventing ids would produce a paywall that can never complete a purchase.
 * The owner must create the products in Play Console (see
 * `reports/VAAYU_BILLING_IMPLEMENTATION.md` § Play Console setup) and list
 * their exact ids here. Until then [isConfigured] is false and
 * [BillingManager] refuses to launch any purchase with an honest
 * "not set up yet" state instead of a fake success.
 *
 * Product ids are public identifiers (shown in Play order emails/receipts),
 * not secrets — listing them here is safe.
 */
interface BillingProductConfig {
    /** Play subscription product ids (e.g. "skycast_premium_monthly"). */
    val subscriptionIds: List<String>

    /** Play one-time product ids (e.g. "skycast_premium_lifetime"). */
    val oneTimeIds: List<String>

    /**
     * Preferred subscription base-plan id per subscription product, used to
     * pick the offer token when a subscription has several offers.
     * When absent the first returned offer is used.
     */
    val preferredBasePlans: Map<String, String>

    val isConfigured: Boolean
        get() = subscriptionIds.isNotEmpty() || oneTimeIds.isNotEmpty()

    fun allIds(): Set<String> = (subscriptionIds + oneTimeIds).toSet()
}

@Singleton
class BillingProducts @Inject constructor() : BillingProductConfig {
    override val subscriptionIds: List<String> = emptyList()
    override val oneTimeIds: List<String> = emptyList()
    override val preferredBasePlans: Map<String, String> = emptyMap()
}
