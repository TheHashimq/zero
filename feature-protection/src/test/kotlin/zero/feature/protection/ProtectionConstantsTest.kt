/*
 * MODULE: feature-protection tests
 * PURPOSE: Locks the timing constants to their documented values.
 * TAKES: ProtectionConstants.
 * GIVES: Pass/fail results.
 * OWNS: No production state.
 * DOES NOT OWN: Behavior that uses the constants.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Grace is 3 minutes; redirect guard is 1500 ms.
 * FAILURE BEHAVIOR: Assertion failures identify a changed constant.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals

class ProtectionConstantsTest {
    /** Grace is exactly 3 minutes. */
    @Test
    fun graceIsThreeMinutes() {
        assertEquals(180_000L, ProtectionConstants.GRACE_MILLIS)
    }

    /** Redirect guard is exactly 1500 ms. */
    @Test
    fun redirectGuardIs1500Millis() {
        assertEquals(1_500L, ProtectionConstants.REDIRECT_GUARD_MILLIS)
    }
}
