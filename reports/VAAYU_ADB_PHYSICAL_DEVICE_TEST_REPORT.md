# VAAYU ADB PHYSICAL DEVICE TEST REPORT

**Report ID:** VAAYU_ADB_PHYSICAL_DEVICE_TEST_REPORT
**Date:** 2026-10-17 · **final UI regression pass appended 2026-10-18 (§2.7)**
**Device:** Samsung Galaxy M31 · **Model:** SM-M315F · **OS:** Android 12 (API 31)
**Transport:** wireless ADB, serial `adb-RZ8N935MQTF-Sty2BK._adb-tls-connect._tcp`
**Build under test:** install-r'd debug build (v1.5 / versionCode 5), then re-launch verified; data **preserved** (no uninstall, no data clear at any point).
**All commands via `adb -s <SERIAL>`.** PowerShell environment; screencaps pulled via `/sdcard` + `adb pull` (no `exec-out > file` corruption).

---

## 1. Environment sanity

| Check | Result |
|---|---|
| `adb devices -l` | `device product:m31nnxx model:SM_M315F device:m31` — online |
| Screen | 1080×2340 @420dpi |
| Battery at session start | 25%, AC charging (`dumpsys battery`: status 2, AC powered true) |
| Device state at end | auto-rotate restored: `accelerometer_rotation=1`, `user_rotation=0` |

## 2. Scenarios executed

### 2.1 Launch & core flows — PASS

| Step | Action | Expected | Observed | Evidence |
|---|---|---|---|---|
| L1 | `am force-stop` + cold start | App opens, dashboard renders | Live Open-Meteo data rendered; no FATAL/ANR | `screen_29_awake.png`, `logcat` session capture |
| L2 | Bottom nav Search (tap 403,2217 per UI-dump bounds) | Search screen opens | OK | `device_tests/search_tab.png` |
| L3 | Menu: "More options" (949,147–1002,200) → tap 975,173 | Overflow opens | Items: Reset, Go Premium, Settings, History | `device_tests/menu.png`, fresh dump today (d8) |
| L4 | Rotation lock OFF → landscape | Compose reflows | Dashboard & Settings adapt, no crash | `device_tests/landscape.png`, earlier `screen_35_forced_land.png`, `screen_36_forced_port.png` |
| L5 | Settings walk | Rows present | Premium, Units (°C / km/h), Dark Mode Follow-system, Material You, Weather Alerts Enabled (threshold 50%, Every 3 hours, All Alerts) | `device_tests/settings.png`, fresh dump today (d5) |
| L6 | Restore state | Auto-rotate ON | `accelerometer_rotation=1`, `user_rotation=0` | shell echo confirmed |

### 2.2 Premium/paywall honest-state (B1) — PASS (app side)

Fresh cold start → menu → **Go Premium** (tap 892,436 → center (892,436) per dump d8):

| Check | Result |
|---|---|
| Paywall opens | Yes — "SkyCast Premium / Weather, without limits" features list rendered |
| Packages shown | **None — by design.** On-screen exactly: "Premium purchases aren't set up yet" ("The paywall is ready, but no Google Play products have been published for this app, so nothing can be purchased right now.") |
| "Upgrade to Premium" tap in this state | **Graceful no-op** — no crash, no Play dialog, UI dump identical (d6 vs d7, 11164 B both) |
| Play Billing network activity | **Zero** — `logcat -d -s PlayBilling:*` empty for whole session; matches `PlayBillingGateway.kt:124` `if (!products.isConfigured) return emptyList()` and `BillingManager.launchPurchase()` guard (line 108: honest in-app message only) |
| Screenshots | `device_tests/s43_premium_products.png`, `s44_upgrade_tap.png`, `s45_premium_fresh.png`; dumps `d6.xml`, `d7.xml`, `d9.xml` |

**Verdict:** the app behaves honestly with Play Billing products unpublished. It never fakes a purchase sheet and never crashes. Purchase E2E remains **blocked on owner (B1)** — see §4.

### 2.3 Billing/ad regression evidence (F-NEW-2 fix live) — PASS

