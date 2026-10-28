/*
 * MODULE: foundation-clock tests
 * PURPOSE: Verifies that a simulated reboot resets monotonic time and is detected.
 * TAKES: DefaultZeroClock with fake sources.
 * GIVES: Pass/fail result for end-to-end reboot behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Persistence of the stored boot ID.
 * DEPENDENCIES: foundation-clock, foundation-testing.
 * CONTRACT: After a simulated reboot, monotonic time restarts and reconcile reports REBOOTED.
 * FAILURE BEHAVIOR: Assertion failures identify the broken reboot path.
 */
package zero.foundation.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource

/** Tests reboot behavior using deterministic fake time and boot-ID sources. */
class RebootIntegrationTest {
    /** A reboot resets monotonic time to a small value and changes the boot ID. */
    @Test
    fun rebootResetsMonotonicAndReportsRebooted() {
        val mono = FakeMonotonicSource()
        val ids = FakeBootIdSource("boot-1")
        val clock = DefaultZeroClock(mono, FakeWallClockSource(), ids)

        mono.advance(5_000L)
        assertEquals(5_000L, clock.monotonicMillis())

        ids.bootId = "boot-2"
        mono.setTo(0L)

        assertEquals(0L, clock.monotonicMillis())
        assertEquals(
            BootState.REBOOTED,
            BootReconciler.reconcile("boot-1", clock.currentBootId()),
        )
    }
}
