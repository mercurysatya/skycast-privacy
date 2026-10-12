# VAAYU Audit Report — Vayu Weather (`com.vayu.weather`)

Date: 2026-10-11 (UTC). Device: Samsung SM-M315F, Android 12 (API 31), wireless ADB.
Repo: `C:\Weather_App`, single module `:app`. All changes uncommitted (HEAD `c14d026`).

## 1. Inventory (verified by inspection)

| Item | Value |
|---|---|
| applicationId | `com.vayu.weather` (preserved) |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 24 |
| versionCode / versionName | 5 / 1.5 |
| AGP / Kotlin / Gradle wrapper | 9.4.1 / 2.3.21 / 9.7.1 (Java toolchain 21 via foojay; system Java is 21) |
| UI | Jetpack Compose + Material3, manual state-based nav (`SkyCastNavController`, no NavHost) |
| DI / async | Hilt 2.60.1, Room 2.8.4, DataStore Preferences 1.2.1, WorkManager 2.11.2 |
| Weather provider | Open-Meteo (no API key) + RainViewer tiles; `OpenMeteoApi.kt`, `WeatherDto.kt` |
| Location | Play Services Location 21.4.0 (`DefaultLocationTracker.kt`) |
| Ads / consent | play-services-ads 25.4.0, UMP user-messaging-platform 4.0.0, Firebase BOM 34.18.0 |
| Signing | `key.properties` present (gitignored); release uses R8 (`isMinifyEnabled=true`, `isShrinkResources=true`), debug does not |
| AdMob IDs | Resolved at build time from `gradle.properties` → `local.properties` → Google test defaults (`app/build.gradle.kts:21-29`). Production IDs present in `local.properties` (values never printed). Debug `buildTypes` force Google test IDs. No production IDs hardcoded in source. |
| Lint config | Several checks disabled, `abortOnError=false`; true result read from `app/build/reports/lint-results-debug.xml` directly |

Dead code documented, not deleted: `PushNotificationWorker` / `PushNotificationReceiver`,
`WeatherDashboard.kt` (~105 KB, superseded by `SkyCastHomeScreen.kt`).

## 2. Baseline verification (executed, not assumed)

| Check | Result |
|---|---|
| `assembleDebug` | PASS |
| `testDebugUnitTest` | PASS — 29 suites, 338 tests, 0 failures, 0 errors, 0 skipped (316 baseline + 15 `BillingManagerTest` incl. Phase 3 cancellation-regression test + 7 `BillingEntitlementResolverTest`) |
| `lintDebug` | PASS — 0 issues in `lint-results-debug.xml` (BUILD SUCCESSFUL) |
| `assembleRelease` + `bundleRelease` | PASS 2026-10-12 — BUILD SUCCESSFUL, `signReleaseBundle` executed, fresh AAB ~33.5 MB; `lintVitalRelease` "No issues found"; merged manifest confirms `com.android.vending.BILLING` via merger (see Play checklist) |
| ADB cold start (SM-M315F) | PASS — live weather renders (27° Clear Night/Clear Day), 0 crashes in logcat |
| Search / Favorites / Map tabs | PASS — render; map alert chip no longer overlaps compass |
| BACK from Alerts / Detail | PASS — returns to dashboard |
| Premium paywall from Settings | PASS — opens |
| Settings rotation persistence | PASS (run 2) |

`connectedDebugAndroidTest`: NOT RUN (no instrumentation harness executed on device).

## 3. Findings

### Fixed and verified (code + unit tests + ADB where applicable)

1. **Temperature display truncated instead of rounded** — `UnitsConverter.kt`, notification worker used
   `toInt()`. Fixed to `roundToInt()`; 4 existing tests that codified truncation updated.
2. **Pressure source inconsistent** — now prefers `pressure_msl`, falls back to `surface_pressure`
   (new `WeatherDto` field + mapper; `WeatherMappers.kt`). Domain/UI untouched.
3. **Cache key used raw GPS floats** — quantized to 3 decimals (`cacheKeyFor` in
   `WeatherRepositoryImpl.kt`); stale entries (>7 days) pruned after insert. New `WeatherCacheKeyTest` (4 tests).
4. **Mapper fabricated 0.0 for null temps** — caused false "Cold Alert (0 °C)". Hourly/daily entries with
   null temperatures are now dropped (4 new regression tests in `WeatherMapperTest.kt`).
5. **Alert worker retried forever without location** — falls back to last-persisted coordinates, has network
   constraint + attempt caps (`WeatherAlertWorker.kt`, `WeatherNotificationWorker.kt`). Notification worker also
   got exception handling + quiet-hours fix.
6. **History "on this day" queries ignored device timezone** — DAO now uses `'localtime'`
   (`WeatherDao.kt`). `deleteAllLocalData` clears extras; OkHttp cache moved to network interceptor
   (`NetworkModule.kt`); `backup_rules.xml` DataStore path fixed.
7. **Widget rain-hour shifted by timezone** — parses in API timezone with guarded parse (`WeatherWidget.kt`).
8. **Banner recreated per composition / ads before consent** — `AdManager` singleton banner +
   `consentVersion` state; `AdBanner.kt` rewritten. Interstitial removed from Weather→Detail open (kept,
   capped 3-min/10-per-session, for Travel/Compare entry — flagged for owner review).
9. **Location fix never timed out** — `DefaultLocationTracker` has `@SuppressLint` + `fusedLastLocation`
   5 s timeout. Splash rotation fixed (`MainActivity.kt`).
