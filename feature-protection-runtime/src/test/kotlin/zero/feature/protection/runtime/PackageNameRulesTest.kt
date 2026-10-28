/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies package name validation.
 * TAKES: Valid, invalid, and boundary-length names.
 * GIVES: Pass/fail results for validation.
 * OWNS: No production state.
 * DOES NOT OWN: Persistence.
 * DEPENDENCIES: feature-protection-runtime.
 * CONTRACT: Two or more segments, letter-first, at most 255 characters.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong validation decision.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageNameRulesTest {
    /** Typical package names are accepted. */
    @Test
    fun acceptsWellFormedNames() {
        assertTrue(PackageNameRules.isValid("com.android.settings"))
        assertTrue(PackageNameRules.isValid("com.example.my_app2"))
    }

    /** Single-segment, empty, digit-first, double-dot, and space-containing names are rejected. */
    @Test
    fun rejectsMalformedNames() {
        listOf("settings", "", "com.1app.x", "com..x", "com.has space.x", "com.a/b").forEach { bad ->
            assertFalse(PackageNameRules.isValid(bad), "expected '$bad' to be rejected")
        }
    }

    /** Exactly 255 characters is accepted; 256 is rejected. */
    @Test
    fun enforcesLengthBoundary() {
        val at255 = "a." + "b".repeat(253)
        val at256 = "a." + "b".repeat(254)

        assertTrue(PackageNameRules.isValid(at255))
        assertFalse(PackageNameRules.isValid(at256))
    }
}
