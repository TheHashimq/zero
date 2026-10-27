/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Reports whether the accessibility service is enabled and connected.
 * TAKES: Two probes: is the service enabled in Settings, and is it connected.
 * GIVES: OK when both hold; DEGRADED with the reason otherwise.
 * OWNS: No state.
 * DOES NOT OWN: Reading Android settings. The app supplies the enabled probe.
 * DEPENDENCIES: foundation-health.
 * CONTRACT: Component name is "accessibility". Enabled-but-disconnected is never reported as OK.
 * FAILURE BEHAVIOR: Probe exceptions propagate to HealthService, which reports FAILED.
 */
package zero.feature.protection.runtime

import zero.foundation.health.HealthReport
import zero.foundation.health.HealthSource
import zero.foundation.health.HealthState

/** Health of Zero's accessibility service. */
class ProtectionAccessibilityHealthSource(
    private val enabled: () -> Boolean,
    private val connected: () -> Boolean,
) : HealthSource {
    override val component: String = "accessibility"

    override fun check(): HealthReport = when {
        !enabled() -> HealthReport(component, HealthState.DEGRADED, "not enabled in Settings")
        !connected() -> HealthReport(component, HealthState.DEGRADED, "enabled, not connected")
        else -> HealthReport(component, HealthState.OK, "enabled and connected")
    }
}
