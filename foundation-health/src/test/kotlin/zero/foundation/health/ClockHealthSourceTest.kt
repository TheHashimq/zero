/*
 * MODULE: foundation-health tests
 * PURPOSE: Verifies clock health reflects boot ID availability.
 * TAKES: Fake clock sources with and without a boot ID.
 * GIVES: Pass/fail results for clock health.
 * OWNS: No production state.
 * DOES NOT OWN: Reboot classification.
 * DEPENDENCIES: foundation-health, foundation-clock, foundation-testing.
 * CONTRACT: Readable boot ID is OK; missing boot ID is DEGRADED.
 * FAILURE BEHAVIOR: Assertion failures identify an incorrect clock mapping.
 */
package zero.foundation.health

import kotlin.test.Test
import kotlin.test.assertEquals
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource

/** Tests clock boot-ID health reporting. */
class ClockHealthSourceTest {
    private fun clockWithBootId(bootId: String?) = DefaultZeroClock(
        FakeMonotonicSource(),
        FakeWallClockSource(),
        FakeBootIdSource(bootId),
    )

    /** A readable boot ID is OK. */
    @Test
    fun readableBootIdIsOk() {
        val report = ClockHealthSource(clockWithBootId("boot-1")).check()

        assertEquals(HealthState.OK, report.state)
        assertEquals("clock", report.component)
    }

    /** A missing boot ID is DEGRADED. */
    @Test
    fun missingBootIdIsDegraded() {
        assertEquals(HealthState.DEGRADED, ClockHealthSource(clockWithBootId(null)).check().state)
    }
}
