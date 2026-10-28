/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies module startup through the real registry.
 * TAKES: A registry, real ZeroServices built from fakes, and a bad context.
 * GIVES: Pass/fail results for module startup.
 * OWNS: No production state.
 * DOES NOT OWN: Accessibility or screen behavior.
 * DEPENDENCIES: feature-protection-runtime, foundation-registry, foundation-testing.
 * CONTRACT: A valid context exposes coordinator and repository and registers both entries; an invalid context fails the module.
 * FAILURE BEHAVIOR: Assertion failures identify a broken startup path.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

class ProtectionModuleTest {
    private fun services(navigation: NavigationRegistry): ZeroServices {
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())
        val clock = DefaultZeroClock(FakeMonotonicSource(), FakeWallClockSource(), FakeBootIdSource("boot-1"))
        val events = EventService(PersistentEventLog(stores.storeFor(ModuleId("events")), clock), EventBus())
        return ZeroServices(clock, stores, events, HealthService(emptyList()), navigation)
    }

    /** A valid start exposes the coordinator and repository and registers both entries. */
    @Test
    fun startRegistersNavigationAndExposesCoordinatorAndRepository() {
        val navigation = NavigationRegistry()
        val module = ProtectionModule("com.TheHashimq.zero")
        val registry = ModuleRegistry()
        registry.register(module)

        registry.startAll(services(navigation))

        assertEquals(ModuleStatus.RUNNING, registry.status(ModuleId("protection")))
        assertNotNull(module.coordinator)
        assertNotNull(module.repository)
        assertEquals(
            listOf("protection", "protected-apps"),
            navigation.grouped().getValue(NavCategory.PROTECTION).map { it.id },
        )
    }

    /** A context that is not ZeroServices fails the module and leaves no coordinator or repository. */
    @Test
    fun startWithoutZeroServicesFailsTheModule() {
        val module = ProtectionModule("com.TheHashimq.zero")
        val registry = ModuleRegistry()
        registry.register(module)

        val report = registry.startAll(object : ModuleContext {})

        assertTrue(report.failed.containsKey(ModuleId("protection")))
        assertEquals(ModuleStatus.FAILED, registry.status(ModuleId("protection")))
        assertNull(module.coordinator)
        assertNull(module.repository)
    }
}
