# SkyCast Weather (Vayu) — Product Requirements Specification

| Field | Value |
|---|---|
| **Document ID** | VAAYU-PRS-001 |
| **Version** | 1.0 — Draft for owner review |
| **Date** | 2026-10-28 |
| **Status** | Not approved; decisions required |
| **Product** | SkyCast Weather, Android (`com.vayu.weather`) |
| **Baseline** | v1.5 / versionCode 5; source HEAD `ee3970f` |
| **Evidence cut-off** | 2026-10-28 |
| **Owner** | mercurysatya |
| **Prepared by** | Senior Business Analyst (AI-assisted) |
| **Supersedes** | None. Supplements the audit series; conflicts are recorded, not overwritten. |

> **Purpose.** This document defines what the product promises, what it actually does, and what must be true before it is released. It deliberately separates verified behaviour, public claims, gaps, and owner decisions. It does **not** assume Play Console access, user research, or legal advice.

---

## 0. How to read this document

### 0.1 Evidence tags

| Tag | Meaning |
|---|---|
| **V** | Verified in current source, build artifact, test output, or repository state |
| **P** | Partially implemented, limited scope, or verified only on one device/API level |
| **C** | Claimed in store copy, policy, README, or a report, but no supporting implementation was found |
| **X** | Not implemented, dead code, or a visible control with no effect |
| **U** | Unresolved contradiction or decision; do not treat either side as settled |

A report statement is not upgraded to **V** merely because it is written down. Where a report conflicts with source or artifacts, the conflict appears in §12.

### 0.2 Priority definitions

| Priority | Meaning | Release consequence |
|---|---|---|
| **P0** | Legal, store-compliance, revenue, safety, or truthfulness blocker | Must be closed before production submission |
| **P1** | Core product promise or material quality gap | Required for the next planned release; may be closed by removing the promise |
| **P2** | Quality, accessibility, scalability, or polish | Scheduled after P0/P1 |
| **P3** | Opportunity, not committed | Requires a separate product decision and funding |

### 0.3 Requirement provenance

| Code | Meaning |
|---|---|
| **D** | Derived from an existing public/product promise (store listing, policy, README, Premium UI copy) |
| **I** | Derived from implemented behaviour; the requirement ratifies what already ships |
| **B** | BA-proposed control needed to close a gap; **not previously promised**, owner must approve |
| **L** | Legal, Play policy, or platform obligation |

### 0.4 Reading rule for conflicts

- For **current behaviour**, running source and artifacts outrank reports.
- For **required behaviour**, an approved public promise outranks current code.
- When the promise and the code disagree, this document records both in §12 and states the decision needed. It does not silently choose one.

---

## 1. Executive summary

SkyCast is a functional, local-first Android weather app. The core experience — current conditions, hourly and 7-day forecasts, search, favorites, compare, travel, radar/cloud map, advisories, widget, themes, and units — is implemented and covered by a substantial unit-test baseline. The latest recorded unit run is **375/375 passing across 32 suites**, with **0 failures, 0 errors, 0 skipped**; lint is **0 errors, 0 warnings, 1 hint**.

The release is not blocked primarily by missing weather functionality. It is blocked by **four classes of product-truth debt**:

1. **Revenue path is incomplete.** `BillingProducts` contains empty product lists, so no Premium product can be purchased. The Premium screen advertises benefits that are not gated or not implemented.
2. **Privacy claims are contradicted by the app.** The published policy says location is never stored, logged, or shared; the app stores last location and coordinate-keyed records, logs coordinates on geocoder failure, and sends coordinates to third-party weather/map services. “Delete My Data” does not clear settings or last location.
3. **Store copy exceeds the implementation.** “Completely free,” “No personal data collected,” a “3-Day Forecast Widget,” dual EU/US AQI, and temperature/wind map layers are not all true.
4. **Several visible controls are no-ops or partial.** The three notification-time slots are stored and displayed but no worker reads them; quiet hours applies only to the daily notification worker, not severe advisories; temperature/wind map layers load data but render nothing.

There is also a release-process gap: the signed **AAB is dated 2026-10-18**, older than both the release **APK (2026-10-20)** and the **alert-system commit (2026-10-27)**. The AAB must be rebuilt from the release commit before upload.

**Verdict:** engineering core green; product truth and store compliance not green. **Do not submit to production until the P0 gate in §14 is closed.** The fastest safe path is to reduce the promise to what is true, correct the policy and deletion flow, configure billing, rebuild, and verify on API 33+.

**At a glance:** 15 P0 requirements, 18 P1 requirements, 8 P2 requirements, 14 NFRs, 28 business rules, 30 logged contradictions/decisions, and 25 explicit assumptions.

---

## 2. Scope, sources, and method

### 2.1 In scope

- Android app v1.5 (versionCode 5), package `com.vayu.weather`.
- Local data: Room, DataStore, SharedPreferences, widget state.
- Weather, search, favorites, compare, travel, map, AQI, history, advisories, notifications, widget.
- Ads, consent, Premium billing, entitlement.
- Store listing copy, privacy policy (hosted and in-app asset), Data Safety inputs, content rating inputs.
- Release artifacts and test gates.

### 2.2 Out of scope

- Play Console account operations themselves (owner action; this document defines the required outcome only).
- Legal advice, formal GDPR/COPPA certification, tax, pricing strategy.
- iOS, web app, backend services, user accounts (none exist).
- Official emergency-alert feeds (CAP/IPAWS); current advisories are forecast-derived.
- Physical-device procurement and Play review timelines.

### 2.3 Evidence sources

| Source | Use | Caveat |
|---|---|---|
| Source at HEAD `ee3970f` | Current behaviour | Highest authority for what ships |
| Unit-test XML (32 files) | Test baseline | Latest result 2026-10-20 02:19 |
| Lint report | Static quality | Latest 2026-10-20 02:26; 1 hint |
| Release APK/AAB | Artifact state | AAB is older than APK and HEAD |
| Audit reports in `reports/` | Device evidence, history, prior decisions | Some counts and gates are stale |
| `store-listing/README.md` | Public promises | Conflicts with Premium/privacy implementation |
| `privacy-policy.html`, in-app asset | Public promises | Conflicts with storage/logging |
| `README.md` | Repository-facing description | Uses “SkyCast Weather (Vayu)” |

### 2.4 Method

1. Inventory the shipped surfaces and user-facing promises.
2. Trace each promise to code, test, artifact, or device evidence.
3. Record the delta as a requirement, a contradiction, or an assumption.
4. Prioritise by release risk, not by engineering effort.
5. Put implementation recommendations **after** the requirements, in §15.

### 2.5 Limitations

- No Play Console access: billing catalog state, Data Safety submission, content rating, and target audience cannot be independently verified.
- No user research or analytics segmentation: personas are derived from shipped features, not validated.
- The latest instrumented run is a report claim (37 tests, 29 pass, 8 failures), not a fresh run in this pass.
- API 33+ notification permission and offline/airplane behaviour were not verified on the available API 31 device.
- Legal review has not been performed.

---

## 3. Product definition

### 3.1 One-line description

A local-first Android weather app that combines current conditions, hourly and 7-day forecasts, radar/cloud maps, air quality, saved cities, comparison, travel outlook, forecast-derived advisories, and a home-screen widget, monetised by ads with an optional Premium tier.

### 3.2 Product principles

| ID | Principle | Consequence |
|---|---|---|
| PP-01 | **Tell the truth.** Every public claim must map to shipped behaviour. | Unsupported copy is a P0 defect, not marketing licence. |
| PP-02 | **Local-first by default.** No account, no server-side profile. | Data stays on device; third-party processing is disclosed. |
| PP-03 | **Degrade, don’t blank.** Cached data remains usable offline. | Show last-updated time and an offline banner. |
| PP-04 | **Alerts are informational.** Forecast-derived advisories are not official warnings. | Persistent disclaimer; no emergency-authority implication. |
| PP-05 | **No dark patterns.** Premium and deletion flows are explicit. | No pre-ticked consent, no hidden retention, no fake urgency. |
| PP-06 | **One public identity.** | Choose “SkyCast Weather” or “Vayu”; do not mix them in user-facing surfaces. |

### 3.3 Business model

- Free tier with banner ads and a rewarded-ad unlock for hours 24–48 of the hourly outlook.
- Optional Premium: ad-free and extended outlook; further benefits are advertised but not implemented (see §12).
- No accounts, no server-side storage, no paid weather API resale.
- **Assumption A-03:** free-with-ads plus optional Premium is the intended model. If false, the ad and billing requirements change materially.

### 3.4 Brand

- UI label, store listing, and policy use **SkyCast Weather**.
- Package remains `com.vayu.weather`; README title is “SkyCast Weather (Vayu)”; reports use “Vayu”.
- **Decision D-01:** public name. Recommendation: keep **SkyCast Weather** externally; treat “Vayu” as internal/legacy. Package rename is not required for v1.5.

---

## 4. Users and personas

> **Evidence note.** These personas are derived from shipped features and store copy. No interviews, analytics, or support transcripts were available. They are planning tools, not validated segments (A-21).

