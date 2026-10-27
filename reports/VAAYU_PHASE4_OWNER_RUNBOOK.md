# Vayu Weather — Phase 4 Owner Runbook

**Repository**: `C:\Weather_App`, branch `gh-pages`, commit `1f8bcfa` (pushed to `origin/gh-pages`).
**Verification date**: 2026-10-12. All local verification green; two account-side blockers remain.

---

## 1. Current Repository and Verification Status

| Item | Status |
|---|---|
| Unit tests (debug) | **PASS** — 29 suites, 338/338, 0 failures/errors/skipped, exit 0 |
| Lint debug | **PASS** — 0 issues in `lint-results-debug.xml` |
| Release lint (`lintVitalRelease`) | **PASS** — "No issues found", exit 0 |
| Debug build (`assembleDebug`) | **PASS** — exit 0 |
| Release build (`assembleRelease` + `bundleRelease`) | **PASS** — exit 0; fresh artifacts 2026-10-12; `apksigner verify` exit 0; AAB `jar verified.` |
| Production AdMob config (release) | **PASS** — release manifest App ID + all 3 release BuildConfig unit IDs byte-match `local.properties`; zero Google test IDs in release config; zero hardcoded ad IDs in `app/src`; guard F15 fails release build if any ad field carries `ca-app-pub-3940256099942544…` test prefix |
| Device smoke test (SM-M315F) | **PASS** — cold start, navigation, Settings→Premium→BACK, rotation, honest unconfigured paywall, 0 FATAL/ANR in logcat |
| Genuine Billing end-to-end | **BLOCKED** — no Play Console products; purchase/initiate stays honestly disabled |
| UMP consent form | **BLOCKED — REQUIRES OWNER ACTION** — `Publisher misconfiguration … no form(s) configured` still reproduces on device |

**Evidence location**: `reports/` (all 8 reports committed at `1f8bcfa`), `artifacts/` (device screenshots/logcats — local only, gitignored).

---

## 2. Remaining Blockers and Why They Matter

### Blocker B1 — Play Console billing products not configured
- **Why it matters**: Without configured products, no genuine purchase/restore/acknowledgement can complete end-to-end. The app correctly refuses to simulate success — Upgrade stays disabled with "Premium purchases aren't set up yet." A release that claims Premium functionality without Play products would be non-compliant and misleading.
- **What must happen**: Owner creates + activates products in Play Console, pastes exact IDs into `app/src/main/java/com/vayu/weather/data/billing/BillingProducts.kt`, uploads a Play-signed internal-test build, and verifies with license testers.

### Blocker B2 — AdMob UMP consent message not configured/published
- **Why it matters**: The app integrates `play-services-ads 25.4.0 + UMP 4.0.0` correctly — `ConsentManager.gatherConsent()` runs on every cold start, `canRequestAds()` gates all ad loads, and the app does not crash when consent is unavailable. However, the UMP form is absent from the AdMob account, so EEA/EEA users see `Publisher misconfiguration: Failed to read publisher's account configuration; no form(s) configured`. This is a Play policy / privacy compliance risk.
- **What must happen**: Owner publishes a consent message in the AdMob console for the app ID `ca-app-pub-4212146603115396~<redacted>` (suffix recorded in `local.properties`, never printed in reports). After propagation, the warning must vanish and `canRequestAds()` must return `true` on a test device in a relevant region.

### Blocker B3 — `connectedDebugAndroidTest` not run
- **Why it matters**: Instrumented test suite was never executed on device/emulator. No code defect is known, but the suite validates the full test harness.
- **What must happen**: Run `.\gradlew.bat connectedDebugAndroidTest` on an authorized device or emulator. Document result.

### Blocker B4 — Privacy policy reconciliation
- **Why it matters**: The in-app privacy policy (`app/src/main/assets/privacy-policy.html`, 175 lines, June 2026) is newer than the root copy (`privacy-policy.html`, 152 lines). The in-app copy has richer disclosures: Advertising ID, Analytics/Crashlytics/Firebase Remote Config/RainViewer, Privacy Options revisit, and opt-out via Settings > Privacy Options. The root copy lacks these. Before release, the canonical source must be determined and the copies reconciled, with a Premium/purchase disclosure added.
- **What must happen**: See Section 4 below.

