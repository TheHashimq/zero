/*
 * MODULE: foundation-contract
 * PURPOSE: Lists lifecycle states available to registry-managed modules.
 * TAKES: No inputs.
 * GIVES: The finite ModuleStatus state set.
 * OWNS: No mutable state.
 * DOES NOT OWN: State transitions or module lifecycle execution.
 * DEPENDENCIES: None.
 * CONTRACT: The registry reports one of these states for each registered module.
 * FAILURE BEHAVIOR: No operations are defined that can fail.
 */
package zero.foundation.contract

/** Lifecycle status reported for a registered module. */
enum class ModuleStatus {
    REGISTERED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED,
}
