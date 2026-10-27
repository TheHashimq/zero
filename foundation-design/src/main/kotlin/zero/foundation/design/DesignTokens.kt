/*
 * MODULE: foundation-design
 * PURPOSE: Single source of the Zero color palette, as ARGB integers.
 * TAKES: Nothing; constants.
 * GIVES: Background, surface, accent, text, border, and status colors.
 * OWNS: The palette values.
 * DOES NOT OWN: Compose types or theme wiring. The app maps these to Compose colors.
 * DEPENDENCIES: StatusRole.
 * CONTRACT: Every token is fully opaque and matches the design brief palette.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.foundation.design

/** Zero color palette. */
object DesignTokens {
    val BACKGROUND: Int = 0xFF080A0D.toInt()
    val SURFACE: Int = 0xFF11151A.toInt()
    val ELEVATED: Int = 0xFF181D23.toInt()
    val ACCENT: Int = 0xFFFF6B00.toInt()
    val ACCENT_BRIGHT: Int = 0xFFFF8A3D.toInt()
    val TEXT_PRIMARY: Int = 0xFFF2F4F5.toInt()
    val TEXT_SECONDARY: Int = 0xFF9AA3AD.toInt()
    val BORDER: Int = 0xFF252B32.toInt()
    val HEALTHY: Int = 0xFF45C486.toInt()
    val WARNING: Int = 0xFFFFB020.toInt()
    val CRITICAL: Int = 0xFFE05252.toInt()

    /** Every token, for audits and tests. */
    val all: List<Int>
        get() = listOf(
            BACKGROUND, SURFACE, ELEVATED, ACCENT, ACCENT_BRIGHT,
            TEXT_PRIMARY, TEXT_SECONDARY, BORDER, HEALTHY, WARNING, CRITICAL,
        )

    /** Returns the token color for a status role. */
    fun colorFor(role: StatusRole): Int = when (role) {
        StatusRole.HEALTHY -> HEALTHY
        StatusRole.WARNING -> WARNING
        StatusRole.CRITICAL -> CRITICAL
    }
}