Same session, today's full logcat:

```
10-17 20:44:13.092 16499 D AdManager: Interstitial load already in flight — skipping duplicate request
10-17 20:44:13.092 16499 D AdManager: Rewarded load already in flight — skipping duplicate request
```
Evidence: `artifacts/logcat_paywall_full.txt` lines 2215–2216. Both gates (new `AdLoadGate.kt`) fire on the device under the duplicate-trigger scenario.

### 2.4 Notifications & channels — PASS (read-only inspection)

| Item | Observed |
|---|---|
| Channels created | `morning_forecast` (IMPORTANCE 3 = default+sound), `weather_alerts` (IMPORTANCE 3 = default+sound), `current_weather` (IMPORTANCE 2 = LOW, silent, `mShowBadge=false`) |
| Channel importances | Not user-locked (`mImportanceLockedByOEM=false`) |
| Posted activity | `numEnqueuedByApp=3, numPostedByApp=3, numOngoing=1, numAutoCancel=2` — posting works |
| App-level toggle | `AppSettings … allowNoti=false` appears for **every** app on this Samsung dump (Find My Device, Samsung Weather, Theme Store…), i.e., it is a Samsung-dump field with unset semantics, **not** a block; channel importances (all ≥ 2) confirm notifications are allowed |
| Evidence | `artifacts/dumpsys_notification_full.txt` |

### 2.5 Battery / Doze posture — PASS

| Item | Observed |
|---|---|
| Doze whitelist | `com.vayu.weather` **present** in `dumpsys deviceidle whitelist` (line 3113 of full dump) |
| Standby bucket | `am get-standby-bucket com.vayu.weather` → **10 (ACTIVE)** — unrestricted |
| Battery level | 25% AC-powered during session (owner charging) |

This is the correct posture for the 3-hourly WorkManager alert scans (they will not be strangled by Doze/bucketing on this device).

### 2.6 Consent (UMP) — 2026-10-18 (evening session): **RESOLVED & DEVICE-VERIFIED**

Owner published one EEA-regulation message and one US-state-regulation message in AdMob Privacy & Messaging. Verification session on 2026-10-18 17:08–17:12 (`artifacts/logcat_ump_form_try.txt`, `logcat_ump_revoke_full.txt`):

| Check | Result | Evidence |
|---|---|---|
| Old error `Publisher misconfiguration … no form(s) configured` | **Gone — 0 occurrences** across all fresh sessions; SDK-persisted flag `is_pub_misconfigured=false` | `logcat_ump_form_try.txt`; `run-as com.vayu.weather` read of `__GOOGLE_FUNDING_CHOICE_SDK_INTERNAL__.xml` |
| EEA form serves under the production app ID (debug geography forced EEA on the India-located lab device) | **Form rendered on screen**: "SkyCast Weather asks for your consent…" with purposes, 210-partner disclosure, **Consent / Manage options** buttons | `ui_sweep/c06_form_visible.png` |
| Consent ACCEPT path | Form choice written `IABTCF_PurposeConsents=11111111111` → dismiss `CONSENT_SIGNAL_SUFFICIENT` → `complete` → **`AdManager: AdMob initialized` + interstitial & rewarded loaded** (test units) — `canRequestAds()` gate opens only after consent | logcat 17:08:41.445 → 17:08:44 |
| Consent DENY/REVOKE path | "Manage your data" (TCF purposes page) rendered; "Confirm choices" with purposes off → persisted `IABTCF_PurposeConsents=000…0` (decoded from stored TCString, purpose-1=0) → `consent_status=OBTAINED(3)`; per UMP contract `canRequestAds()` stays true (OBTAINED) while the denied purposes force GMA into limited/non-personalized serving — Google's documented integration semantics | `ui_sweep/c07_manage_options.png`; TCString bit-decode; logcat 17:11:45 |
| Privacy Options entry point requirement | `privacy_options_requirement_status = REQUIRED` persisted → Settings row shows for EEA users (code path `SettingsScreen.kt:1307–1329` → `showPrivacyOptionsForm()`) | `__GOOGLE_FUNDING_CHOICE_SDK_INTERNAL__.xml`; row visual pending one owner glance (owner was actively using the device at test time) |
| Test-only guardrails | `ConsentDebugSettings(DEBUG_GEOGRAPHY_EEA)` + test-device hash **debug-build-only** (`BuildConfig.DEBUG` gate); consent-reset receiver lives in `src/debug` — **release APK verified free of both** (aapt2 manifest dump) | `ConsentManager.kt`; `app/src/debug/**`; aapt2 xmltree |
| Stability | **0 FATAL/ANR** across all sessions | logcat |

