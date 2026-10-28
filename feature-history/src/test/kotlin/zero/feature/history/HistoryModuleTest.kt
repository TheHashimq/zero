/*
 * MODULE: feature-history tests
 * PURPOSE: Verifies the History entry registers under Activity and that startup rejects a bad context.
 * TAKES: A real registry and ZeroServices built from fakes.
 * GIVES: Pass/fail results for module startup.
 * OWNS: No production state.
 * DOES NOT OWN: Screen behavior.
 * DEPENDENCIES: feature-history, foundation-registry, foundation-testing.
 * CONTRACT: Successful start registers exactly one entry; a bad context fails the module.
 * FAILURE BEHAVIOR: Assertion failures identify the broken startup path.
 */
package zero.feature.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ModuleStatus
import zero.foundation.events.EventBus
import zero.foundation.events.EventService
import zero.foundation.events.PersistentEventLog
import zero.foundation.health.HealthService
import zero.foundation.navigation.NavCategory
import zero.foundation.navigation.NavigationRegistry
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.registry.ModuleRegistry
import zero.foundation.runtime.ZeroServices
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource
import zero.foundation.testing.InMemoryKeyValueBackend

class HistoryModuleTest {
    private fun services(navigation: NavigationRegistry): ZeroServices {
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())
        val clock = DefaultZeroClock(FakeMonotonicSource(), FakeWallClockSource(), FakeBootIdSource("boot-1"))
        val events = EventService(PersistentEventLog(stores.storeFor(ModuleId("events")), clock), EventBus())
        return ZeroServices(clock, stores, events, HealthService(emptyList()), navigation)
    }

    /** A successful start registers one History entry under Activity. */
    @Test
    fun startRegistersHistoryUnderActivity() {
        val navigation = NavigationRegistry()
        val registry = ModuleRegistry()
        registry.register(HistoryModule())

        registry.startAll(services(navigation))

        assertEquals(ModuleStatus.RUNNING, registry.status(ModuleId("history")))
        assertEquals(listOf("history"), navigation.grouped().getValue(NavCategory.ACTIVITY).map { it.id })
    }

    /** Before startup, recent() is empty rather than throwing. */
    @Test
    fun recentIsEmptyBeforeStart() {
        assertTrue(HistoryModule().recent(10).isEmpty())
    }

    /** A context that is not ZeroServices fails the module and registers nothing. */
    @Test
    fun startWithoutZeroServicesFailsTheModule() {
        val registry = ModuleRegistry()
        registry.register(HistoryModule())

        val report = registry.startAll(object : ModuleContext {})

        assertTrue(report.failed.containsKey(ModuleId("history")))
        assertEquals(ModuleStatus.FAILED, registry.status(ModuleId("history")))
    }
}
