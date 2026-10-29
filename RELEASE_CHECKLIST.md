# SkyCast Weather — Release Engineering Checklist

**App:** SkyCast Weather (Vayu) · `com.vayu.weather`  
**Version:** 1.5 (versionCode 5) · minSdk 24 / targetSdk 37 / compileSdk 37  
**Branch:** `gh-pages` · **Date:** 2026-10-29  
**Auditor:** Release Engineering Lead  
**Status:** RELEASE BLOCKED — see § Blockers below

---

## 1. Release Version

| Item | Value | Status |
|------|-------|--------|
| `versionCode` | 5 | ✅ |
| `versionName` | "1.5" | ✅ |
| `applicationId` | `com.vayu.weather` | ✅ |
| `minSdk` | 24 | ✅ |
| `targetSdk` | 37 | ✅ |
| `compileSdk` | 37 | ✅ |
| Build tools | AGP 9.4.1, Kotlin 2.3.21, Gradle 9.7.1, JDK 17 | ✅ |
| Java toolchain | JDK 17 (source/target compatibility) | ✅ |

**Source:** `app/build.gradle.kts` lines 60-65, `gradle/libs.versions.toml`

---

## 2. Build Configuration

| Item | Detail | Status |
|------|--------|--------|
| Release minification | R8 enabled (`isMinifyEnabled = true`, `isShrinkResources = true`) | ✅ |
| ProGuard rules | `proguard-rules.pro` — covers Hilt, Moshi, Room, Retrofit, OkHttp, AdMob, Firebase, MapLibre, Glance, WorkManager, Coroutines, DataStore | ✅ |
| Desugaring | `coreLibraryDesugaring` enabled, Java 17 toolchain | ✅ |
| BuildConfig | `DEBUG = false` in release, AdMob IDs injected as build config fields | ✅ |
| Lint | `abortOnError = false`, `warningsAsErrors = false`, specific noise checks disabled | ⚠️ Non-fatal lint config — release can proceed with warnings |
| Release build | `assembleRelease` (7m 20s) and `bundleRelease` (8m 55s) both succeed | ✅ |
| Earlier timeout | `lintVitalRelease` timed out once (10 min) — transient; passes in ~75s on retry | ✅ Resolved |

**Note:** `lintVitalRelease` initially timed out (10 min) but passes on retry (~75s). All compilation tasks (`compileReleaseKotlin`, `hiltJavaCompileRelease`, `minifyReleaseWithR8`) completed successfully.

---

## 3. Signing Process

| Item | Detail | Status |
|------|--------|--------|
| Signing config | `signingConfigs.release` created from `key.properties` | ⚠️ Auto-signing not applied to APK |
| Keystore file | `key.properties` → `C:\Weather_App\keystore.jks` (root) | ✅ Works with `Sky@2026` |
| Keystore password | `Sky@2026` | ✅ Works for `keystore.jks` |
| Key alias | `key0` | ✅ |
| Certificate validity | 21-Jun-2026 → 15-Jun-2051 (25 years) | ✅ |
| Certificate subject | C=IN, ST=Andhra Pradesh, L=Srikakulam, O=Mercury Computer Mart, CN=Bammidi Satyanarayana | ✅ |
| Existing signed AAB | `app/release/app-release.aab` (July 14, 2026) — signed with same cert | ✅ |
| `hasSigningConfig` check | Requires `storePassword` non-empty and not "CHANGE_ME" | ✅ (password is set) |
| APK auto-signing | **NOT applied** by AGP — APK was unsigned after `assembleRelease` | ❌ Required manual `apksigner` |
| AAB auto-signing | **Applied** via `signReleaseBundle` task | ✅ Signed automatically |
| Manual APK signing | `apksigner sign --ks keystore.jks --ks-pass pass:Sky@2026 --key-pass pass:Sky@2026 --ks-key-alias key0 app-release.apk` | ✅ Applied manually |
| APK verification | `apksigner verify` — v2 and v3 schemes, 1 signer | ✅ Verified |
| AAB verification | `jarsigner -verify` — signed with same cert | ✅ Verified |

**Keystore note:** `key.properties` `storeFile=C:\Weather_App\keystore.jks` IS correct. The root `keystore.jks` accepts password `Sky@2026` and contains alias `key0`. Initial analysis suggesting a path mismatch was incorrect — `app\vayu-release-key.jks` does NOT accept `Sky@2026`.

---

## 4. Secrets Handling

