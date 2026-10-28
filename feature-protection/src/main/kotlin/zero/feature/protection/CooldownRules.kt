/*
 * MODULE: feature-protection
 * PURPOSE: One-way committed minimum for commitment length.
 * TAKES: A requested minimum or commitment duration, in milliseconds.
 * GIVES: RaiseResult for minimum changes; a yes/no for whether a duration is allowed.
 * OWNS: No state.
 * DOES NOT OWN: Storage of the minimum. See CooldownRepository.
 * DEPENDENCIES: None.
 * CONTRACT: The minimum only goes up. Valid minimums are 90 to 180 minutes inclusive.
 *           A commitment is allowed when its duration is at least the minimum and at most 180 minutes.
 * FAILURE BEHAVIOR: CommittedMinimum rejects out-of-range values with IllegalArgumentException.
 *                   Other functions never throw.
 */
package zero.feature.protection

/** Outcome of asking to raise the committed minimum. */
sealed interface RaiseResult {
    /** The minimum was raised to [minimum]. */
    data class Raised(val minimum: CommittedMinimum) : RaiseResult

    /** The requested value equals the current minimum. Nothing changes. */
    data object Unchanged : RaiseResult

    /** The requested value is above 180 minutes. */
    data object RejectedAboveMax : RaiseResult

    /** The requested value is below the current minimum. The minimum never goes down. */
    data object RejectedDecrease : RaiseResult
}

/** A committed minimum, always within 90 to 180 minutes. */
@JvmInline
value class CommittedMinimum(val millis: Long) {
    init {
        require(millis in CooldownRules.FLOOR_MILLIS..CooldownRules.MAX_MILLIS) {
            "committed minimum must be within ${CooldownRules.FLOOR_MILLIS}..${CooldownRules.MAX_MILLIS}, was $millis"
        }
    }
}

/** Pure rules for the one-way commitment minimum. */
object CooldownRules {
    /** Starting minimum and lowest possible value: 90 minutes. */
    const val FLOOR_MILLIS: Long = 90L * 60L * 1000L

    /** Hard cap for both the minimum and any commitment: 180 minutes. */
    const val MAX_MILLIS: Long = 180L * 60L * 1000L

    /** Returns the outcome of raising [current] to [requested]. */
    fun raise(current: CommittedMinimum, requested: Long): RaiseResult = when {
        requested > MAX_MILLIS -> RaiseResult.RejectedAboveMax
        requested < current.millis -> RaiseResult.RejectedDecrease
        requested < current.millis -> RaiseResult.RejectedDecrease
        requested == current.millis -> RaiseResult.Unchanged
        else -> RaiseResult.Raised(CommittedMinimum(requested))
    }

    /** True when a commitment of [durationMillis] is allowed under [committedMinimum]. */
    fun isAllowedDuration(durationMillis: Long, committedMinimum: Long): Boolean =
        durationMillis in committedMinimum..MAX_MILLIS
}
