# VAAYU DEPENDENCY UPDATE REPORT

**Report ID:** VAAYU_DEPENDENCY_UPDATE_REPORT
**Date:** 2026-10-17
**Scope:** dependency versions audit for the store release build; minimal, evidence-backed changes only.
**Build verification after change:** `./gradlew assembleDebug testDebugUnitTest lintDebug` → BUILD SUCCESSFUL; later `assembleRelease bundleRelease` → BUILD SUCCESSFUL (9m 8s). **Final regression 2026-10-18:** everything re-run green after the two UI fixes (see §5 last row).

---

## 1. Current version baseline (as audited)

| Component | Version in repo (`gradle/libs.versions.toml`) | Note |
|---|---|---|
| Android Gradle Plugin | 9.4.1 | modern AGP line |
| Gradle wrapper | 9.7.1 | |
| Kotlin | 2.3.21 | |
| compileSdk / targetSdk | **37 / 37** | Android 17 APIs |
| minSdk | 24 | |
| Room | 2.8.4 | KSP |
| Play Billing | **9.1.0** | latest stable — see §2.1 |
| play-services-ads | 25.4.0 → **25.5.0 (applied)** | see §3 |
| UMP (`user-messaging-platform`) | 4.0.0 | explicit pin; matches ads 25.x transitive |
| Firebase BOM leaf libs | analytics / crashlytics / config / appcheck (BOM-managed) | |
| Compose BOM | platform-managed | |
| OkHttp / Retrofit / Moshi | current stable line | |

## 2. Up-to-date confirmations (evidence-based, no change required)

### 2.1 Play Billing 9.1.0 — LATEST STABLE, exceeds Play policy
Source: official release notes `developer.android.com/google/play/billing/release-notes` (fetched 2026-10-17):
- "**Google Play Billing Library 9.1.0 Release (2026-06-18)**" is the newest entry; 9.0.0 released 2026-05-19.
- Play policy notice on the same page: "**By Aug 31, 2026, all new apps and updates to existing apps must use Billing Library version 8 or later**. If you need more time … extension until **Nov 1, 2026**."
- → The app's **9.1.0** satisfies this with a whole major version of headroom. **No upgrade possible (already latest).**
- Important behavioral confirmations that the repo's billing code relies on are documented in the same notes:
  - Billing 8.1.0 introduced `queryPurchasesAsync()` including **suspended subscriptions** with `isSuspended() = true` — "you shouldn't grant access to the purchased subscription". Our `BillingEntitlementResolver` explicitly treats suspended subscriptions as **no entitlement** (kept latest entitlements only on transport failure; revocation requires positive empty-query evidence). Correct per policy.
  - PBL 8.1.0 raised the library's **minSdkVersion to 23** → our app minSdk 24 is compatible.
  - PBL 9.0.0 raised targetSdkVersion to 35 and noted AndroidX.core 1.9+ requirement only for the "Play Store blocked" error-code mapping (compatibility behavior, not a blocker).

### 2.2 UMP SDK 4.0.0 — CURRENT for ads 25.x line
Source: official AdMob release notes `developers.google.com/admob/android/rel-notes` (fetched 2026-10-17):
- ads **25.0.0 (2026-02-17)**: "**Updated User Messaging Platform SDK dependency to version 4.0.0.**"
- → app's explicit pin `com.google.android.ump:user-messaging-platform:4.0.0` matches the SDK line; nothing newer exists on that track. **Keep.**

### 2.3 All other majors were reviewed and deferred with reasons
| Component | Action | Reason |
|---|---|---|
| Compose BOM / lifecycle / navigation3 | Keep | current per BOM resolution; no functional gap found |
| Room 2.8.4 / DataStore / WorkManager | Keep | current stable line, KSP clean |
| OkHttp / Retrofit / Moshi / logging-interceptor | Keep | release-verified; no CVE still applicable on the pinned line |
| Glance widgets / MapLibre compose | Keep | release-verified on device |
| material | Keep | adjacent Compose-M3 primary |

*(Any speculative "bump everything" was rejected: the audit's contract is minimal verified changes with regression evidence.)*

## 3. Upgrade applied this cycle

### play-services-ads 25.4.0 → 25.5.0

- **What changed upstream (official notes):** "**Increased the minimum Android API level to 24** — added `onInitializationFailed()` to `InitializationCompleteCallback` (mediation adapters now report third-party SDK init errors …)"; custom-event adapter `ResponseInfo.getAdapterClassName()` now returns the custom event class name.
- **Why safe:** min API 24 == app minSdk 24 (exact match); no breaking API in note set touches our usage (banner/interstitial/rewarded loads + initialize + show callbacks). Post-upgrade full debug assemble + 345/345 tests + lint passed, then the same build was installed `install -r` on the device and exercised (launch + paywall + ad-gate logs OK).
- **Constraint honored:** UMP transitive dependency resolved to 4.0.0 — consistent with the explicit pin; no version-conflict warnings during builds.

## 4. Forward-looking version guidance (not applied now)

| Trend | Evidence | Recommendation |
|---|---|---|
| **GMA SDK ("legacy") is in maintenance mode** | Banner on the official release-notes page: "Google Mobile Ads SDK is in maintenance mode. For the latest updates and features, migrate … to GMA Next-Gen SDK." | Schedule the Next-Gen SDK migration (owner decision; not a submission blocker). |
| Billing 9.x minor stream | 9.1.0 current | Absorb future 9.x releases at normal cadence. |
| Compose/navigation3 lifecycle | BOM-managed | Continue BOM-managed upgrades. |

## 5. Verification trail

| Task | Result | Artifact |
|---|---|---|
| `assembleDebug` | SUCCESS | Phase D session log |
| `testDebugUnitTest` | **345/345 (30 suites), 0 failures, 0 skipped** | `artifacts/postfix_test_run.log`; JUnit XML stats |
| `lintDebug` | SUCCESS | `artifacts/fresh_lint_debug.log` |
| `assembleRelease bundleRelease` | SUCCESS (signed APK 24,047,844 B; AAB 33,551,150 B) | `app/build/outputs/{apk,bundle}/release/` |
| Device re-verify after upgrade | Launch + paywall + ad gates live (see ADB report §2.3) | `artifacts/logcat_paywall_full.txt` |
| **Final regression 2026-10-18** | `testDebugUnitTest` **345/345** + `lintDebug` **0 errors/0 warnings/1 hint** + `assembleDebug assembleRelease bundleRelease` **SUCCESS** (8m 32s); apksigner V2 re-verified (same cert); rebuilt signed APK **24,047,724 B** / AAB **33,550,815 B**; device Map/History re-checked live, 0 FATAL/ANR | `artifacts/final_test_lint.log`, `artifacts/final_builds.log`, `artifacts/ui_sweep/ui14_map_final.png`, `ui15_history_final.png` |

---

**Conclusion:** single verified dependency action (ads → 25.5.0); Play Billing and UMP confirmed current-by-policy; everything else left untouched by design. The release artifact chain has been rebuilt and cleared green after the change.
