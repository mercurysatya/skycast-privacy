# SkyCast Weather — UI/UX & Accessibility Audit

**App:** SkyCast Weather (Vayu) · `com.vayu.weather` · v1.5 (versionCode 5) · minSdk 24 / targetSdk 37  
**UI framework:** Jetpack Compose + Material 3 · Kotlin 2.3.21  
**Auditor role:** Senior UI/UX Engineer & Accessibility Tester  
**Date:** 2026-10-29  
**Working tree:** Branch `gh-pages`, 36 modified files (uncommitted, from prior audit series)

---

## 1. Testing environment & methods (limitations stated up front)

| Item | Status | Detail |
|------|--------|--------|
| Physical device | **Not available** | No Android device or emulator accessible in this environment. No `adb`, no Android SDK. |
| Instrumented UI tests | **Not run** | `connectedDebugAndroidTest` cannot execute without a device/emulator. |
| Unit tests | **Not run** | `testDebugUnitTest` cannot execute without the Android SDK / compileSdk 37. |
| Gradle build / lint | **Not run** | `assembleDebug`, `assembleRelease`, `lintDebug` require the Android SDK. |
| Screenshots | **None captured** | No device to render UI. |
| Method used | **Static code analysis** | Line-by-line review of every screen, component, theme token, navigation route, and strings resource. Cross-checked against prior audit reports in `reports/`. |

**Configurations reviewed by code inspection (not executed):**
- Screen sizes: phones (720×1560 @320dpi), baseline (1080×2340 @420dpi), tablets/foldables (FlowRow 4-col grid >480dp width), narrow (720×2340 @320dpi), high density (560dpi)
- Orientations: portrait (`user_rotation=0`) + landscape (`user_rotation=1`)
- Font scaling: 1.0 → 1.3 (API 31 max) — reviewed for single-line ellipsis, wrapping, clipping
- System themes: light (`VayuLight`), dark (`VayuDark`), dynamic color (Material You on API 31+)
- Locales: `values` (en), `values-hi`, `values-kn`, `values-ml`, `values-ta`, `values-te`

---

## 2. Screens & interactive elements inventory

| # | Screen / composable | Route | Navigation type | Primary actions |
|---|---------------------|-------|-----------------|-----------------|
| 1 | Splash / gate | — | Auto | None (timed gate) |
| 2 | Onboarding (5 pages) | — | Modal, pager | Location permission, notifications, Skip, Next, Get Started |
| 3 | Home dashboard (SkyCast) | `weather` | Primary (bottom nav) | Refresh, share, settings, alerts, history, detail, unit toggle, theme toggle, metric detail sheets, rewarded ad |
| 4 | Search | `search` | Primary (bottom nav) | Query entry, city pick, favorite toggle, clear recents, browse popular, open travel |
| 5 | Favorites (SkyCast) | `favorites` | Primary (bottom nav) | City pick, remove, long-press drag reorder, browse cities, compare |
| 6 | Map / radar | `map` | Primary (bottom nav) | Layer panel, zoom, compass reset, my location, search, legend, time slider, favorite pins |
| 7 | Settings | `settings` | Secondary (modal) | Units, theme, notifications, alert thresholds, quiet hours, data deletion, premium |
| 8 | Alerts | `alerts` | Secondary (modal) | Severity/weather-type filters, snooze, delete, clear all dialog |
| 9 | Weather detail | `detail` | Secondary (modal) | Back, hourly/daily expansion, metric info items |
| 10 | History | `history` | Secondary (modal) | Range chips, clear-history dialog, charts |
| 11 | Privacy policy | `privacy` | Secondary (modal) | Back (WebView) |
| 12 | Premium paywall | — | Secondary (modal) | Offer selection, purchase, restore, back |
| 13 | Compare | `compare` | Secondary (stack) | Add/remove city, tap city, back |
| 14 | Travel | `travel` | Secondary (stack) | Destination pick, date prev/next, refresh, back |

**Navigation controller:** Custom `SkyCastNavController` (saveable, type-safe `Routes` sealed class). Bottom nav shows only on primary destinations when stack is empty. System back handled by a single `BackHandler` that pops modals in reverse order — no dead-end screens identified.

---

## 3. Defects fixed in this audit (3 items)

All three are **touch-target violations** below WCAG 2.5.5 (minimum 44×44 dp) and below the app's own `SkyCastTokens.TouchTargetMin = 44.dp` / `TouchTargetComfortable = 48.dp`. None change business behavior.

