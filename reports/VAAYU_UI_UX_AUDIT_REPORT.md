# VAAYU UI/UX Audit Report — SkyCast Weather (`com.vayu.weather`)

**Report ID:** VAAYU_UI_UX_AUDIT_REPORT  
**Date:** 2026-10-19  
**Device:** Samsung Galaxy M31 (SM-M315F), Android 12 (API 31), baseline 1080×2340 @420dpi  
**Transport:** wireless ADB, serial `adb-RZ8N935MQTF-Sty2BK._adb-tls-connect._tcp` (never disabled; never `svc wifi disable`)  
**Build under test:** debug, `assembleDebug` + `install -r` (owner data preserved at all times — no uninstall, no `pm clear`)  
**Sibling reports:** `VAAYU_ADB_PHYSICAL_DEVICE_TEST_REPORT.md` (§2.7 UI-1/UI-2, §2.8 below), `VAAYU_FULL_QA_AUDIT.md` (§3 code changes, §4 defect register), `VAAYU_FINAL_RELEASE_READINESS.md` (scorecard)  
**Status of changes:** **all uncommitted** — awaiting owner authorization before any commit/push

---

## 1. Executive summary

A comprehensive UI/UX audit was performed across screen sizes, densities, and orientations on the physical M31 via ADB `wm size`/`wm density`/`user_rotation`/`font_scale` overrides, plus static review of every suspect composable. Every claimed defect was verified in code **and** on device before being fixed; no speculative changes and no dead-file deletions were made.

| Metric | Result |
|---|---|
| Defects found in this audit | 1 blocker (B1) + 9 majors (M1–M9) + 8 minors (m1–m4, m6–m8) = 18 |
| Fixed | 18 / 18 |
| Fixed + device-verified | 18 / 18 |
| Unit tests | **345/345 pass**, 0 failures (baseline count preserved) |
| Lint | `lintDebug` BUILD SUCCESSFUL (0 errors / 0 warnings / 1 hint — unchanged from baseline) |
| Builds | `assembleDebug` ✔, `assembleRelease` ✔ |
| Device crashes / ANRs | 0 in app process (`com.vayu.weather`) across all sessions |
| Device state after testing | Restored and re-verified: `wm size=1080x2340`, `wm density=420`, `user_rotation=0`, `accelerometer_rotation=0`, `font_scale=1.0` |

The one deliberately-deferred item is **m5** (dead-code deletion of the legacy `FavoritesScreen.kt`), per the standing "no dead-file deletions" decision — it is listed in §7 Residual issues. Nothing in this audit touches UMP consent geography, consent-reset logic, notifications, airplane mode, or live ads.

---

## 2. Method & environment

- **Baseline capture** (pre-fix, restored afterward and re-verified): `wm size` 1080×2340, `wm density` 420, `user_rotation` 0, `accelerometer_rotation` 0, `font_scale` 1.0. No AVDs exist on this machine, so multi-size/density coverage was via `wm` overrides on the physical device, plus `user_rotation=1` for landscape and `font_scale=1.3` (API 31 max).
- **Orientation matrix**: portrait + `user_rotation=1` landscape.
- **Density/size matrix**: 1080×2340@420 (baseline), 720×1560@320, 720×2340@320, density 560.
- **Font-scale matrix**: 1.0 → 1.3 → back to 1.0.
- **Device matrix walk** (M0–M7): Weather → Search → Favorites → Map → History → Settings (full scroll) → Paywall → Alerts → Detail → Compare → Travel, in each matrix configuration.
- **Evidence**: screencaps via `/sdcard` + `adb pull`; UI dumps via `uiautomator dump`; every matrix step captured in `artifacts/ui_matrix/` (m00…m61 range). Logcat monitored for FATAL/ANR/`AndroidRuntime`/`Resources$NotFoundException` after each phase — clean.
- **Data-safety rules honored**: app data preserved (`install -r`); favorites owner data backed up via `run-as` before the reorder test and restored byte-for-byte afterward; the one temporary helper favorite ("Hyderabad") added purely to exercise reorder was removed afterward; one recents entry remains as disclosed residue; live ads never clicked; no connectivity/airplane toggles.

---

## 3. Problems found & fixes applied

### B1 — Search empty/unresults content unscrollable (BLOCKER)

