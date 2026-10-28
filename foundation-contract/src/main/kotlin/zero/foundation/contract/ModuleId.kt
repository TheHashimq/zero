/*
 * MODULE: foundation-contract
 * PURPOSE: Defines validated stable identifiers for Zero modules.
 * TAKES: A string identifier.
 * GIVES: An immutable ModuleId value with value-based equality.
 * OWNS: No mutable state.
 * DOES NOT OWN: Module registration or lifecycle execution.
 * DEPENDENCIES: None.
 * CONTRACT: Identifiers match the lowercase module id pattern.
 * FAILURE BEHAVIOR: Invalid identifiers fail construction with IllegalArgumentException.
 */
package zero.foundation.contract

/** Stable, validated identifier used to register and look up a module. */
@JvmInline
value class ModuleId(val value: String) {
    init {
        require(ID_PATTERN.matches(value)) {
            "Invalid module id '$value': must match $ID_PATTERN"
        }
    }

    /** Returns the raw identifier value. */
    override fun toString(): String = value

    companion object {
        /** Pattern accepted for module identifiers. */
        val ID_PATTERN = Regex("[a-z][a-z0-9-]{1,31}")
    }
}
