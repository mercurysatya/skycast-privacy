# VAAYU GOOGLE PLAY COMPLIANCE AUDIT — SkyCast Weather

**Report ID:** VAAYU_GOOGLE_PLAY_COMPLIANCE_AUDIT
**Date:** 2026-10-17
**App:** `com.vayu.weather` (SkyCast Weather), v1.5 / versionCode 5
**Method:** every claim grounded in (a) official Google/Play policy pages fetched today, (b) the merged release artifact (`aapt2 dump` on `app-release.apk`), or (c) live device captures. Nothing asserted from memory.

---

## 1. Target API level — **COMPLIANT**

Official Play policy (support.google.com/googleplay/android-developer/answer/11926878, fetched 2026-10-17):
> "**Starting August 31, 2026 — New apps and app updates must target Android 16 (API level 36) or higher** … Existing apps must target Android 15 (API level 35) or higher to remain available to new users…"

Artifact evidence (`aapt2 dump badging app-release.apk`):
```
package: name='com.vayu.weather' versionCode='5' versionName='1.5' platformBuildVersionCode='37'
minSdkVersion:'24'  targetSdkVersion:'37'
```

**Result:** targetSdk **37 ≥ 36 required** — passes with a full year of headroom to the *next* annual step-up. Store name (`application-label:'SkyCast Weather'`) matches the listing intent.

## 2. Play Billing policy — **COMPLIANT** (app side)

Official release notes (developer.android.com/google/play/billing/release-notes, fetched 2026-10-17):
> "By Aug 31, 2026, all new apps and updates to existing apps must use **Billing Library version 8 or later** … extension until Nov 1, 2026."

App uses **Billing 9.1.0** (latest stable, released 2026-06-18) — > requirement. Merged manifest carries `com.android.vending.BILLING` only; no external-payment/alternative-billing programs enabled. Billing cycle honest-state machine verified on device (ADB report §2.2): with no products configured the paywall says exactly "Premium purchases aren't set up yet…" and attempting "Upgrade to Premium" is a guarded no-op — this satisfies the **"Deceptive Behavior/Monetization"** policy requirement that features not-impossible-to-fulfill not be offered: nothing is offered that cannot be fulfilled.

## 3. Ads & consent (AdMob/UMP) — **COMPLIANT IN CODE, BLOCKED ON OWNER (B2)**

| Item | State | Evidence |
|---|---|---|
| AdMob App ID | Production `ca-app-pub-4212146603115396~1734384492`, injected only in release via `local.properties` (gitignored); **release build fails hard on Google test IDs** unless `-PallowTestAdmobIds=true` | `app/build.gradle.kts` (release guard) |
| Consent gating | `ConsentManager` (UMP) gates ad loads; with no consent the app logs "Skipping interstitial load — no consent" and shows none | `AdManager.kt:127,225`; device logs |
| UMP status | **BLOCKER B2** — `[W UserMessagingPlatform: Publisher misconfiguration … no form(s) configured for the input app ID …]` | `artifacts/logcat_afterfix_coldstart.txt:1947–1948`; `logcat_p3_filtered.txt:25` |
| Frequency controls | Interstitial min-interval (cooldown) + per-session cap + premium skip; duplicate loads de-duped by `AdLoadGate` (fixed today) | `AdManager.kt:175–181`; `artifacts/logcat_paywall_full.txt:2215–2216` |
| Ads adjacent content | Banner placed per ad-policy rules fixed in earlier commits (no accidental clicks next to interactive elements) | `1f8bcfa` "ad-policy fixes" |

**Policy exposure if shipped today without B2:** EEA/UK users would see no consent choice (ads are skipped → no serve → no direct violation, but monetization is zero and a later form publication changes behavior). Google's UMP requirement is that **EAA users be shown the form** — the app asks correctly; the form does not exist server-side. → Owner must publish forms before public testing.

Also note: per official AdMob release notes, **GMA SDK is in maintenance mode**; no policy impact, but plan the Next-Gen migration.

## 4. Data safety & privacy — **GATERED, ONE GATE (`commit+push` authorization), DRAFT COMPLETE**

