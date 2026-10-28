/*
 * MODULE: foundation-design tests
 * PURPOSE: Verifies contrast math and that every text and status color meets WCAG AA on the background.
 * TAKES: Known color pairs and the design tokens.
 * GIVES: Pass/fail results for contrast.
 * OWNS: No production state.
 * DOES NOT OWN: Rendering.
 * DEPENDENCIES: foundation-design.
 * CONTRACT: Black on white is 21:1; all tokens used for text or status meet 4.5:1 on the background.
 * FAILURE BEHAVIOR: Assertion failures name the failing color.
 */
package zero.foundation.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContrastTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    /** Black on white is the maximum contrast, 21:1. */
    @Test
    fun blackOnWhiteIsTwentyOne() {
        assertEquals(21.0, Contrast.ratio(black, white), 0.01)
    }

    /** Primary text meets AA on the background. */
    @Test
    fun primaryTextMeetsAaOnBackground() {
        assertTrue(Contrast.ratio(DesignTokens.TEXT_PRIMARY, DesignTokens.BACKGROUND) >= Contrast.AA_TEXT)
    }

    /** Secondary text meets AA on the background. */
    @Test
    fun secondaryTextMeetsAaOnBackground() {
        assertTrue(Contrast.ratio(DesignTokens.TEXT_SECONDARY, DesignTokens.BACKGROUND) >= Contrast.AA_TEXT)
    }

    /** The accent and all status colors meet AA on the background. */
    @Test
    fun accentAndStatusColorsMeetAaOnBackground() {
        listOf(DesignTokens.ACCENT, DesignTokens.HEALTHY, DesignTokens.WARNING, DesignTokens.CRITICAL)
            .forEach { color ->
                assertTrue(
                    Contrast.ratio(color, DesignTokens.BACKGROUND) >= Contrast.AA_TEXT,
                    "color ${color.toUInt().toString(16)} fails AA on background",
                )
            }
    }
}
