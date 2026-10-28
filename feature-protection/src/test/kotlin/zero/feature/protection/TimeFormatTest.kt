/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies remaining-time formatting at boundaries.
 * TAKES: Explicit millisecond values.
 * GIVES: Pass/fail results for formatting.
 * OWNS: No production state.
 * DOES NOT OWN: State transitions.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Output rounds down to whole seconds and switches format at one hour.
 * FAILURE BEHAVIOR: Assertion failures show the wrong string.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatTest {
    /** Zero displays as 00:00. */
    @Test
    fun zeroIsZeroZero() {
        assertEquals("00:00", TimeFormat.remaining(0L))
    }

    /** 59.999 seconds still shows 00:59, rounding down. */
    @Test
    fun justUnderOneMinuteRoundsDown() {
        assertEquals("00:59", TimeFormat.remaining(59_999L))
    }

    /** Exactly one minute shows 01:00. */
    @Test
    fun exactlyOneMinute() {
        assertEquals("01:00", TimeFormat.remaining(60_000L))
    }

    /** Just under one hour uses mm:ss. */
    @Test
    fun justUnderOneHourUsesMinutes() {
        assertEquals("59:59", TimeFormat.remaining(3_599_999L))
    }

    /** Exactly one hour switches to h:mm:ss. */
    @Test
    fun exactlyOneHourUsesHours() {
        assertEquals("1:00:00", TimeFormat.remaining(3_600_000L))
    }

    /** Negative values display as 00:00. */
    @Test
    fun negativeIsZeroZero() {
        assertEquals("00:00", TimeFormat.remaining(-5_000L))
    }
}
