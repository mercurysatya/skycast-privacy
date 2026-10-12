package com.vayu.weather

import com.vayu.weather.data.billing.BillingEntitlementResolver
import com.vayu.weather.data.billing.BillingPurchaseState
import com.vayu.weather.data.billing.EntitlementSource
import com.vayu.weather.data.billing.PurchaseRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure entitlement-decision tests: verified Play records in, entitlement out.
 * PENDING, cancelled, missing, unknown-product and unconfigured-catalog cases
 * must never produce Premium.
 */
class BillingEntitlementResolverTest {

    private val ids = setOf("skycast_premium_annual")

    private fun record(
        state: BillingPurchaseState,
        product: String = "skycast_premium_annual",
        acknowledged: Boolean = true
    ) = PurchaseRecord(
        purchaseToken = "t",
        orderId = "o",
        productIds = listOf(product),
        state = state,
        acknowledged = acknowledged,
        autoRenewing = true
    )

    @Test
    fun `empty records grant nothing`() {
        val e = BillingEntitlementResolver.resolve(emptyList(), ids)
        assertFalse(e.isPremium)
        assertEquals(EntitlementSource.NONE, e.source)
    }

    @Test
    fun `empty catalog grants nothing even for purchased records`() {
        val e = BillingEntitlementResolver.resolve(
            listOf(record(BillingPurchaseState.PURCHASED)), emptySet()
        )
        assertFalse(e.isPremium)
    }

    @Test
    fun `purchased configured product grants with product id`() {
        val e = BillingEntitlementResolver.resolve(
            listOf(record(BillingPurchaseState.PURCHASED)), ids
        )
        assertTrue(e.isPremium)
        assertEquals(EntitlementSource.GOOGLE_PLAY, e.source)
        assertEquals("skycast_premium_annual", e.productId)
    }

    @Test
    fun `pending purchase grants nothing`() {
        val e = BillingEntitlementResolver.resolve(
            listOf(record(BillingPurchaseState.PENDING)), ids
        )
        assertFalse(e.isPremium)
    }

    @Test
    fun `unspecified state grants nothing`() {
        val e = BillingEntitlementResolver.resolve(
            listOf(record(BillingPurchaseState.UNSPECIFIED)), ids
        )
        assertFalse(e.isPremium)
    }

    @Test
    fun `unknown product grants nothing`() {
        val e = BillingEntitlementResolver.resolve(
            listOf(record(BillingPurchaseState.PURCHASED, product = "other")), ids
        )
        assertFalse(e.isPremium)
    }

    @Test
    fun `acknowledgement needed only when unacknowledged and purchased`() {
        assertTrue(
            BillingEntitlementResolver.needsAcknowledgement(
                record(BillingPurchaseState.PURCHASED, acknowledged = false)
            )
        )
        assertFalse(
            BillingEntitlementResolver.needsAcknowledgement(
                record(BillingPurchaseState.PURCHASED, acknowledged = true)
            )
        )
        assertFalse(
            BillingEntitlementResolver.needsAcknowledgement(
                record(BillingPurchaseState.PENDING, acknowledged = false)
            )
        )
    }
}
