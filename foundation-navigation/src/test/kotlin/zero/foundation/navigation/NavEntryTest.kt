/*
 * MODULE: foundation-navigation tests
 * PURPOSE: Verifies NavEntry validation.
 * TAKES: Valid and invalid entry values.
 * GIVES: Pass/fail results for entry validation.
 * OWNS: No production state.
 * DOES NOT OWN: Registry behavior.
 * DEPENDENCIES: foundation-navigation.
 * CONTRACT: Ids and titles follow the documented rules.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavEntryTest {
    /** A valid entry keeps its title and category. */
    @Test
    fun acceptsValidEntry() {
        val entry = NavEntry("diagnostics", "Diagnostics", NavCategory.SYSTEM)

        assertEquals("Diagnostics", entry.title)
        assertEquals(NavCategory.SYSTEM, entry.category)
    }

    /** Ids with uppercase, underscores, a digit first, or length 1 are rejected. */
    @Test
    fun rejectsInvalidIds() {
        listOf("Bad", "a", "has_underscore", "1abc").forEach { bad ->
            assertFailsWith<IllegalArgumentException>("expected '$bad' to be rejected") {
                NavEntry(bad, "Title", NavCategory.SYSTEM)
            }
        }
    }

    /** A blank title is rejected. */
    @Test
    fun rejectsBlankTitle() {
        assertFailsWith<IllegalArgumentException> {
            NavEntry("valid-id", "   ", NavCategory.SYSTEM)
        }
    }
}