### F1 — WeatherDashboard header action buttons: 36 dp touch target

**File:** `app/src/main/java/com/vayu/weather/presentation/weather/WeatherDashboard.kt`  
**Element:** `SmallIconButton` (private composable) used by `TopBar` for Alerts, History, Share, Theme, Settings (5 instances)  
**Defect:** `Modifier.size(36.dp)` on the clickable `Box`. 36 dp < 44 dp WCAG minimum; difficult to tap accurately, especially with motor impairment or at high font scale.  
**Fix:** `Modifier.size(SkyCastTokens.TouchTargetComfortable)` (48 dp). Added `import com.vayu.weather.ui.theme.SkyCastTokens`.  
**Business impact:** None — visual only; icons remain 18 dp centered in a larger touch target.

### F2 — Favorites card remove button: 36 dp touch target + vague label

**File:** `app/src/main/java/com/vayu/weather/presentation/favorites/SkyCastFavoriteCard.kt`  
**Element:** Trailing `IconButton(Close)` on each favorite city card  
**Defect:** `Modifier.size(36.dp)` (< 44 dp) and hard-coded `contentDescription = "Remove"` (not city-specific, not translatable-friendly).  
**Fix:** `Modifier.size(SkyCastTokens.TouchTargetComfortable)` (48 dp); `contentDescription = stringResource(R.string.remove_from_favorites, cityName)`. Added `import androidx.compose.ui.res.stringResource`.  
**Business impact:** None; label now names the city ("Remove Srikakulam from favorites").

### F3 — Search "Clear all recent searches" button: 28 dp touch target

**File:** `app/src/main/java/com/vayu/weather/presentation/search/SearchScreen.kt`  
**Element:** `IconButton(DeleteSweep)` beside the "Recent searches" header  
**Defect:** `Modifier.size(28.dp)` (< 44 dp).  
**Fix:** `Modifier.size(SkyCastTokens.TouchTargetComfortable)` (48 dp). Added `import com.vayu.weather.ui.theme.SkyCastTokens`.  
**Business impact:** None.

---

## 4. Pre-existing defects already fixed by prior audit (documented, uncommitted)

The prior `VAAYU_UI_UX_AUDIT_REPORT.md` fixed 18 defects (1 blocker + 9 majors + 8 minors), all verified on a physical Samsung Galaxy M31 (API 31) across a size/density/orientation/font-scale matrix. These fixes are present in the working tree but **not committed**. They are re-listed here for completeness; none were re-introduced by my changes.

| ID | Severity | Summary | File(s) |
|----|----------|---------|---------|
| B1 | Blocker | Search empty/unresults content unscrollable (last popular city unreachable) | `SearchScreen.kt` |
| M1 | Major | Alerts "Snooze" was a dead-end (empty callbacks, read-only field, crash on non-numeric input) | `AlertsScreen.kt`, `AlertsViewModel.kt`, `AlertsState.kt`, `strings.xml` |
| M2 | Major | Dashboard sections ignored visibility settings (hourly/details/AQI/sun-moon rendered unconditionally) | `SkyCastHomeScreen.kt` |
| M3 | Major | Map search bar overlapped the compass/zoom control rail | `WeatherMapScreen.kt` |
| M4 | Major | Map legend overlapped favorites strip, stealing taps | `WeatherMapScreen.kt` |
| M5 | Major | Radar time-slider overlapped expanded favorites strip | `WeatherMapScreen.kt` |
| M6 | Major | Compare table columns misaligned with headers when < maxCities | `SkyCastCompareScreen.kt` |
| M7 | Major | Travel date picker ran past the 14-day forecast horizon, no way back | `SkyCastTravelScreen.kt`, `TravelViewModel.kt` |
| M8 | Major | Onboarding CTA could sit under system gesture nav | `OnboardingScreen.kt` |
| M9 | Major | Favorites reorder hint was a lie — no drag gesture existed | `SkyCastFavoritesScreen.kt` + Room migration v4→v5 |
| m1 | Minor | Search double status-bar padding | `SearchScreen.kt` |
| m2 | Minor | Home/Favorites dead band at list bottom (100 dp → 24 dp) | `SkyCastHomeScreen.kt`, `SkyCastFavoritesScreen.kt` |
| m3 | Minor | Map mini-card condition text could overflow | `WeatherMapScreen.kt` |
| m4 | Minor | Alerts severity chips could overflow horizontally | `AlertsScreen.kt` |
| m6 | Minor | Onboarding last page hard-coded English string | `OnboardingScreen.kt`, `strings.xml` |
| m7 | Minor | AdBanner singleton invariant documented | `AdBanner.kt` |
| m8 | Minor | Map watermark could paint off-canvas on narrow captures | `WeatherMapScreen.kt` |

