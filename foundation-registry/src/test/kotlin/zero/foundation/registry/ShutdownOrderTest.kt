/*
 * MODULE: foundation-registry
 * PURPOSE: Verifies reverse shutdown order and shutdown failure handling.
 * TAKES: Fake modules with deterministic shutdown behavior.
 * GIVES: Test results for shutdown ordering, statuses, and failure isolation.
 * OWNS: No production state.
 * DOES NOT OWN: Startup failure behavior or app integration.
 * DEPENDENCIES: foundation-registry, foundation-testing, and Kotlin test/JUnit.
 * CONTRACT: Running modules stop in reverse order and failures do not block others.
 * FAILURE BEHAVIOR: Assertion failures identify a violated shutdown guarantee.
 */
package zero.foundation.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ModuleStatus
import zero.foundation.testing.FakeModule

/** Tests shutdown ordering and recovery from stop callback failures. */
class ShutdownOrderTest {
    /** Stops registered modules in reverse registration order. */
    @Test
    fun stopsInReverseRegistrationOrder() {
        val log = mutableListOf<String>()
        val registry = ModuleRegistry()
        listOf("alpha-one", "beta-two", "gamma-three")
            .map { FakeModule(ModuleId(it), log = log) }
            .forEach(registry::register)
        registry.startAll(object : ModuleContext {})

        registry.stopAll()

        assertEquals(
            listOf("stop:gamma-three", "stop:beta-two", "stop:alpha-one"),
            log.filter { it.startsWith("stop:") },
        )
    }

    /** Reports a stop failure and continues stopping the other modules. */
    @Test
    fun stopFailureIsReportedAndOtherModulesStop() {
        val registry = ModuleRegistry()
        val first = FakeModule(ModuleId("alpha-one"))
        val failure = IllegalStateException("shutdown failed")
        val middle = FakeModule(ModuleId("beta-two"), stopError = failure)
        val last = FakeModule(ModuleId("gamma-three"))
        listOf(first, middle, last).forEach(registry::register)
        registry.startAll(object : ModuleContext {})

        val report = registry.stopAll()

        assertSame(failure, report.failed[middle.id])
        assertEquals(ModuleStatus.FAILED, registry.status(middle.id))
        assertEquals(ModuleStatus.STOPPED, registry.status(first.id))
        assertEquals(ModuleStatus.STOPPED, registry.status(last.id))
        assertEquals(listOf(last.id, first.id), report.stopped)
    }

    /** Marks all successfully stopped modules STOPPED. */
    @Test
    fun successfulStopSetsStoppedStatuses() {
        val registry = ModuleRegistry()
        val modules = listOf("alpha-one", "beta-two", "gamma-three")
            .map { FakeModule(ModuleId(it)) }
        modules.forEach(registry::register)
        registry.startAll(object : ModuleContext {})

        registry.stopAll()

        modules.forEach { assertEquals(ModuleStatus.STOPPED, registry.status(it.id)) }
    }
}
