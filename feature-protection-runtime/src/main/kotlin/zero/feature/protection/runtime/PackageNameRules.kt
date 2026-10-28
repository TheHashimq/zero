/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Validates Android package names before they enter the protected list.
 * TAKES: A candidate package name string.
 * GIVES: true when the name is a well-formed package name.
 * OWNS: No state.
 * DOES NOT OWN: Whether the package is installed or launchable.
 * DEPENDENCIES: None.
 * CONTRACT: At least two dot-separated segments; each starts with a letter and contains letters, digits, or underscores.
 *           Length is at most 255 characters.
 * FAILURE BEHAVIOR: Never throws. Invalid names return false.
 */
package zero.feature.protection.runtime

/** Validation rules for package names. */
object PackageNameRules {
    const val MAX_LENGTH = 255

    private val PATTERN = Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")

    /** Returns true when [name] is a well-formed package name. */
    fun isValid(name: String): Boolean = name.length <= MAX_LENGTH && PATTERN.matches(name)
}