**Deferred (documented in prior report §7):** `m5` (dead-code deletion of legacy `FavoritesScreen.kt`), POST_NOTIFICATIONS UX (API 31 device), offline/airplane fallback (contract-forbidden).

---

## 5. Accessibility review (WCAG + Material guidelines)

### 5.1 Touch targets — 44 dp minimum (WCAG 2.5.5)

| Element | Size | Status | Fix |
|---------|------|--------|-----|
| `WeatherDashboard.SmallIconButton` (×5) | 36 dp | ❌ Fail | ✅ Fixed → 48 dp |
| `SkyCastFavoriteCard` remove `IconButton` | 36 dp | ❌ Fail | ✅ Fixed → 48 dp |
| `SearchScreen` clear-recents `IconButton` | 28 dp | ❌ Fail | ✅ Fixed → 48 dp |
| `SkyCastHomeScreen` `CompactAction` (refresh/share/more) | 48 dp | ✅ Pass | — |
| Map zoom/compass `SmallFloatingActionButton` | 48 dp | ✅ Pass | — |
| Map main FAB (menu) | 52 dp | ✅ Pass | — |
| `FavoritesEmptyStateV2` browse `Button` | Material default (≥48 dp) | ✅ Pass | — |
| `OnboardingScreen` Skip / Get Started `Button` | Material default | ✅ Pass | — |
| Settings rows (`Switch`, `TextButton`, `Slider`) | Material default | ✅ Pass | — |
| `FilterChip` / settings dropdown chips | Material default | ✅ Pass | — |

### 5.2 Content descriptions & screen-reader labels

| Pattern | Status | Notes |
|---------|--------|-------|
| Decorative icons (`contentDescription = null`) inside a parent with `mergeDescendants = true` / explicit `contentDescription` | ✅ Acceptable | E.g. `SkyCastHero`, `SkyCastMetricCard`, `SkyCastSummaryCard`, `SkyCastAlertCard`, `LocationHeader` |
| Functional icon inside a clickable container that exposes its own `contentDescription` | ✅ Acceptable | E.g. `TopBar.SmallIconButton`, `CompactAction`, `DetailInfoItem` (icon has `contentDescription = label`) |
| `DetailInfoItem` (WeatherDetailScreen) | ⚠️ Note | `.semantics { }` empty block is a no-op (not harmful, not helpful). The icon's `contentDescription = label` carries the accessible name. |
| `SkyCastFavoriteCard` remove button | ✅ Fixed | Now `stringResource(R.string.remove_from_favorites, cityName)` |
| `SearchScreen` TextField | ✅ Pass | Leading icon `contentDescription = stringResource(R.string.search_for_city)` |
| `CompareScreen` add-city slot | ✅ Pass | `contentDescription = "Add"` |
| `PremiumScreen` benefit icons | ⚠️ Note | `contentDescription = null` — decorative checkmarks adjacent to visible text labels; acceptable |
| `OnboardingScreen` page icons | ⚠️ Note | `contentDescription = null` — decorative icons adjacent to title/description text; acceptable |

**Missing `contentDescription` candidates** (reviewed, judged decorative because adjacent text or parent semantics convey meaning):
- `WeatherMapScreen`: map pins, weather icons (mini-card uses icon only but city/temp text adjacent), favorite pin icons
- `WeatherDashboard`: daily-row weather icons (`contentDescription = plainWeatherDescription(...)` — ✅ provided)
- `SettingsScreen`: `ExpandMore`/`ArrowDropDown` dropdown icons (`contentDescription = null`) — adjacent to a `TextButton` that is the actual target; acceptable

### 5.3 Contrast

The theme uses `MaterialTheme.colorScheme` with `onBackground`, `onSurface`, `onSurfaceVariant` (alpha 0.5–0.6 for secondary text) and `WeatherOpacity.TEXT_TERTIARY = 0.50`, `TEXT_DISABLED = 0.35`.

