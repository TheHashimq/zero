/*
 * MODULE: foundation-health
 * PURPOSE: Represents one component's status at a point in time.
 * TAKES: A component name, state, and human-readable detail.
 * GIVES: An immutable health report.
 * OWNS: No state.
 * DOES NOT OWN: Deciding the component state.
 * DEPENDENCIES: HealthState.
 * CONTRACT: The component name is never blank.
 * FAILURE BEHAVIOR: A blank component name throws IllegalArgumentException.
 */
package zero.foundation.health

/** One component's health status. */
data class HealthReport(
    /** Stable name of the component, such as "clock" or "accessibility". */
    val component: String,
    /** Severity of the component. */
    val state: HealthState,
    /** Short explanation for logs and diagnostics. */
    val detail: String,
) {
    init {
        require(component.isNotBlank()) { "component must not be blank" }
    }
}
