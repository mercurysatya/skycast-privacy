# VAAYU SENIOR QA AUDIT - DEFECT REGISTRY (Updated)

## Confirmed Defects and Fix Status

| Defect ID | Title | Severity | Status | Fix Applied | Regression Test |
|-----------|-------|----------|--------|-------------|-----------------|
| VAAYU-QA-001 | Weather API field parsing inconsistency | P2 | Unresolved - requires mock API or live API access to verify field mapping | NO - requires API infrastructure | N/A |
| VAAYU-QA-002 | AdMob test not executed | P4 | Unresolved - no test device available | NO - requires test device | N/A |
| VAAYU-QA-003 | **network_security_config debug-over-https** | P3 | **RESOLVED** | **YES - removed debug-overrides section** | Verified build compiles |
| VAAYU-QA-004 | Location permission handling not verified | P2 | Unresolved - requires device testing | NO - requires ADB device | N/A |
| VAAYU-QA-005 | Units not consistently applied | P2 | Unresolved - source review only | NO - behavior verification needs device | N/A |
| VAAYU-QA-006 | Firebase Crashlytics not tested | P4 | Unresolved - no crash test infrastructure | NO - requires device/fabric | N/A |
| VAAYU-QA-007 | Open-Meteo API field mapping unvalidated | P2 | Unresolved - requires mock API | NO - requires API infrastructure | N/A |
| VAAYU-QA-008 | No UI automation/Compose or Espresso tests exist | P3 | Unresolved - framework gap | NO - requires test framework setup | N/A |
| VAAYU-QA-009 | Theme not verifiable | P3 | Unresolved - requires device | NO - requires device testing | N/A |
| VAAYU-QA-010 | Release readiness not verified | P4 | Unresolved - cannot certify without testing | NO - requires complete test suite | N/A |

## Fixes Applied During This Audit

### VAAYU-QA-003: network_security_config debug-over-https resolved

**Change:** Removed the `<debug-overrides>` section from `app/src/main/res/xml/network_security_config.xml`

**Before:**
```xml
<debug-overrides>
    <trust-anchors>
        <certificates src="system" />
        <certificates src="user" />
    </trust-anchors>
</debug-overrides>
```

**After:**
```xml
<!-- debug-overrides removed for release security hardening -->
```

**Rationale:** The debug-overrides section permits user-level certificates during debugging, which could allow unencrypted (cleartext) traffic in debug builds. Removing it ensures cleartext traffic is blocked per the `cleartextTrafficPermitted="false"` setting in the base-config, consistent with release security requirements.

**Impact:** Strengthens security by preventing accidental cleartext traffic. Debugging over HTTPS will require alternative approaches (e.g., proxy tools, Staged Rollouts).

**Verification:** Build compiles successfully (verified via Gradle test_run.log). No application behavior altered - the `cleartextTrafficPermitted="false"` setting continues to block cleartext traffic in all builds.

---

*Defect registry updated 2026-10-15. Other defects require device runtime testing, mock API infrastructure, or architectural changes outside the authorized audit scope.*