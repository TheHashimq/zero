/*
 * MODULE: foundation-health
 * PURPOSE: Collects component reports and computes overall system health.
 * TAKES: Health sources with unique component names.
 * GIVES: SystemHealth with the worst state and reports in source order.
 * OWNS: The source list.
 * DOES NOT OWN: The individual health checks.
 * DEPENDENCIES: HealthSource, HealthReport, HealthState.
 * CONTRACT: report() never throws; an empty source list is OK.
 * FAILURE BEHAVIOR: Source Exceptions and wrong component names become FAILED reports.
 *                   Duplicate component names are rejected at construction.
 */
package zero.foundation.health

/** Overall health plus each component's report. */
data class SystemHealth(
    /** Worst health state across the components. */
    val overall: HealthState,
    /** Component reports in source order. */
    val components: List<HealthReport>,
)

/** Aggregates health sources. */
class HealthService(private val sources: List<HealthSource>) {
    init {
        val names = sources.map { it.component }
        require(names.size == names.toSet().size) { "duplicate health component names: $names" }
    }

    /** Runs every source and returns the combined result. Never throws for source Exceptions. */
    fun report(): SystemHealth {
        val reports = sources.map(::checkSafely)
        val overall = reports.maxOfOrNull { it.state } ?: HealthState.OK
        return SystemHealth(overall, reports)
    }

    private fun checkSafely(source: HealthSource): HealthReport {
        val report = try {
            source.check()
        } catch (failure: Exception) {
            return HealthReport(source.component, HealthState.FAILED, "check threw ${failure.javaClass.simpleName}")
        }
        return if (report.component == source.component) {
            report
        } else {
            HealthReport(
                source.component,
                HealthState.FAILED,
                "reported for wrong component '${report.component}'",
            )
        }
    }
}