**Risk areas (cannot verify contrast ratio without rendered pixels):**
- Secondary/tertiary text at `alpha = 0.5` on light backgrounds (e.g. `bodySmall` at 0.5, `labelSmall` at 0.4–0.5) — may fall below WCAG AA 4.5:1 for normal text or AAA 7:1 at small sizes (<18pt). **Unverifiable statically; requires contrast-tool check on rendered output.**
- White text at `alpha = 0.6` on dark hero backgrounds — likely passes AA but unverified.
- `SkyCastLoadingState` subtitle at `alpha = 0.6` — small body text, potential AA concern.

**Recommendation:** Run a contrast audit (e.g., Accessibility Scanner or pixel-level check) on the built APK, focusing on `labelSmall`/`bodySmall` secondary text on both light and dark themes.

### 5.4 Text overflow & font scaling (1.3×)

| Pattern | Status | Notes |
|---------|--------|-------|
| `Text(..., maxLines = 1, overflow = Ellipsis)` | ✅ | Used consistently for city names, condition text, subtitles (header, hero, daily rows, favorite cards) |
| `SkyCastHomeScreen` subtitle line | ✅ | Region + timestamp joined with ` · `, ellipsis-guarded |
| `SearchScreen` TextField `singleLine = true` | ⚠️ Note | At 1.3× font scale, a long query may not scroll horizontally; Compose `TextField` is single-line by default and does scroll, but verify on device |
| `AlertCard` title `maxLines = 1, overflow = Ellipsis` | ✅ | |
| `MapWeatherCard` description `maxLines = 1, overflow = Ellipsis` | ✅ | (fixed in prior audit m3) |
| `CompareScreen` city name `maxLines = 1` | ✅ | |
| `TravelScreen` destination text | ⚠️ Note | `"${it.name}, ${it.country ?: ""}"` — no maxLines/overflow; long city+country could overflow the card at 1.3× on small screens |

### 5.5 Loading / empty / error states

| State | Coverage | Status |
|-------|----------|--------|
| Loading (initial) | `SkyCastLoadingState` + `WeatherDashboard.LoadingState` shimmer | ✅ Present, `contentDescription = "Loading weather data"` |
| Loading (refresh) | `PullToRefreshBox(isRefreshing=...)`, offline banner | ✅ Present |
| Error (no data) | `SkyCastErrorState` with Retry + Grant Permission | ✅ Present, accessibility labels on buttons |
| Error (map grid) | `MapGridStatusCard` with Retry | ✅ Present |
| Empty (alerts) | Empty state with icon + subtitle | ✅ Present |
| Empty (favorites) | `FavoritesEmptyStateV2` with Browse button | ✅ Present (not a dead end) |
| Empty (search results) | Search-off icon + "No cities found" | ✅ Present (prior fix B1 made it scrollable) |
| Empty (compare) | Empty state with "Add cities" button | ✅ Present (prior fix M6 ensured it is not a dead end) |
| Empty (history) | `EmptyHistoryState` | ✅ Present |
| Empty (travel) | "Pick a destination…" / out-of-range message | ✅ Present (prior fix M7) |

### 5.6 Duplicate controls

| Potential duplicate | Assessment |
|---------------------|------------|
| `FavoritesScreen.kt` (legacy, with drag code) vs `SkyCastFavoritesScreen.kt` (live) | Legacy is **dead code** — not referenced by `VayuApp.kt`. Documented as `m5` (deferred deletion). No runtime duplication. |
| Alerts screen (`AlertsScreen`) vs alert list on home (`SkyCastAlertList`) | Different scope (full history/filter vs current active only) — not duplicates. |
| Unit toggle in header menu vs Settings vs TopBar pill | Same action, different entry points — intentional, not a duplicate defect. |
| Theme toggle in header menu vs Settings vs TopBar | Same action, different entry points — intentional. |

### 5.7 Repeated user actions

No repeated-action traps identified. The snooze flow (prior fix M1) previously required re-tapping; now the duration field is editable and persists.

### 5.8 Keyboard / system navigation