| ID | Persona | Primary goal | What the product offers | Current friction |
|---|---|---|---|---|
| **P1** | **Daily planner** — checks weather before commuting or going out | Know today’s conditions and whether plans change | Current, hourly, 7-day, widget, notifications | Widget lacks the promised 3-day view; notification times do not work |
| **P2** | **Trip planner** — compares destinations and dates | Choose a city/date within the forecast horizon | Travel (+13 days), compare (max 4), favorites | Horizon and compare limits are not always explained up front |
| **P3** | **Weather-sensitive / outdoor user** — heat, UV, wind, rain, air quality | Timely, trustworthy warnings without spam | Derived advisories, severity filters, AQI, UV | No official-alert disclaimer; quiet hours bypassed; AQI shows only EU scale |
| **P4** | **Privacy-conscious user** — no account, wants control | Use weather without being tracked or retaining location | Local-first design, in-app policy, Delete My Data | Policy claims are false; deletion leaves settings/last location |
| **P5** | **Map explorer** — wants spatial context | See rain/clouds and tap for local weather | Radar, cloud, base maps, tap-to-weather, favorite pins | Temperature/wind layer chips exist but render nothing |
| **P6** | **Supporter / Premium buyer** — pays to remove ads and support development | Buy once, get what was advertised | Ad-free, 48-hour outlook | No products configured; two advertised benefits are unsupported |
| **O1** | **Operator / owner** — releases to Play, handles support | Submit a compliant build and answer users | Reports, checklists, policy | Store copy, Data Safety, content rating, and artifact state are inconsistent |

### 4.1 Jobs to be done

- “Tell me whether I need an umbrella, a jacket, or a different plan today.”
- “Show me the weather where I am going, on the date I am going.”
- “Warn me before conditions become dangerous — but not ten times for the same forecast.”
- “Let me keep a few cities and compare them quickly.”
- “Don’t make me create an account, and let me delete what you keep.”
- “If I pay, remove ads and give me the extra forecast you promised.”

### 4.2 Success signals (proposed)

These are product-health indicators, not current analytics commitments. They require consent-compliant instrumentation (A-21, NFR-13).

| Signal | Proposed target | Why |
|---|---|---|
| Core check completes | ≥95% of sessions show current conditions within 3 s | Primary job |
| Offline fallback | 100% of cached sessions show data + last-updated label | Trust |
| Advisory noise | ≤1 notification per advisory identity per forecast day | Anti-spam rule |
| Deletion success | 100% of seeded data categories removed or disclosed | Privacy promise |
| Premium funnel | Purchase success ≥95% of started purchases in license testing | Revenue |
| Crash-free sessions | ≥99.5% | Stability |

---

## 5. Feature status matrix

| ID | Capability | Public promise | Status | Current behaviour / gap | Priority |
|---|---|---|---|---|---|
| F-01 | Current conditions | Yes | **V** | Temperature, feels-like, humidity, wind, UV, visibility, pressure, dew point | — |
| F-02 | Hourly forecast (24 h) | Yes | **V** | Hourly timeline for the current day/first 24 h | — |
| F-03 | 7-day forecast | Yes | **V** | Daily high/low, precipitation, sunrise/sunset | — |
| F-04 | 48-hour outlook | Yes | **P** | Hours 24–48 gated by Premium or rewarded ad; rewarded unlock is session-only (`rememberSaveable`) | P1 |
| F-05 | City search | Yes | **V/P** | 500 ms debounce, deduped recents; geocoding hardcoded to English; generic “Search failed.” | P1 |
| F-06 | Recent searches | Yes | **V** | Last 10, deduped by city | — |
| F-07 | Favorites | Yes | **V** | Room-backed, drag-to-reorder, order persists | — |
| F-08 | Compare cities | Yes | **V** | Max 4; per-city failure does not block others | P1 UX |
| F-09 | Travel outlook | Yes | **V** | Today through +13 days; out-of-range message | P1 UX |
| F-10 | Radar / cloud map | Yes | **V** | RainViewer radar + cloud raster, timeline, base maps | — |
| F-11 | Temperature / wind layers | Implied by map UI | **X** | Grid data loads; `tempPoints`/`windPoints` are collected but never rendered | P0/P1 |
| F-12 | Tap map for weather | Yes | **V** | Tap fetches and shows a weather card | — |
| F-13 | Air quality | Yes | **P** | Model holds EU and US AQI; card displays European AQI only | P0/P1 |
| F-14 | Weather advisories | Yes | **V/P** | 5 types, severity filters, expiry, history, escalation-only notifications | P1 |
| F-15 | Alert history | Yes | **V** | Retains 100 rows; default query returns 50; expired rows kept as history | P1 |
| F-16 | Notification time slots | Yes | **X** | Three slots stored/displayed; no worker reads them | P0 |
| F-17 | Quiet hours | Yes | **P** | Implemented for the daily notification worker; severe-alert worker ignores it | P0 |
| F-18 | Home-screen widget | Yes | **P** | Small/medium/large; current conditions + 3–5 hour strip; no 3-day daily rows | P0/P1 |
| F-19 | Units / theme | Yes | **V** | °C/°F; km/h, mph, m/s, knots; hPa/mm; light/dark/system; dynamic color | — |
| F-20 | Localisation | Yes | **X/P** | 416 English strings; 13 each in hi/kn/ml/ta/te; no language picker; geocoding English | P1 |
| F-21 | Onboarding / permissions | Yes | **V/P** | 5 pages, location + notification prompts; API 33+ path unverified | P0 evidence |
| F-22 | Ads | Yes | **V/P** | Banners on several screens; rewarded ad works; `showInterstitial()` has no call sites | P1 cleanup |
| F-23 | Premium | Yes | **X** | `BillingProducts` lists empty; `isConfigured=false`; cannot purchase | P0 |
| F-24 | Delete My Data | Yes | **P** | Clears Room, AQI memory, widget prefs, streak; leaves settings/last location/premium cache | P0 |
| F-25 | Privacy policy | Yes | **P** | Published and reachable; location/retention/deletion statements contradict code | P0 |
| F-26 | Backup scope | Implied | **P** | Settings DataStore (incl. last location, premium cache) is in cloud backup; DBs/widget prefs excluded | P0 |
| F-27 | Weather history | Yes | **V** | 2,000 snapshots retained | P1 UX |
| F-28 | Share weather | Yes | **V** | Share action with fallback handling | — |
| F-29 | Support / contact | Implied | **X** | No developer/contact email in the published policy; report cites an address not present there | P0 |
| F-30 | Store listing truth | Yes | **U** | “Completely free,” “No personal data collected,” 3-day widget, dual AQI, layer claims | P0 |

---

## 6. End-to-end journeys

| ID | Journey | Trigger | Happy path | Failure / edge path | Requirements |
|---|---|---|---|---|---|
| **J-01** | First run | Install / first launch | Splash → onboarding (5 pages) → location permission → notification permission (API 33+) → dashboard | Location denied → search/manual fallback; notifications denied → app usable, alerts off with explanation; permanently denied → settings deep link | P0-07, P0-12, P1-10 |
| **J-02** | Daily check | App open / widget tap | Cached data <30 min or network fetch → current + hourly + 7-day → last-updated label | Offline: stale cache ≤7 days + banner; no cache: retry state; refresh failure keeps existing data | NFR-03, P1-08 |
| **J-03** | Find and save a city | Search tab | Type → 500 ms debounce → select → dashboard → star → favorites → long-press drag to reorder | Empty results; network error “Search failed.”; duplicate recents deduped | P1-07, P1-16 |
| **J-04** | Compare cities | Compare tab | Select up to 4 favorites → side-by-side weather | 5th selection blocked/explained; one city fails, others still render | P1-11 |
| **J-05** | Plan travel | Travel tab | Pick destination + date (today…+13) → daily summary | Date outside horizon → message; no destination → empty state | P1-11 |
| **J-06** | Explore map | Map tab | Radar/cloud overlay, timeline, tap → weather card, favorite pins | Layer load failure → legend/error; temp/wind selected → currently blank (defect) | P0-10, P1-03 |
| **J-07** | Receive advisory | Background worker every 3 h (default) | Evaluate forecast → create/escalate advisory → notification → alerts list → snooze/history | Same advisory unchanged → silent; de-escalation → silent; quiet hours → currently ignored for this worker | P0-08, P0-09, P1-06 |
| **J-08** | Tune notifications | Settings | Toggle alerts, rain threshold, severity, check interval, quiet hours, three time slots | Time slots do nothing; quiet hours does not cover severe alerts | P0-09 |
| **J-09** | Buy Premium | Premium screen | View benefits → purchase → acknowledge → entitlement → ads off + 48 h outlook | No catalog → no CTA; pending → pending state; cancel/refund → revoke on next Play query; transport failure → keep entitlement | P0-01, P0-02 |
| **J-10** | Delete data | Settings → Delete My Data | Confirm → Room/AQI/widget/streak wiped → success → onboarding | Settings/last location/premium remain; widget may still show data; no disclosure | P0-05 |
| **J-11** | Use widget | Add widget / periodic update | Snapshot → render current + hourly strip → refresh hourly | No data → placeholder; stale >30 min → stale flag; no 3-day rows | P0-10, P1-01 |
| **J-12** | Review privacy / consent | Settings → Privacy | Open hosted/in-app policy; EEA/UK → Privacy Options → change consent | Policy asset differs from host; consent withdrawal must stop ad requests | P0-04, P1-15 |

