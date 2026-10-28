/*
 * MODULE: feature-protection
 * PURPOSE: The action the policy asks the platform to perform.
 * TAKES: Nothing; value types.
 * GIVES: Allow or RedirectHome.
 * OWNS: No state.
 * DOES NOT OWN: Performing the action. The Android adapter does that in Phase 8b.
 * DEPENDENCIES: None.
 * CONTRACT: Exactly two actions exist.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.feature.protection

/** Action the platform should take for a foreground app change. */
sealed interface ProtectionAction {
    /** Leave the app alone. */
    data object Allow : ProtectionAction

    /** Send the user to the Home screen. */
    data object RedirectHome : ProtectionAction
}
