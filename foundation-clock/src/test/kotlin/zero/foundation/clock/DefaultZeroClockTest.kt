/*
 * MODULE: foundation-clock tests
 * PURPOSE: Verifies DefaultZeroClock passes source values through unchanged.
 * TAKES: Fake sources.
 * GIVES: Pass/fail results for pass-through behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Reboot classification.
 * DEPENDENCIES: foundation-clock, foundation-testing.
 * CONTRACT: Each accessor returns exactly what its source returns.
 * FAILURE BEHAVIOR: Assertion failures identify the broken pass-through.
 */
package zero.foundation.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource

/** Tests the source pass-through behavior of DefaultZeroClock. */
class DefaultZeroClockTest {
    /** Returns the exact value the monotonic source reports. */
    @Test
    fun returnsMonotonicValueFromSource() {
        val mono = FakeMonotonicSource()
        val clock = DefaultZeroClock(mono, FakeWallClockSource(), FakeBootIdSource())

        mono.setTo(42L)

        assertEquals(42L, clock.monotonicMillis())
    }

    /** Reflects each advance exactly. */
    @Test
    fun monotonicAdvancesExactlyWithSource() {
        val mono = FakeMonotonicSource()
        val clock = DefaultZeroClock(mono, FakeWallClockSource(), FakeBootIdSource())

        mono.advance(100L)
        assertEquals(100L, clock.monotonicMillis())

        mono.advance(50L)
        assertEquals(150L, clock.monotonicMillis())
    }

    /** Wall time does not change when monotonic time advances. */
    @Test
    fun wallClockIsIndependentOfMonotonic() {
        val mono = FakeMonotonicSource()
        val wall = FakeWallClockSource(now = 1_000L)
        val clock = DefaultZeroClock(mono, wall, FakeBootIdSource())

        mono.advance(10L)
        assertEquals(1_000L, clock.wallClockMillis())

        wall.now = 2_000L
        assertEquals(2_000L, clock.wallClockMillis())
    }

    /** Returns the boot ID from the source. */
    @Test
    fun bootIdPassesThrough() {
        val clock = DefaultZeroClock(
            FakeMonotonicSource(),
            FakeWallClockSource(),
            FakeBootIdSource(bootId = "abc"),
        )

        assertEquals("abc", clock.currentBootId())
    }

    /** Returns null when the boot ID source has no value. */
    @Test
    fun nullBootIdPassesThrough() {
        val clock = DefaultZeroClock(
            FakeMonotonicSource(),
            FakeWallClockSource(),
            FakeBootIdSource(bootId = null),
        )

        assertNull(clock.currentBootId())
    }
}
