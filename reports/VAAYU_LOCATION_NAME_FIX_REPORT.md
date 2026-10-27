# VAAYU LOCATION NAME FIX REPORT — SkyCast Weather (`com.vayu.weather`)

**Report ID:** VAAYU_LOCATION_NAME_FIX_REPORT  
**Date:** 2026-10-20  
**Issue:** Weather home header showed the literal "Current Location" instead of the real city/locality name  
**Device:** Samsung Galaxy M31 (SM-M315F), Android 12 (API 31), baseline 1080×2340 @420dpi  
**Transport:** wireless ADB, serial `adb-RZ8N935MQTF-Sty2BK._adb-tls-connect._tcp` (never disabled)  
**Build under test:** debug, `assembleDebug` + `install -r` — **app data preserved** (no uninstall, no `pm clear`)  
**Status of changes:** **uncommitted** — awaiting owner authorization

---

## 1. Root cause (confirmed, code + device)

The title was never lost in the UI layer — **no place name was ever resolved for the GPS position**, so the UI fell back to the localized string by design.

**Data flow traced**

| Stage | File | Finding |
|---|---|---|
| Location acquisition | `data/location/DefaultLocationTracker.kt` | Returns a raw `android.location.Location` (lat/lon only). **No geocoding anywhere in the app** — `android.location.Geocoder` was never used and the existing `GeocodingApi` (Open-Meteo) only supports *forward* search by name, so it cannot reverse-resolve coordinates. |
| ViewModel | `presentation/weather/WeatherViewModel.kt:148` | In the GPS branch: `currentCityName = null` — *"A GPS fix is the device's own location, not a picked city."* The name was deliberately dropped and **nothing ever set it again** for a GPS position. |
| Fallback branch | `WeatherViewModel.kt:183` | `currentCityName = settingsManager.getLastCity()` — but `LAST_CITY` was never written for GPS (the persist at line ~187 passes the always-null `currentCityName`), so the fallback was empty too. |
| UI state | `presentation/weather/WeatherState.kt:18` | `regionName` ("Region/state name … for richer location display") existed but was **never assigned** — a dead field. |
| Header | `presentation/weather/SkyCastHomeScreen.kt:476` | `displayCity = cityName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.current_location)` → always the fallback for GPS. |
| Weather payload | `data/remote/dto/WeatherDto.kt` | The Open-Meteo forecast response carries lat/lon only — **no city name to reuse** from the weather response. |
| Persistence | `data/local/SettingsManager.kt:443` | `setLastLocation(lat, lon, city)` writes `LAST_CITY` only when the city is non-blank → GPS runs never stored a name. |

**Conclusion:** the human-readable name was missing at the source (never resolved), not lost in mapping. The manual-city path worked, which is why only the GPS header was affected.

**Device reproduction (pre-fix):** `artifacts/location_fix/00_before_current_location.png` — header reads "Current Location", subtitle "Updated 20:19" (no region), while the weather block itself is fully populated (26°, Partly Cloudy, H/L/Feels, rain chart, alerts).

---

## 2. Fix implemented

Reverse geocoding now happens with the **platform `android.location.Geocoder`** — no new dependency, no new permissions, no hardcoded place names, no blocked main-thread work.

### New files

| File | Purpose |
|---|---|
| `domain/location/LocationNameResolver.kt` | `LocationNameResolver` interface + `ResolvedPlaceName(city, region)` data class (domain layer, mirrors the existing `LocationTracker` pattern). |
| `data/location/DefaultLocationNameResolver.kt` | Implementation: platform `Geocoder` reverse lookup, **API 33+ async `GeocodeListener`**, below 33 the blocking API **on `Dispatchers.IO`**, bounded by a **5 s timeout**, `Geocoder.isPresent()` pre-check (API 31+), exceptions swallowed with a log, and an **≈100 m coordinate cache** so repeated refreshes do not trigger a new round trip. Name selection prefers `locality` → `subAdminArea` → `adminArea` → `featureName` (a street-level `featureName` is only a last resort); region = `adminArea`/`subAdminArea`/`countryName`, suppressed when equal to the city. |

### Modified files

