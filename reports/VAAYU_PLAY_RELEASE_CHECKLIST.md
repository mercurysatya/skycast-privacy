# VAAYU Play Release Checklist — v1.5 (versionCode 5), targetSdk 37

Status labels: PASS / FAIL / BLOCKED / NOT RUN / REQUIRES OWNER ACTION.

## 1. Build artifacts — PASS (Phase-2 rebuild 2026-10-12, see §6)
- `testDebugUnitTest`: PASS — 29 suites / 338 tests / 0 failures (316 baseline + 15
  `BillingManagerTest` (incl. Phase 3 cancellation-regression test) +
  7 `BillingEntitlementResolverTest`).
- `assembleDebug`: PASS. `lintDebug`: PASS, 0 issues in `lint-results-debug.xml`.
- `lintVitalRelease`: PASS — "No issues found".
- Phase 3 re-verification (2026-10-12, sequential, final source incl. F13–F15):
  `testDebugUnitTest` PASS (338/338, exit 0) → `lintDebug` PASS (0 issues, exit 0) →
  `assembleDebug` PASS (exit 0) → `lintVitalRelease` PASS ("No issues found", exit 0) →
  `assembleRelease bundleRelease` PASS (exit 0). Fresh artifacts rebuilt from final
  source: `app-release.apk` 24,028,408 B (07:11:27) / `app-release.aab` 33,526,445 B
  (07:11:32), 2026-10-12. `apksigner verify` PASS (exit 0); `jarsigner -verify` on the
  AAB: "jar verified". Release BuildConfig banner/interstitial/rewarded IDs byte-match
  `local.properties`; release manifest App ID matches; zero Google test IDs in release
  config; zero hardcoded ad IDs in `app/src`. Merged release manifest verified:
  `com.android.vending.BILLING` present (via manifest merger, no manual change).
- `connectedDebugAndroidTest`: NOT RUN.

## 2. Release-blocking items (REQUIRES OWNER ACTION)
- **B1. Play product configuration missing (billing code: done, console: pending).**
  Genuine Billing Library 9.1.0 integration is implemented and mock-tested
  (`VAAYU_BILLING_IMPLEMENTATION.md`); the stub setter is deleted, so nothing can
  grant Premium without Play. But NO product/base-plan IDs exist, so purchase stays
  honestly disabled. Owner must create + activate products, paste IDs into
  `BillingProducts`, and verify with license testers on the internal track. No
  server-side verification backend exists (documented limitation + follow-up).
- **B2. UMP message not configured** for app ID `ca-app-pub-4212146603115396~…` (suffix redacted).
  Still reproducing on device 2026-10-12 (logcat `artifacts/logcat_p3_filtered.txt`,
  `Publisher misconfiguration: … no form(s) configured`). EEA consent
  cannot complete until the AdMob console has a published message for this ID.
  GUI steps: `VAAYU_ADMOB_OWNER_ACTIONS.md`.
- **B3. RESOLVED 2026-10-12** — Travel/Compare interstitial entry gates removed
  (conservative default; `VayuApp.kt`, see Fix Log F12). No interstitial at startup,
  refresh, forecast open, or exit (verified by code audit). Rewarded kept for its
  genuine opt-in use case.

## 3. Pre-upload verification (owner / release manager)
1. Confirm the uploaded AAB was built from a clean tree at/after the backup-rules fix and that
   `local.properties` held production IDs (never ship `ca-app-pub-3940256099942544…` test IDs).
2. Play Console: app integrity (signed by upload key), target API 37 acknowledged, Data safety form
   completed (location, advertising ID, diagnostics), content rating questionnaire, news/weather
   declaration if prompted, screenshots + feature graphic, privacy-policy URL (in-app viewer exists).
3. Internal-test track first: cold start, purchase-less Premium copy check, UMP form display (EEA),
   banner/interstitial/rewarded with test-device IDs, rotation, backup/restore of settings only.
4. Staged rollout (e.g. 10% → 50% → 100%) with crash-rate and ANR watch.

## 4. Known minor inconsistency (not blocking)
- `data_extraction_rules.xml` (API 31+) excludes widget prefs + databases but not `weather_streak.xml`,
  while the legacy `backup_rules.xml` (API ≤30) now backs up only the DataStore file. Net effect is
  equivalent for streak on old devices; on API 31+ the streak file backs up. Harmless either way, but
  align the two files if streak ever becomes user-visible.

## 5. Sensitive files (never commit, never publish)
- `local.properties` (production AdMob IDs), `key.properties` + keystore (signing). Both gitignored —
  verified present locally, absent from `git status` tracked changes. Signing credentials untouched
  by Phase 2 (only read at build time, as before).

## 7. Privacy, Data safety and release metadata (evidence-based, 2026-10-12)
- Location: `ACCESS_COARSE/ACCESS_FINE_LOCATION` declared, runtime-gated
  (`DefaultLocationTracker`/onboarding); coordinates sent ephemerally to Open-Meteo per the
  policy; no first-party servers, nothing logged by the app.
- Advertising identifiers: merged release manifest contains `AD_ID` + ad-services
  (`AD_ID/ATTRIBUTION/TOPICS`) permissions via the ads SDK; AdMob data use is disclosed in
  the in-app policy; all requests gated on `canRequestAds()`; privacy-options entry in Settings.
- Firebase: Analytics/Crashlytics/Remote Config/App Check dependencies present (BOM 34.18.0);
  ZERO custom `logEvent/recordException` calls in `app/src/main` (auto-init only) — declare
  SDK-default collection honestly, do not claim zero collection.
- Backup: legacy rules back up only the DataStore file; API 31+ extraction rules exclude
  widget prefs + databases (streak-file inconsistency already noted in §4).
- Privacy policy: in-app assets copy (175 lines, June 2026) is newer than the root copy
  (152 lines) — sync them, and ADD Premium/purchase disclosure (Play billing, entitlement
  tied to Google account, cancellation via Play Store) before Premium ships. Listing URL
  must point at the updated text.
- Data safety form (owner): location (approx + precise), advertising ID, diagnostics;
  retention/deletion = local-only + system Clear Data, matching the policy.
- Content rating / target audience: NOT changed by this work — owner completes the
  questionnaire; do not default to an audience without answering it.
- Versions verified against source: applicationId `com.vayu.weather`, targetSdk 37,
  versionCode 5 / versionName 1.5 — consistent across all current reports (the only "36"
  anywhere is the audit's stale-file callout).
- Subscription terms: paywall copy is now accurate (Play-handled, linked to Google
  account, manage/cancel in Play Store); Play-Console subscription terms, grace periods
  and cancellation info remain owner setup alongside B1.

## 6. Rebuild note
- Phase-2 rebuild from current source (`lintVitalRelease bundleRelease`, 61 executed):
  BUILD SUCCESSFUL, exit 0, `signReleaseBundle` executed. Fresh artifacts 2026-10-12:
  `app-release.aab` (~33.5 MB). Vital lint independently PASS ("No issues found").
  Do NOT upload without clearing B1–B2 in §2. Historical note: the Phase-1 rebuild
  (2026-10-11, backup-rules fix, 66/66 tasks) was superseded by this build.
