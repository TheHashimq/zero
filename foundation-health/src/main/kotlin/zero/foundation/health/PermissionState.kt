/*
 * MODULE: foundation-health
 * PURPOSE: Represents whether a permission or system setting is enabled.
 * TAKES: No inputs; a value type and probe interface.
 * GIVES: GRANTED, DENIED, or UNKNOWN permission states.
 * OWNS: No state.
 * DOES NOT OWN: Reading platform settings.
 * DEPENDENCIES: None.
 * CONTRACT: PermissionProbe reads current state whenever called; values are not cached here.
 * FAILURE BEHAVIOR: Probe Exceptions are reported as FAILED by HealthService.
 */
package zero.foundation.health

/** State of one permission or system setting. */
enum class PermissionState { GRANTED, DENIED, UNKNOWN }

/** Reads the current state of one permission or setting. */
fun interface PermissionProbe {
    /** Returns the permission state right now. */
    fun state(): PermissionState
}