---

## 7. Business rules

| ID | Rule | Evidence / status |
|---|---|---|
| **BR-01** | Temperature is stored in °C; display converts to °F when selected. Wind supports km/h, mph, m/s, knots. Pressure supports hPa; precipitation supports mm. | **I** — `SettingsManager`, converters |
| **BR-02** | Weather cache is fresh for 30 minutes. Stale cache may be shown for up to 7 days as offline fallback. Cache keys quantise coordinates to 3 decimals. | **V** — `CACHE_TTL_MS`, `CACHE_MAX_AGE_MS`, `cacheKeyFor` |
| **BR-03** | Successful location fixes persist `last_lat`, `last_lon`, `last_city` for future fallback. A city selected later must not be overwritten by a stale GPS load. | **V** — `SettingsManager.Keys.LAST_*`; regression tests |
| **BR-04** | An advisory is identified by type + 0.1° location bucket + forecast day. One row per identity; upsert is idempotent. | **V** — `WeatherAdvisoryFingerprint` |
| **BR-05** | Advisory thresholds: rain high at ≥80%; wind high at ≥1.5× user threshold; UV high at ≥11; heat high at ≥threshold+5; cold high at ≤threshold−5. Severity filter gates creation. | **V** — `EvaluateWeatherAdvisoriesUseCase` |
| **BR-06** | Advisory expires at end of the forecast day in the location timezone. Notification only on creation or severity escalation. | **V** — `endOfForecastDay`, `AdvisoryChange` |
| **BR-07** | Limits: compare ≤4 cities; recent searches ≤10; alert rows retained ≤100; weather history ≤2,000; forecast horizon today…+13 days. | **V** — DAO/VM/screens |
| **BR-08** | Search is debounced 500 ms; recent searches dedupe by city and are capped at 10. | **V** — `SearchViewModel`, DAO |
| **BR-09** | Premium is granted only by verified Play purchase state and acknowledgement. There is no local unlock. Cancellation/expiry is applied on the next successful Play query; transport failure must not revoke. | **V** — `BillingManager`, `PremiumManager`, tests |
| **BR-10** | No ad request without UMP consent where required; Premium users see no banner/rewarded/interstitial ads; exactly one banner instance may be composed at a time. | **V/P** — `AdBanner`, `AdManager`, `ConsentManager` |
| **BR-11** | Delete My Data is expected to remove all local user data, or to state exactly what remains. A Play subscription itself is not cancelled by local deletion. | **B/D** — name/subtitle imply all; current code is partial |
| **BR-12** | Quiet hours, when enabled, suppress all user-facing weather notifications, not only the daily summary. Start = end means disabled. | **B** — current severe worker bypasses |
| **BR-13** | Widget refresh is hourly; data older than 30 minutes is marked stale. | **V** — `WidgetSnapshot`, worker |
| **BR-14** | Cloud backup/device transfer must not silently carry precise location or entitlement cache. | **B/L** — current settings backup includes them |
| **BR-15** | Precise coordinates must not appear in production logs. Geocoder failures may log a non-identifying reason, not raw coordinates. | **B/L** — current `DefaultLocationNameResolver` logs lat/lon |
| **BR-16** | A language offered in the UI/store must have complete user-facing string coverage. Partial translations must not be advertised as full support. | **B/D** — 13/416 strings |
| **BR-17** | Public copy must use one product name and must not claim “free,” “no data,” or “private” in absolute terms that the implementation contradicts. | **B/L** — see §12 |
| **BR-18** | Forecast-derived advisories must be labelled informational; they must not be presented as official emergency alerts. | **B/L** — no disclaimer found |
| **BR-19** | Prices shown in-app must come from Play Billing product details, not hardcoded copy; unavailable products must not show a purchase CTA. | **B/L** — catalog empty |
| **BR-20** | Travel dates outside today…+13 must be blocked and explained before a fetch. | **V** — DatePicker max +13 |
| **BR-21** | Compare must block a fifth city and explain the limit. | **V/P** — max 4 in VM; UI messaging to verify |
| **BR-22** | Alert history is history; the active list must exclude expired rows. | **V** — `activeAlerts(now)` |
| **BR-23** | Privacy consent withdrawal must stop ad requests and be reachable from Settings. | **V** — UMP entry point verified |
| **BR-24** | Policy and store must provide a monitored support contact and developer identity. | **B/L** — policy has no contact details |
| **BR-25** | A release artifact must be built from the release commit; versionCode must increase for each Play upload. | **B/L** — AAB stale |
| **BR-26** | Deleting local data must not silently re-create a Premium entitlement on the same device without re-querying Play and explaining the outcome. | **B** — current cache + re-query behaviour needs disclosure |
| **BR-27** | Interactive targets should be ≥48 dp; text must remain usable at 200% font scale; TalkBack must announce controls meaningfully. | **B/L** — 36 dp control noted in audit |
| **BR-28** | User-facing errors must be actionable: say what failed and what the user can do, not only “failed.” | **B** — search error is generic |

---

## 8. Functional requirements

> **P1 note.** Where a P1 requirement says “implement or remove,” the P0 truth requirement already requires the surface not to lie. P1 is the optional delivery of the removed capability if the owner chooses to retain the promise.

### 8.1 P0 — release blockers