---

## 3. Play Console — Google Play Billing Setup Walkthrough

### Step-by-step product creation and activation

1. **Open Play Console** at https://play.google.com/console and select the **Vayu Weather** app (`com.vayu.weather`).

2. **Create subscription product(s)** (Monetize → Products → Subscriptions):
   - Click **Add subscription**.
   - Enter a **product name** (e.g., `Premium Monthly`).
   - Choose a **billing cycle** (e.g., `Monthly`).
   - Set the **price** and **currency** appropriate for your target markets.
   - Click **Save**, then **Activate**.
   - Record the **Product ID** shown (this is the exact string to paste into `BillingProducts.subscriptionIds`). Example placeholder: `premium_monthly`.

   *(If also offering an annual cycle, repeat for a second subscription product with its own Product ID.)*

3. **Create one-time/in-app product(s)** (Monetize → Products → In-app products):
   - Click **Add in-app product**.
   - Enter a **product name** (e.g., `Premium Lifetime`).
   - Set the **product ID** (this is the exact string to paste into `BillingProducts.oneTimeIds`). Example placeholder: `premium_lifetime`.
   - Set the **type** to `One-time`.
   - Set the **price** and **currency**.
   - Click **Save**, then **Activate**.

4. **(Optional) Configure preferred base plans** (if a subscription has multiple base plans / pricing templates):
   - In Play Console, for each subscription product that has multiple base plans (e.g., monthly + annual under one product), note the **Base plan ID** shown under each pricing template.
   - In `BillingProducts.kt`, populate `preferredBasePlans: Map<String, String>` with `subscriptionId → basePlanId` pairs when a subscription has more than one offer.
   - If there is only one base plan per subscription, leave `preferredBasePlans` as `emptyMap()`.

5. **Paste the exact IDs into `BillingProducts.kt`**:
   - Open `app/src/main/java/com/vayu/weather/data/billing/BillingProducts.kt`.
   - The `BillingProducts` class implements `BillingProductConfig` with three fields:
     - `subscriptionIds: List<String>` — list of subscription product IDs from step 2.
     - `oneTimeIds: List<String>` — list of one-time product IDs from step 3.
     - `preferredBasePlans: Map<String, String>` — optional base-plan mappings (empty if not needed).
   - Replace the empty lists with the recorded IDs. Example:
     ```kotlin
     @Singleton
     class BillingProducts @Inject constructor() : BillingProductConfig {
         override val subscriptionIds = listOf("premium_monthly")  // <-- owner ID
         override val oneTimeIds = listOf("premium_lifetime")     // <-- owner ID
         override val preferredBasePlans = mapOf("premium_monthly" to "base_plan_annual") // optional
     }
     ```
   - **Important**: Only add IDs for products that are **Active** in Play Console. An inactive or draft product will cause Play to return `unfetchedProductList` (logged with status codes), and the paywall will remain disabled.

6. **Add a Google group / license-tester list**:
   - In Play Console, navigate to **Testing > Internal testing**.
   - Add testers by email (these are the only accounts that can make genuine purchases without going through production).
   - Upload a **Play-signed** build (release variant with the upload signing key) to the internal track.

7. **Verify with license testers** (on internal-test track builds only):
   - **Purchase**: A tester taps Upgrade → Play sheet opens → successful purchase → Premium unlocks → paywall shows "You're Premium" and `isPremium` propagates to dashboard/Settings/Ad gating.
   - **Pending**: Use the slow test card (`iab.testcard`) — payment is pending; Premium stays disabled until Google confirms.
   - **Restore**: Tap Restore purchases → Play re-queries → entitlement restored for the same Google Play account (including reinstalls).
   - **Cancel**: Tester cancels the subscription in Play Store → next refresh query revokes entitlement (silent, no crash).
   - **Expiry**: Subscription expires naturally → next refresh query revokes entitlement.
   - **Record results** in `reports/VAAYU_BILLING_IMPLEMENTATION.md` under §5 test evidence.

