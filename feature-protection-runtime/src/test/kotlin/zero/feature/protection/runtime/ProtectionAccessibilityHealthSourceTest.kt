/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies the accessibility health mapping.
 * TAKES: Fixed enabled and connected probe values.
 * GIVES: Pass/fail results for the health mapping.
 * OWNS: No production state.
 * DOES NOT OWN: Android settings.
 * DEPENDENCIES: feature-protection-runtime, foundation-health.
 * CONTRACT: OK only when enabled and connected.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong mapping.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import zero.foundation.health.HealthState

class ProtectionAccessibilityHealthSourceTest {
    /** Enabled and connected is OK. */
    @Test
    fun enabledAndConnectedIsOk() {
        val report = ProtectionAccessibilityHealthSource({ true }, { true }).check()
        assertEquals(HealthState.OK, report.state)
    }

    /** Enabled but not connected is DEGRADED, never OK. */
    @Test
    fun enabledButNotConnectedIsDegraded() {
        val report = ProtectionAccessibilityHealthSource({ true }, { false }).check()

        assertEquals(HealthState.DEGRADED, report.state)
        assertTrue(report.detail.contains("not connected"))
    }

    /** Not enabled is DEGRADED, whatever the connection says. */
    @Test
    fun notEnabledIsDegraded() {
        val report = ProtectionAccessibilityHealthSource({ false }, { true }).check()

        assertEquals(HealthState.DEGRADED, report.state)
        assertTrue(report.detail.contains("not enabled"))
    }

    /** The component name is "accessibility". */
    @Test
    fun componentNameIsAccessibility() {
        assertEquals("accessibility", ProtectionAccessibilityHealthSource({ true }, { true }).component)
    }
}
