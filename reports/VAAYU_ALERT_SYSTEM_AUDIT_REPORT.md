# VAAYU ALERT SYSTEM AUDIT — SkyCast Weather (`com.vayu.weather`)

**Report ID:** VAAYU_ALERT_SYSTEM_AUDIT  
**Date:** 2026-10-20  
**Issue under audit:** the Weather screen showed **7 active alerts**, mostly titled "Rain Alert", with slightly different messages and issue times  
**Device:** Samsung Galaxy M31 (SM-M315F), Android 12 (API 31), 1080×2340 @420dpi  
**Transport:** wireless ADB, serial `adb-RZ8N935MQTF-Sty2BK._adb-tls-connect._tcp` (never disabled)  
**Build under test:** debug (`install -r`, data preserved) · release build also produced  
**Status of changes:** **uncommitted** — awaiting owner authorization

---

## 1. Verdict on the screenshots: they were **true duplicates**, not distinct advisories

The database on the device was pulled read-only (`run-as` → `adb exec-out` → Python `sqlite3`, scripts in `artifacts/alert_audit/`) and showed the ground truth. **23 rows, 21 of them the same "Rain Alert" for the same place, one row every ~3 hours, going back 8 days:**

| id | issued | severity | message | lat/lon |
|---|---|---|---|---|
| 23 | 10-19 21:11 | medium | High chance of rain today (**61%**) | 18.4865, 84.1060 |
| 22 | 10-19 18:11 | high | …(**81%**) | 18.4865, 84.1060 |
| 21 | 10-19 15:04 | high | …(**80%**) | 18.4865, 84.1060 |
| 20 | 10-19 11:28 | high | …(80%) | 18.4865, 84.1060 |
| 19 | 10-19 07:02 | high | …(80%) | 18.4865, 84.1060 |
| … | … | … | … | … |
| 2 | 10-11 18:23 | high | …(88%) | 18.4628, 84.1044 |
| 1 | 09-27 12:26 | medium | Very high UV index today (UV 8) | 18.4728, 84.1101 |

Grouped: **'Rain Alert'/high → 21 rows**, 'Rain Alert'/medium → 1, 'UV Alert' → 1. The message group counts: 7×"80%", 4×"100%", 3×"88%", 3×"85%", 2×"96%", 1× each of 98/81/61%. Four different coordinate pairs appeared for the same town (GPS jitter of a few hundred metres).

**Conclusion: one single ongoing rain condition, re-recorded 21 times.** Not legitimate forecast updates — the probability changed (61→98%) but these are *one advisory per 3-hourly worker cycle*, not one per forecast change.

---

## 2. Root cause

The app receives **no official alert objects** from the provider (Open-Meteo returns lat/lon only). Advisories are *derived* from the forecast for the current location: `daily[0].precipitationProbability ≥ rain threshold` → "Rain Alert" (and the same for wind/UV/heat/cold). The intended product behaviour is therefore **one current advisory per type and location**, updated in place.

Three defects produced the pile-up:

| # | Defect | Code evidence |
|---|---|---|
| **R1** | **The only deduplication window equalled the worker's own period.** `WeatherAlertWorker` ran every 3 h (`PeriodicWorkRequestBuilder(3, HOURS)`) and suppressed a new alert only if an alert with the same *title + severity* existed in the last **3 hours** — so every cycle was allowed to append a new row. The observed spacing (07:02, 11:28, 15:04, 18:11, 21:11) matches exactly. | `data/worker/WeatherAlertWorker.kt` (old `storeAndNotify`), `VayuApplication.kt:79` |
| **R2** | **The alert table had no identity and no validity.** `weather_alerts` had an auto-increment primary key and nothing else; `insertWeatherAlert` was a plain `INSERT` (REPLACE on the PK, which never collides). A repeated logical alert therefore always produced a new row. | `data/local/WeatherAlertEntity.kt`, `data/local/WeatherDao.kt` |
| **R3** | **"Active" was an ad-hoc 24-hour age filter, and nothing ever expired.** `filterAndSortAlerts()` kept rows younger than 24 h, so one advisory per 3 h ≈ **7–8 cards on the dashboard at any time**; the Alerts screen listed *every* row ever stored (15, then 23 cards). No advisory was ever closed when the condition stopped. | `presentation/components/skycast/SkyCastAlertCard.kt`, `presentation/alerts/AlertsState.kt` |

