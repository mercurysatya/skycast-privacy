# VAAYU MAP TEMPERATURE/WIND OVERLAY FIX REPORT — SkyCast Weather (`com.vayu.weather`)

**Report ID:** VAAYU_MAP_OVERLAY_FIX_REPORT  
**Date:** 2026-10-28  
**Issue:** Temperature and wind map layers were selectable, the grid data loaded, but no overlay ever rendered (`F-11`, `J-06`, `CR-14`, `P0-10`, `P1-03`)  
**Device:** `VayuApi37` emulator, Android API 37, 1080×2340 @420dpi, serial `emulator-5554`  
**Build under test:** debug, `assembleDebug` + `adb install -r` (app data preserved)  
**Status of changes:** **uncommitted** — awaiting owner authorization

---

## 1. Root cause (confirmed, code + runtime)

### Primary cause: the grid was fetched and then discarded

| Stage | File | Finding |
|---|---|---|
| Layer selection | `presentation/map/WeatherMapScreen.kt` | The layer panel changed `radarState.overlayType`; temperature/wind were valid selectable states. |
| Grid fetch | `presentation/map/MapViewModel.kt` | `loadGridData()` sampled a 5×5 grid and called Open-Meteo 25 times. Runtime logs showed 25× HTTP 200. |
| State exposure | `presentation/map/MapViewModel.kt` | `tempPoints` and `windPoints` were published as `StateFlow`s. |
| UI collection | `presentation/map/WeatherMapScreen.kt` | The screen collected `tempPoints` / `windPoints` but never used them. The old code had only the comment *“Canvas overlays removed because the map library does not support lat/lon → screen projection”*. |
| Render | `presentation/map/WeatherMapScreen.kt` | No `CircleLayer`, `LineLayer`, or other overlay was added for `TEMPERATURE` or `WIND`. Selecting either chip was a visible no-op. |

**Conclusion:** the defect was not a network, data, or coordinate failure. The data arrived correctly and was deliberately dropped at the rendering layer. The static evidence is the deleted “Canvas overlays removed” branch in the diff; the runtime evidence is 25 successful grid requests with zero MapLibre layer-add events before the fix.

### Secondary cause found during the fix: the sampled area did not match the map viewport

An initial implementation rendered the points but still showed only the centre column. Runtime projection logs at zoom 10:

```
Projected (38.964958,-123.501594) -> (-306.0.dp, 880.5.dp)
Projected (38.964958,-123.325813) -> ( -50.0.dp, 880.5.dp)
Projected (38.964958,-123.150032) -> ( 206.0.dp, 880.5.dp)
Projected (38.964958,-122.974250) -> ( 462.0.dp, 880.5.dp)
Projected (38.964958,-122.798469) -> ( 718.0.dp, 880.5.dp)
```

The map composable is ~411 dp wide, but the 5 sample columns spanned ~1024 dp. The old zoom-derived span (`720 / 2^zoom`) assumed 256 px tiles, while MapLibre Compose 0.15.0 renders on a 512 dp tile scale at this zoom. Only the centre column could ever be on screen; the rest of the grid was off-screen on both sides. The original HEAD default span (8°×10°) was even wider.

**Conclusion:** even with the layer restored, a zoom-only span is not reliable. The sampling window must come from the map’s actual visible bounding box.

---

## 2. Fix implemented

No new dependencies. No architecture changes. Public gh-pages repository and source location unchanged.

### New files

| File | Purpose |
|---|---|
| `presentation/map/MapOverlayData.kt` | GeoJSON builders for temperature points and wind arrows; MapLibre `step` colour expressions shared with the legend; viewport-derived `GridBounds`; latitude/longitude-normalised grid sampling; antimeridian-safe longitudes; wind-arrow geometry. |
| `test/java/com/vayu/weather/presentation/map/MapOverlayDataTest.kt` | 14 regression tests covering feature counts, property values, colour bands, arrow direction/shape, viewport bounds, antimeridian/pole clamping, API coordinate order, arrow sizing, and a realistic 25-sample viewport grid. |

### Modified files

| File | Change |
|---|---|
| `presentation/map/MapViewModel.kt` | Added `MapGridState` (loading/error) and `retryGridData()`. `loadGridData(centerLat, centerLon, spanLat, spanLon)` now accepts the viewport-derived span and tracks the last request for retry. On success it publishes points; an empty successful response reports *“No weather data available for this area”*; if every sample request fails (offline/API down) it reports *“Couldn't load the weather layer”*. Removed the dead `visibleBounds` field and the zoom→span guess from `onBoundsChanged()`, which now only refreshes alerts and the centre weather card. |
| `presentation/map/WeatherMapScreen.kt` | Derives `overlayBounds` from `cameraState.viewport.visibleBoundingBox`. A debounced `LaunchedEffect` re-samples the grid when the active overlay or viewport changes. Temperature renders as a blurred `CircleLayer`; wind renders as coloured `LineLayer` arrows. Added a loading/error card with Retry. Selecting temperature/wind auto-shows the matching legend. The no-op opacity slider is hidden for temperature/wind (radar/cloud keep theirs). Removed the dead colour helpers replaced by the shared legend bands. |

