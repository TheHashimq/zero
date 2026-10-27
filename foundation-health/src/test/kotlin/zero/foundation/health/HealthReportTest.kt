/*
 * MODULE: foundation-health tests
 * PURPOSE: Verifies HealthReport validation.
 * TAKES: A blank component name.
 * GIVES: Pass/fail result for the blank-name rule.
 * OWNS: No production state.
 * DOES NOT OWN: Aggregation.
 * DEPENDENCIES: foundation-health.
 * CONTRACT: Blank component names are rejected.
 * FAILURE BEHAVIOR: Assertion failure identifies missing validation.
 */
package zero.foundation.health

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Tests HealthReport component validation. */
class HealthReportTest {
    /** A blank component name is rejected. */
    @Test
    fun blankComponentIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            HealthReport("  ", HealthState.OK, "x")
        }
    }
}
