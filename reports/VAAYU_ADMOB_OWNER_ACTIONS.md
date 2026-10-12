# VAAYU AdMob Owner Actions — Console + consent setup (GUI steps)

Status (2026-10-12, Phase 3): **BLOCKED — REQUIRES OWNER ACTION.** Fresh device logcat
`artifacts/logcat_p3_filtered.txt` still shows `Publisher misconfiguration: Failed to
read publisher's account configuration; no form(s) configured`; the app handles it
without crashing (consent refresh failure logged, weather fully functional). No evidence
exists that a consent message has been published or tested in the AdMob account —
compilation success does not change this status.

Code status first: the app integrates play-services-ads 25.4.0 + UMP 4.0.0 correctly —
`ConsentManager.gatherConsent()` runs on every cold start, uses only official UMP APIs,
gates all ad loads behind `canRequestAds()`, exposes the privacy-options entry in
Settings, and refreshes banner state after consent changes. **None of that can fix the
account-side gap below.** If the message is still missing after these steps, the status
remains BLOCKED / REQUIRES OWNER ACTION — never consider it fixed from a green build.

## A. Confirm the app registration (5 min)
1. Sign in to the AdMob account that owns the production IDs in `local.properties`
   (values stay in that gitignored file — never paste them anywhere else).
2. Apps → find **Vayu Weather**; confirm the package `com.vayu.weather` and that the
   application ID matches the one baked into the release build.
3. If the app entry is missing or shows a different package, add/register it and
   rebuild — ad requests against the wrong ID will keep failing.

## B. Publish the consent message (10–15 min + propagation wait)
1. AdMob → **Privacy & messaging** → select the Vayu app (confirm availability; a newly
   added app can take time to appear).
2. Create a message covering **GDPR/EEA + UK** at minimum (add US-state regions if the
   app targets them). Fill in the app/privacy-policy details when prompted.
3. **Publish** the message. Note the status column: it must read *Published/Active* —
   *Draft* or *Inactive* keeps the publisher-configuration warning.
4. Wait for propagation (commonly minutes to a few hours; allow up to 24 h), then proceed.

## C. Verify on a real device with the app (10 min)
1. On the test phone, add your Google account as a test device ONLY via the official
   UMP debug settings — do not click live ads at any point.
2. Install the release (or debug) build fresh, cold-start it, and confirm the consent
   form actually appears when required (EEA test geography or a relevant region).
3. Pull redacted Logcat and confirm the former warning is gone:
   `Publisher misconfiguration … no form(s) configured` must NOT appear; instead the
   flow should reach `canRequestAds() == true` and initialize Mobile Ads.
4. In Settings, confirm the Privacy Options row appears where required, opens the
   privacy-options form, and dismissing it returns cleanly (snackbar confirmation).
5. If the warning persists: re-check A (wrong app ID in build vs console) and B
   (message published for the right app + regions, propagation elapsed).

## D. After billing products exist (ties to the billing checklist)
- The paywall's Upgrade button stays honestly disabled until Play products are
  published; once they are, repeat a cold start and confirm offers load with
  Play-localized prices before any tester taps Upgrade.
- Purchase/Restore verification belongs to license testers on the internal track —
  never a production purchase, never a live-ad click.