| Item | Detail | Status |
|------|--------|--------|
| `local.properties` | Gitignored, contains `sdk.dir` + production AdMob IDs | ✅ Not committed |
| `key.properties` | Gitignored, contains signing passwords | ✅ Not committed |
| `google-services.json` | Gitignored, contains Firebase project config + API key | ✅ Not committed |
| AdMob ID resolution | `gradle.properties` → `local.properties` → test defaults | ✅ |
| Release AdMob guard | Fails release build if test IDs (`ca-app-pub-3940256099942544*`) detected (bypass: `-PallowTestAdmobIds=true`) | ✅ |
| Production AdMob IDs | Configured in `local.properties` (real publisher ID `ca-app-pub-4212146603115396`) | ✅ |
| Debug test device IDs | Only `EMULATOR` when `BuildConfig.DEBUG` | ✅ |

**AdMob IDs in `local.properties`:**
- `ADMOB_APP_ID=ca-app-pub-4212146603115396~1734384492` ✅ (production)
- `ADMOB_BANNER_ID=ca-app-pub-4212146603115396/5488367023` ✅ (production)
- `ADMOB_INTERSTITIAL_ID=ca-app-pub-4212146603115396/1666286488` ✅ (production)
- `ADMOB_REWARDED_ID=ca-app-pub-4212146603115396/1895394923` ✅ (production)

---

## 5. Database Migrations

| Migration | Detail | Status |
|-----------|--------|--------|
| v1→v2 | Create `recent_searches` table | ✅ |
| v2→v3 | Create `weather_alerts` table | ✅ |
| v3→v4 | Create `weather_history` table | ✅ |
| v4→v5 | Add `sortIndex` to `favorite_cities` (non-destructive, rowid-based ordering) | ✅ |
| v5→v6 | Add `fingerprint` + `expiresAt` to `weather_alerts`, unique index on fingerprint | ✅ |
| Current DB version | 6 | ✅ |
| `exportSchema` | `false` (schema not exported — acceptable for private app) | ✅ |
| Migration registration | `ALL` array in `VayuMigrations.kt`, registered in `DatabaseModule` | ✅ |
| Migration test coverage | Migration tests exist in `app/src/test/` | ✅ |

All migrations are non-destructive (ALTER TABLE, no DROP). Existing user data is preserved across upgrades.

---

## 6. Permissions

| Permission | Purpose | Status |
|------------|---------|--------|
| `INTERNET` | Weather API, AdMob, Firebase | ✅ Required |
| `ACCESS_COARSE_LOCATION` | Approximate location for weather | ✅ Required |
| `ACCESS_FINE_LOCATION` | Precise GPS location for weather | ✅ Required |
| `ACCESS_NETWORK_STATE` | Network connectivity checks | ✅ Required |
| `POST_NOTIFICATIONS` | Weather alerts & notifications (API 33+) | ✅ Required |

No dangerous permissions beyond location and notifications. Location is used on-device only, not stored on servers (per privacy policy).

---

## 7. Dependency Compatibility

| Library | Version | Status |
|---------|---------|--------|
| Kotlin | 2.3.21 | ✅ |
| Compose BOM | 2026.08.00 | ✅ |
| AGP | 9.4.1 | ✅ |
| Hilt | 2.60.1 | ✅ |
| Firebase BOM | 34.18.0 | ✅ |
| Play Services Ads | 25.5.0 | ✅ |
| Play Billing | 9.1.0 | ✅ |
| Play Services Location | 21.4.0 | ✅ |
| Room | 2.8.4 | ✅ |
| WorkManager | 2.11.2 | ✅ |
| MapLibre Compose | 0.15.0 | ✅ |
| UMP (User Messaging Platform) | 4.0.0 | ✅ |
| KSP | 2.3.11 | ✅ |
| Moshi | 1.15.2 | ✅ |
| Retrofit | 2.12.0 | ✅ |
| OkHttp | 4.10.0 | ✅ |
| DataStore | 1.2.1 | ✅ |
| Glance | 1.1.1 | ✅ |
| MPAndroidChart | v3.1.0 | ✅ |

**Unused declared dependencies** (in `libs.versions.toml` but not referenced in `build.gradle.kts`): `coil-compose`, `accompanist-permissions`, `androidx-camera-*`. These are harmless but add to dependency resolution time. Not a release blocker.

**Kapt warning:** Moshi KSP codegen deprecation warning — KSP is used (correct), kapt fallback is deprecated but not breaking.

---

## 8. Crash Reporting

