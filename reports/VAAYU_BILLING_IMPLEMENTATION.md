# VAAYU Billing Implementation — genuine Google Play Billing

Date: 2026-10-12. Library: `com.android.billingclient:billing:9.1.0` (verified resolvable;
PBL 9.x API confirmed against the AAR: `QueryProductDetailsResult` wrapper, non-null
offer tokens/base-plan ids). No product IDs are configured — none existed anywhere in the
project, and none are invented here.

## 1. Architecture

```
PremiumScreen (Compose, visual design preserved)
  │ offers / messages / onPurchase(BillingOffer) / onRestore
  ▼
SettingsViewModel (billingState, launch/restore/refresh, entitlement propagation)
  │                              ▲ entitlement changes
  ▼                              │
BillingManager (@Singleton) ─────┘
  │ StateFlow<BillingUiState>: connection, offers, entitlement, progress, message
  ▼
BillingGateway (interface — the only mockable seam)
  ├── PlayBillingGateway (the ONLY file importing com.android.billingclient)
  └── FakeBillingGateway (unit tests)
BillingProducts → BillingProductConfig (empty until owner configures; bound in BillingModule)
SettingsManager.setPremiumFromBilling() → DataStore cache + PremiumManager.sync()
  → SettingsState.isPremium → dashboard / Settings / AdManager.adsDisabled()
```

- Entitlement source is **only** the verified Play purchase query. The former local
  `setPremium(Boolean)` setter is deleted from `SettingsManager` and `SettingsViewModel`;
  the Upgrade button now opens the Play sheet and cannot grant anything by itself.
- DataStore `premium_entitlement` is a display cache. It is corrected on every app start
  (`SettingsViewModel.init → billingManager.refresh()`) and every resume
  (`VayuApp ON_RESUME → refreshBilling()`), so expiry/cancellation/revocation take effect
  on the next successful query and reinstall restore works for the same Play account.
- Revocation happens **only** on a *successful* Play query returning no eligible purchase
  (`PurchaseQueryResult.succeeded`); transport failures keep the previous state.

## 2. Purchase lifecycle (implemented)

1. Connect with retry (3 attempts, exponential backoff); `BILLING_UNAVAILABLE` maps to UNAVAILABLE.
2. `queryProductDetailsAsync` for configured SUBS + INAPP ids; preferred base-plan offer
   selected when configured, else first offer; Play-localized price/period displayed.
   Unfetched products are logged with status codes to diagnose console misconfiguration.
3. `launchBillingFlow` with product params + offer token; one sheet at a time (the gate is
   released on completion, timeout **and** coroutine cancellation — F13, so tearing down the
   paywall mid-sheet can never wedge later purchases); outcome bridged from
   `PurchasesUpdatedListener` via guarded `CompletableDeferred` (duplicate
   Play callbacks ignored, never crash/double-handle).
4. Outcomes: PURCHASED → mandatory re-query + acknowledge + resolve (grant comes from the
   query, never the callback); PENDING → honest "pending, no Premium yet"; CANCELLED →
   message, retry allowed; FAILED → message, retry allowed; ITEM_ALREADY_OWNED → verify
   via query. NOT_CONFIGURED → disabled with explanation, Play never contacted.
