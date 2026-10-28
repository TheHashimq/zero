/*
 * MODULE: foundation-clock
 * PURPOSE: Decides whether the device rebooted since a boot ID was stored.
 * TAKES: A stored boot ID (or null) and the current boot ID (or null).
 * GIVES: A BootState.
 * OWNS: No state. Pure function.
 * DOES NOT OWN: Reading or writing the stored value. The caller persists it.
 * DEPENDENCIES: BootState.
 * CONTRACT: Returns UNKNOWN whenever the current ID is null, regardless of stored value.
 *           The caller should store the current ID after FIRST_SEEN or REBOOTED,
 *           and must not overwrite the stored ID after UNKNOWN.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.foundation.clock

/** Compares stored and current boot IDs to classify the boot session. */
object BootReconciler {
    /**
     * Classifies the boot session.
     *
     * Evaluation order: current null gives UNKNOWN; stored null gives FIRST_SEEN;
     * equal values give SAME_BOOT; otherwise REBOOTED.
     */
    fun reconcile(storedBootId: String?, currentBootId: String?): BootState = when {
        currentBootId == null -> BootState.UNKNOWN
        storedBootId == null -> BootState.FIRST_SEEN
        storedBootId == currentBootId -> BootState.SAME_BOOT
        else -> BootState.REBOOTED
    }
}
