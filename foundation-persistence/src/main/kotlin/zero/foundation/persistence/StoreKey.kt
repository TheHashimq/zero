/*
 * MODULE: foundation-persistence
 * PURPOSE: Validated name of one value inside a module's store.
 * TAKES: A string key.
 * GIVES: An immutable StoreKey value with value-based equality.
 * OWNS: No mutable state.
 * DOES NOT OWN: Namespacing. See NamespacedStore.
 * DEPENDENCIES: None.
 * CONTRACT: Keys are 1 to 64 characters, start with a lowercase letter, and contain only
 *           lowercase letters, digits, dots, underscores, and hyphens.
 * FAILURE BEHAVIOR: Invalid keys fail construction with IllegalArgumentException.
 */
package zero.foundation.persistence

/** Validated name of a value within a module's store. */
@JvmInline
value class StoreKey(val value: String) {
    init {
        require(PATTERN.matches(value)) {
            "Invalid store key '$value': must match $PATTERN"
        }
    }

    /** Returns the raw key value. */
    override fun toString(): String = value

    companion object {
        /** Pattern accepted for store keys. */
        val PATTERN = Regex("[a-z][a-z0-9._-]{0,63}")
    }
}
