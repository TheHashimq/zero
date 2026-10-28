/*
 * MODULE: foundation-navigation
 * PURPOSE: Fixed set of menu categories.
 * TAKES: Nothing; an enum.
 * GIVES: Categories in display order, each with a label.
 * OWNS: No state.
 * DOES NOT OWN: Which entries belong to which category. Modules decide that.
 * DEPENDENCIES: None.
 * CONTRACT: Declaration order is the display order.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.foundation.navigation

/** Menu categories, in display order. */
enum class NavCategory(val label: String) {
    PROTECTION("Protection"),
    PERSONAL("Personal"),
    ACTIVITY("Activity"),
    SYSTEM("System"),
}
