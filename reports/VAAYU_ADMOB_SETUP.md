# VAAYU AdMob Setup — review and owner actions

No production ad IDs appear in this report. ID values live only in gitignored `local.properties`.

## 1. Integration (verified by inspection + device logcat)

| Item | Status |
|---|---|
| play-services-ads 25.4.0, UMP 4.0.0 | Integrated (`gradle/libs.versions.toml`, `app/build.gradle.kts:209`) |
| App ID wiring | `manifestPlaceholders["ADMOB_APP_ID"]` + `buildConfigField`s; resolved `gradle.properties` → `local.properties` → Google test defaults (`app/build.gradle.kts:21-29`). Production IDs present in `local.properties` (release manifest + BuildConfig byte-verified against it 2026-10-12, values not recorded here); debug build forces Google's official test unit IDs for banner/interstitial/rewarded (`:100-102`). PASS |
| Banner | Singleton via `AdManager`, `consentVersion`-gated `AdBanner`. PASS (review) |
| Interstitial | REMOVED from Travel/Compare entry 2026-10-12 (conservative weather-app default; no evidence the gate was appropriate). `AdManager` caps/preload infra retained for future policy-compliant placements only. No interstitial at startup/refresh/detail-open/exit (verified) |
| Rewarded | Kept: genuine user-initiated use case (unlock extended 48-hour outlook), clear value exchange, not-ready toast + preload. PASS (review) |
| Consent | `ConsentManager.gatherConsent()` before SDK init; banner waits on consent version |

## 2. BLOCKER — UMP form not configured (BLOCKED — REQUIRES OWNER ACTION)
- Device logcat (`artifacts/logcat_coldstart.txt:2093-2094`, reproduced again in
  `artifacts/logcat_p3_filtered.txt` on 2026-10-12): UMP warns `Publisher misconfiguration:
  Failed to read publisher's account configuration; no form(s) configured for the input app ID …
  ca-app-pub-4212146603115396~…` (suffix redacted here; full value in local logcat only).
  The app does not crash when consent refresh fails (verified in Phase-3 smoke test).
- Action: in the AdMob console, create/attach a UMP consent message for this app ID (EU/EEA + rest of
  world as needed), then cold-start the app on a test device and confirm the form can display and
  `ConsentManager.canRequestAds()` gates correctly. Until then, EEA consent cannot complete.

## 3. Pre-launch ad checks for the owner
1. Replace nothing in code — production IDs already resolve from `local.properties` for release builds.
   Guard (hardened 2026-10-12, F15): the release build fails closed if **any** of the four ad fields
   (manifest App ID, banner, interstitial, rewarded) carries Google's `ca-app-pub-3940256099942544…`
   test prefix, and the check now runs on `assemble*Release` **and** `bundle*Release` tasks
   (bypass only via explicit `-PallowTestAdmobIds=true`). Verified on the built artifact:
   release manifest App ID + all three release BuildConfig unit IDs match `local.properties`,
   zero test IDs present, zero hardcoded ad IDs in `app/src`.
2. Travel/Compare interstitials were removed from code (F12) — nothing to configure; the
   owner should keep them removed unless a policy-compliant placement is later justified.
3. Test with Google test-device IDs on a real device before release; keep `allowTestAdmobIds` OFF for
   any store-bound build.
4. Re-test banner anchoring on small screens and landscape after the `AdBanner` rewrite.

## 4. Commands (PowerShell, from `C:\Weather_App`)
- `.\gradlew.bat assembleDebug` — debug build uses Google test IDs.
- `.\gradlew.bat assembleRelease bundleRelease` — release resolves production IDs from `local.properties`.

## 5. Ad placements audited 2026-10-12 (all call sites)

- Banners (7, all singleton `AdView`, consent-gated, premium-suppressed): dashboard
  `WeatherDashboard.kt:545`, Search `:184`, Detail `:147`, Compare `:97`, Travel `:105`,
  History `:212`, Settings `:1414`. None on Map/Home-shell/Alerts/Premium. No recomposition
  reload (singleton + `consentVersion` recompose trigger). No live-ad clicks in tests.
- Interstitial: entry gates on Travel and Compare (`VayuApp.kt`) REMOVED 2026-10-12 —
  conservative weather-app default, no evidence the placement was appropriate. Startup,
  refresh, forecast-detail open, and exit paths verified ad-free.
- Rewarded: kept — genuine user-initiated use case (unlock extended 48-hour outlook),
  clear value exchange, not-ready toast + preload.