| File | Change |
|---|---|
| `di/LocationModule.kt` | `@Binds @Singleton` for `LocationNameResolver` (same pattern as `LocationTracker`). |
| `presentation/weather/WeatherViewModel.kt` | Injects the resolver. GPS branch: keeps a name only when it already belongs to the **same coordinates**, otherwise clears it and calls `resolveNameForLocation(lat, lon)`. New `resolveNameForLocation()`: skips the lookup when a name is known, runs it asynchronously, applies the result **only if the coordinates still match** (stale guard), updates `currentCityName` + `state.regionName`, and persists the name via `setLastLocation`. `loadWeatherForCity()` (manual pick) **cancels the in-flight lookup** and sets the region. `refreshWithCoords()` re-resolves only when no name is known, so a refresh never drops the name. |
| `presentation/weather/SkyCastHomeScreen.kt` | Header subtitle now shows `Region · Updated HH:mm` when a region is known — the "updated" stamp is no longer hidden by the region. Title logic unchanged (it now receives a real name). |
| `presentation/VayuApp.kt` | Manual selections (Search result, Favorites card, Compare card) pass `city.admin1` as the region. |
| `presentation/favorites/SkyCastFavoritesScreen.kt` | `mutableStateOf(0f)` → `mutableFloatStateOf(0f)` (lint hint cleanup, keeps the lint baseline at 0/0/1). |

### Behaviour guarantees

- **Asynchronous** — the platform lookup runs on `Dispatchers.IO` with a 5 s timeout; the UI thread is never blocked.
- **No stale overwrite** — a result is applied only when `currentLat/currentLon` still match the lookup's coordinates; a manual pick cancels the pending lookup.
- **Manual + saved cities preserved** — `loadWeatherForCity()` sets the name/region itself and is never overwritten by a GPS lookup.
- **No repeated lookups** — a known name short-circuits the resolver, and the resolver caches per ~100 m coordinate grid.
- **Graceful fallbacks** — denied permission, missing geocoder backend, empty locality, timeout or network failure all leave the existing localized `R.string.current_location` fallback in place; no error banner, no crash, nothing suppressed.
- **Persistence** — the resolved name is stored as `LAST_CITY`, so the no-GPS fallback path, the home widget and the notification worker now show the real city too.

---

## 3. Unit tests (regression coverage)

`app/src/test/java/com/vayu/weather/presentation/weather/WeatherViewModelTest.kt` — 7 new tests added (345 → 352):

| Test | Asserts |
|---|---|
| `gps location resolves the place name for the header` | Name + region reach the UI state. |
| `resolved place name is persisted for the next launch` | `setLastLocation(lat, lon, "Srikakulam")` is written exactly once. |
| `unresolvable place keeps the current-location fallback without error` | Null name → fallback retained, weather still loaded, no error. |
| `failing place lookup does not break the weather load` | A throwing resolver must not break the load. |
| `manual city selection is not overwritten by a late place lookup` | "London"/"England" survive a concurrent lookup. |
| `place lookup of a stale position does not overwrite the current one` | A late result for old coordinates is discarded. |
| `a known place name is not looked up again` | A refresh performs **no** second lookup (no repeated geocoding). |

---

## 4. Build / test / lint results

| Command | Result |
|---|---|
| `.\gradlew testDebugUnitTest` | **BUILD SUCCESSFUL — 352/352 pass**, 30 suites, 0 failures, 0 errors, 0 skipped (baseline was 345/345; +7 new). |
| `.\gradlew lintDebug` | **BUILD SUCCESSFUL — 0 errors, 0 warnings, 1 hint** (identical to the pre-existing baseline; the one extra hint introduced by the earlier favourites work was removed). |
| `.\gradlew assembleDebug` | BUILD SUCCESSFUL |
| `.\gradlew assembleRelease` | BUILD SUCCESSFUL |

---

## 5. Physical-device verification (M31, API 31)