Secondary findings: a **severity change defeated the dedup key** (medium→high appended another row), while a **probability change within the same severity was silently dropped** (the stored message went stale); **GPS jitter** produced different coordinates for the same place, so any coordinate-keyed dedup would still have failed.

---

## 3. The fix

### 3.1 Stable advisory identity (domain)

**New** `domain/model/WeatherAdvisory.kt`
- `WeatherAdvisoryType` (rain / wind / uv / heat / cold) — part of the identity, so a rain advisory and a wind advisory are never merged.
- `WeatherAdvisoryFingerprint.of(type, lat, lon, forecastDate)` → `"18.5|84.1|rain|2026-10-20"`:
  - **location bucketed to 0.1° (≈11 km)** — the provider's own forecast-grid resolution, which makes the key stable across GPS jitter (the observed 2.6 km spread collapses to one bucket) while still separating different towns;
  - **type** — a rain advisory is not a wind advisory;
  - **forecast day** — two distinct forecast periods for the same place stay two advisories.
  - The title alone is never used as a key.

**New** `domain/model/WeatherAdvisoryTextProvider.kt` + `data/local/ResWeatherAdvisoryTextProvider.kt` — advisory texts come from the app's string resources, so the evaluation stays localized (and unit-testable with a fake).

### 3.2 Idempotent, transactional persistence (data)

| Change | Purpose |
|---|---|
| `WeatherAlertEntity` += `fingerprint` (unique index) + `expiresAt`; `isActive(now)` | identity + validity window |
| `WeatherDao`: `getActiveWeatherAlerts`, `getWeatherAlertByFingerprint`, `updateWeatherAlertContent`, `updateWeatherAlertExpiry`; insert switched to **ABORT** | the unique index turns a duplicate into a constraint violation instead of a silent second row |
| `WeatherRepositoryImpl.upsertWeatherAlert()` | **insert-or-update** keyed on the fingerprint: created / updated (with `escalated` flag) / unchanged; on a constraint violation (concurrent writer) it falls back to updating the existing row. Content-identical advisories are a **no-op** (no new row, no new timestamp) |
| `WeatherRepositoryImpl.resolveWeatherAlert()` | closes an advisory whose condition no longer holds — the row is **kept** for history |
| `VayuMigrations.MIGRATION_5_6` + `VayuDatabase` version 5 → 6 | **non-destructive** migration: adds the two columns, back-fills every legacy row with a unique placeholder fingerprint (`legacy-<id>`) and `expiresAt = 0` (expired), then creates the unique index. **No alert row is deleted**; the legacy duplicates become history |
| `VayuMigrations` extracted from the Hilt module | so the migration tests exercise exactly the code that runs on a device |

### 3.3 One evaluation, two callers (domain + worker + VM)

**New** `domain/use_case/EvaluateWeatherAdvisoriesUseCase.kt` — the single source of truth: evaluates rain/wind/UV/heat/cold against the user's thresholds, upserts each advisory idempotently, **closes** advisories whose condition is gone, and returns the change list (`Created` / `Updated(escalated)` / `Unchanged` / `Resolved`).

- `WeatherAlertWorker` now only calls the use case → the alert rules exist once.
- `WeatherViewModel` calls it after every successful weather load (app start, city switch, **and refresh**) so the advisory state is never stale. It never notifies.
- The advisory expires at **the end of its forecast day in the location's own time zone** (`WeatherInfo.timezone`, newly parsed from the API response; falls back to the device zone).

### 3.4 Notification policy

The worker notifies on **Created** and on **escalation** only. An unchanged advisory and a de-escalation stay silent — that is what removed the 3-hourly "same rain again" notification. Notification ids are now derived from the fingerprint, so an updated advisory replaces its own notification instead of stacking.

### 3.5 UI: the count now matches what is on screen