| ID | Requirement | Measurable acceptance criteria | Prov. | Current |
|---|---|---|---|---|
| **FR-P0-01** | **Purchasable Premium catalog.** The app shall offer at least one Premium product whose ID exactly matches Play Console, and shall not present a purchase CTA when the catalog is unconfigured. | 1) `BillingProducts.isConfigured == true`; at least one subscription or one-time ID is non-empty. 2) IDs match Console character-for-character (case-sensitive). 3) In a license-test account, purchase completes in ≤60 s, entitlement is active immediately, and survives process death and reinstall/restore. 4) Purchase is acknowledged; pending shows “pending,” not Premium. 5) Cancel/refund removes Premium on the next successful Play query; a transport failure does not. 6) Empty catalog → “Purchases unavailable,” no price/CTA, no crash. 7) Automated tests cover purchased/pending/canceled/error. | D/I/L | **X** — B1 |
| **FR-P0-02** | **Premium promise alignment.** Every Premium benefit shown shall map to a shipped, testable capability, or be removed. | 1) An inventory maps each benefit string to code/test or to “removed.” 2) If “Every premium layer” remains: each map layer is Premium-gated and visibly renders; otherwise remove. 3) If “Priority alerts” remains: Premium alerts demonstrably rank first and the behaviour is tested; otherwise remove. 4) If “free of trackers” remains: Firebase Analytics/Crashlytics collection is disabled for Premium and verified by log/network inspection; otherwise change the copy. 5) No benefit contains an absolute claim without a test. | D/L | **U** — 2 of 5 claims unsupported |
| **FR-P0-03** | **Store listing truth.** The listing shall describe only shipped behaviour and shall not make absolute free/privacy claims the app contradicts. | 1) “Completely free” is replaced by “free with optional Premium” (or equivalent) and IAP is disclosed. 2) “No personal data collected” is removed or qualified consistently with Data Safety and the policy. 3) Each feature line has code evidence; 3-day widget, dual AQI, and map layers are either implemented or removed. 4) Target audience/content rating is consistent with the policy’s 13+ position. 5) Owner and legal reviewer sign off; a dated copy is archived. | D/L | **U** |
| **FR-P0-04** | **Privacy policy truth.** The policy shall accurately describe collection, local storage, third-party transmission, retention, and deletion. | 1) §1.1 no longer says location is “never stored, logged, or shared”; it states local last-location storage and coordinate-keyed Room data. 2) It names the third parties that receive coordinates (Open-Meteo, geocoding, RainViewer/MapLibre as applicable). 3) §4 retention matches actual limits (30 min fresh, 7-day stale, 10 recents, 100 alerts, 2,000 history, settings until cleared). 4) §5 names in-app Delete My Data and explains what remains. 5) Hosted and in-app assets are byte-identical; effective date/version present. 6) Legal review sign-off. | D/L | **P** — multiple false statements |
| **FR-P0-05** | **Complete, disclosed deletion.** Delete My Data shall delete or explicitly disclose every local user-data category. | 1) After confirm: favorites, recents, cache, alerts, history = 0 rows; AQI memory, widget prefs, streak prefs cleared. 2) `last_lat`, `last_lon`, `last_city`, onboarding state, and preference keys are cleared or reset to defaults. 3) Premium cache is cleared and re-queried; an active Play subscription may re-enable Premium, and the UI explains this. 4) The confirmation lists retained categories before deletion. 5) Failure is atomic or compensated; no silent half-wipe. 6) Widget updates to empty within one refresh; app returns to onboarding; no crash. 7) Seeded-data test covers every category. | D/L | **P** |
| **FR-P0-06** | **Accurate Data Safety submission.** The submitted form shall match manifest, SDK behaviour, and policy. | 1) Categories assessed and documented: location, device/other IDs (AAID), app activity/analytics, crash/diagnostics, purchase history. 2) All SDKs accounted for: AdMob/UMP, Firebase Analytics/Crashlytics/Remote Config/App Check, Play Billing, RainViewer, MapLibre, Open-Meteo. 3) Purposes, optionality, and sharing are consistent with the policy. 4) Submission date/screenshot retained; no “no data collected” answer. 5) Owner sign-off. | L | **P** — draft not submitted; versions stale |
| **FR-P0-07** | **Target audience and content rating.** The app shall declare an age-appropriate audience and complete the rating questionnaire consistently with ads, IAP, location, and the policy. | 1) Console target audience is 13+ unless a documented alternative is approved. 2) Rating certificate discloses ads, in-app purchases, and location use. 3) Listing is not child-directed; policy children section is consistent. 4) Screenshot/date retained. | L | **U** — console value not verifiable |
| **FR-P0-08** | **Advisory safety disclaimer.** Forecast-derived advisories shall be labelled informational, not official emergency warnings. | 1) Alerts screen shows a persistent disclaimer. 2) First-run/onboarding explains the advisory basis. 3) Notification copy identifies the advisory as forecast-based where length permits. 4) No “emergency/extreme” label without a defined severity and disclaimer. 5) Copy points users to official local sources for life-safety decisions. 6) String/screenshot test. | B/L | **X** — no disclaimer found |
| **FR-P0-09** | **Truthful notification controls.** Every notification control shall affect delivery, or be removed/disabled with an explanation. | 1) Changing slot 1/2/3 changes delivery within ±5 min in a WorkManager test; disabling suppresses it. 2) Quiet hours suppresses current, morning, and severe-alert notifications; test across midnight; start=end is disabled. 3) Check Frequency changes the alert worker’s next interval within one cycle. 4) If not wired, slots are hidden in release and no dead setting remains. | D/I/L | **X/P** — slots unused; severe bypass |
| **FR-P0-10** | **No visible no-op controls.** Every visible feature control shall produce the promised result or be removed. | 1) Temperature/wind overlay renders within 2 s with a legend, or is hidden. 2) 3-day widget claim is implemented or removed from settings/store. 3) Dual-AQI claim is implemented or removed. 4) Premium layer/priority claims handled by FR-P0-02. 5) A UI test asserts no blank overlay selection and no dead setting row. | D/I | **X** |
| **FR-P0-11** | **Backup privacy.** Precise location and entitlement cache shall not be silently restored to another device. | 1) Cloud backup/device transfer excludes `last_lat/lon/city` and the Premium cache, or the policy discloses inclusion and obtains consent. 2) Restore test on a second device: no last location; app asks for location/search. 3) Premium is re-queried from Play; an active subscription restores; an inactive one does not. 4) Backup rules lint/check. | B/L | **P** — settings DataStore included |
| **FR-P0-12** | **API 33+ notification permission.** The runtime permission flow shall be correct on API 33 and above. | 1) On API 33/34/35: rationale then system prompt on first eligible launch. 2) Denial → app usable, alerts off, explanation visible. 3) Permanent denial → “Enable in settings” deep link. 4) No prompt on API <33; no crash if denied; worker no-ops. 5) Evidence from emulator/device attached. | L | **P** — untested on API 31 device |
| **FR-P0-13** | **Release artifacts from HEAD.** The signed AAB/APK shall be built from the release commit and be newer than it. | 1) AAB timestamp > HEAD commit time. 2) `versionCode` monotonic for each upload; `versionName` correct. 3) `bundletool` validates; Play internal-testing upload/dry run succeeds. 4) SHA-256 recorded. 5) Release signing only; no debug key; no secrets. | B/L | **P** — AAB 2026-10-18, HEAD 2026-10-27 |
| **FR-P0-14** | **Release test gate.** The candidate shall pass unit, instrumented, and lint gates. | 1) Unit: ≥375 tests, 0 failures/errors/skipped. 2) Instrumented: 37/37 pass, or each failure has an approved waiver with rationale; no critical-path waiver. 3) Lint: 0 errors, 0 warnings; the 1 hint fixed or accepted in writing. 4) Manual smoke on API 24, 31, 33/35, plus one tablet/foldable. | I/L | **P** — 8 instrumented failures recorded; 1 hint |
| **FR-P0-15** | **Developer identity and support contact.** Policy and store shall identify the developer and a monitored contact. | 1) Legal/developer name, contact email, and effective date appear in the policy. 2) Play support email matches. 3) The contact is monitored; an auto-acknowledgement or response expectation is documented. 4) GDPR controller/processor roles stated if EEA/UK distribution continues. | B/L | **X** — no contact details in policy |

### 8.2 P1 — core promise and material quality

| ID | Requirement | Measurable acceptance criteria | Prov. | Current |
|---|---|---|---|---|
| **FR-P1-01** | **3-day widget (if promise retained).** Medium/large widget shall show today plus two daily rows, or the promise is removed. | Widget renders three daily high/low rows from `daily[0..2]`; updates within 1 h; empty/error states; screenshot test on API 31 and 35. | D | **X** — hourly strip only |
| **FR-P1-02** | **Dual AQI (if promise retained).** Show EU and US AQI with explicit labels, or remove the dual-standard claim. | Both values visible when available; label states “European AQI” / “US AQI”; fallback when one is null; store copy matches. | D | **P** — EU only |
| **FR-P1-03** | **Temperature/wind map layers (if retained).** Selecting either layer shall render a visible overlay with legend. | Selecting layer changes the canvas within 2 s; legend and units shown; no data → explicit empty/error state; screenshot test. | D | **X** |
| **FR-P1-04** | **Complete localisation (if retained).** Every shipped locale shall have full string coverage; partial translations are not advertised. | 100% key parity for each offered locale; no English fallback inside translated screens; pseudo-locale build; language claim matches actual coverage. | D | **X** — 13/416 |
| **FR-P1-05** | **Persistent 48-hour unlock (if “unlocked” implies persistence).** Rewarded unlock shall survive rotation and process death for a defined period, or copy shall say “this session.” | Rotate/process-death test; unlock state matches copy; Premium always unlocks; no duplicate reward prompt. | D | **P** — session-only |
| **FR-P1-06** | **Snooze suppresses notifications.** Snoozing an advisory shall prevent re-notification for the selected duration, or the control is relabelled. | Snoozed advisory creates no notification during the window; reappears after; test with fixed clock. | D | **U** — list-only snooze |
| **FR-P1-07** | **Search quality.** Geocoding shall follow the app locale where supported; errors shall be actionable. | Query in a supported locale returns localised names; error states distinguish offline, timeout, and no results; recent list deduped/capped at 10. | B | **P** |
| **FR-P1-08** | **Offline UX.** Cached data shall remain visible with a last-updated label and offline banner. | Airplane-mode cold start with cache ≤7 days shows data + banner; no cache shows retry; refresh failure preserves existing data; test on device. | D | **P** — logic exists, device unverified |
| **FR-P1-09** | **Alert history UX.** Active vs history shall be clear; retention limits shall be documented. | Active list excludes expired rows; history shows retained rows (≤100); empty states; filter counts match visible list. | I | **V/P** — default query 50 |
| **FR-P1-10** | **Limit UX for compare/travel.** Limits shall be visible before the user hits them. | Compare blocks the 5th selection with explanation; travel date picker max +13 and out-of-range copy; tests. | I | **V/P** |
| **FR-P1-11** | **Accessibility.** Interactive targets ≥48 dp; text usable at 200% font; TalkBack labels meaningful. | 48 dp audit; 200% font screenshot pass; TalkBack traversal of home/alerts/settings; contrast ≥4.5:1 for body text; fix the 36 dp control. | B/L | **P** — partial audit |
| **FR-P1-12** | **Brand consistency.** One public name across app, store, policy, README; legacy name only in internal docs/package. | App label, store title, policy title, and footer match; “Vayu” absent from user-facing surfaces except package/internal docs; owner sign-off. | B | **P** — mixed naming |
| **FR-P1-13** | **Support path.** A user can reach support from the app/store. | Settings/About or store listing opens a monitored email/support URL; privacy policy contact matches. | B | **X** |
| **FR-P1-14** | **Premium tracker claim (if retained).** If Premium is sold as “free of trackers,” analytics/crash collection shall be disabled for Premium. | Premium account → no Firebase Analytics events or Crashlytics non-fatal collection; verified by log/network; policy/Data Safety updated. | D/L | **X** — Firebase initialised unconditionally |
| **FR-P1-15** | **Widget freshness.** Widget shall mark stale data and open the app on tap. | >30 min → stale indicator; tap opens dashboard; no data → placeholder; API 31/35 screenshots. | I | **P** |
| **FR-P1-16** | **Dead ad code.** Unused interstitial code shall be removed or wired with policy-compliant caps. | Either no `showInterstitial` dead path, or call site + 3-min/10-per-session caps + tests; no unnecessary preload. | I | **X** — no call site |
| **FR-P1-17** | **Data retention transparency.** In-app copy shall state what is retained and for how long. | About/Privacy section lists cache, recents, alerts, history, settings; matches actual limits; link to policy. | B | **X** |
| **FR-P1-18** | **Notification grouping/stacking.** Multiple advisories shall not produce an unusable notification stack. | ≤5 active notifications; grouped by location/type; stable IDs; test with 5 types. | B | **U** |

