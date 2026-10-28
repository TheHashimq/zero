/*
 * MODULE: foundation-design tests
 * PURPOSE: Verifies the type scale decreases from display to label.
 * TAKES: TypeScale constants.
 * GIVES: Pass/fail result for ordering.
 * OWNS: No production state.
 * DOES NOT OWN: Font rendering.
 * DEPENDENCIES: foundation-design.
 * CONTRACT: Sizes strictly decrease.
 * FAILURE BEHAVIOR: Assertion failure names the broken order.
 */
package zero.foundation.design

import kotlin.test.Test
import kotlin.test.assertEquals

class TypeScaleTest {
    /** Display, headline, title, body, label are strictly decreasing. */
    @Test
    fun sizesStrictlyDecrease() {
        val sizes = listOf(
            TypeScale.DISPLAY_SP,
            TypeScale.HEADLINE_SP,
            TypeScale.TITLE_SP,
            TypeScale.BODY_SP,
            TypeScale.LABEL_SP,
        )
        assertEquals(sizes.sortedDescending(), sizes)
        assertEquals(sizes.size, sizes.toSet().size)
    }
}
