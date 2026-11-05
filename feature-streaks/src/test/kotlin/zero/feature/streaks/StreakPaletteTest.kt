/*
 * MODULE: feature-streaks tests
 * PURPOSE: Verifies palette size, distinctness, theme contrast, and least-used picking.
 * TAKES: The palette constants and a seeded Random.
 * GIVES: Pass/fail results for the palette.
 * OWNS: No production state.
 * DOES NOT OWN: Rendering.
 * DEPENDENCIES: feature-streaks, foundation-design.
 * CONTRACT: Every palette color is at least 3:1 against Zero's background, which is the WCAG threshold for graphics.
 * FAILURE BEHAVIOR: Assertion failures name the color that fails.
 */
package zero.feature.streaks

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import zero.foundation.design.Contrast
import zero.foundation.design.DesignTokens

class StreakPaletteTest {
    private val graphicMinimum = 3.0

    /** The palette has twelve distinct colors, and SIZE matches the list. */
    @Test
    fun paletteHasTwelveDistinctColors() {
        assertEquals(12, StreakPalette.SIZE)
        assertEquals(StreakPalette.SIZE, StreakPalette.COLORS.size)
        assertEquals(12, StreakPalette.COLORS.toSet().size)
    }

    /** Every palette color is visible on Zero's background. */
    @Test
    fun everyColorIsVisibleOnZeroBackground() {
        StreakPalette.COLORS.forEach { color ->
            assertTrue(
                Contrast.ratio(color, DesignTokens.BACKGROUND) >= graphicMinimum,
                "color ${color.toUInt().toString(16)} is below 3:1 on the background",
            )
        }
    }

    /** A color used by fewer streaks is picked before one used by more. */
    @Test
    fun pickPrefersLeastUsedColor() {
        val used = listOf(0, 0, 1)

        val picked = StreakPalette.pickIndex(used, Random(7))

        assertTrue(picked !in listOf(0, 1), "picked $picked, which is already used")
    }

    /** The same seed always picks the same color. */
    @Test
    fun pickIsDeterministicWithSeed() {
        val first = StreakPalette.pickIndex(emptyList(), Random(42))
        val second = StreakPalette.pickIndex(emptyList(), Random(42))

        assertEquals(first, second)
    }
}