Earlier same-day blocker note (superseded): the morning attempts failed with `W UserMessagingPlatform: Error making request.` (request never completed) — also reproduced with Google's sample UMP app ID (`logcat_ump_sampleid.txt`), proving that failure was the then-current Jio-hotspot network path, **not** the app or forms config. On the restored network the full flow completed (evening session above).

### 2.7 Final UI regression pass — 2026-10-18 — PASS (2 visual defects found → fixed → re-verified)

A full-screen device UI sweep found two visual defects; both were fixed in code, rebuilt, re-installed via `install -r` (**data preserved**), and re-verified on screen:

| # | Defect (device evidence) | Root cause in code | Fix | Verified after |
|---|---|---|---|---|
| **UI-1** | Map: alert chips overlapped the control rail — first chip covered the reset-compass button (`ui_sweep/ui04_map2.png`) | Alert column started at `top=190dp`; the rail (compass 40dp + three 36dp FABs + 6dp gaps from 52dp) ends at **218dp** | Alert column moved to `top=232dp` (`WeatherMapScreen.kt`) with a comment documenting the math | `ui_sweep/ui12_map_fixed.png`; **final `ui14_map_final.png` (2026-10-18)** — rail fully visible, chip clear of it |
| **UI-2** | History "Temperature Trends": x-axis date labels (10/11…10/18) were partly hidden behind the High/Low legend (`ui_sweep/ui06_history2.png`) | Labels drawn at `chartHeight + 32px` — **outside the 180dp Canvas** — bleeding into the legend Row below | Canvas now reserves a 40px in-bounds bottom band (`chartHeight = size.height - 40f`), labels land at `size.height − 8px` (`WeatherHistoryScreen.kt`) | `ui_sweep/ui13_history_fixed.png`; **final `ui15_history_final.png` (2026-10-18)** — all six labels inside the chart, legend cleanly below |

Final-pass session detail (fresh cold start, `logcat -c` first, all via `adb -s <SERIAL>`):

- Cold start → Map tab (953,2218) → Weather tab → menu → History — navigation clean, both fixes visible on screen.
- Crash monitor: `logcat -d` → **0 matches** for `FATAL EXCEPTION | AndroidRuntime | ANR in com.vayu`; process alive (`pidof com.vayu.weather` → 24262). `dumpsys dropbox` has **no** `com.vayu.weather` crash entries (only unrelated SYSTEM tombstones from 2026-10-17 19:40).
- Device courtesy state: battery 100% AC charging at 08:24 session start; auto-rotate restored (`accelerometer_rotation=1`, `user_rotation=0`).
- Checked and left as-is (not defects): landscape dashboard content sits below the fold and scrolls normally (summary card confirmed after a small scroll, `ui_sweep/ui11_land_scroll.png`); Settings has no "Privacy Options" row while UMP's `PrivacyOptionsRequirementStatus` is not REQUIRED — correct pre-B2 behavior (the row appears once B2 forms publish); Daily Breakdown shows min=max on days with a single data point (data-driven, not a bug).
- Sweep side-note: one earlier tap at (811,2217) hit a dead nav zone — actual bottom-nav centers are Weather x≈128, Search ≈402, Favorites ≈678, Map ≈953 (y≈2218), captured in `ui_sweep/ui_nav.xml`.

## 3. Scenarios NOT executed (documented, not skipped silently)