### 8.3 P2 — quality and scale

| ID | Requirement | Acceptance criteria | Prov. |
|---|---|---|---|
| **FR-P2-01** | Tablet/foldable layouts | No clipped content at 600–900 dp; two-pane where useful; screenshot matrix | B |
| **FR-P2-02** | Reduced-motion support | System “remove animations” respected; no essential info lost | B |
| **FR-P2-03** | In-app language picker (if multiple locales retained) | Locale override persists; geocoding language follows; restart test | B |
| **FR-P2-04** | History trends/export | Chart from ≤2,000 snapshots; CSV/share export; no PII | B |
| **FR-P2-05** | Widget previews | Size picker shows representative preview and accurate description | B |
| **FR-P2-06** | Share card polish | Share includes city, temperature, condition, timestamp, attribution | B |
| **FR-P2-07** | Remote-config kill switches | Ads/Premium/map overlays can be disabled without a release; fail-safe defaults | B |
| **FR-P2-08** | Performance budgets | Cold start p95 ≤2.5 s on M31; tab switch ≤300 ms; search first result ≤1.5 s; map ≥30 fps | B |

### 8.4 P3 — opportunities, not committed

These are **not** requirements. They require a separate product decision and are listed to prevent them being smuggled in as assumptions.

- **P3-01** Official CAP/IPAWS emergency-alert integration with an authority agreement.
- **P3-02** Accounts and cross-device sync (conflicts with PP-02; requires a privacy redesign).
- **P3-03** Wear OS companion.
- **P3-04** iOS/web clients.
- **P3-05** Climate/historical analytics beyond the 2,000-snapshot local history.

---

## 9. Non-functional requirements

| ID | Requirement | Target / acceptance | Verification | Prov. |
|---|---|---|---|---|
| **NFR-01** | Performance | Cold start p95 ≤2.5 s on the M31; tab switch ≤300 ms; search first result ≤1.5 s p95; map interaction ≥30 fps | Macrobenchmark/device trace | B |
| **NFR-02** | Stability | Crash-free sessions ≥99.5%; ANR below Play’s bad-behaviour threshold; no crash on permission denial | Play vitals + device matrix | B |
| **NFR-03** | Offline resilience | Cache ≤7 days renders with last-updated label; no blank screen when cache exists; retry when absent | Airplane-mode test | D |
| **NFR-04** | Security | TLS only; no cleartext; no secrets in repo; no coordinates in production logs; backup excludes sensitive data | `network_security_config`, grep, `bmgr` restore test | L |
| **NFR-05** | Privacy | Data minimisation; consent-gated ads; accurate policy; deletion completes or discloses | Policy-to-code matrix; seeded deletion test | L |
| **NFR-06** | Accessibility | ≥48 dp targets; 200% font; TalkBack labels; 4.5:1 body contrast | Accessibility Scanner + manual matrix | L |
| **NFR-07** | Localisation | 100% key parity for each shipped locale; no mixed-language screens; RTL if an RTL locale is added | String diff + pseudo-locale | B |
| **NFR-08** | Compatibility | API 24–37; notification permission on 33+; no clipping on small/large/foldable | Device/emulator matrix | L |
| **NFR-09** | Testability | 0 unit failures; instrumented critical paths pass; deterministic clock in alert tests | CI/report | I |
| **NFR-10** | Maintainability | Lint 0 errors/0 warnings; no dead user-visible code; dependencies reviewed | Lint + code review | B |
| **NFR-11** | Battery/network | Periodic work ≥1 h; network constraints; cache prevents redundant fetches | WorkManager inspection | B |
| **NFR-12** | Store compliance | Target API current; Data Safety submitted; rating complete; ads/billing policies met | Console evidence | L |
| **NFR-13** | Observability privacy | No PII in analytics/crash; consent state respected; mapping files uploaded for release | SDK config + event audit | L |
| **NFR-14** | Documentation truth | README/report claims dated and scoped; no stale gate presented as current | Doc review | B |

---

## 10. Edge cases and failure modes

| ID | Condition | Expected behaviour | Current evidence | Requirement |
|---|---|---|---|---|
| **EC-01** | Location permission denied | App usable via search; explanation; retry path | Onboarding/home request; fallback exists | P0-12, P1-08 |
| **EC-02** | GPS off / no fix | Use last known location; otherwise search prompt | `last_lat/lon` fallback | BR-03 |
| **EC-03** | Offline, cache <7 days | Show cached data + offline banner + timestamp | Fallback implemented; device unverified | NFR-03 |
| **EC-04** | Offline, no cache | Retry state, no crash | Error state exists | NFR-03 |
| **EC-05** | API timeout / 5xx / malformed JSON | Keep existing data; actionable retry; no blank | `handleLoadFailure` preserves data | NFR-03 |
| **EC-06** | Geocoder failure | User still sees coordinates/weather; no raw coordinates in logs | Current code logs lat/lon | BR-15 |
| **EC-07** | Search rapid typing | Debounce 500 ms; cancel previous; no flicker | Implemented | BR-08 |
| **EC-08** | Search no results / error | Distinct empty vs error message | Empty exists; error generic | P1-07 |
| **EC-09** | 5th compare city | Blocked with explanation; no silent drop | VM max 4 | BR-21 |
| **EC-10** | Travel date > +13 | Blocked before fetch; message | DatePicker max +13 | BR-20 |
| **EC-11** | DST / timezone change | Advisory expiry uses location timezone; no double/missed day | `endOfForecastDay` | BR-06 |
| **EC-12** | Midnight forecast rollover | New day = new advisory identity; old expires | Fingerprint includes forecast day | BR-04 |
| **EC-13** | Threshold boundary (49/50/80, 1.49/1.5×, UV 10/11, heat +4/+5, cold −4/−5) | Exact boundary behaviour matches documented rule; tests at both sides | Unit tests exist for advisories | BR-05 |
| **EC-14** | Duplicate evaluation / worker overlap | Idempotent upsert; one row; no duplicate notification | Unique fingerprint + upsert | BR-04 |
| **EC-15** | Advisory escalation → de-escalation | Notify on escalation; silent on de-escalation; row updates | `AdvisoryChange.Updated` | BR-06 |
| **EC-16** | Notification permission denied/revoked | No crash; alerts off; re-enable path | Worker checks permission; UI path partial | P0-12 |
| **EC-17** | Quiet hours across midnight | Correct suppression window; start=end disabled | Implemented for daily worker only | P0-09 |
| **EC-18** | Reboot / WorkManager reschedule | Unique work remains; no duplicate schedules | `KEEP` policy | P0-09 |
| **EC-19** | Doze / battery saver | Work may be delayed; no data corruption | WorkManager constraints | NFR-11 |
| **EC-20** | Process death during rewarded unlock | Copy matches session-only behaviour or unlock persists | `rememberSaveable` | P1-05 |
| **EC-21** | Purchase pending/cancel/refund/offline | Pending ≠ Premium; cancel revokes next query; offline keeps entitlement | Tests cover transport failure | P0-01 |
| **EC-22** | Billing catalog empty | No price/CTA; “unavailable”; no crash | Current `isConfigured=false` path | P0-01 |
| **EC-23** | Consent withdrawn | No ad requests; privacy options still reachable | UMP verified | BR-23 |
| **EC-24** | Ad load failure | App usable; no blank banner hole; retry later | Banner singleton | P1-16 |
| **EC-25** | Widget no data / stale / locale change | Placeholder, stale flag, translated labels | Snapshot/stale flag | P1-15 |
| **EC-26** | Delete while worker runs | No resurrection of deleted rows; worker no-ops or repopulates only after explicit refresh | Transaction + workers need review | P0-05 |
| **EC-27** | Restore to new device | No precise location; Premium re-queried | Settings backup includes location | P0-11 |
| **EC-28** | 200% font / landscape / small screen | No clipped controls; scrollable; 48 dp targets | UI audit partial | P1-11 |
| **EC-29** | Long city names / RTL | Ellipsis/wrap; layout intact | Not fully evidenced | P1-11 |
| **EC-30** | Alert history >100 / weather history >2,000 | Oldest trimmed; UI stable | DAO trims | BR-07 |

---

## 11. Dependencies and owner actions

