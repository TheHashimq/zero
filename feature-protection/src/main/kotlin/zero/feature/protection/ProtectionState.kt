/*
 * MODULE: feature-protection
 * PURPOSE: Names the protection lifecycle states.
 * TAKES: Nothing; value types.
 * GIVES: Armed, Countdown, and Grace, with their end times.
 * OWNS: No state.
 * DOES NOT OWN: Transitions. See ProtectionMachine.
 * DEPENDENCIES: ProtectionConstants.
 * CONTRACT: Every time is monotonic milliseconds. Grace starts when its countdown ends.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.feature.protection

/** Protection lifecycle. All times are monotonic milliseconds. */
sealed interface ProtectionState {
    /** Protection is on, but no commitment is running. Nothing is blocked. */
    data object Armed : ProtectionState

    /** A commitment is running. Protected packages are blocked until [endsAtMillis]. */
    data class Countdown(
        val startedAtMillis: Long,
        val durationMillis: Long,
    ) : ProtectionState {
        val endsAtMillis: Long get() = startedAtMillis + durationMillis
    }

    /** Short window after a countdown ends. Nothing is blocked. */
    data class Grace(val startedAtMillis: Long) : ProtectionState {
        val endsAtMillis: Long get() = startedAtMillis + ProtectionConstants.GRACE_MILLIS
    }
}
