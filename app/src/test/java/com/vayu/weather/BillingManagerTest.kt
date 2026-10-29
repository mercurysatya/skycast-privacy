package com.vayu.weather

import android.app.Activity
import com.vayu.weather.data.billing.BillingConnection
import com.vayu.weather.data.billing.BillingGateway
import com.vayu.weather.data.billing.BillingManager
import com.vayu.weather.data.billing.BillingOffer
import com.vayu.weather.data.billing.BillingProductConfig
import com.vayu.weather.data.billing.BillingPurchaseState
import com.vayu.weather.data.billing.EntitlementSource
import com.vayu.weather.data.billing.PurchaseOutcome
import com.vayu.weather.data.billing.PurchaseQueryResult
import com.vayu.weather.data.billing.PurchaseRecord
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Genuine-billing regression tests. A fake [BillingGateway] drives every
 * purchase/restore scenario so the entitlement rules are proven on plain
 * JUnit without the Play Store:
 *
 * - Premium is granted ONLY from a verified PURCHASED record for a
 *   configured product — never from a button press, a PENDING payment, a
 *   cancelled/failed sheet, or an unconfigured product list.
 * - Acknowledgement happens exactly once per token and is retried on failure.
 * - A failed Play query never revokes an existing entitlement; only a
 *   successful empty query does (expiry/cancellation path).
 */
class BillingManagerTest {

    private class FakeProducts(
        override val subscriptionIds: List<String> = listOf("skycast_premium_annual"),
        override val oneTimeIds: List<String> = emptyList(),
        override val preferredBasePlans: Map<String, String> = emptyMap()
    ) : BillingProductConfig

    private class FakeGateway : BillingGateway {
        var connection: BillingConnection = BillingConnection.CONNECTED
        var offers: List<BillingOffer> = emptyList()
        var queryResult: PurchaseQueryResult = PurchaseQueryResult(emptyList(), true)
        val scriptedOutcomes = ArrayDeque<PurchaseOutcome>()
        val ackResults = ArrayDeque<Boolean>()
        var launchCalls = 0
        var ackCalls = 0
        var hangInLaunch = false
        val ackedTokens = mutableListOf<String>()
        var lastOffer: BillingOffer? = null

        override suspend fun ensureConnected() = connection
        override suspend fun queryOffers() = offers
        override suspend fun queryPurchases() = queryResult.records
        override suspend fun queryPurchasesResult() = queryResult
        override suspend fun launchPurchase(activity: Activity, offer: BillingOffer): PurchaseOutcome {
            launchCalls++
            lastOffer = offer
            if (hangInLaunch) awaitCancellation()
            return scriptedOutcomes.removeFirstOrNull() ?: PurchaseOutcome.FAILED
        }

        override suspend fun acknowledge(record: PurchaseRecord): Boolean {
            ackCalls++
            ackedTokens += record.purchaseToken
            return ackResults.removeFirstOrNull() ?: true
        }
    }

    private val activity: Activity = mock(Activity::class.java)

    private fun purchased(
        token: String = "token-1",
        product: String = "skycast_premium_annual",
        acknowledged: Boolean = false
    ) = PurchaseRecord(
        purchaseToken = token,
        orderId = "order-1",
        productIds = listOf(product),
        state = BillingPurchaseState.PURCHASED,
        acknowledged = acknowledged,
        autoRenewing = true
    )

    private fun pending(token: String = "token-p") = PurchaseRecord(
        purchaseToken = token,
        orderId = null,
        productIds = listOf("skycast_premium_annual"),
        state = BillingPurchaseState.PENDING,
        acknowledged = false,
        autoRenewing = false
    )

    private fun offer() = BillingOffer(
        productId = "skycast_premium_annual",
        basePlanId = "annual-base",
        offerToken = "offer-token-1",
        title = "Annual",
        displayPrice = "₹299.00",
        displayPeriod = "/ year",
        isSubscription = true
    )

    // ------------------------------------------------------------ connection

    @Test
    fun `disconnected service keeps previous entitlement`() = runTest {
        val gateway = FakeGateway()
        val manager = BillingManager(gateway, FakeProducts())
        gateway.queryResult = PurchaseQueryResult(listOf(purchased()), true)
        manager.refresh()
        assertTrue(manager.state.value.entitlement.isPremium)

        gateway.connection = BillingConnection.DISCONNECTED
        manager.refresh()
        assertEquals(BillingConnection.DISCONNECTED, manager.state.value.connection)
        assertTrue("A transport failure must not revoke Premium", manager.state.value.entitlement.isPremium)
    }