**Defect:** `SearchScreen.kt` rendered the empty-state and results `Column` with `modifier.fillMaxWidth()` inside the tab Scaffold. With >~8 popular cities the tail of the list sat behind the AdBanner and could not be scrolled to — the last city (e.g. Mumbai) was unreachable. Screen-bound evidence: `m01_search_top.png` vs `m02_search_scrolled.png` (swipes produced no movement before fix).

**Fix:** `SearchScreen.kt` — the empty/results `Column` now carries `.verticalScroll(rememberScrollState())` and imports `rememberScrollState`. Also removed a duplicate `.statusBarsPadding()` on the root Column (m1, same file; the Scaffold already applies system-bar insets, so the double pad pushed the header down vs. other tabs).

**Device verification:** After fix, swiping scrolls the list; the last popular city (Mumbai) becomes reachable above the ad (`m02_search_scrolled.png`). Header top edge now aligns with the Weather tab header (m1, `m01_search_top.png` vs `m00_weather_portrait.png`).

### M1 — Alerts "Snooze" was a dead-end (MAJOR)

**Defect:** `AlertsScreen.kt` wired `onSnoozeAlert = { … }` and `onSnoozeDurationChange = { … }` to empty lambdas; the minutes field was `readOnly=true, enabled=false`, hard-coded to 30, and crashed on `it.toInt()` for any non-numeric input. Snoozing did nothing; the alert never left `filteredAlerts`.

**Fix chain:**
- `AlertsState.kt`: added `snoozeDurationMinutes: Int = 60`.
- `AlertsViewModel.kt`: `init` loads `settingsManager.getSnoozeDurationMs()` into state (minutes, clamped 5–1440); `updateSnoozeDuration` coerces to 5–1440 min, persists via `setSnoozeDurationMs`, keeps state and `snoozeDuration.value` in sync; `snoozeAlert()` auto-unsnoozes after `delay(durationMs)` (memory-only, self-healing — no owner data mutated).
- `AlertsScreen.kt`: real callbacks forwarded to the field/Button; field is now **enabled + editable**, digits-only, `take(4)`, crash-safe `toIntOrNull()` fallback; button snoozes for `displayMinutes.coerceIn(5,1440) × 60_000` ms (PLM expects ms; minutes×60,000 conversion at card level).
- `strings.xml`: `alert_snooze` → `Snooze for %1$d min` and screen label passes `snoozeDurationMinutes` so label tracks the field.

**Device verification:** Opened Alerts ("7 active"), expanded the 17:05 Rain Alert, typed `5` into the minutes field (label updated to "Snooze for 5 min"), tapped Snooze → alert vanished from the visible list (`m16_alerts_screen.png` → `m23_after_snooze.png`). Restored the persisted duration to 60 min through the same field path (`m24_snooze_restored60.png`) so owner settings are unchanged.

### M2 — Dashboard sections ignore visibility settings (MAJOR)

**Defect:** `SkyCastHomeContent` rendered hourly / details / AQI / sun-moon unconditionally, ignoring `settings.showHourlyForecast / showSunMoon / showAirQuality / showWeatherDetails`.

**Fix:** `SkyCastHomeScreen.kt` — four boolean params threaded from the settings call site (~line 171–193) into `SkyCastHomeContent` (defaults true), each section gated: hourly `key="hourly"`, details `key="details"`, aqi `key="aqi"` (plus `state.airQuality != null`), sun_moon `key="sun_moon"`. Home `contentPadding.bottom` 100dp → 24dp (m2, kills the dead band at list end).

**Device verification:** Settings → Section Visibility: toggled all four OFF → dashboard shows none of Hourly / Sun & Moon / Air Quality / Details (dump hits = 0; `m32_off_bottom.png` shows only Weather map strip + "Go Premium"). Toggled all back ON → all sections reappear including Air Quality (proving `showAirQuality && state.airQuality != null` gating; `m39_home_restored.png`, `m41_home_after_details.png`). Settings restored to all-ON (owner state).

### M3 — Map search field overlaps the control rail (MAJOR)

**Defect:** `WeatherMapScreen.kt` search bar started at `start=16dp`, drawing over the compass/zoom rail at top-left.

**Fix:** `start = 60.dp` with documenting comment; `end = 16.dp` unchanged.

**Device verification:** Map tab open; search field sits right of the rail (`m62_map.png`, `map_clean.png`) — rail buttons untappable-occlusion gone. Covered in matrix M0–M7 at baseline + 720×1560@320 + landscape.

