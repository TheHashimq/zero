/*
 * MODULE: foundation-events tests
 * PURPOSE: Verifies persistent event sequencing, retention, recovery, and write failures.
 * TAKES: PersistentEventLog instances backed by an in-memory namespaced store.
 * GIVES: Pass/fail results for durable append and recent-event behavior.
 * OWNS: No production state.
 * DOES NOT OWN: Platform persistence implementation.
 * DEPENDENCIES: foundation-events, foundation-persistence, foundation-testing.
 * CONTRACT: Events are sequenced, retained within capacity, and survive log recreation.
 * FAILURE BEHAVIOR: Assertion failures identify broken persistence guarantees.
 */
package zero.foundation.events

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.persistence.PersistenceException
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource
import zero.foundation.testing.InMemoryKeyValueBackend

/** Tests the persistent event log with deterministic clock and storage sources. */
class PersistentEventLogTest {
    private val backend = InMemoryKeyValueBackend()
    private val stores = ModuleStoreFactory(backend)
    private val source = ModuleId("alpha-one")
    private val type = EventType("sample.kind")
    private val clock: ZeroClock = DefaultZeroClock(
        FakeMonotonicSource(500L),
        FakeWallClockSource(900L),
        FakeBootIdSource("boot-1"),
    )

    private fun log(capacity: Int = 500) =
        PersistentEventLog(stores.storeFor(ModuleId("events")), clock, capacity)

    /** Sequences start at 0 and increase by one. */
    @Test
    fun sequenceIncreasesFromZero() {
        val log = log()

        assertEquals(0L, log.append(source, type, emptyMap()).sequence)
        assertEquals(1L, log.append(source, type, emptyMap()).sequence)
    }

    /** Recent returns the newest events first and honors the limit. */
    @Test
    fun recentIsNewestFirstAndLimited() {
        val log = log()
        repeat(4) { log.append(source, type, mapOf("n" to it.toString())) }

        val recent = log.recent(2)

        assertEquals(listOf(3L, 2L), recent.map { it.sequence })
        assertEquals("3", recent.first().payload["n"])
    }

    /** Appending past capacity drops the oldest events. */
    @Test
    fun retentionKeepsOnlyNewestCapacity() {
        val log = log(capacity = 3)
        repeat(5) { log.append(source, type, emptyMap()) }

        assertEquals(listOf(4L, 3L, 2L), log.recent(10).map { it.sequence })
    }

    /** A corrupted entry is skipped, and the other events are still returned. */
    @Test
    fun corruptedEntryIsSkipped() {
        val log = log()
        repeat(3) { log.append(source, type, emptyMap()) }
        backend.write("module.events.event.1", "garbage")

        assertEquals(listOf(2L, 0L), log.recent(10).map { it.sequence })
    }

    /** A new log instance over the same store sees earlier events. */
    @Test
    fun eventsSurviveNewLogInstance() {
        log().append(source, type, mapOf("kept" to "yes"))

        val reopened = log().recent(10)

        assertEquals(1, reopened.size)
        assertEquals("yes", reopened.single().payload["kept"])
    }

    /** A failed event write does not advance the sequence counter. */
    @Test
    fun failedWriteLeavesNoGap() {
        val log = log()
        log.append(source, type, emptyMap())
        backend.failWriteForKey = "module.events.event.1"

        assertFailsWith<PersistenceException> { log.append(source, type, emptyMap()) }

        assertEquals(listOf(0L), log.recent(10).map { it.sequence })
        assertEquals(1L, log.append(source, type, emptyMap()).sequence)
    }
}
