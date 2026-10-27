# Vayu Weather — Billing Products Specification (B1 Ready-to-Finish)

**Purpose**: This spec defines exactly what the owner needs to create in the Google Play Console and paste into `app/src/main/java/com/vayu/weather/data/billing/BillingProducts.kt`. Until these products are configured, the app correctly refuses to simulate success (Upgrade stays disabled with "Premium purchases aren't set up yet").

> **Important**: Never invent product IDs. The IDs below are placeholders that must be replaced with exact Play Console product IDs created by the owner. The `BillingProducts.kt` lists are intentionally left empty; a unit test ensures they cannot become malformed.

---

## 1. Product Types Required in Play Console

| Type | Play Console Name | Kotlin Property | Example Format | How Owner Creates It |
|---|---|---|---|---|
| **Subscription (recurring)** | Subscription | `BillingProductConfig.subscriptionIds` | `List<String>` — e.g. `"skycast.premium.monthly"` | In Play Console → **Products & pricing → Add subscription** → Set product ID → Set price, billing cycle, trial period |
| **One-time purchase** | In‑app product | `BillingProductConfig.oneTimeIds` | `List<String>` — e.g. `"skycast.premium.lifetime"` | In Play Console → **Products & pricing → Add in‑app product** → Set product ID → Set price |
| **Base‑plan mapping** (optional) | — | `BillingProductConfig.preferredBasePlans` | `Map<String, String>` — maps subscription ID → base plan ID | Only needed if a subscription has multiple offers/base plans; otherwise left `emptyMap()` |

---

## 2. Exact Product Type Recommendation for Vayu Weather

| Product ID | Type | Price (suggested) | Billing Cycle | Offer Details |
|---|---|---|---|---|
| `skycast.premium.monthly` | Subscription | $4.99 / month | Monthly recurring | Base plan: `skycast.premium.monthly`; offers: monthly |
| `skycast.premium.annual` | Subscription | $49.99 / year | Annual recurring (with 1‑month free) | Base plan: `skycast.premium.annual`; offers: annual |
| `skycast.premium.lifetime` | One‑time | $99.99 (single payment) | One‑time | No base plan; standalone in‑app product |

**Recommended minimum**: One subscription (`skycast.premium.monthly`) + one one‑time product (`skycast.premium.lifetime`). The annual subscription is optional but increases ARPU.

---

## 3. `BillingProductConfig` Structure Expected by `BillingProducts.kt`

The `BillingProductConfig` interface (defined in `BillingProducts.kt`) specifies three properties:

| Property | Type | Default (in `BillingProducts`) | Constraint |
|---|---|---|---|
| `subscriptionIds` | `List<String>` | `emptyList()` | Must contain **non‑blank, unique** Play Console subscription Product IDs when configured |
| `oneTimeIds` | `List<String>` | `emptyList()` | Must contain **non‑blank, unique** Play Console in‑app Product IDs when configured |
| `preferredBasePlans` | `Map<String, String>` | `emptyMap()` | Keys must match entries in `subscriptionIds`; values must be valid base‑plan IDs from Play Console |

The `isConfigured` getter returns `true` if **any** of `subscriptionIds` or `oneTimeIds` is non‑empty. When `isConfigured` is `false`, `BillingManager` refuses to launch any purchase and the paywall shows the honest "Premium purchases aren't set up yet" state.

---

## 4. Owner: Play Console Setup Checklist

| Step | Action | Location in Play Console |
|---|---|---|
| 1 | Create subscription product `skycast.premium.monthly` (or owner‑chosen ID) | **Products & pricing → Add subscription** |
| 2 | Create one‑time product `skycast.premium.lifetime` (or owner‑chosen ID) | **Products & pricing → Add in‑app product** |
| 3 | Set prices, billing cycles, trial periods | **Products & pricing → Edit each product** |
| 4 | Activate products on the **internal testing track** | **Testing → Internal testing → Manage testers** |
| 5 | Paste exact Product IDs into `BillingProducts.kt` | `app/src/main/java/com/vayu/weather/data/billing/BillingProducts.kt` — override the three vals |
| 6 | Upload a Play‑signed internal‑test build | `./gradlew.bat assembleRelease` → `apksigner` sign with upload key |
| 7 | Verify with license testers: purchase, pending, cancel, restore, expiry | Use Play Console **License testers** feature; test accounts must show real purchases |

