/*
 * MODULE: foundation-events
 * PURPOSE: Validated name of an event kind.
 * TAKES: A string.
 * GIVES: An immutable EventType with value equality.
 * OWNS: No state.
 * DOES NOT OWN: Event payloads or meaning.
 * DEPENDENCIES: None.
 * CONTRACT: 1 to 48 characters, starting with a lowercase letter, then lowercase letters, digits, dots, or hyphens.
 * FAILURE BEHAVIOR: Invalid values throw IllegalArgumentException at construction.
 */
package zero.foundation.events

/** Validated name of an event kind, such as "protection.escape-attempt". */
@JvmInline
value class EventType(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Invalid event type '$value': must match $PATTERN" }
    }

    /** Returns the raw event type value. */
    override fun toString(): String = value

    companion object {
        /** Pattern accepted for event types. */
        val PATTERN = Regex("[a-z][a-z0-9.-]{0,47}")
    }
}