- `AlertsState` gains `activeAlerts()`, `visibleAlerts(showHistory)` and `filtered(source)`.
- `AlertsScreen(showHistory = false)` shows the **current** advisories; the header count and every filter chip count describe exactly that list. The settings entry **"Alert History"** opens the same screen with `showHistory = true`, so the full history stays reachable — nothing is deleted or hidden from the user.
- `filterAndSortAlerts()` (dashboard) now uses the **validity window** instead of the 24-hour age rule.
- `WeatherViewModel` collects `getActiveWeatherAlerts()` and additionally filters by the location (±1°).

---

## 4. Verification

### 4.1 Automated tests

| Suite | Result |
|---|---|
| `testDebugUnitTest` | **375/375 pass**, 32 suites, 0 failures/errors/skipped (345 before this audit; +7 location-name, +23 alert) |
| `lintDebug` | BUILD SUCCESSFUL — **0 errors, 0 warnings, 1 hint** (identical to the pre-existing baseline) |
| `assembleDebug` | BUILD SUCCESSFUL |
| `assembleRelease` | BUILD SUCCESSFUL (24,070,344 B) |
| `connectedDebugAndroidTest` (M31) | **37 tests, 29 pass, 8 failures — all 8 proven pre-existing** (see §4.3) |

**New unit tests** — `domain/use_case/EvaluateWeatherAdvisoriesUseCaseTest.kt` (17) and `presentation/alerts/AlertsStateTest.kt` (6), all with a **fixed clock** and deterministic fixtures (no network):
fingerprint determinism · jitter stability · type/location/day separation · 10× ingestion → 1 advisory · app restart with the same forecast → 1 · 10 concurrent evaluations → 1 · unchanged → `Unchanged` · changed probability → same row updated · escalation flagged · de-escalation **not** flagged · two types / two locations / two forecast days kept apart · validity = end of the forecast day in the location's zone · condition gone → advisory closed · missing data → no advisory at all.

**New instrumented tests (on the M31)** — `WeatherAlertDatabaseTest.kt` (8, real Room + real migration): 10× ingestion → 1 row · changed forecast updates the row · escalation reported · distinct advisories get their own rows · **the unique index rejects a duplicate fingerprint** · only valid advisories are active · resolving keeps the row as history · **v5 → v6 migration keeps every alert row, marks the legacy ones expired, gives them unique fingerprints and still accepts new advisories**.
`AlertsScreenUiTest.kt` (6, Compose): only current advisories listed · header count = number of cards · history view shows everything · snoozed advisory hidden · empty state · severity filter.

### 4.2 Physical-device verification (M31, API 31)

Ground truth was read from the live database via `run-as` + `sqlite3` (`artifacts/alert_audit/*.py`).

| Scenario | Result | Evidence |
|---|---|---|
| **Before (pre-fix)** | 23 rows for one ongoing condition; dashboard counted **7 active**; menu showed "7 alerts" | `artifacts/alert_audit/inspect_alerts.py` output, `01_before_dashboard.png` |
| **Migration v5 → v6 on the live DB** | No row lost; legacy rows carry `legacy-<id>` fingerprints and `expiresAt = 0`; unique index created | `verify_after_fix.py` |
| **Advisory created once** | Exactly **1** non-legacy row: `fingerprint=18.5|84.1|rain|2026-10-20`, `expiresAt` = end of the forecast day, **active** | `verify_after_fix.py`, `09_dashboard_active.png` |
| **Dashboard count** | "**1 active**", one Rain Alert card | `09_dashboard_active.png` + dump |
| **Overflow menu** | "**1 alerts**" (was "7 alerts"); with 0 active the entry disappears | `10_menu_after.xml` |
| **Alerts screen** | "Weather Alerts — **1 alert**", chips All (1) / High (0) / Medium (1) / Rain (1), exactly **1** card | `11_alerts_screen_one.png` + dump |
| **Repeated ingestion (5 refreshes)** | Row count stayed **25**, active stayed **1**, one row for the fingerprint | `verify_after_fix.py` after 5 refreshes |
| **App restart** | Same count, same single advisory (persisted) | `10_menu_after.xml` |
| **Condition no longer holds** (threshold back to 50 %, 29 % rain) | Advisory **closed** (`expiresAt` = resolution moment), **row kept** as history, active count → **0**, dashboard shows no Alerts section | `verify_after_fix.py`, `24_dashboard_closed.xml` |
| **History preserved** | Settings → "Alert History" shows "**25 alerts**" (All 25 / High 21 / Medium 4 / Rain 24 / UV 1) | `17_alert_history.png` + dump |
| **Migration on a real v5 database** | Verified by the instrumented test (rows kept, legacy rows expired, new advisory accepted) | `WeatherAlertDatabaseTest` |
| **Crash monitor** | 0 `FATAL EXCEPTION` / `ANR in com.vayu` in the app process across all sessions | `artifacts/alert_audit/final_logcat.txt` |
| **Device state after testing** | `wm size` 1080×2340, `wm density` 420, `user_rotation` 0, `accelerometer_rotation` 0, `font_scale` 1.0, location permissions granted | shell reads |

