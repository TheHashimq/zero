/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies budget limits, access windows, cooldowns, daily caps, day changes, and tighten-only edits.
 * TAKES: Explicit budgets, states, days, and times.
 * GIVES: Pass/fail results for the budget rules.
 * OWNS: No production state.
 * DOES NOT OWN: Time measurement or the block screen.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one budget rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong value.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetRulesTest {
    private val day = "2026-11-20"
    private val nextDay = "2026-11-21"
    private val minute = 60_000L

    private fun budget(access: Int = 2, cooldown: Int = 10, cap: Int = 20) =
        Budget("com.whatsapp", access, cooldown, cap)

    /** Out-of-range values are rejected when a budget is created. */
    @Test
    fun rejectsOutOfRangeValues() {
        assertFailsWith<IllegalArgumentException> { Budget("com.x", 0, 10, 20) }
        assertFailsWith<IllegalArgumentException> { Budget("com.x", 2, 4, 20) }
        assertFailsWith<IllegalArgumentException> { Budget("com.x", 5, 10, 3) }
        assertFailsWith<IllegalArgumentException> { Budget("com.x", 2, 10, 481) }
    }

    /** The first visit starts an access window without blocking. */
    @Test
    fun firstVisitStartsAccessWindow() {
        val step = BudgetRules.advance(BudgetState(day), budget(access = 2), day, 0L, 0L)

        assertFalse(step.sendHome)
        assertEquals(2 * minute, step.state.accessLeftMillis)
    }

    /** Using the whole access window sends the user Home and starts the cooldown. */
    @Test
    fun usingFullAccessStartsCooldown() {
        val step = BudgetRules.advance(BudgetState(day), budget(access = 2, cooldown = 10), day, 0L, 2 * minute)

        assertTrue(step.sendHome)
        assertEquals(10 * minute, step.state.cooldownUntilMillis)
    }

    /** During the cooldown, the app is blocked. */
    @Test
    fun blockedDuringCooldown() {
        val first = BudgetRules.advance(BudgetState(day), budget(), day, 0L, 2 * minute).state
        val later = BudgetRules.advance(first, budget(), day, 1 * minute, 0L)

        assertTrue(later.sendHome)
    }

    /** When the cooldown ends, a new full access window starts. */
    @Test
    fun newWindowAfterCooldown() {
        val first = BudgetRules.advance(BudgetState(day), budget(), day, 0L, 2 * minute).state
        val later = BudgetRules.advance(first, budget(), day, 10 * minute, 0L)

        assertFalse(later.sendHome)
        assertEquals(2 * minute, later.state.accessLeftMillis)
    }

    /** Partial time carries over inside the same access window. */
    @Test
    fun partialTimeCarriesOver() {
        val first = BudgetRules.advance(BudgetState(day), budget(access = 2), day, 0L, 1 * minute)
        assertFalse(first.sendHome)
        assertEquals(1 * minute, first.state.accessLeftMillis)

        val second = BudgetRules.advance(first.state, budget(access = 2), day, 1 * minute, 1 * minute)
        assertTrue(second.sendHome)
    }

    /** The daily cap stops access even when a new window would otherwise start. */
    @Test
    fun dailyCapStopsAccess() {
        val b = budget(access = 3, cooldown = 5, cap = 5)
        val first = BudgetRules.advance(BudgetState(day), b, day, 0L, 3 * minute).state
        val second = BudgetRules.advance(first, b, day, 5 * minute, 3 * minute)

        assertTrue(second.sendHome)
        assertEquals(5 * minute, second.state.usedTodayMillis)
    }

    /** A new day resets usage, so access is available again. */
    @Test
    fun newDayResetsUsage() {
        val b = budget(access = 2, cooldown = 10, cap = 2)
        val used = BudgetRules.advance(BudgetState(day), b, day, 0L, 2 * minute).state
        val tomorrow = BudgetRules.advance(used, b, nextDay, 20 * minute, 0L)

        assertFalse(tomorrow.sendHome)
        assertEquals(0L, tomorrow.state.usedTodayMillis)
    }

    /** A cooldown that is still running carries into the next day, so midnight doesn't end it early. */
    @Test
    fun cooldownCarriesIntoNewDay() {
        val b = budget(access = 2, cooldown = 10)
        val exhausted = BudgetRules.advance(BudgetState(day), b, day, 0L, 2 * minute).state
        val tomorrow = BudgetRules.advance(exhausted, b, nextDay, 5 * minute, 0L)

        assertTrue(tomorrow.sendHome)
    }

    /** Less access is allowed. */
    @Test
    fun tighteningLowersAccess() {
        val old = budget(access = 5, cooldown = 10, cap = 60)

        assertTrue(BudgetRules.tighten(old, budget(access = 3, cooldown = 10, cap = 60)).isSuccess)
    }

    /** More access is refused. */
    @Test
    fun refusesMoreAccess() {
        val old = budget(access = 5, cooldown = 10, cap = 60)

        assertTrue(BudgetRules.tighten(old, budget(access = 6, cooldown = 10, cap = 60)).isFailure)
    }

    /** A shorter cooldown is refused. */
    @Test
    fun refusesShorterCooldown() {
        val old = budget(access = 5, cooldown = 10, cap = 60)

        assertTrue(BudgetRules.tighten(old, budget(access = 5, cooldown = 9, cap = 60)).isFailure)
    }

    /** A higher daily cap is refused. */
    @Test
    fun refusesHigherCap() {
        val old = budget(access = 5, cooldown = 10, cap = 60)

        assertTrue(BudgetRules.tighten(old, budget(access = 5, cooldown = 10, cap = 61)).isFailure)
    }

    /** Keeping the same values is allowed. */
    @Test
    fun sameValuesAreAllowed() {
        val old = budget()

        assertTrue(BudgetRules.isTightening(old, old))
    }

    /** Comparing budgets for different apps is rejected. */
    @Test
    fun comparingDifferentAppsIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            BudgetRules.isTightening(budget(), Budget("com.other", 2, 10, 20))
        }
    }
}
