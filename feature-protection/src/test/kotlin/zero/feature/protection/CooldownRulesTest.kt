/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies the one-way minimum rules and the commitment duration check.
 * TAKES: Explicit minimums and requested values.
 * GIVES: Pass/fail results for the rules.
 * OWNS: No production state.
 * DOES NOT OWN: Storage.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CooldownRulesTest {
    private val minute = 60_000L

    /** The floor and the cap are both valid committed minimums. */
    @Test
    fun floorAndMaxAreValidMinimums() {
        assertEquals(90 * minute, CommittedMinimum(CooldownRules.FLOOR_MILLIS).millis)
        assertEquals(180 * minute, CommittedMinimum(CooldownRules.MAX_MILLIS).millis)
    }

    /** Values below the floor cannot form a committed minimum. */
    @Test
    fun belowFloorCannotBeCommittedMinimum() {
        assertFailsWith<IllegalArgumentException> { CommittedMinimum(89 * minute) }
    }

    /** Raising from 90 to 120 minutes is accepted and returns the new minimum. */
    @Test
    fun raiseToHigherValueIsAccepted() {
        val result = CooldownRules.raise(CommittedMinimum(90 * minute), 120 * minute)

        assertEquals(RaiseResult.Raised(CommittedMinimum(120 * minute)), result)
    }

    /** Requesting the current value changes nothing. */
    @Test
    fun raiseToSameValueIsUnchanged() {
        assertEquals(
            RaiseResult.Unchanged,
            CooldownRules.raise(CommittedMinimum(120 * minute), 120 * minute),
        )
    }

    /** Requesting a value above 180 minutes is rejected. */
    @Test
    fun raiseAboveMaxIsRejected() {
        assertEquals(
            RaiseResult.RejectedAboveMax,
            CooldownRules.raise(CommittedMinimum(90 * minute), 181 * minute),
        )
    }

    /** Requesting a lower value is rejected, so the minimum never goes down. */
    @Test
    fun decreaseIsRejected() {
        assertEquals(
            RaiseResult.RejectedDecrease,
            CooldownRules.raise(CommittedMinimum(120 * minute), 90 * minute),
        )
    }

    /** Raising to exactly 180 minutes is accepted. */
    @Test
    fun raiseToMaxIsAccepted() {
        assertEquals(
            RaiseResult.Raised(CommittedMinimum(180 * minute)),
            CooldownRules.raise(CommittedMinimum(90 * minute), 180 * minute),
        )
    }

    /** A duration is allowed only when it is at least the minimum and at most 180 minutes. */
    @Test
    fun durationMustBeAtLeastMinimumAndAtMostMax() {
        assertFalse(CooldownRules.isAllowedDuration(119 * minute, 120 * minute))
        assertTrue(CooldownRules.isAllowedDuration(120 * minute, 120 * minute))
        assertTrue(CooldownRules.isAllowedDuration(180 * minute, 120 * minute))
        assertFalse(CooldownRules.isAllowedDuration(181 * minute, 120 * minute))
    }
}