All via `adb -s <SERIAL>`; app installed with `install -r` (**data preserved**); no uninstall, no `pm clear`, no connectivity/airplane changes, no ad clicks.

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 1 | Current location resolves to a real name | **PASS** — header shows **"Duppulapadu"** (locality), subtitle "Andhra Pradesh · Updated 23:28" | `02_after_load.png`, `11_final_verified.png` |
| 2 | Name appears after the loading state | **PASS** — cold start shows the loading state, then the resolved name appears | `01_loading.png` → `02_after_load.png` |
| 3 | Refresh does not revert to "Current Location" | **PASS** — a11y node after refresh: `content-desc="Location: Duppulapadu, Andhra Pradesh"` | `03_after_refresh.png` + dump |
| 4 | Location change updates name **and** weather | **PASS** — tapping the saved city switched the header to "Srikakulam, Andhra Pradesh" and reloaded that location's weather | `05_saved_city.png` + dump |
| 5 | Manual / saved city names retained | **PASS** — Favorites → Srikakulam → `Location: Srikakulam, Andhra Pradesh` (name + region from the city's `admin1`) | `05_saved_city.png` |
| 6a | Approximate location (coarse only, fine revoked) | **PASS** — resolves and displays "Duppulapadu, Andhra Pradesh"; no crash | `06_approximate_location.png` |
| 6b | Denied location permission (both revoked) | **PASS** — the app falls back to the persisted coordinates **and the persisted name** ("Duppulapadu"); no error screen, no crash | `07_permission_denied.png` + dump |
| 6c | Unavailable geocoding / no locality | **PASS (unit-tested)** — resolver returns `null`/throws → localized fallback retained, weather still loads, no error. Not reproducible on-device (the device has a working geocoder backend) | unit tests above |
| 7 | Leave and reopen the app | **PASS** — force-stop + relaunch → `Location: Duppulapadu, Andhra Pradesh` (no wrong/stale name) | `04_after_restart.png` + dump |
| 8a | Font scale 1.3 | **PASS** — title fully rendered, no clipping: `Duppulapadu [116,120][421,179]` | `08_font_scale_1_3.png` |
| 8b | 720×1560 @320dpi (small screen) | **PASS** — title fully rendered: `Duppulapadu [88,92][271,127]` (screen width 720) | `09_small_720x1560_320.png` |
| 8c | Landscape (`user_rotation=1`) | **PASS** — title + region + timestamp all visible | `10_landscape.png` |
| — | Crash monitor | **PASS** — 0 `FATAL EXCEPTION` / `ANR in com.vayu` / `Resources$NotFoundException` in the app process | `final_logcat.txt` |

**Device state restored and re-verified after testing:** `wm size` = 1080×2340, `wm density` = 420, `user_rotation` = 0, `accelerometer_rotation` = 0, `font_scale` = 1.0. Location permissions restored to the original state (`ACCESS_FINE_LOCATION: granted=true`, `ACCESS_COARSE_LOCATION: granted=true`).

**Owner data:** untouched. The only persisted change is the app's own new behaviour: `LAST_CITY` now holds the resolved locality ("Duppulapadu"), which is what the fallback path, the widget and the notification workers read.

### Before / after

| | Before (pre-fix build) | After (fixed build) |
|---|---|---|
| Header | `Current Location` / `Updated 20:19` | `Duppulapadu` / `Andhra Pradesh · Updated 23:28` |
| Files | `artifacts/location_fix/00_before_current_location.png` | `artifacts/location_fix/02_after_load.png`, `11_final_verified.png` |

---

## 6. Remaining limitations

1. **No geocoder backend** (some AOSP builds without Google Play Services): the platform `Geocoder` reports "not present" and the app keeps the localized "Current Location" fallback. A server-side reverse-geocode API would be needed for those devices — not added, to avoid a new dependency/endpoint.
2. **No network** during the lookup: same fallback; the name is re-resolved on the next successful load.
3. **Named locality vs. city**: the platform returns the most specific available place (here a village/locality). That is intentional — "city or locality" per the report — and is more accurate than a hardcoded guess.
4. **"Geocoder backend unavailable"** is only logged, never surfaced to the user (no error banner for a cosmetic title).
5. The weather payload still carries no city name (Open-Meteo returns lat/lon only), so the platform geocoder remains the single source for the GPS title.
6. Pre-existing, unrelated: history snapshots taken before a name is resolved are stored as "Unknown" (unchanged behaviour, not a regression).

---

## 7. Files changed (all uncommitted)

```
app/src/main/java/com/vayu/weather/domain/location/LocationNameResolver.kt            (new)
app/src/main/java/com/vayu/weather/data/location/DefaultLocationNameResolver.kt      (new)
app/src/main/java/com/vayu/weather/di/LocationModule.kt                             (binding)
app/src/main/java/com/vayu/weather/presentation/weather/WeatherViewModel.kt         (resolve + guards + persist)
app/src/main/java/com/vayu/weather/presentation/weather/SkyCastHomeScreen.kt        (subtitle: region + timestamp)
app/src/main/java/com/vayu/weather/presentation/VayuApp.kt                           (region for manual picks)
app/src/main/java/com/vayu/weather/presentation/favorites/SkyCastFavoritesScreen.kt (lint hint cleanup)
app/src/test/java/com/vayu/weather/presentation/weather/WeatherViewModelTest.kt     (+7 regression tests)
```

No pre-existing user changes or untracked files were modified; no commit or push was made.

---

## 8. Verdict

The Weather screen now displays the real resolved locality instead of remaining on "Current Location": reverse geocoding runs off the main thread, is guarded against stale results, never overrides a manual or saved city, and degrades to the existing localized fallback in every failure mode. 352/352 unit tests, lint 0/0/1, debug and release builds green, and every executable device scenario above was verified live on the M31 with before/after screenshots and logcat evidence.