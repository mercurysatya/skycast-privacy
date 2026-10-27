# VAAYU SECURITY & PERFORMANCE AUDIT — SkyCast Weather

**Report ID:** VAAYU_SECURITY_PERFORMANCE_AUDIT
**Date:** 2026-10-17
**Scope:** secrets & repository hygiene, network security, backup/extraction policy, logging of sensitive data, exported surface, R8/minification, artifact inspection, and performance posture (startup, caching, scheduling).
**Method:** static source inspection + `git ls-files` scans + `aapt2 dump` on the signed release APK + device-side dumps where relevant.

---

## A. SECURITY

### A1. Secrets & repository hygiene — **PASS**

| Check | Method | Result |
|---|---|---|
| Signing material not tracked | `git ls-files` (246 tracked files) | No `*.jks`, `key.properties`, `local.properties`, `google-services.json`, `*.pem`, `*.p8` tracked |
| `gradle.properties` contains no secrets | `Select-String ADMOB\|KEY\|SECRET\|PASSWORD\|TOKEN` on tracked copy | No matches |
| Production AdMob ids injected only at build time | `app/build.gradle.kts` | Resolution order property → `local.properties` (gitignored) → Google **test** defaults; a release build **fails hard** if test IDs slip into a release (guard in `buildTypes.release`) |
| `key.properties` / `keystore.jks` | local only | exist locally (release signing verified with apksigner); untracked ✔ |
| `app/google-services.json` | untracked | exists locally; untracked ✔ (note: fresh clones need a copy or the Google services plugin fails — build hygiene, not security; recorded as QA-A3) |

### A2. Network security — **PASS**

`app/src/main/res/xml/network_security_config.xml` (sole config in the tree — debug override **removed** in an earlier owner hardening edit, preserved during this audit):
```xml
<base-config cleartextTrafficPermitted="false">
    <trust-anchors><certificates src="system"/></trust-anchors>
</base-config>
```
- Cleartext: **denied globally**; no per-domain exceptions; no `debug-overrides` trust anchors (user profiles cannot silently MITM debug builds via user CAs).
- Merged release manifest confirmed: `networkSecurityConfig=@xml/…` present, **no** `usesCleartextTraffic` override, **no** `debuggable` attribute in `app-release.apk` (aapt2 xmltree).

### A3. Data storage, backup & extraction policy — **PASS**

- `allowBackup=true` but strictly scoped:
  - `fullBackupContent`: `<include domain="file" path="datastore/settings_prefs.preferences_pb"/>` — **only** settings DataStore is backed up; widget snapshot prefs and databases excluded.
  - `dataExtractionRules` (Android 12+): `weather_widget_prefs.xml` and `database` path `.` excluded from **cloud-backup** and **device-transfer**.
- Databases/DataStore are app-private (`deviceProtected` storage not needed).
- **Net effect:** backups do not leak forecast history/app caches; only user-visible settings travel. Good practice.

### A4. Logging of sensitive data — **PASS**

- `OkHttp HttpLoggingInterceptor`: `Level.BODY` in debug, **`Level.NONE` in release** (`NetworkModule.kt:29–34`).
- No `Log.*`/`Timber` statement prints coordinates, tokens, or user identifiers anywhere in `app/src/main` (targeted greps for lat/lon/location tokens found only non-PII operational logs: `Location unavailable, retrying`, `Failed to get location`, ad lifecycle messages, `MobileAds initialization failed`). AdManager logs contain ad lifecycle only.
- Device log capture across the whole test session showed no request/response bodies (release build = NONE level).

### A5. Exported surface & components — **PASS**

Merged release manifest: exactly two exported components:
1. `MainActivity` (LAUNCHER — required).
2. `WeatherWidgetReceiver` (`APPWIDGET_UPDATE` intent-filter — required by platform rules for widget updates).
Providers (`androidx-startup`, `FileProvider`) are `exported=false` with scoped `file_paths.xml` (`cache/privacy`, `cache/` share — cache-only, no external storage exposure). No services with remote interfaces.

### A6. Billing integrity — **PASS** (previously audited; unchanged)

`BillingEntitlementResolver` grants entitlement only from `queryPurchasesResult()`, treats **suspended** subscriptions as non-entitled (matches Billing 8.1.0+ semantics per official notes), never from local cache alone; acknowledgment with retry on next refresh; no purchase simulation path exists (device-verified honest no-op).