8. **After products are verified**, re-run the full verification sequence to confirm:
   - `testDebugUnitTest` PASS (338/338) — the unconfigured-disables-purchase test now also confirms offers load.
   - Device smoke test: Settings → Premium → Upgrade → purchase → Premium state → BACK → Settings.

---

## 4. Privacy Policy Reconciliation

### Current state
- **Root copy**: `privacy-policy.html` (152 lines) — older, lacks Advertising ID, Analytics/Crashlytics/Firebase Remote Config/RainViewer disclosures, Privacy Options revisit, and opt-out via Settings > Privacy Options.
- **In-app copy**: `app/src/main/assets/privacy-policy.html` (175 lines, June 2026) — newer, includes all of the above.

### Reconciliation decision
The **in-app copy is the canonical source** (it is the version actually displayed to users inside the app, it is newer (June 2026 vs. unclear date for root), and it already reflects the actual SDK behavior present in the app). The root copy should be updated to match, with an added Premium/purchase disclosure.

### Required additions to the in-app privacy policy (already present in the 175-line version, but must remain after sync)
The in-app copy already includes:
- Advertising ID collection (`1.2 Automatically Collected Information`)
- Google AdMob data use governed by Google Privacy Policy (`1.2`)
- Analytics, Crashlytics, Firebase Remote Config, RainViewer (`3. Third-Party Services`)
- Privacy Options revisit and opt-out (`8. Your Rights`, item 4)
- Data deletion via Clear Data / uninstall (`5. Data Retention` / `5. Data Deletion`)

### Action: Sync root copy to in-app copy
1. Copy the full content of `app/src/main/assets/privacy-policy.html` (175 lines) and overwrite `privacy-policy.html` (root, 152 lines).
2. Add a **Premium/purchase disclosure** section. Insert after section `9. Changes to This Policy` (or as a new section `11. Premium Subscriptions`) before the `Contact Us` section:

```
<h2>11. Premium Subscriptions and billing</h2>
<p>
    The App offers optional Premium subscriptions through Google Play Billing. 
    Subscriptions are linked to your Google account and can be managed or canceled 
    anytime in the Google Play Store. When a Premium subscription is active, 
    advertisements are removed from the App and extended forecast features are 
    unlocked. Subscription data (product ID, purchase time, renewal state) is 
    stored locally on the device and transmitted to Google Play per Google's 
    billing API. You can revoke the entitlement at any time by canceling the 
    subscription in the Google Play Store; the App will detect the cancellation 
    on the next Play query and disable Premium features accordingly.
</p>
```

3. Update the `last-updated` date on both files to `October 2026`.

### Play Console Data safety form
The following disclosures must be completed in the Play Console **Data safety** section, matching the actual SDK behavior:

| Category | Disclosure (matching in-app policy) |
|---|---|
| **Data collected** | Location (approx. + precise, runtime-gated), Advertising ID, Crash/analytics data (app opens, screen views, crash reports), Personalization (ads ID for AdMob targeting) |
| **Data used to track you** | Advertising ID (for AdMob personalized ads, gated by UMP consent) |
| **Data linked to you** | Advertising ID (resettable), approximate location (IP-level, at query time) |
| **Data not linked to you** | Crashlytics stack traces (anonymized), Remote Config values (anonymous instance ID) |
| **Categories** | Location, Ads / Marketing, Analytics |
| **Purpose** | Location: weather forecasts; Ads: AdMob personalized/non-personalized (per consent); Analytics: app usage stability |

The **retention/deletion** entry should read: *Local-only + system Clear Data — matching the in-app privacy policy. All weather data, preferences, search history, and alerts are stored locally on the device and are automatically removed when you clear app data or uninstall. Since we do not store any data on our servers, there is no server-side data to delete.*

---

## 5. Commands to Rerun Verification

After owner actions are complete, run these PowerShell commands sequentially (stop on first failure):