| ID | Dependency | Affects | Owner | Status |
|---|---|---|---|---|
| **B1** | Create Play Console subscription/one-time products and put exact IDs in `BillingProducts` | FR-P0-01, P0-02, P0-03 | Owner | **Open** |
| **B2** | UMP consent configuration and privacy options | Ads, BR-10/23 | Owner/eng | **Closed** per prior audit; device-verified |
| **B3** | Publish privacy policy | FR-P0-04 | Owner | **Closed** — hosted URL returns HTTP 200; content needs correction |
| **B4** | Rewrite store listing | FR-P0-03 | Owner | **Open** |
| **B5** | Submit Data Safety form | FR-P0-06 | Owner | **Open** — draft only |
| **B6** | Complete content rating and target audience | FR-P0-07 | Owner | **Open** |
| **B7** | Play closed-testing requirement for the account | Release timeline | Owner | **Open** (per release paperwork) |
| **B8** | Legal/developer identity and support contact | FR-P0-15 | Owner | **Open** |
| **B9** | Rebuild and sign AAB from HEAD | FR-P0-13 | Eng | **Open** |
| **B10** | API 33+ device/emulator verification | FR-P0-12, P0-14 | Eng | **Open** |
| **B11** | Resolve or waive 8 instrumented failures | FR-P0-14 | Eng | **Open** |
| **B12** | Decide public brand | FR-P1-12 | Owner | **Open** |
| **B13** | Decide Premium benefit scope (implement vs remove) | FR-P0-02 | Owner | **Open** |
| **B14** | Decide unsupported feature scope (widget/AQI/layers/notification slots) | FR-P0-09/10, P1-01/02/03 | Owner | **Open** |
| **B15** | Decide localisation scope | FR-P1-04 | Owner | **Open** |
| **B16** | Legal review of policy/disclaimer | FR-P0-04/08/15 | Owner | **Open** |

### 11.1 Dependency chain

```
B1 ──► FR-P0-01 ──► FR-P0-02 ──► FR-P0-03
B13 ──► FR-P0-02
B14 ──► FR-P0-09/10 ──► P1-01/02/03
B4  ──► FR-P0-03
B5  ──► FR-P0-06
B6  ──► FR-P0-07
B8  ──► FR-P0-15
B9  ──► FR-P0-13
B10 ──► FR-P0-12/14
B11 ──► FR-P0-14
B16 ──► FR-P0-04/08
```

**Critical path:** B1/B13 → P0-01/02 → B4 → P0-03 → B5/B6/B8 → P0-06/07/15 → B9/B10/B11 → P0-13/14 → submission.

---

## 12. Contradiction and ambiguity register

> These are **not** resolved here. Each needs an owner decision or a code/copy change. “Evidence” cites the source of each side.

| ID | Conflict | Evidence | Impact | Decision needed |
|---|---|---|---|---|
| **CR-01** | Premium advertised but unpurchasable | `PremiumManager` benefit list + Premium screen vs `BillingProducts` empty lists and `isConfigured=false` | Revenue blocked; user sees a dead purchase path | Configure products or hide Premium |
| **CR-02** | Billing product IDs inconsistent | `VAAYU_BILLING_PRODUCTS_SPEC.md` suggests `skycast.premium.monthly`; tests use `skycast_premium_annual`; code has none | Wrong ID = no purchase, console mismatch | Choose exact Console IDs; single source of truth |
| **CR-03** | “Completely free” vs Premium | `store-listing/README.md` line 11 vs Premium screen/billing | Misleading listing; possible policy action | Reword to “free with optional Premium” |
| **CR-04** | “No personal data collected” vs location/AAID/Firebase/purchase | Store listing line 44 vs manifest, Firebase Analytics, AdMob, billing | False privacy claim; Data Safety mismatch | Remove/qualify; disclose categories |
| **CR-05** | Location “never stored, logged, or shared” vs code | `privacy-policy.html` §1.1 vs `SettingsManager.Keys.LAST_*`, Room coordinate records, `DefaultLocationNameResolver` logs, Open-Meteo/geocoding/RainViewer calls | Policy is factually wrong; trust/legal risk | Rewrite policy; remove coordinate logs |
| **CR-06** | Policy internally contradictory | §1.1 “never stored” vs §5 “all weather data, preferences, search history, and alerts are stored locally” | Confusing deletion/retention story | Reconcile sections |
| **CR-07** | Deletion instructions omit the in-app action | Policy §5 lists only OS Clear Data/uninstall vs Settings → Delete My Data | Users may not find the promised control | Add in-app action to policy |
| **CR-08** | “Delete My Data” does not delete all local data | String subtitle “Remove all local data” vs repository clears Room/AQI/widget/streak only; settings survive | Privacy promise broken; reinstall-like expectation fails | Clear settings or disclose retention |
| **CR-09** | “Every premium layer” unsupported | `PremiumManager` copy vs radar/cloud free, temp/wind not rendered, no Premium gating | Paid benefit false | Implement gating or remove copy |
| **CR-10** | “Priority alerts” unsupported | `PremiumManager` copy vs no ranking/gating in alert code | Paid benefit false | Implement ranking or remove copy |
| **CR-11** | “Free of trackers” vs Firebase initialization | `PremiumManager` copy vs `VayuApplication.initializeFirebase()` unconditional; no `setAnalyticsCollectionEnabled` | Paid privacy benefit false | Disable for Premium or change copy |
| **CR-12** | 3-day widget vs hourly strip | Store + settings strings say 3-day; `WeatherWidget` renders current + 3–5 hourly items | Feature claim false | Implement 3 daily rows or remove claim |
| **CR-13** | Dual EU/US AQI vs EU-only display | Store copy vs `AirQuality.aqiLabel` prefers EU; card shows one value | Feature claim false | Show both or remove claim |
| **CR-14** | Temperature/wind layers vs blank map | Overlay options + grid fetch vs `tempPoints`/`windPoints` never used in `WeatherMapScreen` | Visible no-op; user distrust | Render or hide |
| **CR-15** | Notification time slots vs unused prefs | Settings UI/state/manager only; no worker reads `getNotificationTime*` | User sets times that never apply | Wire or remove |
| **CR-16** | Quiet hours vs severe alerts | `WeatherNotificationWorker.isQuietHours()` only; `WeatherAlertWorker` has no quiet-hours check | Night-time severe notifications bypass user setting | Apply to all or relabel |
| **CR-17** | “Severe-weather alerts” vs derived advisories | Onboarding/store language vs `WeatherAdvisory` docs: no official alert objects; derived from forecast | Users may over-trust; safety risk | Disclaimer + label |
| **CR-18** | Target audience ambiguity | Policy says not directed under 13; console value not verifiable in repo | Rating/policy mismatch risk | Confirm and document 13+ |
| **CR-19** | Test counts differ across reports | Readiness/UI reports say 345/338; current XML says 375/32 suites | Stale evidence presented as current | Date-scope reports; cite 375 as current |
| **CR-20** | AAB older than source/APK | AAB 2026-10-18 vs APK 2026-10-20, HEAD 2026-10-27 | Uploading stale binary omits alert fixes | Rebuild from HEAD |
| **CR-21** | Readiness report says policy push pending | `VAAYU_FINAL_RELEASE_READINESS.md` vs policy URL HTTP 200 and repo files | Stale blocker; planning confusion | Update report or add addendum |
| **CR-22** | Data Safety draft SDK versions stale | Draft says ads 25.4.0/Crashlytics 2.11.2; build/report use newer | Wrong form answers | Regenerate from final dependency graph |
| **CR-23** | Interstitials described but never shown | `AdManager` comments/implementation vs `showInterstitial()` no call sites | Dead code; unclear ad strategy | Remove or wire with caps |
| **CR-24** | Six-language claim vs 13 strings each | `README.md` line 29 vs `values-hi/kn/ml/ta/te` each 13 strings of 416 | Overstated localisation | Complete or narrow claim |
| **CR-25** | Brand naming | UI/store/policy “SkyCast Weather”; package/reports “Vayu”; README “SkyCast Weather (Vayu)” | Minor confusion; package rename costly | Pick external name; document internal alias |
| **CR-26** | P0-10 overlap with P1 feature delivery | “Remove or implement” vs optional implementation | Scope ambiguity | Owner chooses per feature; P0 closes truth, P1 delivers |
| **CR-27** | Premium after local deletion | Deleting cache vs Play re-query restoring entitlement | User may think deletion cancelled subscription | Disclose; do not cancel Play subscription locally |
| **CR-28** | “No server-side data” vs third-party SDK processing | Policy §4 vs Firebase/AdMob/RainViewer/Open-Meteo processing | Absolute claim is misleading | Explain third-party processing |
| **CR-29** | Weather history retention undisclosed | 2,000 snapshots in Room vs policy retention section | Data retention mismatch | Disclose or reduce |
| **CR-30** | 36 dp control vs 48 dp guideline | UI audit notes 36 dp favorite IconButton | Accessibility gap | Expand touch target |

---

## 13. Assumptions

> These are **not** requirements. If an assumption is false, the linked requirement or decision changes.

