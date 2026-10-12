# VAAYU Fix Log — root-cause fixes with before/after evidence

All items keep: app ID, signing, single-module architecture, manual state nav, Open-Meteo provider,
existing UI/UX. Build is green after each group (`assembleDebug` PASS, `testDebugUnitTest` 316/316 PASS,
`lintDebug` 0 issues).

## F1. Unit display truncation → rounding
- Files: `data/converter/UnitsConverter.kt`, `data/worker/WeatherNotificationWorker.kt`,
  `app/src/test/.../UnitConversionTest.kt`, `ProductionAuditTest.kt`
- Before: `toInt()` truncated 26.9 °C → "26°".
- After: `roundToInt()` → "27°". 4 tests that codified truncation updated to expect rounding.
- Evidence: unit suite PASS.

## F2. Pressure prefers sea-level
- Files: `data/remote/dto/WeatherDto.kt` (+1 field), `data/mapper/WeatherMappers.kt`
- Before: surface pressure only.
- After: `pressure_msl` preferred, `surface_pressure` fallback. Domain/UI untouched.
- Evidence: 2 new mapper tests (MSL preferred / surface fallback) PASS.

## F3. Cache-key quantization + stale prune
- File: `data/repository/WeatherRepositoryImpl.kt` (+ `cacheKeyFor`), `data/local/WeatherDao.kt`,
  `data/di` untouched; new test `WeatherCacheKeyTest.kt` (4 tests)
- Before: raw GPS floats → duplicate cache rows for the same city.
- After: coordinates quantized to 3 decimals; entries older than 7 days pruned after insert;
  `deleteAllLocalData` also clears extras.
- Evidence: 4 new tests PASS (locale-independent, sign kept, distinct cities distinct, nearby fixes share key).

## F4. Mapper drops null-temperature entries
- File: `data/mapper/WeatherMappers.kt`; tests in `WeatherMapperTest.kt` (+4)
- Before: null hourly/daily temps became 0.0 → false "Cold Alert (0 °C)".
- After: entries with null temp skipped.
- Evidence: 2 new drop-tests + 2 pressure tests PASS.

## F5. Alert/notification worker guards
- Files: `data/worker/WeatherAlertWorker.kt`, `data/worker/WeatherNotificationWorker.kt`
- Before: alert worker retried indefinitely with no location; notification worker threw on bad payloads;
  quiet-hours window wrong overnight.
- After: last-persisted coordinates fallback, network constraint, attempt caps on both workers;
  exception handling + quiet-hours fix in notification worker.

## F6. DAO timezone, HTTP cache, backup rules, widget tz
- Files: `data/local/WeatherDao.kt` (`'localtime'` on-this-day queries),
  `di/NetworkModule.kt` (OkHttp cache via network interceptor), `res/xml/backup_rules.xml`
  (DataStore path), `presentation/widget/WeatherWidget.kt` (rain-hour parsed in API timezone, guarded).

## F7. Ads: singleton banner, consent gating, interstitial placement
- Files: `presentation/ads/AdManager.kt`, `presentation/ads/AdBanner.kt`, `presentation/VayuApp.kt`
- Before: banner instance recreated per composition; ads could request before UMP consent;
  interstitial fired on Weather→Detail (forecast) open.
- After: singleton banner + `consentVersion` state; `AdBanner` rewritten; interstitial removed from
  forecast open; kept capped (3 min / 10 per session) on Travel/Compare entry (owner to confirm).

## F8. Location timeout + splash rotation
- Files: `data/location/DefaultLocationTracker.kt` (`@SuppressLint` + `fusedLastLocation` 5 s timeout),
  `MainActivity.kt` (splash rotation), `VayuApplication.kt`.

## F9. Navigation, state, touch targets, strings
- File: `presentation/VayuApp.kt` (heaviest target), `SkyCastHomeScreen.kt`, `SearchScreen.kt`,
  `WeatherMapScreen.kt`, `SkyCastCompareScreen.kt`, `SkyCastFavoriteCard.kt`,
  `SkyCastMetricDetailSheet.kt`, `res/values/strings.xml`
- Single ordered `BackHandler` for all modal screens; `rememberSaveable` modal flags; onboarding flash
  fix + post-onboarding weather load; Settings→Premium ordering; share try/catch; localized labels;
  rewarded-ad not-ready toast (`ad_not_ready`) + `history`/`alerts`/`plan_a_trip` strings; 36 dp favorite
  IconButton; metric-sheet dismiss effect; Compare empty-state button; hoisted view-models +
  `PendingCitySelection`; map alert column + debounced pan sync.

## F10. Premium→BACK dropped Settings (regression found 2026-10-11, fixed same day)
- File: `presentation/VayuApp.kt` (~3 lines + branch reorder)
- Before: `onOpenPremium` set `showSettings=false; showPremium=true`; the `showSettings` branch also
  rendered before `showPremium`, so the paywall could never overlay Settings. BACK from Premium
  (`showPremium=false`) therefore landed on the dashboard — proven by
  `artifacts/screen_15_after_back.png` (dashboard where Settings was expected) and
  `artifacts/screen_16_rotated.png`.
