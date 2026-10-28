/*
 * MODULE: foundation-design
 * PURPOSE: WCAG 2.x relative luminance and contrast ratio for ARGB colors.
 * TAKES: Two ARGB integers.
 * GIVES: A contrast ratio from 1.0 to 21.0, and the AA threshold for normal text.
 * OWNS: No state.
 * DOES NOT OWN: Choosing colors. The design tokens do that.
 * DEPENDENCIES: None.
 * CONTRACT: The ratio is symmetric in its arguments. Alpha is ignored.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.foundation.design

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG contrast calculations. */
object Contrast {
    /** Minimum contrast for normal text under WCAG AA. */
    const val AA_TEXT: Double = 4.5

    /** Relative luminance of an ARGB color, from 0.0 to 1.0. */
    fun luminance(argb: Int): Double {
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** Contrast ratio between two ARGB colors. */
    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
}