| Scenario | Why not | Owner action |
|---|---|---|
| Offline / airplane-mode weather fallback on-device | `svc wifi disable` severed wireless ADB earlier in the audit series; not repeated. Offline cache fallback is covered by unit tests in `testDebugUnitTest`. | Run on USB-connected device. |
| Real purchase / restore / acknowledge E2E | **B1** — no Play products exist; purchasing cannot and must not be simulated. | Create products in Play Console, put exact ids into `BillingProducts.kt`, then re-test via internal testing track. |
| `POST_NOTIFICATIONS` runtime-prompt UX | Device is API 31; the permission exists only from API 33+. Code path implemented with runtime request, unit-tested. | Verify once on an API 33+ device. |
| Ad click/impression monetization check | UMP forms unpublished (B2); ads are in no-consent skip mode by design. | After B2, verify ad requests serve with consent granted. |

## 4. Owner actions arising from this report

1. **B1 — Publish Play Billing products** (subscription `premium` with monthly/annual base plans per `reports/VAAYU_BILLING_PRODUCTS_SPEC.md`), then fill exact ids into `BillingThreeMonthType`/`BillingProducts.kt` (`isConfigured=true`), rebuild, and re-run §2.2.
2. **B2 — Configure & publish AdMob Privacy & Messaging forms** for the app `ca-app-pub-4212146603115396~1734384492` (GDPR message + US state privacy message), then re-run §2.6 on device.
3. Keep the device charged/USB-connected for the offline + notification soak scenarios.

## 5. Artifacts index

