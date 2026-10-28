/*
 * MODULE: foundation-navigation
 * PURPOSE: Collects menu entries from modules during startup and groups them for the shell.
 * TAKES: NavEntry values before freeze().
 * GIVES: Entries grouped by category, in display order.
 * OWNS: The entry map and the frozen flag.
 * DOES NOT OWN: Rendering or screen behavior.
 * DEPENDENCIES: NavEntry, NavCategory.
 * CONTRACT: Entries cannot be added after freeze(). Each id appears once.
 *           grouped() sorts categories by enum order, then entries by order, then id.
 * FAILURE BEHAVIOR: Duplicate ids throw IllegalArgumentException. Registration after freeze throws IllegalStateException.
 *                   Not thread-safe; call from one thread.
 */
package zero.foundation.navigation

/** Collects and groups navigation entries. */
class NavigationRegistry {
    private val entries = LinkedHashMap<String, NavEntry>()
    private var frozen = false

    /** True after freeze() has been called. */
    val isFrozen: Boolean get() = frozen

    /** Adds an entry. Rejects duplicates and any call after freeze(). */
    fun register(entry: NavEntry) {
        check(!frozen) { "Navigation is frozen; '${entry.id}' cannot be added" }
        require(!entries.containsKey(entry.id)) { "Duplicate navigation id '${entry.id}'" }
        entries[entry.id] = entry
    }

    /** Stops further registration. The composition root calls this once after module startup. */
    fun freeze() {
        frozen = true
    }

    /** Entries grouped by category. Empty categories are omitted. */
    fun grouped(): Map<NavCategory, List<NavEntry>> {
        val result = LinkedHashMap<NavCategory, List<NavEntry>>()
        for (category in NavCategory.entries) {
            val inCategory = entries.values
                .filter { it.category == category }
                .sortedWith(compareBy<NavEntry>({ it.order }, { it.id }))
            if (inCategory.isNotEmpty()) result[category] = inCategory
        }
        return result
    }
}