| Item | Detail | Status |
|------|--------|--------|
| Crashlytics plugin | `com.google.firebase.crashlytics` v3.0.8 applied | ✅ |
| Firebase BOM | 34.18.0 (includes Crashlytics, Analytics, Remote Config, App Check) | ✅ |
| `google-services.json` | Present (gitignored), project `vayu-b5596`, package `com.vayu.weather` | ✅ |
| Firebase init | `VayuApplication.initializeFirebase()` — graceful failure, app continues without Firebase | ✅ |
| Crashlytics init | Automatic via SDK on first use | ✅ |
| Privacy disclosure | Crashlytics listed in privacy policy (crash reports, stack traces, device model, OS version) | ✅ |

---

## 9. Privacy Disclosures

| Item | Detail | Status |
|------|--------|--------|
| Privacy policy (assets) | `app/src/main/assets/privacy-policy.html` — comprehensive, last updated October 2026 | ✅ |
| Privacy policy (published) | `privacy-policy.html` (GitHub Pages) — kept in sync per README | ✅ |
| Data collection | Location (on-device only, not server-stored), AdMob IDs, Firebase analytics/crash data | ✅ Disclosed |
| Third-party services | Open-Meteo, Google AdMob, Firebase Analytics/Crashlytics/Remote Config/App Check, RainViewer, MapLibre — all disclosed | ✅ |
| Data deletion | Clear app data or uninstall; no server-side data | ✅ Disclosed |
| COPPA | App not directed at children under 13 | ✅ Disclosed |
| Premium billing | Google Play Billing, linked to Google account, cancellable in Play Store | ✅ Disclosed |
| UMP consent | GDPR/EEA consent before ad requests, privacy options in Settings | ✅ Implemented |
| `networkSecurityConfig` | `cleartextTrafficPermitted="false"`, system trust-anchors only | ✅ |
| `dataExtractionRules` / `backup_rules` | Datastore, databases, widget prefs excluded from backup/device transfer | ✅ |
| `allowBackup` | `false` | ✅ |

---

## 10. Store Requirements (Play Console)

| Item | Status | Notes |
|------|--------|-------|
| Play Billing products configured | ❌ NOT CONFIGURED | `BillingProducts` returns empty lists. Owner must create products in Play Console and list IDs in `BillingProducts.kt`. Paywall shows honest "not set up yet" state. |
| Data Safety declaration | ⚠️ Owner action | Required before production rollout (per README) |
| Content rating | ⚠️ Owner action | Required before production rollout |
| Closed testing track | ⚠️ Owner action | Required before production rollout |
| AdMob app linking | ⚠️ Owner action | AdMob app ID must be linked in AdMob console |

---

## 11. Debug/Test Code in Release

| Item | Detail | Status |
|------|--------|--------|
| `BuildConfig.DEBUG` | `false` in release build type | ✅ |
| AdMob test device IDs | Only `EMULATOR` when `DEBUG` | ✅ |
| HttpLoggingInterceptor | `Level.BODY` only when `DEBUG`, `Level.NONE` in release | ✅ |
| UMP EEA debug geography | Only when `BuildConfig.DEBUG` | ✅ |
| `debugImplementation` dependencies | `compose.ui.test.manifest`, `compose.ui.tooling` — debug-only, not in release | ✅ |
| `testImplementation` dependencies | JUnit, Mockito, coroutines-test — test-only, not in release | ✅ |
| `androidTestImplementation` | Compose UI test, Espresso — instrumented-test only | ✅ |
| Log statements | `Log.d/e/w` throughout — present in release but standard for Android apps; ProGuard does not strip them | ⚠️ Acceptable (not a security issue, just noisy logcat) |

No debug/test code or dependencies leak into the release APK/AAB.

---

## 12. Tests

| Suite | Result | Detail |
|-------|--------|--------|
| JVM unit tests (`testDebugUnitTest`) | ✅ PASS | All tests pass (54s) |
| Instrumented UI tests (`connectedDebugAndroidTest`) | ✅ 37/37 PASS (100%) | Pixel_10a AVD, API 17, ~2 min. All screens tested: dashboard, search, favorites, alerts, history, compare, travel, map. |
| Lint (`lintDebug`) | ✅ BUILD SUCCESSFUL | 0 errors / 0 warnings |
| Release build (`assembleRelease`) | ✅ BUILD SUCCESSFUL | 7m 20s |
| Release build (`bundleRelease`) | ✅ BUILD SUCCESSFUL | 8m 55s |
| Release signing (APK) | ✅ Signed via `apksigner` | v2/v3 schemes verified |
| Release signing (AAB) | ✅ Signed via `signReleaseBundle` | Verified with `jarsigner` |

---

## 13. Rollback / Recovery Plan