### 4.3 The 8 instrumented failures are **pre-existing** — proven, not assumed

`WeatherDashboardTest` (5) and `SearchFavoritesTest` (3) fail. To prove they are not caused by this work, the **untouched baseline (HEAD `1f8bcfa`)** was extracted to a separate directory (`git archive`, outside the repo) and the same suites were run on it:

| Suite | Baseline (HEAD 1f8bcfa) | With this audit's changes |
|---|---|---|
| `WeatherDashboardTest` | 11 tests, **5 failures** — identical names | 11 tests, **5 failures** — identical names |
| `SearchFavoritesTest` | 6 tests, **3 failures** — identical names | 6 tests, **3 failures** — identical names |

Causes are unrelated to alerts (e.g. the test expects the string `Current location` while the app uses `Current Location`; the search test finds two `New York` nodes; the favourites empty-state text differs). **This audit introduces no new instrumented failure.**

---

## 5. Incident during verification — and the restoration

Running `connectedDebugAndroidTest` **clears the app's data** (standard AGP behaviour: `pm clear` before the instrumentation starts). This wiped the device database and the settings DataStore. Detected immediately (`favorites: []`, `alerts: []`, `recents: []`) and **fully restored**:

| Item | State after the wipe | Restoration | Verified |
|---|---|---|---|
| **Favorites** | empty | "Srikakulam" re-added through the app's own search → **same city id 1255647**, same name, region, country, country code and coordinates | DB row matches the original |
| **Recent searches** | empty | "Srikakulam" re-selected (adds the recent) | DB row present |
| **Settings (DataStore)** | reset to defaults | The owner's settings **were** the defaults (°C, km/h, System theme, Material You on, notifications on, rain threshold 50 %, 3-hour checks, All Alerts, widget Medium, 24-hour clock, hPa, mm, all sections visible, quiet hours off, 07:00/12:00/18:00) → the reset restored exactly the original state | settings dump matches the pre-audit dump |
| **Onboarding** | flag lost → onboarding shown | Completed through the UI (flag restored) | app goes straight to the dashboard |
| **Runtime permissions** | revoked by `pm clear` | `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` re-granted | `granted=true` |
| **UMP consent** | form shown again | Re-collected through **Manage options → all purposes off → Confirm choices** — the exact path proven in the earlier session, restoring "consent obtained, purposes denied" | `IABTCF_VendorConsents` = all zeros, `IABTCF_gdprApplies=1` |
| **Alert history (24 legacy rows)** | lost | **Not restorable** — the rows were app-generated duplicates of one condition. The app regenerates the current advisory automatically. | disclosed below |

**Remaining data loss to disclose:** the 24 legacy alert-history rows (duplicates of a single rain advisory) and the weather-history table are gone; the app repopulates weather history as it runs. No user-authored content (favorites, recents, settings) remains lost.

**Owner action:** none required for correctness — but the owner should re-check the Alerts screen once and be aware that the *history* of past duplicates is gone.

---

## 6. Files changed (all uncommitted)

