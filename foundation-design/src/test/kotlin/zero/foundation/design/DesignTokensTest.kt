/*
 * MODULE: foundation-design tests
 * PURPOSE: Verifies the palette matches the brief and status colors are distinct.
 * TAKES: The DesignTokens object.
 * GIVES: Pass/fail results for palette values and opacity.
 * OWNS: No production state.
 * DOES NOT OWN: Compose rendering.
 * DEPENDENCIES: foundation-design.
 * CONTRACT: Palette values match the brief; every token is opaque.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong token.
 */
package zero.foundation.design

import kotlin.test.Test
import kotlin.test.assertEquals

class DesignTokensTest {
    /** Key tokens equal the values in the design brief. */
    @Test
    fun matchesBriefPalette() {
        assertEquals(0xFF080A0D.toInt(), DesignTokens.BACKGROUND)
        assertEquals(0xFFFF6B00.toInt(), DesignTokens.ACCENT)
        assertEquals(0xFFE05252.toInt(), DesignTokens.CRITICAL)
    }

    /** Every token has a fully opaque alpha channel. */
    @Test
    fun everyTokenIsFullyOpaque() {
        DesignTokens.all.forEach { color ->
            assertEquals(0xFF, (color ushr 24) and 0xFF)
        }
    }

    /** The three status roles map to three different colors. */
    @Test
    fun statusRolesHaveDistinctColors() {
        val colors = StatusRole.entries.map { DesignTokens.colorFor(it) }

        assertEquals(3, colors.toSet().size)
    }
}
