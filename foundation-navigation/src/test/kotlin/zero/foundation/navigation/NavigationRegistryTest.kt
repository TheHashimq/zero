/*
 * MODULE: foundation-navigation tests
 * PURPOSE: Verifies grouping order, duplicate rejection, and freeze behavior.
 * TAKES: Entries registered in several orders.
 * GIVES: Pass/fail results for the registry.
 * OWNS: No production state.
 * DOES NOT OWN: Rendering.
 * DEPENDENCIES: foundation-navigation.
 * CONTRACT: grouped() order is fixed; freeze stops registration.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationRegistryTest {
    private fun entry(id: String, category: NavCategory, order: Int = 0) =
        NavEntry(id, id.uppercase(), category, order)

    /** Categories follow enum order; entries follow order, then id. */
    @Test
    fun groupedFollowsCategoryThenOrderThenId() {
        val nav = NavigationRegistry()
        nav.register(entry("zeta-one", NavCategory.SYSTEM, order = 1))
        nav.register(entry("alpha-two", NavCategory.SYSTEM, order = 1))
        nav.register(entry("beta-three", NavCategory.SYSTEM, order = 0))
        nav.register(entry("gamma-four", NavCategory.PROTECTION))

        val grouped = nav.grouped()

        assertEquals(listOf(NavCategory.PROTECTION, NavCategory.SYSTEM), grouped.keys.toList())
        assertEquals(
            listOf("beta-three", "alpha-two", "zeta-one"),
            grouped.getValue(NavCategory.SYSTEM).map { it.id },
        )
    }

    /** Categories with no entries do not appear in grouped(). */
    @Test
    fun emptyCategoriesAreOmitted() {
        val nav = NavigationRegistry()
        nav.register(entry("only-one", NavCategory.PERSONAL))

        assertEquals(listOf(NavCategory.PERSONAL), nav.grouped().keys.toList())
    }

    /** A second entry with the same id is rejected, and the first remains. */
    @Test
    fun duplicateIdIsRejectedAndFirstKept() {
        val nav = NavigationRegistry()
        nav.register(entry("alpha-one", NavCategory.SYSTEM))

        assertFailsWith<IllegalArgumentException> {
            nav.register(NavEntry("alpha-one", "Other", NavCategory.ACTIVITY))
        }

        assertEquals("ALPHA-ONE", nav.grouped().getValue(NavCategory.SYSTEM).single().title)
    }

    /** Registration after freeze() throws IllegalStateException. */
    @Test
    fun registrationAfterFreezeIsRejected() {
        val nav = NavigationRegistry()
        nav.freeze()

        assertFailsWith<IllegalStateException> {
            nav.register(entry("late-one", NavCategory.SYSTEM))
        }
    }

    /** isFrozen is false until freeze() is called. */
    @Test
    fun freezeIsReported() {
        val nav = NavigationRegistry()
        assertFalse(nav.isFrozen)

        nav.freeze()

        assertTrue(nav.isFrozen)
    }
}
