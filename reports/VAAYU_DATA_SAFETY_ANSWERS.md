# Vayu Weather — Data Safety Form Answer Sheet (B5)

**Purpose**: Answer sheet for Google Play Console Data Safety section, populated from the merged release manifest and actual SDK dependencies. Entries are **drafted to match actual SDK behavior** but **not yet submitted** to the Play Console (owner action B5).

> **Note**: This form reflects the state as of 2026-10-14. The Play Console Data Safety section must be completed and submitted by the owner before release. Draft entries are based on observed SDK behavior from code inspection and local testing; no user data was collected or fabricated for this sheet.

## 1. Collected/Shared Data Categories

| Question | Collected/Shared | Purpose | Optional/Required | Source Evidence |
|---|---|---|---|---|
| **User ID/Identifier** | No | — | — | No user ID generation or storage in `app/src/main` |
| **Profile Data** | No | — | — | No profile data collection |
| **Phone Data** | No | — | — | No phone state or telephony permissions |
| **Messages** | No | — | — | No SMS or messaging permissions |
| **Body Sensors** | No | — | — | No body sensor permissions |
| **Location** | Yes (runtime‑gated) | Weather display: determine user location for forecast | Required (but user can deny or revoke) | `AndroidManifest.xml`: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`; runtime permission request in `presentation/location/LocationPermissionHandler.kt` |
| **Contacts** | No | — | — | No contacts permissions or access |
| **Photos/Media** | No | — | — | No media/store permissions |
| **Camera** | No | — | — | No camera permissions |
| **Files** | No (app‑private only) | — | — | Room database and DataStore are app‑private; `Clear Data` deletes all local stores |
| **Camera** | No | — | — | — |
| **Microphone** | No | — | — | No audio recording |
| **Physical Attributes** | No | — | — | No body measurement or attribute collection |
| **Other** | Advertising ID | Ads / frequency control | Required (per SDK) | `play-services-ads 25.4.0` initializes AAID; `ConsentManager` gates ad requests; manifest does not explicitly request `AD_ID` but SDK acquires it per Google Play policies |
| **Other** | Crashlytics error data | Stability / bug fixing | Required (per SDK) | `com.crashlytics.sdk.android:crashlytics:2.11.2` auto‑collection; `AndroidManifest.xml` does not add explicit permissions; enabled via `apply plugin: 'com.google.firebase.crashlytics'` |
| **Other** | Firebase Analytics events | Measurement / app usage | Required (per SDK) | `com.google.firebase:firebase-analytics:21.3.0`; events are automatic (first‑open, app\_name, etc.) per Firebase BOM 34.18.0 |
| **Other** | Firebase Remote Config | Feature flag / remote updates | Optional (not used aggressively) | `com.google.firebase:firebase-config:21.3.0`; no Remote Config calls found in `app/src/main` source; present but not actively configured |
| **Other** | RainViewer tile data | Weather map tiles | Optional (network‑fetched) | `RainViewer` tile URLs fetched over network; no SDK-specific data collection beyond standard HTTP |

## 2. Purpose Description

| Category | Purpose String (for Play Console) |
|---|---|
| Location | "To determine user location for accurate weather forecast display" |
| Advertising ID | "To deliver and measure advertising performance" |
| Crashlytics | "To fix crashes and improve app stability" |
| Firebase Analytics | "To understand app usage and improve user experience" |
| Remote Config | "To download feature flags and configuration remotely" |
| RainViewer | "To display weather radar maps and tile-based map overlays" |

## 3. Data Type Details

| Data Type | Collected? | Retention | Deletion Mechanism |
|---|---|---|---|
| **Advertising ID** | Yes (SDK‑provided) | Until user reset or app uninstall | `Settings → Apps → Vayu Weather → Clear Data`; user can reset AAID in Google Settings |
| **Crashlytics data** | Yes (SDK‑provided) | Indefinite (per Firebase terms) | `Settings → Apps → Vayu Weather → Clear Data` (clears crash reports stored locally) |
| **Firebase Analytics data** | Yes (SDK‑provided) | Indefinite (per Firebase terms) | `Settings → Apps → Vayu Weather → Clear Data` (clears analytics cache) |
| **Location** | Yes (user‑granted) | Until cache expiry or app uninstall | `Settings → Apps → Vayu Weather → Clear Data`; location permissions revoked in Android Settings removes future collection |
| **Remote Config** | No (present in dependencies, not actively used) | — | — |
| **RainViewer tiles** | Yes (network‑fetched) | Until app cache clears | `Settings → Apps → Vayu Weather → Clear Data` clears tile cache |

## 4. User Controls

| Control | Available? | Description |
|---|---|---|
| Opt‑out of Advertising ID | Partially | User can reset AAID via Google Settings; app respects Android's AD_ID permission policy |
| Opt‑out of Firebase Analytics | No direct in‑app toggle | User can disable analytics collection by opt‑out via `Settings → Ads → Opt out of Ads Personalization`; app does not set an opt‑out flag |
| Revoke location permission | Yes | `Settings → Apps → Vayu Weather → Permissions → Location`; app shows honest fallback UI when location is unavailable |
| Clear app data | Yes | `Settings → Apps → Vayu Weather → Clear Data` deletes all local storage: Room DB, DataStore, cache, crash reports, analytics cache |

## 5. Source Evidence (Manifest & SDK)

| SDK / Manifest | Key Evidence | Line / File |
|---|---|---|
| `play-services-ads 25.4.0` | `ConsentManager.gatherConsent()`; `canRequestAds()`; AAID initialization; no manual `AD_ID` permission | `MainActivity.kt`, `ads/AdManager.kt` |
| `play-services-location 21.4.0` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` in manifest; `FusedLocationProvider` | `AndroidManifest.xml` (manifest merger) |
| `com.crashlytics.sdk.android:crashlytics:2.11.2` | Auto‑collection enabled via `apply plugin: 'com.google.firebase.crashlytics'`; no custom logging of user data | `build.gradle.kts` |
| `com.google.firebase:firebase-analytics 21.3.0` | Automatic events: first‑open, app\_name; no custom PII events | `build.gradle.kts` |
| `com.google.firebase:firebase-config 21.3.0` | Dependency present; no active fetch calls in `app/src/main` | `build.gradle.kts` |
| `androidx.room:room‑runtime 2.8.4` | Room DB files in `app/files/` (app‑private); `Clear Data` deletes | `data/local/WeatherDatabase.kt` |
| `androidx.datastore:datastore‑preferences 1.2.1` | DataStore file in app private directory; `Clear Data` deletes | `data/telemetry/WeatherDataStore.kt` |