### 4.1 The shipped privacy policy is stale — HIGH-PRIORITY OWNER ACTION
Fetched live page `https://mercurysatya.github.io/skycast-privacy/privacy-policy.html` (fetched 2026-10-17):
- "Last updated: **June 2026**"
- Contains: location, AdMob, Open-Meteo, MapLibre, COPPA, deletion, contact.
- **Missing** (relative to the app's actual data flows): Firebase **Analytics**, **Crashlytics**, **Remote Config**, RainViewer tiles, Premium-subscription local-storage & Play Billing linkage, EEA/UK consent language.

The **already-authored** uncommitted `privacy-policy.html` + `app/src/main/assets/privacy-policy.html` fill all of those (verified by diff: October 2026 header, AdMob-consent paragraph, Firebase ×3, RainViewer, subscription section, "Privacy Options" revisit). In addition, the in-app viewer reads the *asset* copy, so the in-app document already matches current code even before the site update.

**Action required:** commit **and push** the two policy files (owner authorization — the only reason it hasn't happened) so Console's Privacy Policy URL serves the complete document. Until then the Data Safety form would conflict with the live text.

### 4.2 Data Safety form draft exists and matches the merged artifact
`reports/VAAYU_DATA_SAFETY_ANSWERS.md` (drafted 2026-10-14) declares:
- Location (runtime-gated, required feature), Advertising ID (SDK-acquired), Crashlytics diagnostics, Firebase Analytics, Remote Config (present/inactive), RainViewer tile fetch.
Cross-checked against `aapt2 dump badging` — the merged manifest adds exactly what the form expects:
```
uses-permission: com.android.vending.BILLING
uses-permission: com.google.android.gms.permission.AD_ID
uses-permission: android.permission.ACCESS_ADSERVICES_{AD_ID,ATTRIBUTION,TOPICS}
uses-permission: android.permission.{WAKE_LOCK, FOREGROUND_SERVICE}
uses-permission: com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE
```
**Owner action:** submit this form in Console after the policy push (above), so the declared flows match the published policy.

## 5. Permissions & components — **COMPLIANT / MINIMAL**

Source set (declared by app): `INTERNET`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`. All other entries are SDK-merged and accounted for in the Data Safety sheet. No `RECEIVE_BOOT_COMPLETED`, no contacts/storage/mic/camera. Exported surface = `MainActivity` (launcher, required) + `WeatherWidgetReceiver` (`APPWIDGET_UPDATE`, required by Android 12+ widget rule) — both justified; FileProvider and androidx-startup provider are `exported=false` (verified in merged XML tree).

## 6. Age rating / target audience — **LOW RISK, ACTION PLAINTED**

- Policy text: "not directed at children under the age of 13" — consistent with a weather utility.
- The app must declare target age **13+** in Console Content Rating questionnaire; SDK-level: ads SDK 25.3.0 notes `setAgeRestrictedTreatment()` (new API replacing child/under-age tags) — our code sets no such tags (general audience default). No action beyond the questionnaire.

## 7. Testing requirements (new personal accounts) — **OWNER-DEPENDENT**

Play Help (answer/113469, fetched today): "Developers with personal accounts created after November 13, 2023, must meet specific testing requirements before … available to users" (answer/14151465 — e.g., closed-testing minimum with testers). If this is the first submission of this account, **closed testing with ≥ N testers for the required soak period** is part of the launch path — this dovetails with the recommended **READY FOR INTERNAL/CLOSED TESTING** decision.

## 8. Signing — **READY (owner confirms Play App Signing)**

- `app-release.aab` is signed (V2 verified; CN = Bammidi Satyanarayana / Mercury Computer Mart).
- Play App Signing enrollment is a Console-side action only the owner can perform; recommended before first upload so Google manages the install key.

## 9. Store listing paperwork status (from prior audits, unchanged)

| Item | Status |
|---|---|
| Data Safety form | Draft ready (`VAAYU_DATA_SAFETY_ANSWERS.md`) — submit after policy push |
| Content rating questionnaire | Owner action |
| Privacy policy URL | **Pending commit+push of updated policy** (then this URL) |
| Ads declaration (Console "Advertising ID") | Auto-handles via manifest merge once flagged as ad-supported |
| Screenshots/feature graphic | Not part of this audit |

## 10. Consolidated compliance decision matrix

| Gate | Owner playbook |
|---|---|
| **B2 (UMP forms)** — absolute gate for ads-enabled public testing | AdMob → Privacy & messaging → create GDPR + US-state messages for app ID `ca-app-pub-4212146603115396~1734384492` → publish → re-run device consent check |
| **B1 (Play products)** — gate for any purchase path | Create subscription products per spec; insert ids into `BillingProducts.kt`; re-verify paywall |
| **Policy push** — so the listing URL serves the complete policy | Commit + push both `privacy-policy.html` files (explicit authorization), then set the URL in Console |
| Data Safety submit | After policy push (see §4.2) |
| Closed testing launch | Satisfies new-account testing requirement (§7) |

## 11. Post-audit regression note (2026-10-18)

The final device regression fixed two **layout-only** visual defects (map alert-chip cover of the control rail; history chart labels bleeding into the legend). They touch no manifest entry, permission, consent flow, data flow, or payment behavior — **no compliance impact**. All gates in §10 remain exactly as stated: B2, B1, policy commit+push, Data Safety submit, closed-testing launch.

---

**Compliance verdict:** the app *artifact* is compliant on target-API, billing-lib, permission scope, and honest-state requirements. What blocks Play submission is purely **owner-side paperwork/console setup**: B2, B1, policy commit/push, Data Safety submit, and (for a fresh personal account) closed testing per §7.