- Compose `clickable`, `IconButton`, `Button`, `Switch`, `Slider`, `TextField`, `DropdownMenu` all provide native TalkBack/keyboard support.
- `BackHandler` centralises system back for all modals — consistent behavior.
- `BottomNavigation` / `NavigationBar` items are keyboard- and TalkBack-accessible (labels + icons).
- **Unverifiable:** actual TalkBack traversal order, focus capture in `DropdownMenu`, `ModalBottomSheet` (`SkyCastMetricDetailSheet`) focus management.

### 5.9 Scrolling

| Screen | Scroll container | Status |
|--------|------------------|--------|
| Search | `LazyColumn` + `EmptySearchContent.verticalScroll` | ✅ (prior fix B1) |
| Favorites | `LazyColumn` | ✅ |
| Settings | `verticalScroll(rememberScrollState())` | ✅ |
| History | `LazyColumn` | ✅ |
| Alerts | `LazyColumn` | ✅ |
| Detail | `LazyColumn` | ✅ |
| Map | MapLibre map (gesture) + overlays | ✅ |
| Home | `LazyColumn` | ✅ (prior fix m2: bottom padding 100→24 dp) |
| Premium | `verticalScroll(rememberScrollState())` | ✅ |
| Travel | `LazyColumn` | ✅ |
| Compare | `LazyColumn` | ✅ |

---

## 6. What remains unverified

The following cannot be confirmed without a physical device, emulator, Android SDK, and screen-reader tooling:

1. **Physical-device layout** across all screen sizes, densities (320/420/560 dpi), orientations, and font scales (1.0× / 1.3×).
2. **TalkBack / screen-reader** traversal order, announcement accuracy, focus management in modals (`DropdownMenu`, `ModalBottomSheet`, dialogs).
3. **Contrast ratios** (WCAG AA/AAA) for secondary/tertiary text on light and dark themes.
4. **Touch-target verification** of the three fixed buttons on actual touch input (48 dp target confirmed in code only).
5. **System theme switching** (light/dark/system/dynamic color) visual legibility.
6. **AdMob banner rendering** (singleton invariant, consent-gated loading) — code-reviewed only.
7. **Play Billing paywall** — no products configured; purchase flow is code-path reviewed, not exercised.
8. **UMP consent form** — gated by EEA geography; not testable from non-EEA location.
9. **POST_NOTIFICATIONS runtime prompt** (API 33+) — test device is API 31.
10. **Offline / airplane-mode behavior** — contract-forbidden (would break wireless ADB on the prior test device).
11. **Crashlytics / Firebase** integration in production.
12. **Home-screen widget (Glance)** — not part of the interactive screen audit (no Compose UI screen).
13. **Notifications / quiet hours** — settings reviewed, delivery not tested.

---

## 7. Files changed in this audit

| File | Change | Defect |
|------|--------|--------|
| `presentation/weather/WeatherDashboard.kt` | `SmallIconButton` 36 dp → `TouchTargetComfortable` (48 dp); added `SkyCastTokens` import | F1 |
| `presentation/favorites/SkyCastFavoriteCard.kt` | Remove `IconButton` 36 dp → 48 dp; `contentDescription` → `stringResource(R.string.remove_from_favorites, cityName)`; added `stringResource` import | F2 |
| `presentation/search/SearchScreen.kt` | Clear-recents `IconButton` 28 dp → 48 dp; added `SkyCastTokens` import | F3 |

All changes are in the working tree, **uncommitted**, consistent with the prior audit series' state (36 other files already modified, uncommitted).

---

## 8. Verdict

The app has a mature, token-driven design system (`SkyCastTokens`, `SkyCastColors`, `WeatherSpacing`, `WeatherShapes`) with 48 dp comfortable touch targets defined but not uniformly applied. The three touch-target defects fixed here (F1–F3) bring the header action buttons, favorite-card remove button, and search clear-recents button into compliance with WCAG 2.5.5 and the app's own `TouchTargetComfortable = 48 dp` token.

**Screen coverage:** All 14 screens and their interactive elements were inspected. No dead-end screens, broken buttons, misleading labels, or duplicate controls were found beyond the documented pre-existing defects (all already fixed in the working tree).

**Release blockers (pre-existing, owner-side):** No Google Play Billing products configured (paywall non-actionable), Data Safety declaration, content rating, and closed-testing setup remain owner Play Console actions (per `VAAYU_FINAL_RELEASE_READINESS.md`).

**Next steps:** Commit the working-tree changes, then run the full device matrix (size/density/orientation/font-scale), TalkBack verification, and contrast testing on physical hardware before production rollout.