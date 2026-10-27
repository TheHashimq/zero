/*
 * MODULE: foundation-clock
 * PURPOSE: Abstracts the identifier of the current boot session.
 * TAKES: Nothing.
 * GIVES: A boot identifier string, or null when the platform cannot supply one.
 * OWNS: No state.
 * DOES NOT OWN: Comparing identifiers. See BootReconciler.
 * DEPENDENCIES: None.
 * CONTRACT: The same value is returned for the whole boot session and changes after a reboot.
 * FAILURE BEHAVIOR: Returns null when the identifier is unavailable. Does not throw.
 */
package zero.foundation.clock

/** Supplies the identifier of the current boot session, or null if unavailable. */
fun interface BootIdSource {
    /** Returns the current boot identifier, or null if the platform cannot provide one. */
    fun currentBootId(): String?
}