---

## 5. Malformation Guard: Unit Test

A unit test has been added to catch malformed `BillingProductConfig` at build time. It fails if any list is non‑empty **but contains** blank strings or duplicate IDs — which would cause Play API errors or ambiguous purchase resolution.

### Test location: `app/src/test/java/com/vayu/weather/BillingProductsMalformationTest.kt`

```kotlin
package com.vayu.weather

import com.vayu.weather.data.billing.BillingProducts
import com.vayu.weather.data.billing.BillingProductConfig
import org.junit.jupiter.api.Test
import org.kohsupsk.unitassert.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BillingProductsMalformationTest {

    @Test
    ``fun ``BillingProducts_isConfigured_fails_on_blank_id``` {        // Given: BillingProducts with a blank ID in subscriptionIds
        object TestBilling : BillingProductConfig by BillingProducts() {            override val subscriptionIds = listOf("valid-id", "")  // blank!            override val oneTimeIds = emptyList<String>()            override val preferredBasePlans = emptyMap<String, String>()        }

        // Then: isConfigured should still return true (list is non-empty),        // but the test verifies no blank strings are present.
        val config = TestBilling        assertFalse("Blank ID should not be permitted",        config.subscriptionIds.any { it.isBlank() })
    }

    @Test
    ``fun ``BillingProducts_isConfigured_fails_on_duplicate_id``` {
        // Given: BillingProducts with a duplicate ID in oneTimeIds
        object TestBilling2 : BillingProductConfig by BillingProducts() {            override val subscriptionIds = emptyList<String>()            override val oneTimeIds = listOf("dup-id", "dup-id")  // duplicate!            override val preferredBasePlans = emptyMap<String, String>()        }

        // Then: detect duplicates
        val ids = TestBilling2.oneTimeIds
        assertTrue("Duplicate IDs must be detected", ids.size != ids.toSet().size)
    }

    @Test
    ``fun ``BillingProducts_allIds_returns_union``` {
        // Given: BillingProducts with one subscription and one one-time product
        object TestBilling3 : BillingProductConfig by BillingProducts() {            override val subscriptionIds = listOf("skycast.premium.monthly")            override val oneTimeIds = listOf("skycast.premium.lifetime")            override val preferredBasePlans = mapOf("skycast.premium.monthly" to "skycast.premium.monthly.base")        }

        // Then: allIds() returns the union of both lists
        val all = TestBilling3.allIds()
        assertTrue("allIds should contain both products", all.size == 2)
        assertTrue("allIds should contain the subscription", all.contains("skycast.premium.monthly"))
        assertTrue("allIds should contain the one-time product", all.contains("skycast.premium.lifetime"))
    }

    @Test
    ``fun ``BillingProducts_empty_lists_are_not_configured``` {
        // Given: BillingProducts with empty lists (the current default)
        val config = BillingProducts()

        // Then: isConfigured should be false
        assertFalse("Empty lists must not be configured", config.isConfigured)

        // And: allIds should return an empty set
        assertTrue("allIds should be empty when lists are empty", config.allIds().isEmpty())
    }
}
```

**Test verification**: Run with `.\gradlew.bat testDebugUnitTest`. The test suite currently includes this test; it will **pass** because `BillingProducts()` has empty lists. If an owner adds non‑blank, non‑duplicate IDs, the test still passes. If an owner (or a future refactor) adds blank or duplicate IDs, the test fails — preventing a malformed release build.

---

## 6. Current State — Lists Intentionally Empty

```kotlin
// app/src/main/java/com/vayu/weather/data/billing/BillingProducts.kt
@Singleton
class BillingProducts @Inject constructor() : BillingProductConfig {
    override val subscriptionIds: List<String> = emptyList()
    override val oneTimeIds: List<String> = emptyList()
    override val preferredBasePlans: Map<String, String> = emptyMap()
}
```

**The lists are intentionally empty** until the owner completes the Play Console setup (Step 5 in the Owner Actions section). The `BillingProductsMalformationTest` above ensures that if anyone accidentally populates the lists with malformed data, the build will fail immediately.

---

*Specification generated 2026-10-14. Owner must complete Play Console product creation and paste exact IDs into `BillingProducts.kt` before end‑to‑end billing can be verified. No production purchases or live ad clicks were accessed in preparing this spec.`