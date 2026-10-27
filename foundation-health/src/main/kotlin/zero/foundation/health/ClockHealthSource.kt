/*
 * MODULE: foundation-health
 * PURPOSE: Reports whether reboot detection is available.
 * TAKES: A ZeroClock.
 * GIVES: OK when the boot ID is readable; DEGRADED when it is unavailable.
 * OWNS: No state.
 * DOES NOT OWN: Reboot classification. See BootReconciler.
 * DEPENDENCIES: ZeroClock, HealthSource.
 * CONTRACT: Reports boot ID availability only.
 * FAILURE BEHAVIOR: An unavailable boot ID is DEGRADED.
 */
package zero.foundation.health

import zero.foundation.clock.ZeroClock

/** Health source for the clock's boot identity. */
class ClockHealthSource(private val clock: ZeroClock) : HealthSource {
    override val component: String = "clock"

    override fun check(): HealthReport =
        if (clock.currentBootId() != null) {
            HealthReport(component, HealthState.OK, "boot id readable")
        } else {
            HealthReport(component, HealthState.DEGRADED, "boot id unavailable; reboot detection is limited")
        }
}
