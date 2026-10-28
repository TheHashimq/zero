/*
 * MODULE: feature-history
 * PURPOSE: Turns recorded events into labeled rows grouped by local day.
 * TAKES: A list of ZeroEvent (newest first) and the local offset in milliseconds.
 * GIVES: HistoryDay groups, newest day first, each with rows in the same order as the input.
 * OWNS: No state.
 * DOES NOT OWN: Reading the log, or any protection decision. Wall-clock times are display only.
 * DEPENDENCIES: foundation-events.
 * CONTRACT: Output contains exactly the input events, in input order. Dates use integer math, not java.time.
 *           Unknown event types are shown under their raw name, never dropped.
 * FAILURE BEHAVIOR: Never throws. Empty input gives an empty list.
 */
package zero.feature.history

import zero.foundation.events.ZeroEvent

/** One displayed event. */
data class HistoryRow(val timeText: String, val title: String, val detail: String?)

/** Rows that fall on the same local date. */
data class HistoryDay(val dateText: String, val rows: List<HistoryRow>)

/** Pure formatting for the history list. */
object HistoryFormatter {
    private const val DAY_MILLIS = 86_400_000L

    /** Groups [events] by local date, keeping input order inside each day. */
    fun days(events: List<ZeroEvent>, offsetMillis: Long): List<HistoryDay> {
        val grouped = LinkedHashMap<String, MutableList<HistoryRow>>()
        for (event in events) {
            val local = event.wallMillis + offsetMillis
            val dayNumber = Math.floorDiv(local, DAY_MILLIS)
            val date = formatDate(dayNumber)
            grouped.getOrPut(date) { mutableListOf() }.add(toRow(event, local))
        }
        return grouped.map { (date, rows) -> HistoryDay(date, rows) }
    }

    /** Human-readable label for a known event type, or the raw type name. */
    fun titleFor(type: String): String = when (type) {
        "protection.started" -> "Commitment started"
        "protection.grace-started" -> "Grace started"
        "protection.armed" -> "Protection armed"
        "protection.blocked-open" -> "Blocked app opened"
        "protection.reboot-restart" -> "Restarted after reboot"
        "protection.state-corrupt" -> "Saved state was unreadable"
        else -> type
    }

    private fun toRow(event: ZeroEvent, local: Long): HistoryRow {
        val type = event.type.value
        val detail = if (type == "protection.blocked-open") event.payload["package"] else null
        return HistoryRow(formatTime(local), titleFor(type), detail)
    }

    private fun formatTime(local: Long): String {
        val msInDay = Math.floorMod(local, DAY_MILLIS)
        val minutes = (msInDay / 60_000L) % 60L
        val hours = msInDay / 3_600_000L
        return "%02d:%02d".format(java.util.Locale.ROOT, hours, minutes)
    }

    /** Converts days since 1970-01-01 to yyyy-MM-dd using the proleptic Gregorian calendar. */
    internal fun formatDate(daysSinceEpoch: Long): String {
        val z = daysSinceEpoch + 719_468L
        val era = Math.floorDiv(z, 146_097L)
        val doe = z - era * 146_097L
        val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val day = doy - (153L * mp + 2L) / 5L + 1L
        val month = if (mp < 10L) mp + 3L else mp - 9L
        val year = yoe + era * 400L + if (month <= 2L) 1L else 0L
        return "%04d-%02d-%02d".format(java.util.Locale.ROOT, year, month, day)
    }
}
