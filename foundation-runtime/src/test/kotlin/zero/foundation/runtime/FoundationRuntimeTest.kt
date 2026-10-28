/*
 * MODULE: foundation-runtime tests
 * PURPOSE: Verifies that ZeroServices hands the same instances to modules.
 * TAKES: Fake clock sources, an in-memory backend, and a registry.
 * GIVES: Pass/fail results for service identity and module access.
 * OWNS: No production state.
 * DOES NOT OWN: Platform adapters or the app composition root.
 * DEPENDENCIES: foundation-runtime, foundation-registry, foundation-testing.
 * CONTRACT: Services received by modules are the instances supplied at construction.
 * FAILURE BEHAVIOR: Assertion failures identify a broken service handoff.
 */
package zero.foundation.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ZeroModule
import zero.foundation.events.EventBus
import zero.foundation.events.EventService
import zero.foundation.events.PersistentEventLog
import zero.foundation.health.HealthService
import zero.foundation.navigation.NavigationRegistry
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.persistence.StoreKey
import zero.foundation.registry.ModuleRegistry
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource
import zero.foundation.testing.InMemoryKeyValueBackend

class FoundationRuntimeTest {
    private fun clock(): ZeroClock = DefaultZeroClock(
        FakeMonotonicSource(),
        FakeWallClockSource(),
        FakeBootIdSource(),
    )

    private fun events(stores: ModuleStoreFactory, clock: ZeroClock): EventService =
        EventService(PersistentEventLog(stores.storeFor(ModuleId("events")), clock), EventBus())

    private fun services(
        clock: ZeroClock = clock(),
        stores: ModuleStoreFactory = ModuleStoreFactory(InMemoryKeyValueBackend()),
    ): ZeroServices = ZeroServices(
        clock,
        stores,
        events(stores, clock),
        HealthService(emptyList()),
        NavigationRegistry(),
    )

    /** Exposes the exact clock instance passed to the constructor. */
    @Test
    fun exposesTheClockItWasGiven() {
        val clock = clock()

        assertSame(clock, services(clock = clock).clock)
    }

    /** Exposes the exact store factory instance passed to the constructor. */
    @Test
    fun exposesTheStoreFactoryItWasGiven() {
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())

        assertSame(stores, services(stores = stores).stores)
    }

    /** Exposes the exact event service instance passed to the constructor. */
    @Test
    fun exposesTheEventServiceItWasGiven() {
        val clock = clock()
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())
        val eventService = events(stores, clock)

        val services = ZeroServices(clock, stores, eventService, HealthService(emptyList()), NavigationRegistry())

        assertSame(eventService, services.events)
    }

    /** Exposes the exact health service instance passed to the constructor. */
    @Test
    fun exposesTheHealthServiceItWasGiven() {
        val clock = clock()
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())
        val health = HealthService(emptyList())

        val services = ZeroServices(clock, stores, events(stores, clock), health, NavigationRegistry())

        assertSame(health, services.health)
    }

    /** Exposes the exact navigation registry instance passed to the constructor. */
    @Test
    fun exposesTheNavigationRegistryItWasGiven() {
        val clock = clock()
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())
        val navigation = NavigationRegistry()

        val services = ZeroServices(clock, stores, events(stores, clock), HealthService(emptyList()), navigation)

        assertSame(navigation, services.navigation)
    }

    /** A module started by the registry receives the same services and can persist a value. */
    @Test
    fun startedModuleReceivesServicesAndCanPersist() {
        val backend = InMemoryKeyValueBackend()
        val stores = ModuleStoreFactory(backend)
        val services = services(stores = stores)
        val module = WritingModule(stores)
        val registry = ModuleRegistry()
        registry.register(module)

        registry.startAll(services)

        assertSame(services, module.received)
        assertEquals(setOf("module.writer-one.ran"), backend.keys)
    }

    /** Test module that records its context and writes one value into its own store. */
    private class WritingModule(private val stores: ModuleStoreFactory) : ZeroModule {
        override val id = ModuleId("writer-one")
        override val displayName = "Writer"
        var received: ModuleContext? = null
            private set

        override fun onStart(context: ModuleContext) {
            received = context
            stores.storeFor(id).putString(StoreKey("ran"), "yes")
        }

        override fun onStop() {}
    }
}
