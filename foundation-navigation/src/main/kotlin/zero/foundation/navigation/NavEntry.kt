/*
 * MODULE: foundation-navigation
 * PURPOSE: One menu item that a module contributes.
 * TAKES: A route id, a title, a category, and a sort order.
 * GIVES: An immutable entry.
 * OWNS: No state.
 * DOES NOT OWN: The screen behind the route. The shell owns rendering.
 * DEPENDENCIES: NavCategory.
 * CONTRACT: Ids are 2 to 40 characters: a lowercase letter, then lowercase letters, digits, or hyphens.
 *           Titles are not blank.
 * FAILURE BEHAVIOR: Invalid values throw IllegalArgumentException at construction.
 */
package zero.foundation.navigation

/** One menu item contributed by a module. */
data class NavEntry(
    val id: String,
    val title: String,
    val category: NavCategory,
    val order: Int = 0,
) {
    init {
        require(ID_PATTERN.matches(id)) { "Invalid navigation id '$id': must match $ID_PATTERN" }
        require(title.isNotBlank()) { "Navigation title must not be blank for '$id'" }
    }

    companion object {
        /** Pattern accepted for navigation ids. */
        val ID_PATTERN = Regex("[a-z][a-z0-9-]{1,39}")
    }
}
