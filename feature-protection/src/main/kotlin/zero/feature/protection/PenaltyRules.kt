/*
 * MODULE: feature-protection
 * PURPOSE: How long a penalty lasts for each install attempt, and how attempts are counted per day.
 * TAKES: An attempt number (1 for the first attempt of the day), a day key, and the current time in milliseconds.
 * GIVES: The penalty length in minutes, and a new penalty state after each attempt.
 * OWNS: The step and the cap.
 * DOES NOT OWN: Showing the penalty screen or saving the state. The app does that.
 * DEPENDENCIES: None.
 * CONTRACT: Attempt 1 gives 5 minutes, attempt 2 gives 10, and so on, capped at 30 minutes.
 *           The count resets when the day key changes. Each attempt restarts the penalty from now.
 * FAILURE BEHAVIOR: An attempt below 1 throws IllegalArgumentException.
 */
package zero.feature.protection

/** Saved penalty state for one day. */
data class PenaltyState(val dayKey: String, val attempts: Int, val lockedUntilMillis: Long)

object PenaltyRules {
    const val STEP_MINUTES: Int = 5
    const val MAX_MINUTES: Int = 30

    fun minutesFor(attempt: Int): Int {
        require(attempt >= 1) { "attempt must be at least 1, was $attempt" }
        return minOf(attempt * STEP_MINUTES, MAX_MINUTES)
    }

    /** The new state after one more attempt on [dayKey]. */
    fun recordAttempt(previous: PenaltyState?, dayKey: String, nowMillis: Long): PenaltyState {
        val base = if (previous == null || previous.dayKey != dayKey) PenaltyState(dayKey, 0, 0L) else previous
        val attempts = base.attempts + 1
        val lockedUntil = nowMillis + minutesFor(attempts) * 60_000L
        return PenaltyState(dayKey, attempts, lockedUntil)
    }

    /** True while a penalty is running. */
    fun isLocked(state: PenaltyState?, nowMillis: Long): Boolean =
        state != null && nowMillis < state.lockedUntilMillis

    /** Milliseconds left in the penalty, or zero when none is running. */
    fun remainingMillis(state: PenaltyState?, nowMillis: Long): Long =
        if (state == null) 0L else maxOf(0L, state.lockedUntilMillis - nowMillis)
}
