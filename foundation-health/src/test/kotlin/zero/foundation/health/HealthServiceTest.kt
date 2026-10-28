/*
 * MODULE: foundation-health tests
 * PURPOSE: Verifies aggregation, failure isolation, and component validation.
 * TAKES: Stub health sources.
 * GIVES: Pass/fail results for HealthService.
 * OWNS: No production state.
 * DOES NOT OWN: Individual checks.
 * DEPENDENCIES: foundation-health.
 * CONTRACT: report() never throws and reports every source.
 * FAILURE BEHAVIOR: Assertion failures identify the broken aggregation rule.
 */
package zero.foundation.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Tests health aggregation and source validation. */
class HealthServiceTest {
    private class StubSource(
        override val component: String,
        private val result: () -> HealthReport,
    ) : HealthSource {
        override fun check(): HealthReport = result()
    }

    private fun ok(name: String) = StubSource(name) { HealthReport(name, HealthState.OK, "fine") }

    private fun reporting(name: String, state: HealthState) =
        StubSource(name) { HealthReport(name, state, "stub") }

    /** No sources means nothing is wrong. */
    @Test
    fun emptySystemIsOk() {
        assertEquals(HealthState.OK, HealthService(emptyList()).report().overall)
    }

    /** The overall state is the most severe component state. */
    @Test
    fun overallIsWorstComponent() {
        val degraded = HealthService(listOf(ok("a"), reporting("b", HealthState.DEGRADED), ok("c")))
        val failed = HealthService(listOf(reporting("b", HealthState.DEGRADED), reporting("c", HealthState.FAILED)))

        assertEquals(HealthState.DEGRADED, degraded.report().overall)
        assertEquals(HealthState.FAILED, failed.report().overall)
    }

    /** A source that throws becomes FAILED, and other sources still report. */
    @Test
    fun throwingSourceIsFailedAndOthersStillReport() {
        val health = HealthService(
            listOf(StubSource("bad") { throw IllegalStateException("boom") }, ok("good")),
        )

        val system = health.report()

        assertEquals(HealthState.FAILED, system.overall)
        assertEquals(listOf("bad", "good"), system.components.map { it.component })
        assertEquals(HealthState.FAILED, system.components[0].state)
        assertEquals("check threw IllegalStateException", system.components[0].detail)
        assertEquals(HealthState.OK, system.components[1].state)
    }

    /** A report naming a different component is treated as FAILED for the source's name. */
    @Test
    fun wrongComponentNameIsFailed() {
        val health = HealthService(
            listOf(StubSource("expected") { HealthReport("other", HealthState.OK, "x") }),
        )

        val report = health.report().components.single()

        assertEquals("expected", report.component)
        assertEquals(HealthState.FAILED, report.state)
    }

    /** Two sources with the same component name are rejected at construction. */
    @Test
    fun duplicateComponentNamesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            HealthService(listOf(ok("dup"), ok("dup")))
        }
    }
}