5. Acknowledgement immediately after verification, once per token per process; already-
   acknowledged records never re-acked; failed acks retry on the next refresh (inside
   Play's deadline, not repeated after success).

## 3. Configured product IDs — NONE (REQUIRES OWNER ACTION)

`BillingProducts.subscriptionIds / oneTimeIds` are empty. The paywall therefore shows
"Premium purchases aren't set up yet" with the Upgrade button disabled. No fake prices
are displayed (the old hardcoded ₹49/₹299/₹799 UI plans are removed).
Verified on device 2026-10-12 (Phase-3 smoke test, `artifacts/screen_58_p3_paywall.png`):
honest disabled state, no prices, no false success on restore.

### Play Console setup checklist (owner)
1. Play Console → the `com.vayu.weather` app → Monetize → Products → Subscriptions:
   create the subscription(s) (e.g. monthly + annual with base plans), activate them.
2. For a lifetime option: Products → In-app products → create, activate.
3. Copy the exact product IDs (and base-plan IDs, if several offers per subscription).
4. Paste them into `BillingProducts` — the exact fields (currently empty lists):
   - `subscriptionIds: List<String>` — the subscription **product IDs** from Play Console
     (shown as "Product ID" on each subscription, e.g. `premium_monthly`);
   - `oneTimeIds: List<String>` — in-app **product IDs** for any lifetime option;
   - `preferredBasePlans: Map<String, String>` — optional `subscriptionId → basePlanId`
     pairs (the "Base plan ID" shown under each subscription's pricing template) when a
     subscription has more than one base plan (e.g. monthly + yearly under one product);
   - optional `BillingProductConfig` offer/period preferences — leave unset unless a
     specific offer must be preferred.
   Keep these lists empty until the products are **Active** in Play Console; an inactive
   or draft product makes Play return `unfetchedProductList` (logged with status codes).
5. Add a Google group / license-tester list; upload a build to the **internal testing**
   track (Play-signed) — purchases only work on Play-signed builds, never on local APKs.
6. Verify with a license tester: purchase, cancel, pending (slow test card), restore on
   reinstall, expiry — then record results here before any rollout.

## 4. Limitations (honest)

- **Client-only verification.** No secure backend exists in this project. Firebase
  Analytics/Crashlytics/Remote Config/App Check are present but are NOT purchase-
  verification backends and are not used as such. Client-side `queryPurchasesAsync`
  is the officially supported flow without a backend and is sufficient for entitlement
  gating, but a rooted device could theoretically misreport; recommended follow-up:
  add a backend holding the Play Developer API service-account key and verify
  `purchaseToken` server-side (`purchases.subscriptionsv2.get`) before granting.
- **No end-to-end proof yet.** 22 mock unit tests pass (below); the genuine sheet,
  acknowledgement round-trip, and restore can only be verified with §3 configured
  plus license testers on the internal track. Not claimed until then.
- Transient cache: on cold start the DataStore cache seeds the UI until the first
  Play query returns (usually <2 s); a stale `true` is corrected immediately after.

## 5. Test evidence (mock, not end-to-end)

`BillingManagerTest` (15) + `BillingEntitlementResolverTest` (7), all PASS:
disconnected-keeps-entitlement · unconfigured-disables-purchase · offer retrieval ·
cancel/fail/pending grant nothing · fail-then-retry succeeds · success acks once and
grants (source=GOOGLE_PLAY) · ack-failure retries · duplicate queries ack once ·
already-acked never re-acked · restore-after-restart · honest empty-restore message ·
expiry revokes (silent) · unknown product denied · **cancelled launch resets progress
and allows retry (Phase-3 regression test for F13)** · resolver
PENDING/UNSPECIFIED/empty denials. Full suite (338/338) + builds: see release
checklist §1.

Phase-3 source audit conclusions (2026-10-12): a button press, a pending purchase or an
unsuccessful/failed query **cannot** independently grant Premium — the only grant path is
a successful `queryPurchasesAsync` returning an eligible purchase, and revocation happens
only on a *successful* empty query, so transient connection/query failures never wrongly
revoke a verified entitlement.

## 6. Files

- NEW `data/billing/BillingModels.kt`, `BillingProducts.kt` (`BillingProductConfig`),
  `BillingEntitlementResolver.kt`, `BillingGateway.kt`, `PlayBillingGateway.kt`,
  `BillingManager.kt`; NEW `di/BillingModule.kt`; NEW tests `BillingManagerTest.kt`,
  `BillingEntitlementResolverTest.kt`.
- EDIT `data/local/SettingsManager.kt` (`setPremiumFromBilling`, stub setter deleted),
  `presentation/settings/SettingsViewModel.kt` (billing delegation, stub deleted),
  `presentation/premium/PremiumScreen.kt` (real offers/disabled-state/restore),
  `presentation/premium/PremiumManager.kt` (hardcoded plans removed, docs),
  `presentation/VayuApp.kt` (paywall wiring, resume re-verify),
  `gradle/libs.versions.toml` + `app/build.gradle.kts` (billing 9.1.0).