| ID | Assumption | Why it matters | If false |
|---|---|---|---|
| **A-01** | External product name is “SkyCast Weather”; “Vayu” is internal/legacy. | Brand consistency | Rename all public surfaces; package rename is separate |
| **A-02** | Primary market is India/English, with five Indian languages planned. | Localisation priority | Reorder languages or drop partial resources |
| **A-03** | Business model is free + ads + optional Premium. | Ads/billing requirements | Remove one monetisation path |
| **A-04** | Prices in `VAAYU_BILLING_PRODUCTS_SPEC.md` are proposals, not approved. | Store copy/pricing | Use approved prices; update specs |
| **A-05** | Store copy predates Premium/privacy changes; it was not intentionally deceptive. | Remediation tone | Treat as a compliance incident |
| **A-06** | Privacy policy is stale relative to code; intent remains local-first, not server collection. | Policy rewrite | If server collection exists, stop release and investigate |
| **A-07** | Advisories are informational, not life-safety; no official CAP feed is planned for v1.5. | Disclaimer scope | Requires authority integration/legal review |
| **A-08** | 13+ audience is intended; “Everyone” (if present in Console) is a mistake. | Content rating | Reassess child-directed design/ads |
| **A-09** | M31 API 31 is representative; API 33+ behaviour is expected to match. | Test coverage | Add device matrix before release |
| **A-10** | 345/338 test counts in older reports are historical snapshots; 375 is current. | Evidence quality | Re-run and reconcile |
| **A-11** | AAB will be rebuilt before upload; staleness is a process gap, not a code gap. | Release plan | If not rebuilt, alert fixes are absent |
| **A-12** | No backend/user accounts are planned. | Privacy architecture | Major redesign of deletion/sync |
| **A-13** | English is the canonical string source; partial translations are resource constraints, not a shipped promise. | Localisation claim | Complete translations before claiming support |
| **A-14** | Owner can provide Play Console access/decisions; BA cannot verify console state. | P0-01/06/07 | Requirements remain unverified |
| **A-15** | “Delete My Data” is intended to remove all user data, not only database rows. | Deletion scope | Rename to “Delete weather data” and disclose |
| **A-16** | Keeping the Premium cache after deletion may be intentional to avoid losing a purchase; it still must be disclosed. | Deletion UX | Clear cache and force Play re-query with explanation |
| **A-17** | UMP consent applies to EEA/UK; other regions follow Google defaults. | Ad consent | Reassess per-market consent |
| **A-18** | Retention limits (10/100/2,000) are product decisions, not legal requirements. | Retention policy | Adjust to legal advice |
| **A-19** | 7-day stale cache is an acceptable offline fallback. | Offline promise | Shorten/lengthen with UX copy |
| **A-20** | Owner authorises store/policy changes; legal review may still be required. | Remediation | Changes cannot ship |
| **A-21** | No validated personas/analytics exist; personas are derived. | Requirements confidence | Replace with research |
| **A-22** | Billing IDs will follow Play Console, not the report’s dotted examples. | Billing correctness | Purchases fail |
| **A-23** | Signing keys and Play account are available to the owner. | Release | Cannot rebuild/upload |
| **A-24** | Package rename is out of scope for v1.5. | Brand fix | Schedule a migration release |
| **A-25** | “Severe” in UI means derived high severity, not an official government warning. | Safety copy | Add authority sourcing |

---

## 14. Release readiness gate

### 14.1 P0 scorecard

| P0 | Requirement | Status | Evidence / blocker |
|---|---|---|---|
| P0-01 | Purchasable Premium catalog | **Open** | `BillingProducts` empty; B1 |
| P0-02 | Premium promise alignment | **Open** | 2 unsupported benefits; decision B13 |
| P0-03 | Store listing truth | **Open** | “Free,” “no data,” 3-day widget, dual AQI, layers |
| P0-04 | Privacy policy truth | **Open** | Location storage/logging/sharing contradictions |
| P0-05 | Complete/disclosed deletion | **Open** | Settings/last location/premium cache remain |
| P0-06 | Data Safety submitted | **Open** | Draft only; B5 |
| P0-07 | Target audience/content rating | **Open** | Console value unverified; B6 |
| P0-08 | Advisory disclaimer | **Open** | No disclaimer found |
| P0-09 | Truthful notification controls | **Open** | Time slots unused; quiet hours bypass |
| P0-10 | No visible no-op controls | **Open** | Temp/wind layers; 3-day widget; dual AQI |
| P0-11 | Backup privacy | **Open** | Settings DataStore includes last location |
| P0-12 | API 33+ notification permission | **Open** | Untested on API 31 device; B10 |
| P0-13 | Artifacts from HEAD | **Open** | AAB 2026-10-18 vs HEAD 2026-10-27 |
| P0-14 | Test/release gate | **Open** | 8 instrumented failures recorded; 1 lint hint |
| P0-15 | Identity/support contact | **Open** | No contact details in policy |

### 14.2 Closed or healthy items

- UMP consent configuration (B2) — closed and device-verified per prior audit.
- Privacy policy publication (B3) — hosted URL returns HTTP 200; content correction still required.
- Unit-test baseline — 375/375, 32 suites, 0 failures/errors/skipped (2026-10-20).
- Lint — 0 errors, 0 warnings, 1 hint (2026-10-20).
- No secrets/keystores/local config found in git history (prior audit).
- Source and README/landing page pushed; working tree clean; `origin/gh-pages` matches.

### 14.3 Gate rule

> A P0 item is closed only when the acceptance criteria in §8.1 are met **and** evidence is attached (test output, screenshot, console screenshot, or artifact hash). “Code written” is not closure. “Report says done” is not closure unless the report is current and the evidence is reproducible.

---

## 15. Implementation recommendation (post-requirements)

> This section is a recommendation, not a requirement. It follows from the P0/P1 analysis and should be approved by the owner.

### 15.1 Recommended path: “truth-first v1.5.x”

**Goal:** ship a smaller, honest, compliant release quickly, then add the removed capabilities in a later version.

**Indicative effort:** 8–12 developer-days plus owner console time. Ranges are planning aids, not commitments.

| Phase | Work | Closes | Effort |
|---|---|---|---|
| **0. Decide** | Brand, Premium scope, unsupported features, localisation scope, pricing | B12–B15 | 0.5 day |
| **1. Truth fixes** | Rewrite store copy; correct policy; add advisory disclaimer; fix/hide notification slots; remove unsupported Premium/map/widget/AQI claims; complete deletion | P0-02/03/04/05/08/09/10 | 3–4 days |
| **2. Revenue + compliance** | Configure Play products; wire exact IDs; purchase/restore/cancel tests; Data Safety; content rating; support contact | P0-01/06/07/15 | 2–3 days (owner-heavy) |
| **3. Release engineering** | Rebuild AAB from HEAD; API 33+ permission test; offline test; instrumented failures; lint hint; upload dry run | P0-11/12/13/14 | 2–3 days |
| **4. Verify** | Full regression; manual matrix; closed testing; sign-off | P0 gate | 1–2 days |

### 15.2 Fastest safe scope cut

If the owner wants the shortest path to a compliant listing, do **not** build the missing features yet. Instead:

1. Remove “Every premium layer” and “Priority alerts” from Premium; keep ad-free + 48-hour outlook (both work).
2. Remove “completely free” and “No personal data collected” from the listing.
3. Remove the 3-day widget claim (keep the working hourly widget) **or** implement three daily rows.
4. Remove the dual-AQI claim (keep the EU AQI card) **or** show both values.
5. Hide temperature/wind map layer chips (keep radar/cloud, which work).
6. Hide the three notification-time rows until wired; keep Quiet Hours and fix it for severe alerts.
7. Add the advisory disclaimer and correct the policy/deletion flow.
8. Configure one monthly + one lifetime product; make the Premium screen show Play prices.
9. Rebuild the AAB and verify on API 33+.

This preserves the genuinely strong core: forecasts, search, favorites, compare, travel, radar/cloud, tap-to-weather, alerts/history, widget, themes, units, and offline cache.

### 15.3 Alternative: “feature-complete v1.6”

Implement the 3-day widget, dual AQI, temperature/wind overlays, full five-language localisation, persistent rewarded unlock, and full notification scheduling. **Effort:** 4–6 weeks. **Risk:** higher, because it delays the revenue/compliance fixes and expands the test matrix. Recommended only after v1.5.x is live and stable.

### 15.4 Top risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Privacy claim found false by reviewer/user | High | High | Policy rewrite + stop coordinate logs + deletion fix |
| Store rejection for Data Safety/rating | Medium | High | Submit accurate forms; archive evidence |
| Billing remains unpurchasable | High | High | B1 owner action; license-test purchase |
| Stale AAB uploads without alert fixes | Medium | High | Rebuild from HEAD; hash + timestamp check |
| Alerts over-trusted as official warnings | Medium | High | Disclaimer + “forecast-based” copy |
| Instrumented failures hide a regression | Medium | Medium | Resolve or waive with evidence; API matrix |
| Localisation claim disappoints users | Medium | Medium | Complete or narrow the claim |

### 15.5 Definition of done for the release