### Behaviour guarantees

- **Renders within the acceptance window** — a successful 25-sample load appears immediately; the status card covers the loading interval.
- **Viewport-accurate** — the grid centre and span are the visible bounding box, so all five sample rows/columns cover the current screen at every zoom.
- **Pan/zoom refresh** — a 700 ms debounce re-samples after the camera settles; in-flight requests are cancelled when a newer viewport arrives.
- **Explicit empty/error state** — an empty or failed grid is never an unexplained blank map; the selected layer shows a status card and Retry.
- **Legend truth** — the map layer and legend use the same colour stops; legend auto-opens for both data layers.
- **No dead control** — temperature/wind opacity is no longer offered because it had no effect on these layers.
- **Clean layer lifecycle** — switching overlays removes the previous MapLibre layer/source before adding the new one; selecting None removes both overlay and legend.

---

## 3. Unit tests (regression coverage)

`MapOverlayDataTest` — **14/14 pass**:

| Area | Tests |
|---|---|
| Temperature data | One point per sample with `lon`/`lat` order and `temp` property; empty grid produces zero features; colour bands match the legend. |
| Wind data | One line per sample with `speed`; arrows point downwind for N/E/S wind; each arrow has a shaft and two head arms; colour bands match the legend. |
| Grid sizing | Viewport bounds produce the expected centre/spans; antimeridian viewport wraps correctly; coordinates are `(latitude, longitude)` for Open-Meteo; poles/antimeridian stay renderable; arrow length is shorter than spacing. |
| Integration-shaped | A real viewport-sized grid produces 25 temperature features and 25 wind features. |
| Longitude | Normalisation wraps 190°/−190°/180°/360° correctly. |

**Full suite:** 34 suites, **395/395 pass**, 0 failures, 0 errors, 0 skipped (baseline 381 + 14 new).

---

## 4. Build / lint results