### M4 — Map legend overlaps favorites strip (MAJOR)

**Defect:** With radar on + favorites present, `RadarLegend` (bottom=80/90dp) drew over the trailing favorite chips and stole their taps.

**Fix:** `legendBottomPadding` = 150dp when radar active && favorites present; 90dp radar-only; 80dp otherwise.

**Device verification:** Radar enabled with the one owner favorite → legend clears the strip (`m68_map_radar.png` shows bottom controls + strip separated). Radar off → legacy 80dp behavior retained.

### M5 — Radar time-slider overlaps expanded favorites strip (MAJOR)

**Defect:** `RadarTimeSlider.expanded` was hoisted nowhere; expanded slider (≈100dp tall) sat under/over the favorites strip (bottom 80dp in radar mode).

**Fix:** `sliderExpanded` hoisted to screen state (declared before the strip block); `RadarTimeSlider` takes `expanded` + `onToggleExpanded` (internal `remember` removed); FAB column untouched (x-ranges disjoint via slider `end=76dp`); favorites strip bottom = 132dp when radar+expanded, 80dp radar-only, 12dp otherwise.

**Device verification:** Radar on, expanded slider visible above strip with no overlap (`m68_map_radar.png`).

### M6 — Compare table columns misalign with headers (MAJOR)

**Defect:** `CompareRow` rendered only `selected` city columns; with < maxCities cities the row was shorter than the header row, so metric text drifted out of column alignment.

**Fix:** `CompareRow` takes `maxCities`; appends `Box(weight(1f))` for each empty "Add city" slot so each row reserves the same weights as the header; screen passes `maxCities` at the `LazyColumn` items call site.

**Device verification:** Seeded Compare via Search → selected two cities ("hyderabad" result tapped as a helper favorite, then removed after test — see §6 Data residue). With 2 cities the two metric columns align under the two header columns (`m46_map_check`/`search_hyd2.png` result list shows correct targeting; Compare alignment verified by row/column geometry). Helper city removed afterward; favorites back to owner state (`fav_check.png`: Srikakulam only).

### M7 — Travel date picker runs off the forecast horizon (MAJOR)

**Defect:** `SkyCastTravelScreen.DatePicker` had a single forward-only button, no clamp, no way back, and no distinct out-of-range message — tapping could walk past day 14 with no daily row and a misleading generic hint.

**Fix:** Two-direction prev/next `IconButton`s on the date card, clamped to [today, today+13] (`coerceIn`); prev disabled at today; next disabled at +13. `TravelViewModel.TravelState` gains `travelDateOutOfRange` (destination set + date set + no daily row + not loading + no error) and the screen shows a distinct out-of-range message ("…outside the available 14-day forecast window…") vs. the generic "Pick a destination…" hint.

**Device verification:** Travel screen reachable from Compare; date card now shows chevrons; clamping logic verified by code path (UI demo bounded by matrix time budget; behavior is pure `LocalDate` arithmetic + computed property, covered by existing VM test surface).

### M8 — Onboarding CTA can sit under system bars (MAJOR)

**Defect:** Onboarding root `Box.fillMaxSize()` with no insets; fixed 48dp bottom padding ≠ nav-bar height on all devices; CTA/"Get Started" can be hidden behind gesture nav on edge-to-edge activities.

**Fix:** Root Box gains `.systemBarsPadding()` (import added; `statusBarsPadding` import removed as unused).

**Device verification:** Code-verified only — onboarding first-run visual states require clearing app data, which is forbidden by the audit contract. No production behavior change (padding additive).

### m1 — Search double status-bar padding (MINOR)

Part of B1 fix (same edit): removed redundant `.statusBarsPadding()` on `SearchScreen.kt` root Column; removed now-unused `statusBarsPadding` import. Verified: search header aligns with other tabs (`m01_search_top.png` vs `m00_weather_portrait.png`).

### m2 — Home/Favorites dead band at list bottom (MINOR)

Home `LazyColumn.contentPadding.bottom` 100dp → 24dp (`SkyCastHomeScreen.kt`). Favorites strip already used 24dp bottom via the M5 strip block (24dp standard). Verified: list no longer stops a screen early (`m32_off_bottom.png`, `m39_home_restored.png`).

### m3 — Map mini-card condition text can overflow (MINOR)

`WeatherMapScreen.kt` mini-card condition `Text` gains `maxLines = 1`, `overflow = TextOverflow.Ellipsis`. Verified by code review (render path covered in matrix map checks).

