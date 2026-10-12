package com.vayu.weather.data.billing

/**
 * Pure entitlement decision: verified Play purchase records in, entitlement out.
 *
 * Rules (no exceptions):
 * - Only [BillingPurchaseState.PURCHASED] records for a *configured* product id
 *   grant Premium. PENDING, UNSPECIFIED, unknown products, or an empty list
 *   yield [PremiumEntitlement.NONE].
 * - Acknowledgement is tracked separately by [BillingManager] (Play requires
 *   acknowledgement within 3 days or the purchase is refunded); the resolver
 *   answers only "is there a currently valid purchase".
 * - Expiry/cancellation/revocation surface here automatically: Play stops
 *   returning the purchase from `queryPurchasesAsync`, so the next refresh
 *   resolves to NONE and the manager revokes the entitlement.
 */
object BillingEntitlementResolver {

    fun resolve(
        records: List<PurchaseRecord>,
        configuredIds: Set<String>
    ): PremiumEntitlement {
        if (configuredIds.isEmpty()) return PremiumEntitlement.NONE
        val granting = records.firstOrNull { record ->
            record.state == BillingPurchaseState.PURCHASED &&
                record.productIds.any { it in configuredIds }
        } ?: return PremiumEntitlement.NONE
        val productId = granting.productIds.firstOrNull { it in configuredIds }
        return PremiumEntitlement(
            isPremium = true,
            source = EntitlementSource.GOOGLE_PLAY,
            productId = productId
        )
    }

    /** True when Play still requires acknowledgement for this record. */
    fun needsAcknowledgement(record: PurchaseRecord): Boolean =
        record.state == BillingPurchaseState.PURCHASED && !record.acknowledged
}