10. **Modal BACK dead-ends** — single ordered `BackHandler` covers all modal screens; modal flags are
    `rememberSaveable` (`VayuApp.kt:96-137`). Onboarding flash fixed; weather loads on onboarding complete;
    Settings→Premium ordering fixed; share fallback wrapped in try/catch; nav labels/strings localized;
    rewarded-ad not-ready toast; `SkyCastFavoriteCard` 36 dp touch target; `SkyCastMetricDetailSheet`
    `LaunchedEffect` dismiss; Compare empty-state "Add cities" button; Compare/Travel/Favorites/History
    view-models hoisted with `PendingCitySelection` (fixes dead-end "Add city" / "Pick destination" flows);
    map alert markers shown as column (max 2 + "+N more") with debounced `onBoundsChanged` pan sync.
11. **Premium→BACK lost Settings (found 2026-10-11 during re-test)** — `VayuApp.kt` cleared `showSettings`
    when opening Premium, so BACK landed on the dashboard (`artifacts/screen_15_after_back.png` proves it).
    Fixed with the same overlay pattern PrivacyPolicy already used: `showPremium` branch now renders
    *before* `showSettings`, `onOpenPremium` keeps Settings underneath, BACK pops Premium first and reveals
    Settings. `BackHandler` order already matched. `assembleDebug` PASS, unit tests PASS (316/316) after fix.
    On-device re-verification PENDING (device re-locked; see ADB report §5).

### Phase 2 (2026-10-12) — billing, ads, consent, privacy
12. **Genuine Play Billing implemented (was A2).** Billing Library 9.1.0, layered
    `BillingManager` + mockable `BillingGateway`, pure entitlement resolver, Hilt wiring,
    paywall driven by Play-localized offers, honest disabled state (verified on device:
    no fake prices, Upgrade disabled), stub setter deleted, restore = Play re-query,
    refresh on start/resume. 22/22 new unit tests PASS. No product IDs exist, so purchase
    stays disabled — owner console setup required (checklist in billing report). No
    verification backend exists (documented limitation). Full lifecycle details:
    `VAAYU_BILLING_IMPLEMENTATION.md`.
13. **Travel/Compare interstitial gates removed (was A3).** Conservative default; startup,
    refresh, detail-open, exit verified ad-free; rewarded kept for its genuine opt-in use
    case. Full placement audit: `VAAYU_ADMOB_SETUP.md` §5.
14. **UMP/consent code verified, account gap remains (A1).** Official UMP APIs, correct
    sequencing, privacy-options entry — but the publisher-configuration warning still
    reproduces on device 2026-10-12. GUI owner steps: `VAAYU_ADMOB_OWNER_ACTIONS.md`.
15. **Privacy/Data-safety evidence checklist** added to the Play checklist (§7); policy
    sync + purchase disclosure flagged for the owner before Premium ships.

### Phase 3 (2026-10-12) — final verification audit
16. **Purchase-gate leak on coroutine cancellation (F13).** `PlayBillingGateway.launchPurchase`
    could leave `pendingOutcome` set if the calling scope was cancelled while the Play sheet
    was open, permanently failing every later purchase. Fixed with `NonCancellable` gate
    cleanup + identity-checked timeout cleanup; regression test added (15th
    `BillingManagerTest` case). No entitlement path changed — button press, pending purchase
    and failed/empty queries still cannot grant Premium; only a successful non-empty Play
    purchase query sets it, and only a *successful* empty query revokes (transient failures
    keep the last verified state).
17. **`detailsCache` concurrent-write race (F14).** Overlapping product-detail refreshes
    mutated shared cache on Play's callback thread; now collected locally and merged under
    the gateway mutex.
18. **Release ad-ID guard covered only the App ID (F15).** `isTestAdmobId` now fails the
    release build if any of the four ad fields (app ID, banner, interstitial, rewarded)
    carries Google's test prefix. Verified: release BuildConfig matches
    `local.properties` for all three unit IDs; no test IDs in release config; no
    hardcoded ad IDs anywhere in `app/src`.
19. **Stale root report corrected.** `FINAL_VERIFICATION_REPORT.md` targetSdk 36 → 37
    (matches `app/build.gradle.kts`); no other content altered.

### REQUIRES OWNER ACTION (cannot be done in code)

- **A1. UMP consent form not configured.** Logcat: `Publisher misconfiguration … no form(s) configured for
  the input app ID … ca-app-pub-4212146603115396~…` (suffix redacted). Configure the UMP message in the
  AdMob console for this app ID. Until then EU/EEA consent flow cannot complete.
- **A2. Play product setup (billing code: done 2026-10-12, console: pending).** Create +
  activate subscription/in-app products, paste IDs into `BillingProducts`, verify with
  license testers on the internal track. Purchase initiation stays disabled until then —
  by design, not a defect. Consider a server-side verification backend as follow-up.
- **A3. RESOLVED 2026-10-12** — Travel/Compare interstitial gates removed (F12).

### Notes (no action)

- `FINAL_VERIFICATION_REPORT.md` in repo root: targetSdk line corrected to 37 on 2026-10-12;
  remaining content predates Phases 2–3 — treat `reports/*` as authoritative.
- `lint` config suppressions remain; result verified via XML, not console.
- No secrets are recorded in any report (AdMob IDs / keystore credentials redacted or omitted).
