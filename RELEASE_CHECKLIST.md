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
| Release build | `assembleRelease` timed out at `lintVitalRelease` (10 min) — did not complete | ❌ BLOCKER |

**Note:** The release build was initiated but timed out during `lintVitalRelease` after 10 minutes. The lint vital report needs to be inspected separately. All compilation tasks (`compileReleaseKotlin`, `hiltJavaCompileRelease`, `minifyReleaseWithR8`) completed successfully before the timeout.

---

## 3. Signing Process

| Item | Detail | Status |
|------|--------|--------|
| Signing config | `signingConfigs.release` created from `key.properties` | ⚠️ Misconfigured (see below) |
| Keystore file | `key.properties` → `C:\Weather_App\keystore.jks` (root) | ❌ WRONG PATH |
| Working keystore | `app\vayu-release-key.jks` (PKCS12, alias `key0`) | ✅ Exists & valid |
| Keystore password | `Sky@2026` | ✅ Works for `app\vayu-release-key.jks` |
| Root keystore password | `Sky@2026` | ❌ INCORRECT — keytool rejected it |
| Key alias | `key0` | ✅ |
| Certificate validity | 21-Jun-2026 → 15-Jun-2051 (25 years) | ✅ |
| Certificate subject | C=IN, ST=Andhra Pradesh, L=Srikakulam, O=Mercury Computer Mart, CN=Bammidi Satyanarayana | ✅ |
| Existing signed AAB | `app/release/app-release.aab` (July 14, 2026) — signed with same cert | ✅ |
| `hasSigningConfig` check | Requires `storePassword` non-empty and not "CHANGE_ME" | ✅ (password is set) |

**CRITICAL BLOCKER:** `key.properties` specifies `storeFile=C:\\Weather_App\\keystore.jks` (root level), but:
- The root `keystore.jks` rejects password `Sky@2026` (keytool error: "keystore password was incorrect")
- The working keystore is `app\vayu-release-key.jks` which accepts `Sky@2026` and contains alias `key0`

**Fix required:** Update `key.properties` `storeFile` to point to `app\vayu-release-key.jks` (or copy the working keystore to the root path). Until fixed, `assembleRelease` without `-PallowTestAdmobIds` will either use the wrong keystore or fail signing.

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
| Release build (`assembleRelease`) | ❌ TIMED OUT | Timed out at `lintVitalRelease` after 10 min. Compilation + R8 minification succeeded before timeout. |
| Release signing | ❌ NOT VERIFIED | Keystore path mismatch prevents verification |

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
| B1 | **Keystore path mismatch**: `key.properties` `storeFile` points to `C:\Weather_App\keystore.jks` (wrong password). Working keystore is `app\vayu-release-key.jks`. | **CRITICAL** | Update `key.properties` `storeFile` to `app\vayu-release-key.jks` or fix root keystore password |
| B2 | **Release build timeout**: `assembleRelease` times out at `lintVitalRelease` | **HIGH** | Investigate lint vital errors; may need to increase timeout or fix lint issues |
| B3 | **Play Billing products not configured**: `BillingProducts` returns empty lists; Premium paywall cannot complete purchases | **HIGH** (owner-side) | Create products in Play Console; add IDs to `BillingProducts.kt` |
| B4 | **Data Safety declaration** | **HIGH** (owner-side) | Complete in Play Console |
| B5 | **Content rating** | **MEDIUM** (owner-side) | Complete in Play Console |
| B6 | **Closed testing track** | **MEDIUM** (owner-side) | Set up in Play Console before production rollout |

---

## 15. Evidence Summary

| Evidence | Location |
|----------|----------|
| Unit test results | `app/build/reports/tests/testDebugUnitTest/` |
| Instrumented test results | `app/build/reports/androidTests/connected/debug/index.html` |
| Instrumented test XML | `app/build/outputs/androidTestResults/connected/debug/Pixel_10a(AVD) - 17/TEST-*.xml` |
| Lint report | `app/build/reports/lint-Release.html` (not generated due to timeout) |
| Release AAB (existing) | `app/release/app-release.aab` (signed, July 14, 2026) |
| Keystore (working) | `app\vayu-release-key.jks` |
| Signed AAB certificate | CN=Bammidi Satyanarayana, valid to 15-Jun-2051 |
| Release build log | `build_release.log` (partial — timed out) |

---

## 16. Release Decision

**Status: BLOCKED**

The app cannot be released to production until:
1. **B1** (keystore path) is fixed — without this, release APK/AAB cannot be properly signed
2. **B2** (release build timeout) is investigated — lintVitalRelease must complete
3. **B3** (Billing products) is configured by the owner in Play Console

Once B1 and B2 are resolved, `./gradlew bundleRelease` should produce a signed AAB. B3–B6 are owner-side Play Console actions that do not block the build but block production release.

**Do not publish or deploy without explicit owner authorization.**