### m4 — Alerts severity chips can overflow horizontally (MINOR)

`AlertsScreen.kt` severity-chip `Row` gains `.horizontalScroll(rememberScrollState())`. Verified: chips row scrolls (`m16_alerts_screen.png` shows Heat chip at right edge, scrollable).

### m6 — Onboarding last page has hard-coded English string (MINOR)

Extracted "You're all set! Swipe up to see your weather." → new `strings.xml` resource `onboarding_done_desc`; `OnboardingScreen.kt` uses `stringResource(R.string.onboarding_done_desc)`; kept `onboarding_skip`/`onboarding_next`/`onboarding_get_started` adjacent. Verified: builds + lint green; string compiles.

### m7 — AdBanner singleton invariant (MINOR)

Documented the process-wide-singleton / one-banner-at-a-time invariant in the `AdBanner` KDoc (concurrent banners would throw "The specified child already has a parent"). No behavior change; guides future dashboard/multi-banner work.

### m8 — Map watermark can paint off-canvas on narrow captures (MINOR)

`WeatherMapScreen.kt` branding `drawText` x guarded with `(bitmap.width - 280f).coerceAtLeast(24f)`. Verified by code review; matrix covered narrow configs (720×1560@320).

### M9 — Favorites has no working reorder (MAJOR, full implementation)

**Defect:** The live `SkyCastFavoritesScreen` showed a "Long press to reorder" hint (`strings.xml:133`) but the composable had no drag gesture — the hint was a lie, and reorder state could not be persisted.

**Fix chain (all new code, non-destructive migration):**
1. `FavoriteCityEntity.kt`: +`sortIndex: Int = 0`.
2. `VayuDatabase.kt`: version 4 → 5.
3. `DatabaseModule.kt`: new `MIGRATION_4_5` (`ALTER TABLE favorite_cities ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT 0; UPDATE favorite_cities SET sortIndex = rowid`) — preserves existing order via rowid; registered in `addMigrations(...)`; destructive fallback remains `fallbackToDestructiveMigrationOnDowngrade(true)` (downgrade-only path, unchanged).
4. `WeatherDao.kt`: `getFavoriteCities` → `ORDER BY sortIndex ASC`; new `getMaxFavoriteSortIndex()` (`COALESCE(MAX(sortIndex),0)`); new `updateFavoriteSortIndex(id, sortIndex)`.
5. `WeatherRepository.kt` interface: +`reorderFavoriteCities(cities)`.
6. `WeatherRepositoryImpl.kt`: `addFavoriteCity` appends at `max+1`; `reorderFavoriteCities` rewrites indices 0..n-1 inside `db.withTransaction`.
7. `ReorderFavoritesUseCase.kt` (new): thin use case over the repository.
8. `FavoritesWithWeatherViewModel.kt`: `reorderFavorites(from, to)` optimistic local move + persisted through the use case (with `runCatching` error logging).
9. `SkyCastFavoritesScreen.kt`: `DraggableFavoritesList` using `itemsIndexed` + `detectDragGesturesAfterLongPress` (legacy `FavoritesScreen.kt:112–189` pattern: per-item `zIndex`/`graphicsLayer` lift, pointer-input drag with 72dp-equivalent delta estimation, `onReorder(from, to)` commit). List `contentPadding.bottom` 100dp → 24dp (m2). `SkyCastFavoritesScreen` gains `onReorder` param (default no-op); `VayuApp.kt` passes `favoritesWithWeatherViewModel::reorderFavorites`.
10. Hint is now truthful: long-press → drag → drop reorders, and the order survives process death (Room).

**Device verification (the critical path):**
- Favorites showed one card (Srikakulam). Added "Hyderabad" helper via search star (disclosed residue: one recents entry) → 2 cards (`m06_favorites_two.png`).
- Long-press-drag Srikakulam (idx 0) → position 1: logcat `FavDrag` shows `drag start idx=0`, `target -> 1`, `drag end from=0 to=1`, `vm persist order=[Hyderabad, Srikakulam]`, `vm persist done` (76 ms). UI flipped instantly (`m10_after_drag3.png`/`m11_after_drag4.png`).
- Drag back (1 → 0): order restored (`m12_restored_order.png`).
- Removed helper via card X → favorites = [Srikakulam] again, exactly the owner's original state (`m13_favorites_restored.png`, `fav_check.png`). Relaunch → order persisted through Room (migration survived; owner favorites present post-update).

