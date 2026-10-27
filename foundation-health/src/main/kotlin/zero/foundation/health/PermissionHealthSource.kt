/*
 * MODULE: foundation-health
 * PURPOSE: Turns a permission's state into a health report.
 * TAKES: A component name and a PermissionProbe.
 * GIVES: OK when granted; DEGRADED when denied or unknown.
 * OWNS: No state.
 * DOES NOT OWN: Platform permission reading or feature policy for denial.
 * DEPENDENCIES: PermissionProbe, HealthSource.
 * CONTRACT: Reports reflect the setting, not whether a service is currently connected.
 * FAILURE BEHAVIOR: Probe Exceptions propagate to HealthService, which reports FAILED.
 */
package zero.foundation.health

/** Health source for one permission or system setting. */
class PermissionHealthSource(
    override val component: String,
    private val probe: PermissionProbe,
) : HealthSource {
    override fun check(): HealthReport = when (val state = probe.state()) {
        PermissionState.GRANTED -> HealthReport(component, HealthState.OK, "granted")
        PermissionState.DENIED -> HealthReport(component, HealthState.DEGRADED, "not granted")
        PermissionState.UNKNOWN -> HealthReport(component, HealthState.DEGRADED, "could not be determined ($state)")
    }
}
