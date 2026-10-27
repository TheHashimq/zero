/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies every transition, boundary, rejection, and the reboot rule.
 * TAKES: Explicit states and events with fixed times.
 * GIVES: Pass/fail results for the state machine.
 * OWNS: No production state.
 * DOES NOT OWN: Policy decisions.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken transition.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtectionMachineTest {
    private val grace = ProtectionConstants.GRACE_MILLIS

    /** Start from Armed begins a countdown at the given time with the given duration. */
    @Test
    fun startFromArmedBeginsCountdown() {
        val t = ProtectionMachine.reduce(
            ProtectionState.Armed,
            ProtectionEvent.Start(durationMillis = 1_000L, nowMillis = 500L, bootKnown = true),
        )

        assertTrue(t.accepted)
        assertEquals(ProtectionState.Countdown(startedAtMillis = 500L, durationMillis = 1_000L), t.state)
    }

    /** Start during a countdown is rejected and leaves the countdown unchanged. */
    @Test
    fun startWhileCountdownIsRejected() {
        val state = ProtectionState.Countdown(0L, 1_000L)

        val t = ProtectionMachine.reduce(state, ProtectionEvent.Start(5_000L, 10L, true))

        assertFalse(t.accepted)
        assertEquals(state, t.state)
    }

    /** Start during grace is rejected and leaves grace unchanged. */
    @Test
    fun startWhileGraceIsRejected() {
        val state = ProtectionState.Grace(100L)

        val t = ProtectionMachine.reduce(state, ProtectionEvent.Start(5_000L, 10L, true))

        assertFalse(t.accepted)
        assertEquals(state, t.state)
    }

    /** Start is rejected when the boot ID is unknown, so reboot restarts cannot be detected. */
    @Test
    fun startWithoutKnownBootIsRejected() {
        val t = ProtectionMachine.reduce(
            ProtectionState.Armed,
            ProtectionEvent.Start(1_000L, 0L, bootKnown = false),
        )

        assertFalse(t.accepted)
        assertEquals(ProtectionState.Armed, t.state)
    }

    /** A zero duration throws instead of starting an empty commitment. */
    @Test
    fun startWithZeroDurationThrows() {
        assertFailsWith<IllegalArgumentException> {
            ProtectionMachine.reduce(ProtectionState.Armed, ProtectionEvent.Start(0L, 0L, true))
        }
    }

    /** A negative duration throws instead of starting an invalid commitment. */
    @Test
    fun startWithNegativeDurationThrows() {
        assertFailsWith<IllegalArgumentException> {
            ProtectionMachine.reduce(ProtectionState.Armed, ProtectionEvent.Start(-1L, 0L, true))
        }
    }

    /** A countdown is still running one millisecond before its end. */
    @Test
    fun tickBeforeEndKeepsCountdown() {
        val state = ProtectionState.Countdown(0L, 1_000L)

        assertEquals(state, ProtectionMachine.reduce(state, ProtectionEvent.Tick(999L)).state)
    }

    /** At the exact end time, the countdown becomes grace, starting at the countdown end. */
    @Test
    fun tickAtEndEntersGraceAtCountdownEnd() {
        val t = ProtectionMachine.reduce(ProtectionState.Countdown(0L, 1_000L), ProtectionEvent.Tick(1_000L))

        assertEquals(ProtectionState.Grace(1_000L), t.state)
    }

    /** Grace is still active one millisecond before its end. */
    @Test
    fun tickDuringGraceKeepsGrace() {
        val state = ProtectionState.Grace(1_000L)

        assertEquals(state, ProtectionMachine.reduce(state, ProtectionEvent.Tick(1_000L + grace - 1)).state)
    }

    /** At the exact grace end, protection re-arms. */
    @Test
    fun tickAtGraceEndReturnsToArmed() {
        val t = ProtectionMachine.reduce(ProtectionState.Grace(1_000L), ProtectionEvent.Tick(1_000L + grace))

        assertEquals(ProtectionState.Armed, t.state)
    }

    /** A tick long after the countdown ended goes straight to Armed, not Grace. */
    @Test
    fun lateTickSkipsExpiredGrace() {
        val t = ProtectionMachine.reduce(
            ProtectionState.Countdown(0L, 1_000L),
            ProtectionEvent.Tick(1_000L + grace + 5L),
        )

        assertEquals(ProtectionState.Armed, t.state)
    }

    /** Screen off during grace revokes the grace window. */
    @Test
    fun screenOffRevokesGrace() {
        val t = ProtectionMachine.reduce(ProtectionState.Grace(1_000L), ProtectionEvent.ScreenOff)

        assertEquals(ProtectionState.Armed, t.state)
    }

    /** Screen off during a countdown does not end the commitment. */
    @Test
    fun screenOffDuringCountdownIsIgnored() {
        val state = ProtectionState.Countdown(0L, 1_000L)

        assertEquals(state, ProtectionMachine.reduce(state, ProtectionEvent.ScreenOff).state)
    }

    /** A reboot during a countdown restarts it from the new time with the full duration. */
    @Test
    fun rebootRestartsCountdownWithFullDuration() {
        val t = ProtectionMachine.reduce(
            ProtectionState.Countdown(0L, 1_000L),
            ProtectionEvent.BootChanged(200L),
        )

        assertEquals(ProtectionState.Countdown(200L, 1_000L), t.state)
    }

    /** A reboot during grace ends grace, since the grace window cannot survive a restart. */
    @Test
    fun rebootEndsGrace() {
        val t = ProtectionMachine.reduce(ProtectionState.Grace(1_000L), ProtectionEvent.BootChanged(1_100L))

        assertEquals(ProtectionState.Armed, t.state)
    }

    /** A reboot while armed changes nothing. */
    @Test
    fun rebootWhileArmedIsIgnored() {
        val t = ProtectionMachine.reduce(ProtectionState.Armed, ProtectionEvent.BootChanged(100L))

        assertEquals(ProtectionState.Armed, t.state)
    }

    /** A time earlier than the countdown start keeps the countdown. The machine does not guess. */
    @Test
    fun backwardTimeKeepsCountdown() {
        val state = ProtectionState.Countdown(5_000L, 1_000L)

        assertEquals(state, ProtectionMachine.reduce(state, ProtectionEvent.Tick(100L)).state)
    }

    /** The countdown end is start plus duration. */
    @Test
    fun countdownEndIsStartPlusDuration() {
        assertEquals(1_500L, ProtectionState.Countdown(startedAtMillis = 500L, durationMillis = 1_000L).endsAtMillis)
    }
}
