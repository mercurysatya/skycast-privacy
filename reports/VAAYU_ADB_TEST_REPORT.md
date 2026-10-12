# VAAYU ADB Test Report — Samsung SM-M315F (Android 12 / API 31)

Connection: wireless ADB, serial `adb-RZ8N935MQTF-Sty2BK._adb-tls-connect._tcp`.
APK: `app-debug.apk` installed with `adb install -r` (data-preserving): Success.
Logcats: `artifacts/logcat_coldstart.txt` (baseline), `artifacts/logcat_afterfix_coldstart.txt` (post-fix).
Method notes: `adb` is not on PATH — full SDK path used; logcat read via `shell logcat -d`
(client-side `-d` hangs over TLS); screenshots via device `screencap` + `pull`
(PowerShell `exec-out` redirection corrupts PNGs — never use it for binaries).
Rotation tests restore `user_rotation` / `accelerometer_rotation` afterwards.

## 1. Cold start — PASS
- Live weather renders (27° Clear Night, feels-like, H/L, hourly strip, alerts). 0 crashes
  (`AndroidRuntime` matches in post-fix logcat are runtime init lines only, no FATAL).
- Evidence: `screen_01_coldstart.png`, `screen_20_unlocked_check.png`, `screen_24_app.png`, `screen_27_current.png`.

## 2. Tabs — PASS
- Search, Favorites, Map render. Map alert chip no longer overlaps the compass.
- Evidence: `screen_02_search.png`, `screen_03_favorites.png`, `screen_04_map.png`.

## 3. BACK from Alerts and Detail — PASS
- Both return to the dashboard (pre-fix regression, now fixed).
- Evidence: `screen_07_after_back.png`, `screen_08_detail.png`, `screen_18_after_back.png`
  (hourly detail BACK verified 2026-10-11).

## 4. Premium from Settings — PASS (opened, BACK verified on device 2026-10-12)
- Paywall opens from Settings. Rotation persistence of Settings: PASS in run 2
  (`screen_12_settings_after_rotation.png`).
- 2026-10-11 run 3 found a real regression: BACK from Premium landed on the dashboard instead of
  Settings (`screen_15_after_back.png` = dashboard, expected Settings; `screen_16_rotated.png` =
  dashboard landscape). Root cause in `VayuApp.kt` (settings flag cleared on Premium open); fixed
  same day with the PrivacyPolicy overlay pattern (see Fix Log F10).
- 2026-10-12 on-device re-verification (SM-M315F, app debug build with fix, all taps from
  `uiautomator` node bounds — no blind coordinates): Home (`screen_31_home.png`, overflow node
  `More options` @ 2152,154) → overflow `Settings` item (@ 1940,426) → Settings
  (`screen_32_settings.png`, SkyCast Premium banner visible) → `Premium` row (@ 361,296) →
  paywall (`screen_33_premium.png`) → BACK → **Settings** (`screen_34_after_back.png`).
  PASS — BACK returns to Settings, not Home/Privacy Policy/overlay.

## 5. Premium-fix rotation + responsiveness — PASS (2026-10-12, rotation restored)
- Forced landscape (`accelerometer_rotation=0`, `user_rotation=1`): Settings renders correctly
  (`screen_35_forced_land.png`). Forced portrait (`user_rotation=0`): Settings fully rendered —
  Units, Appearance, Notifications (`screen_36_forced_port.png`).
- Responsiveness after rotation: tapped Premium row (node-verified) → paywall with plans
  (`screen_37_responsive_premium.png`) → BACK → Settings (`screen_38_final_settings.png`).
- Rotation settings restored on device (`accelerometer_rotation` verified `1`; `user_rotation` left
  at recorded original `0`). No purchase attempted. Prior lock-screen interruptions (device
  auto-locks in ~1 min; `svc power stayon true` does not stop the secure keyguard) were worked
  around by owner unlock + single scripted passes — no lock bypass attempted at any point.

## 6. Incidental observations (informational)
- Taps at x�905 hit Share, not the 3-dot overflow (overflow sits at the far right edge, x�1000).
- UMP logcat shows `Publisher misconfiguration … no form(s) configured for the input app ID …
  ca-app-pub-4212146603115396~…` (suffix redacted) — owner must configure the UMP message (see AdMob report).
- `connectedDebugAndroidTest`: NOT RUN (no instrumentation run executed).

## 7. Phase-3 smoke test (2026-10-12, fresh debug APK rebuilt from final source) — PASS
- Device re-checked with `adb devices -l` first (still connected, wireless, authorized);
  `adb install -r` Success.
- Cold start: splash → dashboard loads live weather for Current Location (27°, Clear Sky,
  feels/H/L, AI summary, rain chart, 3 alerts) — `screen_50_p3_coldstart.png`,
  `screen_51_p3_weather.png`.
- Navigation: Search (`screen_52_p3_search.png`, Google test banner renders — correct debug
  test config, weather unaffected), Favorites (`screen_53_p3_favorites.png`), Map
  (`screen_54_p3_map.png`, tiles + alerts), Home top bar (`screen_55_p3_home_top.png`),
  overflow menu (`screen_56_p3_menu.png`), Settings (`screen_57_p3_settings.png`) — all PASS.
- Settings → Premium: paywall opens (`screen_58_p3_paywall.png`) and shows the honest
  unconfigured state — "Premium purchases aren't set up yet", `Upgrade to Premium` disabled,
  no prices, no fake success. Billing unavailable/unconfigured path: PASS.
- Paywall rotation (forced landscape, then restored): PASS — content re-lays out, state kept
  (`screen_59_p3_paywall_rotation.png`).
- BACK from paywall → Settings (not Home): PASS (`screen_60_p3_back_settings.png`).
- Logcat (`artifacts/logcat_p3_filtered.txt`): 0 `FATAL EXCEPTION`, 0 `ANR in`; banner load
  does not block weather; UMP `Publisher misconfiguration … no form(s) configured` still
  present (BLOCKED — REQUIRES OWNER ACTION). No production ads clicked, no purchase attempted.

## Verdicts
- Cold start / tabs / Alerts-BACK / Detail-BACK / Settings rotation: PASS.
- Premium→BACK→Settings after F10 fix: PASS on device 2026-10-12 (node-verified taps,
  screenshots `screen_31`–`screen_38`, rotation restored, clean logcat).
- Phase-2 billing build on device 2026-10-12: cold start PASS (`screen_40_p2_coldstart.png`);
  paywall shows honest unconfigured state, Upgrade disabled (`screen_41_p2_premium.png`);
  rotation while Premium open PASS (`screen_42_p2_premium_land.png`, rotation restored);
  Premium→BACK→Settings PASS (`screen_43_p2_after_back.png`); logcat
  (`logcat_p2_filtered.txt`) has no FATAL/ANR, UMP warning still present (owner action).
  Genuine purchase/restore/ad-suppression scenarios: NOT RUN (no Play Console products).
- Phase-3 end-to-end smoke test on device 2026-10-12: PASS (screens `screen_50`–`screen_60`,
  `logcat_p3_filtered.txt`, rotation restored, 0 FATAL/ANR) — navigation, Settings→Premium→BACK,
  rotation, honest billing-unconfigured state and debug test-banner all verified on hardware.