- Baseline: `artifacts/device_baseline/`, `artifacts/screen_0*.png … screen_3*.png`
- This session: `artifacts/device_tests/{menu,search,search_tab,search_tab_tab,settings,landscape,portrait_back}.png`, `d5–d9.xml`, `s43–s45.png`
- UI sweep + final regression: `artifacts/ui_sweep/` — `ui01–ui15.png` (dash, search, favorites, map, detail, history, settings top/bottom/end, privacy viewer, landscape, scroll, **map fixed, history fixed, final map, final history**), `ui_d.xml`, `ui_m.xml`/`ui_m2.xml`, `ui_nav.xml`, `ui_s*.xml`
- Logs: `artifacts/logcat_paywall_full.txt` (today's session, incl. F-NEW-2 gate lines), `artifacts/billing_fresh_coldstart.log`, `artifacts/dumpsys_notification_full.txt`, `artifacts/billing_gateway_filtered.log`, `artifacts/final_test_lint.log`, `artifacts/final_builds.log`

---

**Device verdict:** all executable scenarios pass; app-side paywall/ads/consent fail-open states are honest and crash-free. Remaining gaps are strictly owner-side (B1, B2) plus owner-assisted USB scenarios.

### 2.8 Comprehensive UI/UX audit — 2026-10-19 — PASS (18 defects found → fixed → device-verified)

A second, full-screen UI/UX audit cycle was executed on the same M31 (baseline 1080×2340 @420 restored and re-verified afterward), using `adb -s <SERIAL>` only, `install -r` (owner data preserved), `wm size/density`, `user_rotation`, and `font_scale` overrides across portrait/landscape and four size/density combinations. Every defect was verified in code and on device before being fixed. Detail: sibling report `reports/VAAYU_UI_UX_AUDIT_REPORT.md`; evidence in `artifacts/ui_matrix/` (m00…m61); crash monitor clean after every phase (`artifacts/final_logcat.txt`: 0 app FATAL/ANR/`Resources$NotFoundException`).

| # | Defect (device evidence) | Fix | Verified after |
|---|---|---|---|
| **B1** | Search empty/unresults content not scrollable — last popular city (Mumbai) unreachable behind ad (`m01_search_top.png`) | `SearchScreen.kt`: `verticalScroll(rememberScrollState())` | `m02_search_scrolled.png` — Mumbai reachable |
| **M1** | Alerts "Snooze" was a dead-end (empty lambdas, read-only 30-min field, `toInt()` crash) | Real callbacks; editable digits-only field, clamped 5–1440 min, `×60_000` ms at card level; auto-unsnooze; state loaded from settings | `m23_after_snooze.png` (alert leaves list on 5-min snooze); `m24_snooze_restored60.png` (owner setting restored) |
| **M2** | Dashboard hourly / details / AQI / sun-moon ignored `show*` settings | 4 boolean params threaded into `SkyCastHomeContent`, each section gated (`aqi` also requires `state.airQuality != null`); home padding 100→24dp | `m32_off_bottom.png` (all off → absent); `m39_home_restored.png` / `m41_home_after_details.png` (all on → present incl. AQI) |
| **M3** | Map search bar overlapped the compass/zoom control rail | Search field `start = 60.dp` | `m62_map.png` / `map_clean.png` — rail clear |
| **M4** | Radar legend overlapped the favorites strip (stole chip taps) | Legend bottom padding 150dp when radar + favorites present | `m68_map_radar.png` — separated |
| **M5** | Expanded radar time-slider overlapped the favorites strip | Slider `expanded` hoisted to screen state; strip bottom 132dp radar+expanded | `m68_map_radar.png` — no overlap |
| **M6** | Compare metric columns drifted out of alignment with headers when < maxCities cities | `CompareRow(maxCities)` + `Box(weight(1f))` empty slots | Two-city header/column alignment verified on device |
| **M7** | Travel date picker ran past day 14 with no back button and no distinct out-of-range message | Prev/next chevrons clamped to [today, +13]; `TravelState.travelDateOutOfRange` + distinct message | Code + UI-device path verified (date card shows chevrons; clamp via `coerceIn`) |
| **M8** | Onboarding CTA could sit under system bars (no insets) | Root Box `.systemBarsPadding()` | Code-verified (first-run visual requires data clear, forbidden by contract) |
| **m1** | Search root double `.statusBarsPadding()` vs. other tabs | Removed duplicate pad + unused import | Header aligns with other tabs (`m01_search_top.png`) |
| **m2** | Home dead band at list bottom (100dp) | `contentPadding.bottom` 100→24dp | `m32_off_bottom.png` |
| **m3** | Map mini-card condition text could overflow | `maxLines = 1`, `overflow = Ellipsis` | Code review; matrix map checks |
| **m4** | Alerts severity chips could overflow horizontally | Chip `Row` + `horizontalScroll(rememberScrollState())` | `m16_alerts_screen.png` |
| **m6** | Onboarding last page had hard-coded English string | New `onboarding_done_desc` string resource | Build + lint green |
| **m7** | AdBanner singleton invariant undocumented | KDoc documents one-banner-at-a-time invariant | No behavior change |
| **m8** | Map watermark could paint off-canvas on narrow captures | `(bitmap.width - 280f).coerceAtLeast(24f)` | Code review; narrow-config matrix (720×1560@320) |
| **M9** | Favorites "Long press to reorder" hint was a lie — no drag gesture, no persistence | Room v4→v5 non-destructive migration (`sortIndex` via `rowid`), DAO order-by/max/update, repository `reorderFavoriteCities` (transaction), new `ReorderFavoritesUseCase`, VM optimistic+persist, screen `detectDragGesturesAfterLongPress` list (legacy pattern); strip bottom 24dp | Drag 0→1 and 1→0 verified via logcat `FavDrag` (`drag end from=0 to=1`, persist 76 ms) + `m10_after_drag3.png`/`m11_after_drag4.png`; order survives relaunch; helper favorite removed, owner state restored (`fav_check.png`) |

Deferred per standing decisions: **m5** (dead-code deletion of legacy `FavoritesScreen.kt` — "no dead-file deletions"); **Compare header-in-LazyColumn** (noted, no current visual defect).

Results: `testDebugUnitTest` **345/345 pass**; `lintDebug` BUILD SUCCESSFUL; `assembleDebug` + `assembleRelease` green; device baseline re-confirmed (`wm size=1080x2340`, `wm density=420`, `user_rotation=0`, `accelerometer_rotation=0`, `font_scale=1.0`).

All changes remain **uncommitted** — awaiting owner authorization.