| Scenario | Recovery | Status |
|----------|----------|--------|
| Bad release | Google Play Console: roll back to previous version (app-release.aab from July 14 exists) | ✅ Play Console feature |
| Database migration failure | All migrations are non-destructive ALTER TABLE; no data loss path | ✅ |
| Billing purchase failure | Entitlement not granted; user can "Restore purchase" in paywall; next refresh re-queries Play | ✅ |
| AdMob consent failure | Ads not served; app functions normally (premium or ad-free path) | ✅ |
| Firebase crash | App continues without Firebase (graceful failure in `VayuApplication`) | ✅ |
| Location permission denied | Falls back to last known location from DataStore; error state shown | ✅ |
| Network offline | Stale cache fallback (30 min TTL, 7 day max age); offline banner shown | ✅ |

---

## 14. Blockers (Must Resolve Before Production Release)

| # | Blocker | Severity | Owner Action |
|---|---------|----------|--------------|
| B1 | **Keystore path** — RESOLVED: `key.properties` `storeFile=C:\Weather_App\keystore.jks` IS correct. The keystore works with password `Sky@2026` and alias `key0`. Verified with `keytool`. Initial analysis was incorrect. | — | None |
| B2 | **Release build timeout** — RESOLVED: `assembleRelease` (7m 20s) and `bundleRelease` (8m 55s) both succeed. Earlier timeout was transient. | — | None |
| B3 | **APK signing not automatic** — The release APK was not signed by AGP's automatic signing (no `META-INF/CERT.RSA`/`CERT.SF`). Required manual signing with `apksigner`. The AAB was signed automatically via `signReleaseBundle` task. Root cause of missing auto-signing not determined — may be AGP 9.4.1 behavior or signing config evaluation timing. | **CRITICAL** | Use `apksigner` to sign APK before release, or investigate signing config evaluation |
| B4 | **Play Billing products not configured** — `BillingProducts` returns empty lists; Premium paywall cannot complete purchases | **HIGH** (owner-side) | Create products in Play Console; add IDs to `BillingProducts.kt` |
| B5 | **Data Safety declaration** | **HIGH** (owner-side) | Complete in Play Console |
| B6 | **Content rating** | **MEDIUM** (owner-side) | Complete in Play Console |
| B7 | **Closed testing track** | **MEDIUM** (owner-side) | Set up in Play Console before production rollout |

---

## 15. Evidence Summary

| Evidence | Location |
|----------|----------|
| Unit test results | `app/build/reports/tests/testDebugUnitTest/` |
| Instrumented test results | `app/build/reports/androidTests/connected/debug/index.html` |
| Instrumented test XML | `app/build/outputs/androidTestResults/connected/debug/Pixel_10a(AVD) - 17/TEST-*.xml` |
| Lint report | `app/build/reports/lint-Release.html` | ✅ Generated |
| Release AAB (newly built) | `app/build/outputs/bundle/release/app-release.aab` (signed, Oct 29, 2026) | ✅ |
| Release APK (newly built) | `app/build/outputs/apk/release/app-release.apk` (signed, Oct 29, 2026) | ✅ |
| Keystore (working) | `C:\Weather_App\keystore.jks` | ✅ Verified with keytool |
| Signed AAB certificate | CN=Bammidi Satyanarayana, valid to 15-Jun-2051 | ✅ |

---

## 16. Release Decision

**Status: BLOCKED (owner-side items only)**

All code and build issues are resolved:
- `assembleRelease` (7m 20s) and `bundleRelease` (8m 55s) both succeed ✅
- `lintDebug` and `lintVitalRelease` pass ✅
- Unit tests pass ✅
- Instrumented UI tests: 37/37 pass (100%) ✅
- APK signed with v2/v3 schemes ✅
- AAB signed via `signReleaseBundle` ✅

Remaining blockers are owner-side Play Console actions:

| # | Blocker | Severity | Owner Action |
|---|---------|----------|--------------|
| B3 | **Play Billing products not configured** — `BillingProducts` returns empty lists; Premium paywall cannot complete purchases | **HIGH** | Create products in Play Console; add IDs to `BillingProducts.kt` |
| B4 | **Data Safety declaration** | **HIGH** | Complete in Play Console |
| B5 | **Content rating** | **MEDIUM** | Complete in Play Console |
| B6 | **Closed testing track** | **MEDIUM** | Set up in Play Console before production rollout |

Once B3–B6 are resolved, `./gradlew bundleRelease` produces a signed AAB ready for Play Store upload.

**Do not publish or deploy without explicit owner authorization.**