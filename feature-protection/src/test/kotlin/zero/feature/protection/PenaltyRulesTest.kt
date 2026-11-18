/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies penalty lengths, the cap, daily counting, and remaining time.
 * TAKES: Explicit attempt numbers, day keys, and times.
 * GIVES: Pass/fail results for the penalty rules.
 * OWNS: No production state.
 * DOES NOT OWN: The penalty screen or storage.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one penalty rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong value.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PenaltyRulesTest {
    private val minute = 60_000L

    /** The first attempt gives 5 minutes. */
    @Test
    fun firstAttemptIsFiveMinutes() {
        assertEquals(5, PenaltyRules.minutesFor(1))
    }

    /** Each further attempt adds 5 minutes. */
    @Test
    fun attemptsAddFiveMinutesEach() {
        assertEquals(10, PenaltyRules.minutesFor(2))
        assertEquals(25, PenaltyRules.minutesFor(5))
    }

    /** The sixth attempt reaches the 30 minute cap, and later attempts stay there. */
    @Test
    fun lengthIsCappedAtThirtyMinutes() {
        assertEquals(30, PenaltyRules.minutesFor(6))
        assertEquals(30, PenaltyRules.minutesFor(20))
    }

    /** An attempt below 1 is rejected. */
    @Test
    fun zeroAttemptIsRejected() {
        assertFailsWith<IllegalArgumentException> { PenaltyRules.minutesFor(0) }
    }

    /** Attempts on the same day count up. */
    @Test
    fun attemptsOnSameDayCountUp() {
        val first = PenaltyRules.recordAttempt(null, "2026-11-20", 1_000L)
        val second = PenaltyRules.recordAttempt(first, "2026-11-20", 2_000L)

        assertEquals(2, second.attempts)
        assertEquals(2_000L + 10 * minute, second.lockedUntilMillis)
    }

    /** The count starts again on a new day. */
    @Test
    fun newDayResetsCount() {
        val yesterday = PenaltyRules.recordAttempt(null, "2026-11-19", 1_000L)
        val today = PenaltyRules.recordAttempt(yesterday, "2026-11-20", 2_000L)

        assertEquals(1, today.attempts)
        assertEquals(5, (today.lockedUntilMillis - 2_000L) / minute)
    }

    /** The penalty is running until its end time, and not after. */
    @Test
    fun lockedUntilTheEndTime() {
        val state = PenaltyRules.recordAttempt(null, "2026-11-20", 0L)

        assertTrue(PenaltyRules.isLocked(state, 4 * minute))
        assertFalse(PenaltyRules.isLocked(state, 5 * minute))
    }

    /** Remaining time is never negative. */
    @Test
    fun remainingTimeIsNeverNegative() {
        val state = PenaltyRules.recordAttempt(null, "2026-11-20", 0L)

        assertEquals(5 * minute, PenaltyRules.remainingMillis(state, 0L))
        assertEquals(0L, PenaltyRules.remainingMillis(state, 10 * minute))
        assertEquals(0L, PenaltyRules.remainingMillis(null, 0L))
    }
}