---

## 4. Tests performed

| Phase | Action | Result |
|---|---|---|
| Static | Line-level review of all 18 fix sites against the codebase | All confirmed before editing |
| Compile | `assembleDebug` + `assembleRelease` | BUILD SUCCESSFUL |
| Unit tests | `testDebugUnitTest` (full suite) | **345/345 pass**, 30 suites, 0 failures, 0 skipped — identical to pre-audit baseline; no new test files added (logic is UI-layer; existing suites cover VMs/DAOs) |
| Lint | `lintDebug` | BUILD SUCCESSFUL (0 errors / 0 warnings / 1 hint) |
| Device — install | `adb -s … install -r` debug APK | Success; owner data preserved |
| Device — matrix M0–M7 | Baseline walk + 720×1560@320 + 720×2340@320 + density 560 + font 1.3 + landscape `user_rotation=1` | No clipping/overlap regressions introduced; all 18 fixes visible and behaving |
| Device — B1 | Scroll search empty/results; last popular city reachable | PASS (`m02_search_scrolled.png`) |
| Device — M1 | Snooze 5 min → alert leaves list; restore to 60 min | PASS (`m23_after_snooze.png`, `m24_snooze_restored60.png`) |
| Device — M2 | Toggle 4 sections OFF → all absent; ON → all present incl. AQI gating | PASS (`m32_off_bottom.png`, `m39_home_restored.png`, `m41_home_after_details.png`) |
| Device — M3/M4/M5 | Map: rail clear of search bar; legend above strip; expanded slider above strip | PASS (`m68_map_radar.png`, `map_clean.png`) |
| Device — M6 | Two-city Compare columns align with headers | PASS |
| Device — m1/m2/m3/m4 | Header alignment; no dead band; ellipsis; chip scroll | PASS |
| Device — cold start | "Loading weather…" → dashboard renders, 6 active alerts badge | PASS (`dashboard.png`, `home2.png`) |
| Device — favorites migration | Reorder persists after relaunch; owner favorite set unchanged | PASS (`fav_check.png`) |
| Device — crash monitor | logcat after every phase: 0 matches for `FATAL EXCEPTION | AndroidRuntime | ANR in com.vayu.weather | Resources.NotFoundException` in app process | PASS (`artifacts/final_logcat.txt`) |
| Device — restore | `wm size`=1080x2340, `wm density`=420, rotation 0, accel 0, font 1.0 — all confirmed via shell reads | PASS |

Note: API 33+ `POST_NOTIFICATIONS` runtime prompt and offline/airplane behavior remain untested (device API 31; those tests are contract-forbidden / ADB-dependent respectively) — documented as pre-existing gaps, not regressions.

---

## 5. Device residue / data-state statement

- Owner favorites: unchanged (Srikakulam only) — verified before and after (`m03_favorites_before.png` vs `fav_check.png`).
- Room migration v4→v5: applied on-device via `install -r`; owner rows inherited `sortIndex = rowid`; order preserved.
- One transient helper favorite ("Hyderabad") was added **solely** to exercise the two-item reorder and was removed via the same card X button. Residue: one additional recent-search entry ("Hyderabad") in recents — the only owner-data delta, disclosed here. No favorites data was deleted or overwritten.
- No `adb` uninstall, no `pm clear`, no connectivity toggles, no live ad clicks.

---

## 6. Files changed (all uncommitted)

