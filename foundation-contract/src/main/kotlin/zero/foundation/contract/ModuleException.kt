/*
 * MODULE: foundation-contract
 * PURPOSE: Provides a base exception for module-specific failures.
 * TAKES: A message and an optional cause.
 * GIVES: A RuntimeException subtype for module errors.
 * OWNS: No mutable state beyond standard exception details.
 * DOES NOT OWN: Registry error handling or failure recovery.
 * DEPENDENCIES: Kotlin/JVM runtime.
 * CONTRACT: Preserves the supplied message and cause.
 * FAILURE BEHAVIOR: Construction has standard RuntimeException behavior.
 */
package zero.foundation.contract

/** Base exception for failures defined by module implementations. */
open class ModuleException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
