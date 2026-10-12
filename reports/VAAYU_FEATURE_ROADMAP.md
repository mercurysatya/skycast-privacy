# VAAYU Feature Roadmap — post-release candidates (no commitments)

Ordered by user value vs. risk. None of these change the audited architecture
(single-module, manual nav, Open-Meteo, AdMob) unless stated.

## P1 — Must resolve before/with 1.5 store submission
1. **Play Billing for Premium** — replace the entitlement stub (`SettingsViewModel.setPremium`)
   with Google Play Billing (query purchases, acknowledge, restore on reinstall). Fixes the false
   "restored across reinstalls" claim and the listing policy risk.
2. **UMP message configuration** — AdMob console owner action (see AdMob report). No code change.
3. **Data safety form** — declare location, advertising ID, crash/analytics data use to match the
   store listing; verify against actual SDK behavior (ads 25.4.0, Firebase BOM 34.18.0, location 21.4.0).

## P2 — High value, low risk
4. **Rain/hourly glance widget polish** — the tz fix landed; add 12/24-h respect and tap-to-refresh.
5. **Travel-day planner scoring** — extend the capped-interstitial Travel entry into saved trip windows
   with "best hours" scoring from hourly data already cached.
6. **Compare up to 3 cities side-by-side charts** — builds on the fixed Compare add-city flow.
7. **Severe-alert push via system notifications only** — keep `PushNotificationReceiver` out unless FCM
   is truly needed; WorkManager alerts already cover the use case.

## P3 — Platform hardening
8. **Delete dead code** (`WeatherDashboard.kt` ~105 KB, `PushNotification*`) after one green release —
   smaller dex, faster builds.
9. **Screenshot + Robolectric nav tests** for modal BACK matrix (Settings↔Premium↔Privacy↔Alerts↔History↔Detail)
    so regressions like F10 are caught without a device.
10. **Baseline Profiles + R8 tuning** — release already minifies; add startup profile for cold-start time.
11. **Wear OS glance / widgets for Android 15+** — new surface, needs product decision.

## Explicitly out of scope
- Switching weather providers, adding API keys, multi-module splits, or adopting Navigation-Compose —
  rejected to preserve the audited architecture.
