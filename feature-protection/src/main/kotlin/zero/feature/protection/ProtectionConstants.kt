/*
 * MODULE: feature-protection
 * PURPOSE: Fixed timing values for the protection state machine.
 * TAKES: Nothing; constants.
 * GIVES: Grace window length and redirect guard length in milliseconds.
 * OWNS: No state.
 * DOES NOT OWN: Commitment duration. That is supplied at start and changed in Phase 10.
 * DEPENDENCIES: None.
 * CONTRACT: Values are positive and fixed for this phase.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.feature.protection

/** Fixed timing values. */
object ProtectionConstants {
    /** Length of the grace window after a commitment ends: 3 minutes. */
    const val GRACE_MILLIS: Long = 3L * 60L * 1000L

    /** Minimum gap between two redirects for the same app, to prevent redirect loops. */
    const val REDIRECT_GUARD_MILLIS: Long = 1500L
}
