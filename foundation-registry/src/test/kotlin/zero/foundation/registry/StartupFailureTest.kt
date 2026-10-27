/*
 * MODULE: foundation-registry
 * PURPOSE: Verifies startup failure isolation and reporting.
 * TAKES: Fake modules with deterministic startup failures.
 * GIVES: Test results for startup continuation and cleanup behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Successful registration or shutdown-order tests.
 * DEPENDENCIES: foundation-registry, foundation-testing, and Kotlin test/JUnit.
 * CONTRACT: A startup failure marks only its module FAILED and is reported.
 * FAILURE BEHAVIOR: Assertion failures identify incorrect failure isolation.
 */
package zero.foundation.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ModuleStatus
import zero.foundation.testing.FakeModule

/** Tests startup error reporting and continued lifecycle processing. */
class StartupFailureTest {
    /** Marks a module failed and reports the original exception instance. */
    @Test
    fun startFailureIsMarkedAndReported() {
        val registry = ModuleRegistry()
        val failure = IllegalStateException("startup failed")
        val module = FakeModule(ModuleId("beta-two"), startError = failure)
        registry.register(module)

        val report = registry.startAll(object : ModuleContext {})

        assertEquals(ModuleStatus.FAILED, registry.status(module.id))
        assertSame(failure, report.failed[module.id])
    }

    /** Starts the modules before and after the module that fails. */
    @Test
    fun otherModulesStillStartAfterFailure() {
        val registry = ModuleRegistry()
        val first = FakeModule(ModuleId("alpha-one"))
        val failed = FakeModule(ModuleId("beta-two"), startError = IllegalStateException())
        val last = FakeModule(ModuleId("gamma-three"))
        listOf(first, failed, last).forEach(registry::register)

        val report = registry.startAll(object : ModuleContext {})

        assertEquals(listOf(first.id, last.id), report.started)
        assertEquals(ModuleStatus.RUNNING, registry.status(first.id))
        assertEquals(ModuleStatus.FAILED, registry.status(failed.id))
        assertEquals(ModuleStatus.RUNNING, registry.status(last.id))
    }

    /** Does not call onStop for a module that failed during startup. */
    @Test
    fun failedStartIsNotStopped() {
        val log = mutableListOf<String>()
        val registry = ModuleRegistry()
        val module = FakeModule(
            ModuleId("beta-two"),
            startError = IllegalStateException(),
            log = log,
        )
        registry.register(module)
        registry.startAll(object : ModuleContext {})

        registry.stopAll()

        assertEquals(listOf("start:beta-two"), log)
    }

    /** Stops running modules even when another module failed during startup. */
    @Test
    fun runningModulesStopAfterStartupFailure() {
        val log = mutableListOf<String>()
        val registry = ModuleRegistry()
        val first = FakeModule(ModuleId("alpha-one"), log = log)
        val failed = FakeModule(
            ModuleId("beta-two"),
            startError = IllegalStateException(),
            log = log,
        )
        val last = FakeModule(ModuleId("gamma-three"), log = log)
        listOf(first, failed, last).forEach(registry::register)
        registry.startAll(object : ModuleContext {})

        val report = registry.stopAll()

        assertEquals(listOf(last.id, first.id), report.stopped)
        assertEquals(emptyMap(), report.failed)
        assertEquals(listOf("start:alpha-one", "start:beta-two", "start:gamma-three", "stop:gamma-three", "stop:alpha-one"), log)
    }
}