## 6. Uncertain / Flagged Items

| Item | Status | Reason |
|---|---|---|
| **Advertising ID collection** | Flagged — SDK‑provided, not explicitly opted in | AAID is acquired by `play-services-ads` per Google Play policy; app does not currently offer an in‑app opt‑out toggle for ads personalization. Recommendation: add opt‑out toggle if required for Data Safety submission. |
| **Firebase Analytics automatic events** | Flagged — collected by default | Analytics collects first‑open, app\_name, and other automatic events without explicit user consent. Consider adding a notice in the privacy policy. |
| **RainViewer tile HTTP fetch** | Flagged — network data beyond SDKs | Tiles are fetched from `rainviewer.com` over HTTP (if HTTP fallback is configured); ensure HTTPS is used or document as "network‑fetched data for weather display." |
| **Remote Config present but not active** | Flagged — dependency present without clear purpose | Remote Config library is on the classpath; no fetch calls observed. Mark as "optional" or remove dependency if not needed. |

## 7. Draft Play Console Entries (for Owner Submission)

> These entries are drafted based on the evidence above. The owner must review, approve, and submit them in the Google Play Console → **App editing → Data safety**.

| Section | Draft Entry |
|---|---|
| **Data collected** | • Advertising ID • Crashlytics error data • Firebase Analytics events • Location (when permission granted) • Remote Config data (optional) • RainViewer tile data (optional) |
| **Why collected** | • Advertising ID: "To deliver and measure advertising performance" • Crashlytics: "To fix crashes and improve app stability" • Firebase Analytics: "To understand app usage and improve user experience" • Location: "To determine user location for accurate weather forecast display" • Remote Config: "To download feature flags and configuration remotely" • RainViewer: "To display weather radar maps and tile‑based map overlays" |
| **Users can opt‑out** | • User can reset Advertising ID in Google Settings • User can revoke location permission in Android Settings • User can clear app data via `Settings → Apps → Vayu Weather → Clear Data` • Firebase Analytics opt‑out via `Settings → Ads → Opt out of Ads Personalization` |
| **Data retention** | • Advertising ID: Until user reset or app uninstall • Crashlytics: Indefinite (per Firebase terms) • Analytics: Indefinite (per Firebase terms) • Location: Until cache expiry or app uninstall • Remote Config: Until fetched data expires • RainViewer tiles: Until cache cleared |
| **Data security** | • All data stored locally in app‑private directories • `Clear Data` action deletes all local stores • Network traffic uses HTTPS where available |

---

*Data Safety Answer Sheet generated 2026-10-14. Based on merged release manifest, `build.gradle.kts` dependencies, `AndroidManifest.xml` (manifest merger), and runtime inspection of `app/src/main`. No user data was collected or fabricated for this sheet. Owner must review and submit in Play Console.*