    @Test
    fun `reset entitlement prevents a failed refresh from restoring premium`() = runTest {
        val gateway = FakeGateway()
        val manager = BillingManager(gateway, FakeProducts())
        gateway.queryResult = PurchaseQueryResult(listOf(purchased(acknowledged = true)), true)
        manager.refresh()
        assertTrue(manager.state.value.entitlement.isPremium)

        manager.resetEntitlement()
        assertFalse(
            "A local-data wipe must forget the cached entitlement",
            manager.state.value.entitlement.isPremium
        )

        gateway.connection = BillingConnection.DISCONNECTED
        manager.refresh()
        assertFalse(
            "A failed query must not resurrect the pre-deletion entitlement",
            manager.state.value.entitlement.isPremium
        )

        gateway.connection = BillingConnection.CONNECTED
        manager.refresh()
        assertTrue(
            "Only a successful Play query may grant Premium again",
            manager.state.value.entitlement.isPremium
        )
    }

    // -------------------------------------------------------------- products

    @Test
    fun `unconfigured products disable purchase initiation`() = runTest {
        val gateway = FakeGateway()
        val empty = object : BillingProductConfig {
            override val subscriptionIds = emptyList<String>()
            override val oneTimeIds = emptyList<String>()
            override val preferredBasePlans = emptyMap<String, String>()
        }
        val manager = BillingManager(gateway, empty)
        manager.refresh()
        assertFalse(manager.state.value.productsConfigured)

        manager.launchPurchase(activity, offer())
        assertEquals("Play must never be contacted without products", 0, gateway.launchCalls)
        assertFalse(manager.state.value.entitlement.isPremium)
        assertNotNull(manager.state.value.message)
        assertTrue(manager.state.value.message!!.isError)
    }

    @Test
    fun `valid offer retrieval populates ui state`() = runTest {
        val gateway = FakeGateway()
        gateway.offers = listOf(offer())
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        assertTrue(manager.state.value.productsConfigured)
        assertEquals(1, manager.state.value.offers.size)
        assertEquals("₹299.00", manager.state.value.offers[0].displayPrice)
        assertEquals("offer-token-1", manager.state.value.offers[0].offerToken)
    }

    // -------------------------------------------------------------- outcomes

    @Test
    fun `cancelled sheet grants nothing`() = runTest {
        val gateway = FakeGateway()
        gateway.offers = listOf(offer())
        gateway.scriptedOutcomes += PurchaseOutcome.CANCELLED
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        manager.launchPurchase(activity, offer())
        assertFalse(manager.state.value.entitlement.isPremium)
        assertFalse(manager.state.value.purchaseInProgress)
        assertNotNull(manager.state.value.message)
    }

    @Test
    fun `failed sheet grants nothing but allows retry`() = runTest {
        val gateway = FakeGateway()
        gateway.offers = listOf(offer())
        gateway.scriptedOutcomes += PurchaseOutcome.FAILED
        gateway.scriptedOutcomes += PurchaseOutcome.PURCHASED
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()

        manager.launchPurchase(activity, offer())
        assertFalse(manager.state.value.entitlement.isPremium)
        assertTrue(manager.state.value.message!!.isError)

        gateway.queryResult = PurchaseQueryResult(listOf(purchased()), true)
        manager.launchPurchase(activity, offer())
        assertTrue("Retry after failure must be able to succeed", manager.state.value.entitlement.isPremium)
    }

    @Test
    fun `pending payment grants nothing`() = runTest {
        val gateway = FakeGateway()
        gateway.offers = listOf(offer())
        gateway.scriptedOutcomes += PurchaseOutcome.PENDING
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        manager.launchPurchase(activity, offer())
        assertFalse("PENDING must never unlock Premium", manager.state.value.entitlement.isPremium)
        assertNotNull(manager.state.value.message)
    }

    // ------------------------------------------------------- ack + grant flow

    @Test
    fun `successful purchase acknowledges once and grants`() = runTest {
        val gateway = FakeGateway()
        gateway.offers = listOf(offer())
        gateway.scriptedOutcomes += PurchaseOutcome.PURCHASED
        gateway.queryResult = PurchaseQueryResult(listOf(purchased()), true)
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        manager.launchPurchase(activity, offer())

        assertEquals(1, gateway.ackCalls)
        assertEquals(listOf("token-1"), gateway.ackedTokens)
        val entitlement = manager.state.value.entitlement
        assertTrue(entitlement.isPremium)
        assertEquals(EntitlementSource.GOOGLE_PLAY, entitlement.source)
        assertEquals("skycast_premium_annual", entitlement.productId)
    }

