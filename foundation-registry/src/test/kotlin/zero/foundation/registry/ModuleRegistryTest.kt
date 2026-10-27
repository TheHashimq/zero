/*
 * MODULE: foundation-registry
 * PURPOSE: Verifies module registration and successful lifecycle behavior.
 * TAKES: Registry operations and deterministic fake modules.
 * GIVES: Test results for registration, startup, and basic shutdown behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Failure handling and reverse-order-specific tests.
 * DEPENDENCIES: foundation-registry, foundation-testing, and Kotlin test/JUnit.
 * CONTRACT: Exercises the required normal-path registry behavior.
 * FAILURE BEHAVIOR: Assertion failures identify a violated registry guarantee.
 */
package zero.foundation.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ModuleStatus
import zero.foundation.testing.FakeModule
import zero.foundation.testing.ModuleContractAssertions

/** Tests registration and normal module lifecycle behavior. */
class ModuleRegistryTest {
    /** Starts with every newly registered module in REGISTERED status. */
    @Test
    fun registerSetsRegisteredStatus() {
        val registry = ModuleRegistry()
        val module = FakeModule(ModuleId("settings"))

        registry.register(module)

        assertEquals(ModuleStatus.REGISTERED, registry.status(module.id))
    }

    /** Returns registered identifiers in registration order. */
    @Test
    fun registeredIdsPreserveRegistrationOrder() {
        val registry = ModuleRegistry()
        val ids = listOf("alpha-one", "beta-two", "gamma-three").map(::ModuleId)
        ids.forEach { registry.register(FakeModule(it)) }

        assertEquals(ids, registry.registeredIds())
    }

    /** Rejects duplicate ids without replacing the original module. */
    @Test
    fun duplicateRegistrationKeepsFirstModule() {
        val registry = ModuleRegistry()
        val id = ModuleId("settings")
        val first = FakeModule(id, displayName = "First")
        registry.register(first)

        val error = assertFailsWith<DuplicateModuleException> {
            registry.register(FakeModule(id, displayName = "Second"))
        }

        assertEquals(id, error.id)
        assertEquals(listOf(id), registry.registeredIds())
        assertEquals(ModuleStatus.REGISTERED, registry.status(id))
        assertEquals(emptyList(), first.calls)
    }

    /** Starts every module in registration order. */
    @Test
    fun startAllUsesRegistrationOrder() {
        val log = mutableListOf<String>()
        val registry = ModuleRegistry()
        listOf("alpha-one", "beta-two", "gamma-three")
            .map { FakeModule(ModuleId(it), log = log) }
            .forEach(registry::register)

        registry.startAll(object : ModuleContext {})

        assertEquals(
            listOf("start:alpha-one", "start:beta-two", "start:gamma-three"),
            log,
        )
    }

    /** Passes the same context instance to every module. */
    @Test
    fun startAllPassesSameContextInstance() {
        val registry = ModuleRegistry()
        val modules = listOf("alpha-one", "beta-two", "gamma-three")
            .map { FakeModule(ModuleId(it)) }
        modules.forEach(registry::register)
        val context = object : ModuleContext {}

        registry.startAll(context)

        modules.forEach { assertSame(context, it.receivedContext) }
    }

    /** Marks each successfully started module RUNNING. */
    @Test
    fun startAllSetsRunningStatuses() {
        val registry = ModuleRegistry()
        val modules = listOf("alpha-one", "beta-two", "gamma-three")
            .map { FakeModule(ModuleId(it)) }
        modules.forEach(registry::register)

        registry.startAll(object : ModuleContext {})

        modules.forEach { assertEquals(ModuleStatus.RUNNING, registry.status(it.id)) }
    }

    /** Rejects a second startAll call. */
    @Test
    fun startAllCanOnlyBeCalledOnce() {
        val registry = ModuleRegistry()
        registry.startAll(object : ModuleContext {})

        assertFailsWith<IllegalStateException> {
            registry.startAll(object : ModuleContext {})
        }
    }

    /** Rejects registration after startup has begun. */
    @Test
    fun registerAfterStartAllFails() {
        val registry = ModuleRegistry()
        registry.startAll(object : ModuleContext {})

        assertFailsWith<IllegalStateException> {
            registry.register(FakeModule(ModuleId("settings")))
        }
    }

    /** Returns an empty report when stopped before startup. */
    @Test
    fun stopBeforeStartReturnsEmptyReport() {
        val registry = ModuleRegistry()
        registry.register(FakeModule(ModuleId("settings")))

        assertEquals(StopReport(emptyList(), emptyMap()), registry.stopAll())
    }

    /** Stops every running module. */
    @Test
    fun stopAllStopsEveryRunningModule() {
        val log = mutableListOf<String>()
        val registry = ModuleRegistry()
        val modules = listOf("alpha-one", "beta-two", "gamma-three")
            .map { FakeModule(ModuleId(it), log = log) }
        modules.forEach(registry::register)
        registry.startAll(object : ModuleContext {})

        val report = registry.stopAll()

        assertEquals(3, report.stopped.size)
        assertEquals(emptyMap(), report.failed)
        assertEquals(3, log.count { it.startsWith("stop:") })
        modules.forEach { assertEquals(ModuleStatus.STOPPED, registry.status(it.id)) }
    }

    /** Every FakeModule used by the registry tests satisfies the module contract. */
    @Test
    fun fakeModulesSatisfyModuleContract() {
        val module = FakeModule(ModuleId("alpha-one"), displayName = "Alpha")

        ModuleContractAssertions.assertValid(module)
    }

    /** A module with a blank display name fails the contract check. */
    @Test
    fun blankDisplayNameFailsContract() {
        val module = FakeModule(ModuleId("alpha-one"), displayName = " ")

        assertFailsWith<AssertionError> {
            ModuleContractAssertions.assertValid(module)
        }
    }
}
