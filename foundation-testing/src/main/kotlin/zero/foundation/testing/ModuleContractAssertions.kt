/*
 * MODULE: foundation-testing
 * PURPOSE: Provides test assertions for the ZeroModule contract.
 * TAKES: A ZeroModule implementation.
 * GIVES: Validation of the module display name.
 * OWNS: No mutable state.
 * DOES NOT OWN: Module identity validation or registry behavior.
 * DEPENDENCIES: foundation-contract.
 * CONTRACT: Rejects a blank display name with a descriptive AssertionError.
 * FAILURE BEHAVIOR: Throws AssertionError when displayName is blank.
 */
package zero.foundation.testing

import zero.foundation.contract.ZeroModule

/** Assertions shared by tests of ZeroModule implementations. */
object ModuleContractAssertions {
    /**
     * Asserts the basic contract every module must satisfy.
     * Throws AssertionError with a clear message on violation.
     */
    fun assertValid(module: ZeroModule) {
        if (!module.displayName.isNotBlank()) {
            throw AssertionError("displayName must not be blank for ${module.id}")
        }
    }
}