### A7. Minor security observations — **ADVISORY**

| ID | Level | Observation | Recommendation |
|---|---|---|---|
| SEC-A1 | Info | `'arm64-v8a','armeabi-v7a','x86','x86_64'` universal ABI set + `useLegacyPackaging=true` (manifest) | Acceptable for Store AAB (device-filtered); legacy packaging marginally enlarges install size — consider disabling if MapLibre native loading tolerates it (test on device first). |
| SEC-A2 | Info | `minSdk 24` — overall sensible; Android 7.1 era devices get system-only trust anchors (good). | Keep. |

## B. PERFORMANCE

### B1. Artifact metrics (this cycle's release build)

| Metric | Value |
|---|---|
| `app-release.apk` (universal) | 24,047,844 B (≈22.9 MB) |
| `app-release.aab` | 33,551,150 B (source-of-truth delivery artifact; per-device downloads smaller than APK after splits) |
| R8 minify + resource shrink | **enabled** for release |
| ABI | 4 (`arm64-v8a` primary on shipped devices; x86 family kept for emulator) |
| Native code | MapLibre OpenGL stack (present) — Vulkan flag disabled via manifest meta-data |

Rebuilt **2026-10-18** after the final UI regression (`artifacts/final_builds.log`): `app-release.apk` **24,047,724 B** / `app-release.aab` **33,550,815 B** (debug APK 50,404,931 B; apksigner V2 re-verified, same cert). The two UI fixes (C7/C8) are Compose layout/Canvas-only changes — no security or performance surface change; `lintDebug` re-ran at **0 errors, 0 warnings, 1 hint**.

### B2. Runtime posture (unit-tested / code-verified)

| Area | Finding |
|---|---|
| OkHttp | 15s connect / 20s read+write / 30s call timeouts; 10 MB shared HTTP cache with 10-minute `maxAge` network-interceptor rewrite — reduces redundant Open-Meteo traffic (main API has no cache headers) |
| Widget updates | `updatePeriodMillis=3600000` (1h) + Glance/WorkManager — bounded CPU/battery cost; app is Doze-whitelisted on the test device so the 3-hourly alert scans proceed (bucket ACTIVE verified on device) |
| Coroutines/gateways | All network I/O suspended off the main thread; billing queries time-bounded (`QUERY_TIMEOUT_MS`); loads idempotent after F-NEW-2 gate |
| Startup | Splashscreen API used; debug baseline previously measured cold start ≈ 2.5–5.8 s on the M31 (Quad-A3-class) |

### B3. Advisory performance recommendations (non-blocking)

| ID | Priority | Recommendation | Rationale |
|---|---|---|---|
| PERF-A1 | **High value** | Add a **Baseline Profile** (`androidx.baselineprofile` + macrobenchmark) | No `baseline-prof*` exists; typical ~20–30% cold-start and navigation-flavor gains for Compose apps. |
| PERF-A2 | Medium | Slim conservative R8 keeps (`com.google.android.gms.ads.** { *; }`, `org.maplibre.android.** { *; }`, `kotlinx.coroutines.**` keepclassmembers) | Each is a broad reflection keep; narrowing after re-verifying release smoke reduces dex and APK size. |
| PERF-A3 | Medium | Consider trimming the `logging-interceptor` release path (interceptor already NONE in release) | Marginal; keeps behavior identical. |
| PERF-A4 | Low | Universal universal-ABI set in APK dev builds is fine; for leaner local APK distribution consider `splits.abi` (AAB path already optimal) | Optional. |
| PERF-A5 | Low | Trim disabled-lint list or flip `abortOnError=true` post-triage (QA-A1) | Gates future regressions cheaply. |
### B4. Not executed (owner-assisted)

- On-device startup/streaming benchmarking under airplane mode requires USB (wireless-ADB limitation documented in the ADB report). The 2026-10-14 baseline capture remains cited evidence.

---

## Verdict

**Security: no vulnerabilities found at the auditable layer** — TLS-everywhere, no cleartext, no secrets in VCS, minimal exported surface, non-PII logging, scoped backups, honest billing. **Performance: release minified and cache-disciplined; the only material opportunity is a Baseline Profile (PERF-A1).**
