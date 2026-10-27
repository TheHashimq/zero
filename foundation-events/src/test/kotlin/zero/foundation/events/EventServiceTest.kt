/*
 * MODULE: foundation-events tests
 * PURPOSE: Verifies event recording precedes listener notification.
 * TAKES: EventService instances with deterministic persistent logs.
 * GIVES: Pass/fail results for record and publish behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Platform storage or listener implementation.
 * DEPENDENCIES: foundation-events, foundation-persistence, foundation-testing.
 * CONTRACT: Recorded events are visible to listeners and persist despite listener exceptions.
 * FAILURE BEHAVIOR: Assertion failures identify broken event service ordering.
 */
package zero.foundation.events

import kotlin.test.Test
import kotlin.test.assertEquals
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource
import zero.foundation.testing.InMemoryKeyValueBackend

/** Tests EventService coordination between the persistent log and in-process bus. */
class EventServiceTest {
    private val source = ModuleId("alpha-one")
    private val type = EventType("sample.kind")

    private fun service(): EventService {
        val clock = DefaultZeroClock(FakeMonotonicSource(), FakeWallClockSource(), FakeBootIdSource())
        val stores = ModuleStoreFactory(InMemoryKeyValueBackend())
        return EventService(PersistentEventLog(stores.storeFor(ModuleId("events")), clock), EventBus())
    }

    /** Listeners run after recording, so they can read the event back from the log. */
    @Test
    fun eventIsRecordedBeforeListenersRun() {
        val service = service()
        var recentCountSeenByListener = -1
        service.subscribe(null) { recentCountSeenByListener = service.recent(10).size }

        service.record(source, type)

        assertEquals(1, recentCountSeenByListener)
    }

    /** A failing listener is reported in the result, and the event remains recorded. */
    @Test
    fun listenerFailureIsReportedAndEventKept() {
        val service = service()
        service.subscribe(null) { throw IllegalStateException("listener broke") }

        val result = service.record(source, type, mapOf("k" to "v"))

        assertEquals(1, result.listenerFailures.size)
        assertEquals("v", service.recent(1).single().payload["k"])
    }
}