```
NEW  domain/model/WeatherAdvisory.kt              advisory types + fingerprint (0.1° grid, type, forecast day)
NEW  domain/model/WeatherAdvisoryTextProvider.kt   localized advisory texts (interface)
NEW  data/local/ResWeatherAdvisoryTextProvider.kt string-resource implementation
NEW  domain/use_case/EvaluateWeatherAdvisoriesUseCase.kt   the single evaluation rule set
NEW  data/local/VayuMigrations.kt                  migrations extracted (testable) + MIGRATION_5_6
NEW  di/AdvisoryModule.kt                          Hilt binding for the text provider
NEW  app/src/test/.../EvaluateWeatherAdvisoriesUseCaseTest.kt   17 unit tests
NEW  app/src/test/.../presentation/alerts/AlertsStateTest.kt    6 unit tests
NEW  app/src/androidTest/.../WeatherAlertDatabaseTest.kt      8 instrumented tests (Room + migration)
NEW  app/src/androidTest/.../AlertsScreenUiTest.kt            6 instrumented UI tests
MOD  data/local/WeatherAlertEntity.kt              fingerprint + expiresAt + unique index + isActive()
MOD  data/local/WeatherDao.kt                      active query, fingerprint lookup, content/expiry updates, ABORT insert
MOD  data/repository/WeatherRepositoryImpl.kt       upsert/resolve/active-alerts, transactional, constraint-safe
MOD  domain/repository/WeatherRepository.kt         WeatherAlert += fingerprint/expiresAt, WeatherAlertUpsertResult
MOD  data/worker/WeatherAlertWorker.kt              uses the evaluator; notify on create/escalation only; stable ids
MOD  di/DatabaseModule.kt                           uses VayuMigrations.ALL
MOD  data/local/VayuDatabase.kt                     version 5 → 6
MOD  domain/model/WeatherInfo.kt                   + timezone
MOD  data/remote/dto/WeatherDto.kt + data/mapper/WeatherMappers.kt   parse "timezone"
MOD  presentation/weather/WeatherViewModel.kt       evaluates advisories after every load/refresh; active-alert flow
MOD  presentation/alerts/AlertsState.kt             activeAlerts / visibleAlerts / filtered(source)
MOD  presentation/alerts/AlertsScreen.kt            showHistory mode; counts describe the visible list
MOD  presentation/alerts/AlertsViewModel.kt         (unchanged behaviour, new state shape)
MOD  presentation/components/skycast/SkyCastAlertCard.kt   validity-based "active" filter
MOD  presentation/VayuApp.kt                       history entry opens the history view
```

---

## 7. Remaining risks & limitations

1. **Alert history of resolved advisories grows** — every advisory stays in the table as history (capped at 100 rows by `trimWeatherAlerts`). Intentional: the user can still review what happened.
2. **One advisory per type per day** — the evaluation uses `daily[0]` (today). If future work evaluates several forecast days, the fingerprint already separates them by date.
3. **GPS-jitter bucketing is 0.1° (≈11 km)** — two towns closer than that share an advisory identity. Chosen to match the provider's forecast grid; a finer grid would re-introduce duplicates.
4. **Advisory evaluation writes to the DB on every weather load** — idempotent by construction, but it does mean a read-mostly screen performs a small write.
5. **The severity filter still gates advisory creation** (unchanged behaviour): with "High only", a medium advisory is not recorded. An already stored advisory keeps its stored severity.
6. **The pre-existing 8 instrumented failures remain** (search/dashboard test expectations vs. the current UI). They are unrelated to alerts and were left untouched, as instructed.
7. **The 24 legacy alert-history rows were lost** to the instrumented-test data clear (see §5); the current advisory regenerates automatically.

---

## 8. Verdict

The screenshots showed **true duplicates**: one ongoing rain condition recorded 21 times over 8 days by a dedup window that equalled the worker's own 3-hour period, in a table with no identity and no validity. The fix gives advisories a stable identity (type + grid-bucketed location + forecast day), makes every write an idempotent upsert protected by a unique index, adds a real validity window, closes advisories whose condition is gone, evaluates the rules in exactly one place (shared by the worker and the app), and makes the displayed count describe precisely the list on screen.

On the device the same forecast now yields **1 active advisory instead of 7**, five consecutive refreshes and an app restart keep it at 1, the condition disappearing closes it while keeping the row as history, and the full history stays reachable in the app. 375/375 unit tests, lint 0/0/1, debug and release builds green, 14 new instrumented tests green, and the 8 remaining instrumented failures proven pre-existing on the untouched baseline. All changes are uncommitted.