    @Test
    fun `acknowledgement failure retries on next refresh`() = runTest {
        val gateway = FakeGateway()
        gateway.ackResults += false
        gateway.ackResults += true
        gateway.queryResult = PurchaseQueryResult(listOf(purchased()), true)
        val manager = BillingManager(gateway, FakeProducts())

        manager.refresh()
        assertEquals(1, gateway.ackCalls)

        manager.refresh()
        assertEquals("Failed ack must be retried, not dropped", 2, gateway.ackCalls)
        assertTrue(manager.state.value.entitlement.isPremium)
    }

    @Test
    fun `duplicate query results do not duplicate acknowledgement`() = runTest {
        val gateway = FakeGateway()
        gateway.queryResult = PurchaseQueryResult(listOf(purchased()), true)
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        manager.refresh()
        manager.refresh()
        assertEquals("Same token must be acknowledged exactly once", 1, gateway.ackCalls)
        assertTrue(manager.state.value.entitlement.isPremium)
    }

    @Test
    fun `already acknowledged purchases are never re-acknowledged`() = runTest {
        val gateway = FakeGateway()
        gateway.queryResult = PurchaseQueryResult(listOf(purchased(acknowledged = true)), true)
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        assertEquals(0, gateway.ackCalls)
        assertTrue(manager.state.value.entitlement.isPremium)
    }

    // ------------------------------------------------------------- restore

    @Test
    fun `restore after restart grants eligible purchase`() = runTest {
        val gateway = FakeGateway()
        gateway.queryResult = PurchaseQueryResult(listOf(purchased(acknowledged = true)), true)
        // Fresh manager = fresh process after reinstall.
        val manager = BillingManager(gateway, FakeProducts())
        assertFalse(manager.state.value.entitlement.isPremium)
        manager.restorePurchases()
        assertTrue(manager.state.value.entitlement.isPremium)
        assertNotNull(manager.state.value.message)
        assertFalse(manager.state.value.message!!.isError)
    }

    @Test
    fun `restore with nothing owned reports honestly`() = runTest {
        val gateway = FakeGateway()
        val manager = BillingManager(gateway, FakeProducts())
        manager.restorePurchases()
        assertFalse(manager.state.value.entitlement.isPremium)
        assertTrue(manager.state.value.message!!.isError)
    }

    @Test
    fun `missing subscription revokes entitlement`() = runTest {
        val gateway = FakeGateway()
        gateway.queryResult = PurchaseQueryResult(listOf(purchased(acknowledged = true)), true)
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        assertTrue(manager.state.value.entitlement.isPremium)

        // Subscription expired/cancelled: Play returns a successful empty list.
        gateway.queryResult = PurchaseQueryResult(emptyList(), true)
        manager.refresh()
        assertFalse("A successful empty query must revoke Premium", manager.state.value.entitlement.isPremium)
        assertNull("Revocation is silent state sync, not an error banner", manager.state.value.message)
    }

    @Test
    fun `cancelled launch resets progress and allows retry`() = runTest {
        val gateway = FakeGateway()
        gateway.offers = listOf(offer())
        gateway.hangInLaunch = true
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()

        val job = launch { manager.launchPurchase(activity, offer()) }
        testScheduler.advanceUntilIdle()
        assertTrue(manager.state.value.purchaseInProgress)
        job.cancel()
        job.join()
        testScheduler.advanceUntilIdle()
        assertFalse("Cancellation must reset the progress flag", manager.state.value.purchaseInProgress)

        // Retry must reach Play again instead of failing on a stuck gate.
        gateway.hangInLaunch = false
        gateway.scriptedOutcomes += PurchaseOutcome.PURCHASED
        gateway.queryResult = PurchaseQueryResult(listOf(purchased()), true)
        manager.launchPurchase(activity, offer())
        assertEquals(2, gateway.launchCalls)
        assertTrue(manager.state.value.entitlement.isPremium)
    }

    @Test
    fun `unknown product never grants`() = runTest {
        val gateway = FakeGateway()
        gateway.queryResult = PurchaseQueryResult(
            listOf(purchased(product = "some_other_app_product")), true
        )
        val manager = BillingManager(gateway, FakeProducts())
        manager.refresh()
        assertFalse(manager.state.value.entitlement.isPremium)
    }
}
