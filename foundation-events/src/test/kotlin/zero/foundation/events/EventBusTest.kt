/*
 * MODULE: foundation-events tests
 * PURPOSE: Verifies listener filtering, cancellation, and failure isolation.
 * TAKES: EventBus subscriptions and sample events.
 * GIVES: Pass/fail results for synchronous in-process delivery.
 * OWNS: No production state.
 * DOES NOT OWN: Event persistence.
 * DEPENDENCIES: foundation-events and Kotlin test/JUnit.
 * CONTRACT: Matching listeners run in subscription order; listener exceptions do not stop delivery.
 * FAILURE BEHAVIOR: Assertion failures identify incorrect bus behavior.
 */
package zero.foundation.events

import kotlin.test.Test
import kotlin.test.assertEquals
import zero.foundation.contract.ModuleId

/** Tests filtering and synchronous delivery semantics of EventBus. */
class EventBusTest {
    private val source = ModuleId("alpha-one")
    private val typeA = EventType("kind.a")
    private val typeB = EventType("kind.b")

    private fun event(type: EventType) =
        ZeroEvent(0L, 0L, 0L, source, type, emptyMap())

    /** A typed subscription receives only events of that type. */
    @Test
    fun typedSubscriptionFiltersByType() {
        val bus = EventBus()
        val seen = mutableListOf<EventType>()
        bus.subscribe(typeA) { seen.add(it.type) }

        bus.publish(event(typeA))
        bus.publish(event(typeB))

        assertEquals(listOf(typeA), seen)
    }

    /** A null-type subscription receives every event. */
    @Test
    fun nullTypeReceivesAll() {
        val bus = EventBus()
        val seen = mutableListOf<EventType>()
        bus.subscribe(null) { seen.add(it.type) }

        bus.publish(event(typeA))
        bus.publish(event(typeB))

        assertEquals(listOf(typeA, typeB), seen)
    }

    /** After cancel, the listener receives nothing, and cancelling twice is harmless. */
    @Test
    fun cancelStopsDelivery() {
        val bus = EventBus()
        var count = 0
        val subscription = bus.subscribe(null) { count++ }

        bus.publish(event(typeA))
        subscription.cancel()
        subscription.cancel()
        bus.publish(event(typeA))

        assertEquals(1, count)
    }

    /** Cancellation by an earlier listener prevents later delivery during the same publish. */
    @Test
    fun cancellationDuringDeliveryTakesEffectImmediately() {
        val bus = EventBus()
        var laterDeliveryCount = 0
        lateinit var later: EventBus.Subscription
        bus.subscribe(null) { later.cancel() }
        later = bus.subscribe(null) { laterDeliveryCount++ }

        bus.publish(event(typeA))

        assertEquals(0, laterDeliveryCount)
    }

    /** A throwing listener is reported, and the next listener still receives the event. */
    @Test
    fun throwingListenerDoesNotStopOthers() {
        val bus = EventBus()
        var delivered = 0
        bus.subscribe(null) { throw IllegalStateException("boom") }
        bus.subscribe(null) { delivered++ }

        val failures = bus.publish(event(typeA))

        assertEquals(1, delivered)
        assertEquals(1, failures.size)
        assertEquals("boom", failures.single().message)
    }
}
