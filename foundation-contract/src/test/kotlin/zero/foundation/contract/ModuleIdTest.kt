/*
 * MODULE: foundation-contract
 * PURPOSE: Verifies the ModuleId validation and value behavior.
 * TAKES: ModuleId values and invalid identifier strings.
 * GIVES: Deterministic test results for the identifier contract.
 * OWNS: No production state.
 * DOES NOT OWN: Registry or lifecycle tests.
 * DEPENDENCIES: foundation-contract and Kotlin test/JUnit.
 * CONTRACT: Covers valid, invalid, string, and equality behavior.
 * FAILURE BEHAVIOR: Assertion failures identify a violated ModuleId guarantee.
 */
package zero.foundation.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Tests ModuleId validation and value semantics. */
class ModuleIdTest {
    /** Accepts a valid identifier. */
    @Test
    fun acceptsSettings() {
        assertEquals(ModuleId("settings"), ModuleId("settings"))
    }

    /** Rejects an identifier shorter than two characters. */
    @Test
    fun rejectsTooShortIdentifier() {
        assertFailsWith<IllegalArgumentException> { ModuleId("a") }
    }

    /** Rejects uppercase characters. */
    @Test
    fun rejectsUppercaseIdentifier() {
        assertFailsWith<IllegalArgumentException> { ModuleId("Settings") }
    }

    /** Rejects identifiers that do not start with a letter. */
    @Test
    fun rejectsIdentifierWithoutLeadingLetter() {
        assertFailsWith<IllegalArgumentException> { ModuleId("1abc") }
    }

    /** Rejects underscores. */
    @Test
    fun rejectsUnderscore() {
        assertFailsWith<IllegalArgumentException> { ModuleId("my_module") }
    }

    /** Rejects identifiers longer than 32 characters. */
    @Test
    fun rejectsIdentifierLongerThan32Characters() {
        assertFailsWith<IllegalArgumentException> { ModuleId("a".repeat(33)) }
    }

    /** Returns the raw identifier from toString. */
    @Test
    fun toStringReturnsRawValue() {
        assertEquals("settings", ModuleId("settings").toString())
    }

    /** Uses value equality for identical identifiers. */
    @Test
    fun sameStringsProduceEqualIds() {
        assertEquals(ModuleId("settings"), ModuleId("settings"))
    }
}
