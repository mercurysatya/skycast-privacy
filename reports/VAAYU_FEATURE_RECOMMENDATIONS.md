# VAAYU FEATURE RECOMMENDATIONS

**Report ID:** VAAYU_FEATURE_RECOMMENDATIONS
**Date:** 2026-10-17
**Basis:** code audit + device behavior + prior roadmap (`reports/VAAYU_FEATURE_ROADMAP.md`) re-scored against the current, post-fix state of the app. Nothing here is required for the 1.5 store submission unless marked **P0**.

---

## 0. Status refresh of prior roadmap items

| Prior item | Status now |
|---|---|
| P1–Play Billing for Premium | **DONE** (commit `1f8bcfa`, genuine Billing 9.1.0 with entitlement resolver + honest gate). Remaining is owner product creation (B1). |
| P1–UMP message configuration | Still open (B2, owner console action). |
| P1–Data safety form | Draft complete (`VAAYU_DATA_SAFETY_ANSWERS.md`); submission pending policy push. |
| P2–Widget polish (12/24-h, tap refresh) | Not started. Keep. |
| P2–Travel-day planner scoring | Not started. Keep. |
| P2–Compare 3 cities charts | Not started. Keep. |
| P3–Delete dead code (`WeatherDashboard.kt` legacy ~105 KB, `PushNotification*`) | Not started. Keep after a green release. |
| P3–Modal BACK matrix tests (Robolectric/screenshots) | Partially improved (regression tests for location/ads this cycle); keep. The 2026-10-18 device sweep found exactly this class of bug visually (map chip overlap UI-1, chart label bleed UI-2) — reinforcing F9. |
| P3–Baseline Profiles + R8 tuning | Reclassify → **P2 now**, because this is the only real remaining performance gap (PERF-A1). |
| P3–Wear OS / Android 15+ surfaces | Still out of scope until product decision. |

## 1. P0 — owner-side gates (not code features, listed for completeness)

1. **Create Play Billing products** and enroll product ids in `BillingProducts.kt` (B1). Unblocks the previously *untestable* purchase/restore/acknowledge E2E and the ad-free promise of paywall.
2. **Publish AdMob Privacy & Messaging forms** (B2). Unblocks consent UX and monetization in the EEA/UK/US-pilot regions.
3. **Commit + push updated privacy policy** (already authored locally). Aligns listing/policy with shipped data flows.
4. Baseline profile/perf work should wait until after the store test release — include in 1.6 planning.

## 2. P2 — high-value engineering investments (post-submission)

| # | Feature | Why it matters | Effort / risk |
|---|---|---|---|
| F1 | **Baseline Profile + macrobenchmark harness** | Only remaining measurable performance gap (PERF-A1); Compose cold-start and first-map-render both benefit. | Medium / low (pure build + tooling). |
| F2 | **R8 keep-slimming package** | APK ~22.9 MB universal; narrowing the broad keeps (ads/maplibre/coroutines) after smoke tests reduces dex significantly. | Medium / medium (needs release smoke discipline). |
| F3 | **Alert self-soak harness** | A debug-only WorkManager "trigger next scan now" hook lets QA prove the 3-hourly path end-to-end without waiting hours; pairs with notification channel (already verified). | Low / low. |
| F4 | **Debug consent-state inspector** | A debug-only screen showing the live UMP consent info, region gating, and which ads categories are eligible per current consent state — lets the owner validate B2 changes without ad spend. | Medium / low. |
| F5 | **Widget polish** (12/24-h respect, tap-to-refresh) | Carried from prior P2; user-visible daily surface. | Low / low. |
| F6 | **Compare 3 cities side-by-side** | Builds on existing Compare add-city flow; differentiator among weather apps. | Medium / low. |
| F7 | **Travel-day planner** scoring from cached hourly data | Distinct product feature; reuses existing capped-interstitial Travel entry. | Medium / medium. |

## 3. P3 — platform hardening & hygiene

| # | Item | Rationale |
|---|---|---|
| F8 | Delete legacy dead code after a green store cycle | Smaller dex, faster builds; zero user impact. |
| F9 | Screenshot + Robolectric BACK-matrix test suite | Catches regression like the historical F10 without a device; complements the 345 unit tests. |
| F10 | Make Lint a hard gate (`abortOnError=true`) once warnings are triaged | Converts current advisory gate (QA-A1) into a regression net. |
| F11 | Track `app/google-services.json`-via-CI-secret or add README copy step | Fresh-clone reproducibility (QA-A3). |
| F12 | Plan **GMA Next-Gen SDK** migration | Official GMA (legacy) is in maintenance mode as of the release notes; do the migration before any ads-SDK big-bang is forced. |
| F13 | Wear OS glance widget / tablet adaptive layout enhancements | New surface; needs owner product decision. |

## 4. Explicit non-goals (unchanged from prior roadmap)

- Switching weather providers, multi-module refactors, adopting navigation-compose, FCM push channels (WorkManager alerts cover the use case), and OEM-specific theming. These were rejected as breaking the audited architecture without countervailing user value.

## 5. Sequencing proposal (owner-facing)

1. Close B1/B2 owner items, policy push, Data Safety submit → **internal/closed testing**.
2. During soak: F3 (alert soak) + F1 (baseline profile) land quietly.
3. After first green store release: F2 (R8 slimming) + F8 (dead code) in one "performance release" with extra release smoke.
4. Then feature cadence: F5 → F6 → F7.

---

**Recommendation summary:** no feature work blocks 1.5; every open item is either owner-console setup or post-release engineering. The single most valuable next engineering investment is **F1 (Baseline Profile)** for startup performance.
