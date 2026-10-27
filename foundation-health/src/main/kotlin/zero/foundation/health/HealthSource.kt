/*
 * MODULE: foundation-health
 * PURPOSE: Defines a source that reports the health of one component.
 * TAKES: No inputs; a source reads its own live state.
 * GIVES: A HealthReport for the component.
 * OWNS: No state beyond the values it reads.
 * DOES NOT OWN: Aggregation. See HealthService.
 * DEPENDENCIES: HealthReport.
 * CONTRACT: The report component should equal this source's component.
 * FAILURE BEHAVIOR: HealthService converts a thrown Exception into a FAILED report.
 */
package zero.foundation.health

/** Reports the health of one component. */
interface HealthSource {
    /** Stable name of the component this source reports on. */
    val component: String

    /** Returns the component's current health. */
    fun check(): HealthReport
}