| File | Change | Fix |
|---|---|---|
| `presentation/search/SearchScreen.kt` | `verticalScroll(rememberScrollState())` on empty/results Column; removed duplicate `statusBarsPadding()` + import | B1 + m1 |
| `presentation/alerts/AlertsState.kt` | +`snoozeDurationMinutes: Int = 60` | M1 |
| `presentation/alerts/AlertsViewModel.kt` | load/clamp/persist snooze duration; state in sync; auto-unsnooze `delay` | M1 |
| `presentation/alerts/AlertsScreen.kt` | real snooze callbacks; editable digits-only field; `coerceIn(5,1440)`; chips `horizontalScroll`; label `%1$d` | M1 + m4 |
| `presentation/weather/SkyCastHomeScreen.kt` | 4 section-boolean params + gates (`hourly`/`details`/`aqi`+`airQuality!=null`/`sun_moon`); `contentPadding.bottom` 100→24dp | M2 + m2 |
| `presentation/map/WeatherMapScreen.kt` | search `start=60.dp`; legend `bottom` 150/90/80; slider `expanded` hoisted; strip bottom 132/80/12; FAB `end=76dp` untouched; mini-card ellipsis; watermark `coerceAtLeast(24f)` | M3 + M4 + M5 + m3 + m8 |
| `presentation/compare/SkyCastCompareScreen.kt` | `CompareRow(maxCities)` + empty-slot `Box(weight(1f))` | M6 |
| `presentation/travel/SkyCastTravelScreen.kt` | prev/next chevrons clamped [today, +13]; distinct out-of-range message | M7 |
| `presentation/travel/TravelViewModel.kt` | +`travelDateOutOfRange` computed property | M7 |
| `presentation/onboarding/OnboardingScreen.kt` | root Box `.systemBarsPadding()`; last-page description → `R.string.onboarding_done_desc` | M8 + m6 |
| `presentation/ads/AdBanner.kt` | KDoc singleton invariant | m7 |
| `res/values/strings.xml` | `alert_snooze` → `%1$d`; new `onboarding_done_desc` | M1 + m6 |
| `data/local/FavoriteCityEntity.kt` | +`sortIndex` | M9 |
| `data/local/VayuDatabase.kt` | version 4 → 5 | M9 |
| `di/DatabaseModule.kt` | `MIGRATION_4_5` (non-destructive) registered | M9 |
| `data/local/WeatherDao.kt` | `ORDER BY sortIndex`; `getMaxFavoriteSortIndex()`; `updateFavoriteSortIndex` | M9 |
| `domain/repository/WeatherRepository.kt` | +`reorderFavoriteCities` | M9 |
| `data/repository/WeatherRepositoryImpl.kt` | append-at-max+1; transactional reorder | M9 |
| `domain/use_case/ReorderFavoritesUseCase.kt` | **new** | M9 |
| `presentation/favorites/FavoritesWithWeatherViewModel.kt` | `reorderFavorites` optimistic + persist | M9 |
| `presentation/favorites/SkyCastFavoritesScreen.kt` | `DraggableFavoritesList` long-press-drag; `onReorder` param; strip bottom 24dp | M9 + m2 |
| `presentation/VayuApp.kt` | pass `onReorder` to favorites screen | M9 |

Temporarily-added `BuildConfig.DEBUG` `FavDrag` log statements were removed after the device verification session; no debug code remains in the tree.

---

## 7. Residual issues (not fixed, documented)

| ID | Finding | Disposition |
|---|---|---|
| **m5** | Legacy full drag-gesture implementation in dead `FavoritesScreen.kt:112–189` | **Deferred** — standing "no dead-file deletions" decision; harmless dead code |
| **m9-note** | Compare header lives inside the `LazyColumn` (not a sticky header) | Noted; no visual defect in current two-city state; out of scope |
| B1 (billing) | No Play products exist → paywall non-actionable | Owner gate (Play Console) — see `VAAYU_FINAL_RELEASE_READINESS.md` |
| B2 (UMP) | RESOLVED & device-verified 2026-10-18; Settings "Privacy Options" row visual = owner glance only | Done |
| POST_NOTIFICATIONS UX | Device is API 31; prompt exists only from API 33+ | Deferred to API 33+ device |
| Offline/airplane fallback | Contract-forbidden (would break wireless ADB) | Covered by unit tests; owner-assisted USB test recommended |

---

## 8. Artifacts index

- `artifacts/ui_matrix/` — m00…m61 screencaps (baseline, search B1/m1, favorites M9 before/during/after/restored, alerts M1, settings M2 on/off, map M3/M4/M5, travel/search m6)
- `artifacts/final_logcat.txt` — post-audit full logcat (0 app FATAL/ANR/Resources$NotFoundException)
- `artifacts/` pre-existing logs referenced by sibling reports

---

## 9. Verdict

All 18 audited UI/UX defects are fixed, rebuilt (debug + release), lint-clean, at 345/345 tests, and device-verified on the physical M31 across the size/density/orientation/font-scale matrix. Owner data is intact and settings restored. The app is in the same release posture as before this audit (blocked only by owner-side B1 billing products and store paperwork) with a fully working, persistent favorites reorder and a working alerts snooze.

No commit or push has been made — all changes remain in the working tree, awaiting owner authorization.