```powershell
Set-Location C:\Weather_App

.\gradlew.bat testDebugUnitTest          # should be 338/338, exit 0
.\gradlew.bat lintDebug                  # should be 0 issues, exit 0
.\gradlew.bat assembleDebug              # should be exit 0
.\gradlew.bat lintVitalRelease           # should be "No issues found", exit 0
.\gradlew.bat assembleRelease bundleRelease# should be exit 0; apksigner + jarsigner verify
```

Then ADB smoke test (if device available):

```powershell
adb devices -l                             # verify device is connected & authorized
.\gradlew.bat assembleDebug -r           # reinstall fresh debug APK
adb -s <serial> shell am start -n com.vayu.weather/.MainActivity
;# test: cold start, navigation, Settings→Premium→BACK, rotation, billing-unconfigured state
adb -s <shell logcat -d -t 4000 | Select-String -Pattern "FATAL|ANR|Publisher misconfiguration"  # 0 matches required for UMP
```

---

## 6. Evidence Checklist — Conditions to Close Each Blocker

| Blocker | Evidence Required |
|---|---|
| **B1 — Play products configured** | - Play Console: product list shows subscription(s) + in-app product(s) with **Active** status.<br>- `BillingProducts.kt` contains the exact product IDs from Play Console (no placeholders).<br>- Internal-test track build uploaded; license tester can complete purchase, restore, cancel, and expiry.<br>- Device smoke test: Premium unlocks, persists across rotation/restore, BACK→Settings works. |
| **B2 — UMP consent message published** | - AdMob console: **Privacy & messaging** → message status **Published/Active** for app ID `ca-app-pub-4212146603115396~<redacted>`.<br>- On a test device in EEA/UK region: cold-start app → consent form displays → `ConsentManager.canRequestAds()` returns `true`.<br>- Logcat no longer contains `Publisher misconfiguration: Failed to read publisher's account configuration; no form(s) configured`. |
| **B3 — `connectedDebugAndroidTest` run** | - `.\gradlew.bat connectedDebugAndroidTest` exit 0 (or document why it was NOT RUN).<br>- No new failures introduced. |
| **B4 — Privacy policy reconciled** | - Root `privacy-policy.html` matches in-app `app/src/main/assets/privacy-policy.html` (line count within 1 line, content identical).<br>- Both files have `last-updated: October 2026`.<br>- Premium/purchase disclosure section added to the canonical copy.<br>- Data safety form in Play Console entries match actual SDK behavior (location, ads ID, analytics, retention). |

---

## 7. Final Acceptance Checklist

| # | Item | Status | Notes |
|---|---|---|---|
| 1 | `testDebugUnitTest` PASS (338/338) | PASS / FAIL / BLOCKED | |
| 2 | `lintDebug` 0 issues | PASS / FAIL / BLOCKED | |
| 3 | Release build signed & verified (apksigner/jarsigner) | PASS / FAIL / BLOCKED | |
| 4 | Release AdMob guard: zero test IDs in release config | PASS / FAIL / BLOCKED | |
| 5 | Play Console products created + activated | REQUIRES OWNER ACTION | See Section 3 |
| 6 | UMP consent message published & tested | REQUIRES OWNER ACTION | See Section 4 |
| 7 | `connectedDebugAndroidTest` run (or documented NOT RUN) | PASS / FAIL / NOT RUN | |
| 8 | Privacy policy reconciled + Premium disclosure added | REQUIRES OWNER ACTION | See Section 4 |
| 9 | Data safety form completed in Play Console | REQUIRES OWNER ACTION | Match SDK behavior |
|10 | Internal-test track build + license tester evidence | REQUIRES OWNER ACTION | See Section 3 |

**Final decision**: **NO-GO** until owner completes B1 (Play products) and B2 (UMP consent). Code verification is green, but two release-critical account-side blockers remain unresolved.

---
*Runbook generated 2026-10-12. Do not publish, upload, or commit without clearing all B1–B2 blockers and verifying the acceptance checklist.*