- All 15 P0 items closed with evidence.
- Store listing, policy, Data Safety, content rating, and in-app copy mutually consistent.
- A signed AAB built from the release commit and successfully uploaded to internal/closed testing.
- Purchase, restore, cancel, and delete flows verified on a license-test account and a physical device.
- API 33+ notification permission and offline behaviour verified.
- No P0/P1 contradiction remains open without an owner-approved waiver.

---

## 16. Traceability and evidence index

| Area | Primary evidence |
|---|---|
| Build/version | `app/build.gradle.kts` — `versionCode 5`, `versionName "1.5"`, `minSdk 24`, `targetSdk 37` |
| Defaults/limits | `data/local/SettingsManager.kt`; `WeatherDao.kt` |
| Advisories | `domain/use_case/EvaluateWeatherAdvisoriesUseCase.kt`; `domain/model/WeatherAdvisory.kt`; `WeatherAlertWorker.kt`; `VAAYU_ALERT_SYSTEM_AUDIT_REPORT.md` |
| Billing | `data/billing/BillingProducts.kt`; `BillingManager`; `presentation/premium/PremiumManager.kt`; `PremiumScreen.kt` |
| Deletion | `data/repository/WeatherRepositoryImpl.kt` (`deleteAllLocalData`); `SettingsViewModel.deleteAllData`; `strings.xml` |
| Privacy | `privacy-policy.html`; `app/src/main/assets/privacy-policy.html`; `DefaultLocationNameResolver.kt`; Room entities; `backup_rules.xml`; `data_extraction_rules.xml` |
| Notifications | `WeatherNotificationWorker.kt`; `WeatherAlertWorker.kt`; `VayuApplication.kt`; `SettingsScreen.kt` |
| Widget | `presentation/widget/WeatherWidget.kt`; `WidgetSnapshot.kt`; `WeatherWidgetWorker.kt` |
| Map | `presentation/map/WeatherMapScreen.kt`; `MapViewModel.kt` |
| AQI | `domain/model/AirQuality.kt`; `presentation/components/AirQualityCard.kt` |
| Ads | `presentation/ads/AdManager.kt`; `AdBanner.kt`; `ConsentManager` |
| Tests | `app/build/test-results/testDebugUnitTest/*.xml` — 375/375, 32 suites, 0 failures |
| Lint | `app/build/reports/lint-results-debug.*` — 0 errors, 0 warnings, 1 hint |
| Artifacts | `app/build/outputs/apk/release/app-release.apk` (2026-10-20); `app/build/outputs/bundle/release/app-release.aab` (2026-10-18) |
| Store copy | `store-listing/README.md` |
| Prior verdicts | `reports/VAAYU_FINAL_RELEASE_READINESS.md`; `reports/VAAYU_PLAY_RELEASE_CHECKLIST.md`; `reports/VAAYU_UI_UX_AUDIT_REPORT.md`; `reports/VAAYU_DATA_SAFETY_ANSWERS.md`; `reports/VAAYU_SECURITY_PERFORMANCE_AUDIT.md` |

### 16.1 Report caveats

- `VAAYU_FINAL_RELEASE_READINESS.md` predates the alert audit and treats the privacy-policy push as open; the policy is now published.
- `VAAYU_UI_UX_AUDIT_REPORT.md` cites 345 tests; current XML is 375.
- `VAAYU_DATA_SAFETY_ANSWERS.md` is a draft with stale SDK versions and no submission evidence.
- Instrumented results (37 tests, 29 pass, 8 failures) are a report claim, not a fresh run in this pass.
- Older reports may say “all defects fixed”; this document treats a defect as closed only with current evidence.

---

## Appendix A — Current defaults, thresholds, and limits

| Setting | Default / value | Source |
|---|---|---|
| Temperature unit | Celsius | `SettingsManager` |
| Wind unit | km/h | `SettingsManager` |
| Pressure unit | hPa | `SettingsManager` |
| Precipitation unit | mm | `SettingsManager` |
| Theme | System | `SettingsManager` |
| Dynamic colour | On | `SettingsManager` |
| Notifications | On | `SettingsManager` |
| Rain threshold | 50% (range 10–100) | `SettingsManager` / Settings slider |
| Alert check interval | 3 h (options 1/2/3/6/12) | `SettingsManager` / `VayuApplication` |
| Severity filter | All | `SettingsManager` |
| Widget size | Medium | `SettingsManager` |
| Clock | 24-hour | `SettingsManager` |
| Quiet hours | Off; 22:00–07:00 when enabled | `SettingsManager` |
| Notification time slots | 07:00 / 12:00 / 18:00, all enabled (currently unused) | `SettingsManager` |
| Wind alert threshold | 60 | `SettingsManager` |
| UV alert threshold | 8 | `SettingsManager` |
| Heat alert threshold | 40 °C | `SettingsManager` |
| Cold alert threshold | 0 °C | `SettingsManager` |
| Weather cache TTL | 30 min | `WeatherRepositoryImpl` |
| Stale cache fallback | 7 days | `WeatherRepositoryImpl` |
| Compare limit | 4 | `CompareViewModel` |
| Recent searches | 10 | `WeatherDao` |
| Alert retention | 100 rows | `WeatherDao.trimWeatherAlerts` |
| Weather history | 2,000 snapshots | `WeatherRepositoryImpl` |
| Travel horizon | Today…+13 days | `SkyCastTravelScreen` |
| Forecast data source | Open-Meteo | `README`, API interfaces |
| Radar source | RainViewer | `MapViewModel` |
| Map styles | Street, Satellite, Voyager, Dark | `BaseMapStyle` |
| Widget refresh | 1 hour | `VayuApplication` |
| Current-weather notifications | Every 3 hours | `WeatherNotificationWorker` |
| Morning forecast | 07:00 local | `WeatherNotificationWorker` |
| Alert worker | Every 3 hours, network constrained, `KEEP` | `VayuApplication` |

### A.1 Advisory rules

| Type | High-severity rule | Notes |
|---|---|---|
| Rain | Probability ≥80% | User rain threshold still applies to lower-severity advisory |
| Wind | ≥1.5× user threshold | Default threshold 60 |
| UV | ≥11 | High severity |
| Heat | ≥threshold+5 | Default 40 °C → 45 °C |
| Cold | ≤threshold−5 | Default 0 °C → −5 °C |

- Identity: type + 0.1° latitude/longitude bucket + forecast day.
- Expiry: end of forecast day in location timezone.
- Notification: creation or escalation only.
- Storage: 100-row retention; unique fingerprint index; DB v6 migration preserves legacy rows as history.

---

## Appendix B — Glossary

| Term | Meaning |
|---|---|
| **Advisory** | A forecast-derived alert (rain/wind/UV/heat/cold); not an official government warning. |
| **Alert** | A stored advisory record shown in the app; may be active or history. |
| **Entitlement** | The Premium state derived from a verified Play purchase. |
| **Fingerprint** | Stable identity for an advisory (type + location bucket + forecast day). |
| **Last location** | `last_lat`, `last_lon`, `last_city` persisted for fallback. |
| **Premium** | Paid tier: ad-free + 48-hour outlook (plus any owner-approved benefits). |
| **P0/P1/P2/P3** | Release-blocker / core-promise / quality / opportunity priority. |
| **Stale cache** | Cached weather older than 30 min, usable up to 7 days offline. |
| **UMP** | Google User Messaging Platform, used for ad consent. |
| **V** | Verified evidence tag. |
| **X** | Not implemented / dead / no-op evidence tag. |

---

## 17. Open decisions and sign-off

### 17.1 Decisions required before implementation

- [ ] **D-01 Brand:** SkyCast Weather externally, Vayu internal? (A-01, CR-25)
- [ ] **D-02 Premium scope:** ad-free + 48 h only, or implement layers/priority? (CR-09/10/11)
- [ ] **D-03 Prices/IDs:** monthly/lifetime? annual? exact Console IDs? (CR-01/02, A-04)
- [ ] **D-04 Widget:** implement 3 daily rows or remove the claim? (CR-12)
- [ ] **D-05 AQI:** show EU+US or remove dual claim? (CR-13)
- [ ] **D-06 Map layers:** render temp/wind or hide chips? (CR-14)
- [ ] **D-07 Notification times:** wire or hide? (CR-15)
- [ ] **D-08 Quiet hours:** apply to severe alerts or relabel? (CR-16)
- [ ] **D-09 Localisation:** complete five languages or narrow the claim? (CR-24)
- [ ] **D-10 Audience:** confirm 13+ and complete rating. (CR-18)
- [ ] **D-11 Deletion:** clear settings vs disclose retention. (CR-08/27)
- [ ] **D-12 Support contact/legal identity.** (P0-15)

### 17.2 Approval

| Role | Name | Decision | Date |
|---|---|---|---|
| Product owner |  |  |  |
| Engineering lead |  |  |  |
| Legal/privacy reviewer |  |  |  |
| QA/release |  |  |  |

**Document status:** draft. No requirement in this document is approved until the owner signs §17.2. Nothing in this document authorises a code, store, or policy change by itself.