Command:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
```

| Gate | Result |
|---|---|
| `testDebugUnitTest` | **BUILD SUCCESSFUL — 395/395 pass** |
| `lintDebug` | **BUILD SUCCESSFUL — 0 errors, 0 warnings, 1 hint** (same pre-existing hint baseline) |
| `assembleDebug` | **BUILD SUCCESSFUL** |

---

## 5. Emulator verification (API 37, 1080×2340)

Installed with `adb install -r`; no uninstall, no `pm clear`, no data reset.

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 1 | Select Temperature (zoom 10) | **PASS** — full-screen field renders; legend “Temperature −10°C … 35°C+” visible; cool cyan area over the lake | `artifacts/map_overlay_temperature.png` |
| 2 | Zoom in to 11 | **PASS** — field re-samples and still covers the full visible map; boundary/lake colours remain consistent | `artifacts/map_overlay_temperature_zoom11.png` |
| 3 | Select Wind (zoom 11) | **PASS** — 25 coloured arrows distributed across the viewport; legend “Wind Speed Calm … Gale” visible | `artifacts/map_overlay_wind_zoom11.png` |
| 4 | Select None | **PASS** — overlay and legend both disappear; map returns to basemap | `artifacts/map_overlay_none.png` |
| 5 | Pan while Temperature is active | **PASS** — 25 requests with new coordinates; overlay re-samples and covers the moved viewport | `artifacts/map_overlay_temperature_panned.png` |
| 6 | Switch Temperature → Wind → None → Temperature | **PASS** — MapLibre log shows the old layer/source removed before the new one is added; no duplicate/stale layer | logcat (see below) |
| 7 | Offline error state (airplane mode, cold start, empty cache) | **PASS** — status card shows “Couldn't load the weather layer” + Retry; 25 `UnknownHostException` failures; no crash | `artifacts/map_overlay_error_offline.png` |
| 8 | Retry after restoring network | **PASS** — tapping Retry reloads 25 samples and restores the full overlay and legend | `artifacts/map_overlay_error_retry_recovered.png` |
| 9 | Crash/error monitor | **PASS** — 0 `FATAL EXCEPTION`, 0 MapLibre errors, 0 HTTP 4xx/5xx in the app process during normal operation | logcat |

Representative MapLibre lifecycle logs:

```
I/maplibre-compose: Queuing layer temperature-overlay for addition at anchor Top, index 0
I/maplibre-compose: Adding source __MAPLIBRE_COMPOSE_source_0
I/maplibre-compose: Initializing anchor Top with layer temperature-overlay
...
I/maplibre-compose: Removing layer temperature-overlay
I/maplibre-compose: Queuing layer wind-overlay for addition at anchor Top, index 0
I/maplibre-compose: Removing source __MAPLIBRE_COMPOSE_source_0
I/maplibre-compose: Adding source __MAPLIBRE_COMPOSE_source_1
I/maplibre-compose: Initializing anchor Top with layer wind-overlay
```

Pan verification (final build, zoom 11): one settled viewport produced 27 Open-Meteo GET lines — 25 grid samples plus 2 centre-weather refreshes — and 26 unique coordinates because the centre-weather request shares the grid centre. After the swipe the grid spans `39.139..39.335` latitude and `-123.168..-123.026` longitude, centred at `39.237, -123.097`, distinct from the pre-pan centre `-123.150`. All responses were HTTP 200.

Offline verification: airplane mode was enabled, the OkHttp `http_cache` was cleared via `run-as` (app data untouched), and the app was cold-started. Selecting Temperature produced the error card instead of a blank map; MapLibre showed no overlay. After airplane mode was disabled, tapping Retry produced 25 grid GETs and restored the full overlay. The `ping` probe used for a connectivity check lost its ICMP packet (ICMP is commonly filtered), but the subsequent HTTPS requests succeeded.

**Screenshot integrity:** all evidence PNGs were captured with `adb shell screencap -p` + `adb pull` and validated as loadable 1080×2340 images. The previously corrupted `artifacts/map_overlay_temp_fixed.png` (UTF-16 text from PowerShell redirection, not a PNG) was replaced with the valid final Temperature capture.

---

## 6. Files changed

```text
app/src/main/java/com/vayu/weather/presentation/map/MapOverlayData.kt      (new)
app/src/main/java/com/vayu/weather/presentation/map/MapViewModel.kt        (modified)
app/src/main/java/com/vayu/weather/presentation/map/WeatherMapScreen.kt    (modified)
app/src/test/java/com/vayu/weather/presentation/map/MapOverlayDataTest.kt  (new)
```

Evidence:

```text
artifacts/map_overlay_temperature.png
artifacts/map_overlay_temperature_zoom11.png
artifacts/map_overlay_temperature_panned.png
artifacts/map_overlay_wind_zoom11.png
artifacts/map_overlay_none.png
artifacts/map_overlay_error_offline.png
artifacts/map_overlay_error_retry_recovered.png
artifacts/map_overlay_temp_fixed.png      (replaced corrupted file)
```

No commit or push was made. The working tree remains uncommitted.

---

## 7. Residual risks / follow-ups

1. **Request volume** — one settle costs 25 Open-Meteo calls. The 700 ms debounce and job cancellation prevent request storms during a drag, but rapid repeated pans can still approach free-tier rate limits. A future bulk/min–max Open-Meteo request would remove this class of risk.
2. **Coarse field** — 5×5 samples are interpolated by MapLibre blur; small-scale variation (e.g. one valley) is not measured.
3. **Stale data during reload** — the previous grid remains visible until the new viewport load succeeds. If a later load fails while old points exist, the error card is not shown (`hasData` is true). The overlay is transiently in the previous location; a data-bounds check would be needed to fail closed.
4. **Emulator-only runtime evidence** — no physical device was attached. API 37 behaviour (MapLibre rendering, HTTP, lifecycle) is verified; API 24/31 was not re-run for this change.
5. **Instrumented UI/screenshot test** — `P1-03` asks for a screenshot test. The regression gate here is 14 unit tests plus manually captured, validated screenshots; no automated screenshot test was added. The 8 pre-existing instrumented failures remain waived per `VAAYU_ALERT_SYSTEM_AUDIT_REPORT.md` (`B11`).
6. **Legend appearance** — the temperature legend now shows 10 bands instead of 7, matching the exact colour stops used by the layer. Semantics are unchanged; visual output differs slightly.
7. **Opacity control** — temperature/wind no longer expose an opacity slider. This satisfies the “no visible no-op controls” requirement but is a deliberate UI change; radar/cloud sliders are unaffected.
8. **Requirements status** — `F-11`, `J-06`, `CR-14`, `P0-10`, and `P1-03` are now implemented and evidenced, but the source requirement document and historical defect register were not edited in this task. Owner may update their status fields when accepting the fix.
