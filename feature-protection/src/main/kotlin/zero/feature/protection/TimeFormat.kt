/*
 * MODULE: feature-protection
 * PURPOSE: Formats remaining commitment time for display.
 * TAKES: A remaining duration in milliseconds.
 * GIVES: "mm:ss" under one hour, "h:mm:ss" from one hour up.
 * OWNS: No state.
 * DOES NOT OWN: Deciding how much time remains. The state machine does that.
 * DEPENDENCIES: None.
 * CONTRACT: Negative values display as 00:00. Output uses ASCII digits in every locale.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.feature.protection

import java.util.Locale

/** Display formatting for remaining time. */
object TimeFormat {
    /** Formats [millis] as remaining time, rounding down to whole seconds. */
    fun remaining(millis: Long): String {
        val total = maxOf(0L, millis) / 1000L
        val hours = total / 3600L
        val minutes = (total % 3600L) / 60L
        val seconds = total % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}
