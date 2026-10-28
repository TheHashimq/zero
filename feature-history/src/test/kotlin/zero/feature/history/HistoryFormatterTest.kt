/*
 * MODULE: feature-history tests
 * PURPOSE: Verifies labels, grouping, ordering, date math, and time formatting.
 * TAKES: Fixed events with known wall-clock times.
 * GIVES: Pass/fail results for the formatter.
 * OWNS: No production state.
 * DOES NOT OWN: Reading the event log.
 * DEPENDENCIES: feature-history, foundation-events.
 * CONTRACT: Each test asserts one formatting rule.
 * FAILURE BEHAVIOR: Assertion failures show the wrong text.
 */
package zero.feature.history

import kotlin.test.Test
import kotlin.test.assertEquals
import zero.foundation.contract.ModuleId
import zero.foundation.events.EventType
import zero.foundation.events.ZeroEvent

class HistoryFormatterTest {
    private val source = ModuleId("protection")
    private val hour = 3_600_000L
    private val day = 86_400_000L

    private fun event(seq: Long, wall: Long, type: String, payload: Map<String, String> = emptyMap()) =
        ZeroEvent(seq, 0L, wall, source, EventType(type), payload)

    /** Known event types have readable titles. */
    @Test
    fun knownTypesHaveTitles() {
        assertEquals("Commitment started", HistoryFormatter.titleFor("protection.started"))
        assertEquals("Blocked app opened", HistoryFormatter.titleFor("protection.blocked-open"))
    }

    /** Unknown types are shown under their raw name, never dropped. */
    @Test
    fun unknownTypeShowsRawName() {
        assertEquals("protection.future-thing", HistoryFormatter.titleFor("protection.future-thing"))
    }

    /** Input order is kept inside a day, so newest-first input stays newest-first. */
    @Test
    fun rowsKeepInputOrderWithinDay() {
        val events = listOf(
            event(2, 2 * hour, "protection.armed"),
            event(1, 1 * hour, "protection.started"),
        )

        val rows = HistoryFormatter.days(events, 0L).single().rows

        assertEquals(listOf("Protection armed", "Commitment started"), rows.map { it.title })
    }

    /** Events on different local dates form separate groups, newest day first. */
    @Test
    fun groupsByLocalDateNewestFirst() {
        val events = listOf(
            event(2, day + hour, "protection.armed"),
            event(1, hour, "protection.started"),
        )

        val days = HistoryFormatter.days(events, 0L)

        assertEquals(listOf("1970-01-02", "1970-01-01"), days.map { it.dateText })
    }

    /** The local offset moves an event across midnight into the previous day. */
    @Test
    fun localOffsetMovesEventAcrossMidnight() {
        val events = listOf(event(1, day, "protection.started"))

        assertEquals("1970-01-01", HistoryFormatter.days(events, -hour).single().dateText)
        assertEquals("1970-01-02", HistoryFormatter.days(events, 0L).single().dateText)
    }

    /** A blocked-open row carries the package name as its detail. */
    @Test
    fun blockedOpenRowShowsPackageDetail() {
        val events = listOf(event(1, hour, "protection.blocked-open", mapOf("package" to "com.android.camera2")))

        val row = HistoryFormatter.days(events, 0L).single().rows.single()

        assertEquals("com.android.camera2", row.detail)
    }

    /** Date math is correct for a known leap-year date, 2000-03-01. */
    @Test
    fun dateMathMatchesKnownDate() {
        assertEquals("2000-02-29", HistoryFormatter.formatDate(11_016L))
        assertEquals("2000-03-01", HistoryFormatter.formatDate(11_017L))
    }
}