- After: `showPremium` branch renders *before* `showSettings` (same overlay pattern `showPrivacyPolicy`
  already uses); `onOpenPremium` only sets `showPremium=true`, keeping Settings underneath; BACK
  (`BackHandler` pops Premium before Settings; Premium `onBack` clears only `showPremium`) reveals
  Settings again. Both flags are `rememberSaveable`, so rotation preserves the overlay.
- Evidence: `assembleDebug` PASS, `testDebugUnitTest` 316/316 PASS, `lintDebug` 0 issues after fix.
  Fixed APK installed on device (`adb install -r`: Success). On-device verification COMPLETE
  2026-10-12: Home → Settings → Premium → BACK lands on Settings (`screen_32/33/34`); forced
  landscape + portrait keep Settings (`screen_35/36`); Premium re-opens after rotation and BACK
  returns to Settings (`screen_37/38`); rotation settings restored; logcat
  (`artifacts/logcat_premium_filtered.txt`, IDs redacted) shows no FATAL/ANR/app exceptions.

## Phase 2 (2026-10-12) — genuine Play Billing, ad-policy removals

## F11. Real Google Play Billing replaces the local unlock stub
- New: `data/billing/BillingModels.kt`, `BillingProducts.kt` (`BillingProductConfig`, empty —
  no IDs exist to reuse), `BillingEntitlementResolver.kt` (pure), `BillingGateway.kt`
  (mockable seam), `PlayBillingGateway.kt` (only PBL importer; billing 9.1.0, 9.x
  `QueryProductDetailsResult` wrapper + unfetched-product logging verified against the AAR),
  `BillingManager.kt` (connect/retry, offers, launch, ack-once+dedupe+retry, revoke-only-
  on-successful-empty-query); `di/BillingModule.kt`; tests `BillingManagerTest.kt` (14),
  `BillingEntitlementResolverTest.kt` (7).
- Edited: `SettingsManager.kt` (`setPremiumFromBilling` sole writer; stub `setPremium`
  deleted), `SettingsViewModel.kt` (billing delegation; refresh on init, entitlement
  propagation, stub deleted), `PremiumScreen.kt` (Play offers w/ localized pricing, honest
  disabled state when unconfigured, real restore, false reinstall claim removed),
  `PremiumManager.kt` (hardcoded plans removed), `VayuApp.kt` (paywall wiring, resume
  re-verify), `gradle/libs.versions.toml` + `app/build.gradle.kts` (billing 9.1.0).
- Root cause addressed: purchase button can no longer create entitlement; PENDING/
  cancelled/failed/unconfigured paths grant nothing; restore = Play re-query.
- Evidence: 21/21 new tests PASS (full-suite/build/lint totals in Play checklist §1).

## F12. Interstitial entry gates removed (Travel, Compare)
- File: `presentation/VayuApp.kt` (`onOpenTravel`, Favorites `onCompare`).
- Before: `AdManager.showInterstitial` gated navigation into Travel/Compare (capped).
- After: direct navigation; no evidence the interruption was appropriate, so the
  conservative default applies. Caps/preload infra retained for future
  policy-compliant placements only. Startup/refresh/detail/exit remain ad-free.

## F13. Purchase-gate leak on cancellation (Phase 3 audit)
- File: `data/billing/PlayBillingGateway.kt` (`launchPurchase`).
- Before: a coroutine cancellation while the Play sheet was open (paywall torn
  down, scope cancelled) left `pendingOutcome` set forever → every later purchase
  silently returned FAILED (stuck single-sheet gate).
- After: `CancellationException` clears the gate under `NonCancellable` before
  rethrow; timeout path clears only its own deferred (`===` check). Regression
  test: `cancelled launch resets progress and allows retry` in `BillingManagerTest`.

## F14. `detailsCache` concurrent-write race (Phase 3 audit)
- File: `data/billing/PlayBillingGateway.kt` (`queryProductDetails`).
- Before: two overlapping refreshes mutated `detailsCache`/`offers` on Play's
  callback thread without the lock (possible lost/duplicate offers).
- After: callback collects into a local list; merge happens under `lock.withLock`.

## F15. Release guard only checked the App ID (Phase 3 requirement)
- File: `app/build.gradle.kts`.
- Before: `isTestAdmobId` only tested `ADMOB_APP_ID` against Google's test prefix;
  a test banner/interstitial/rewarded ID could ship in release unnoticed.
- After: guard fails the release build if **any** of the four ad fields starts
  with `ca-app-pub-3940256099942544`.

## Not changed (deliberately)
- `PushNotificationWorker` / `PushNotificationReceiver` (dead code), `WeatherDashboard.kt` (~105 KB dead
  code): documented, not deleted, to keep the diff reviewable.
- Premium purchase flow: genuine billing implemented 2026-10-12 (F11); Play product
  configuration still REQUIRES OWNER ACTION (no IDs exist yet).
- Dependencies: no blind updates (AdMob 25.4.0, UMP 4.0.0, Firebase BOM 34.18.0 unchanged).
