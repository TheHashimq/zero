/*
 * MODULE: foundation-health tests
 * PURPOSE: Verifies mapping from permission state to health state.
 * TAKES: Fixed probe results.
 * GIVES: Pass/fail results for the mapping.
 * OWNS: No production state.
 * DOES NOT OWN: Reading Android settings.
 * DEPENDENCIES: foundation-health.
 * CONTRACT: GRANTED is OK; DENIED and UNKNOWN are DEGRADED.
 * FAILURE BEHAVIOR: Assertion failures identify an incorrect mapping.
 */
package zero.foundation.health

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests permission-state mapping. */
class PermissionHealthSourceTest {
    private fun source(state: PermissionState) = PermissionHealthSource("accessibility") { state }

    /** A granted permission is OK. */
    @Test
    fun grantedIsOk() {
        assertEquals(HealthState.OK, source(PermissionState.GRANTED).check().state)
    }

    /** A denied permission is DEGRADED. */
    @Test
    fun deniedIsDegraded() {
        assertEquals(HealthState.DEGRADED, source(PermissionState.DENIED).check().state)
    }

    /** An unknown permission is DEGRADED, not OK. */
    @Test
    fun unknownIsDegraded() {
        assertEquals(HealthState.DEGRADED, source(PermissionState.UNKNOWN).check().state)
    }
}
