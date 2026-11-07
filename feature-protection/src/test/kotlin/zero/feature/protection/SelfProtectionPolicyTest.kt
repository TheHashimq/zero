/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies the self-protection rules for each screen and state.
 * TAKES: Explicit screens and states.
 * GIVES: Pass/fail results for the policy.
 * OWNS: No production state.
 * DOES NOT OWN: Redirect or grace-ending side effects.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong decision.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals

class SelfProtectionPolicyTest {
    private val armed = ProtectionState.Armed
    private val countdown = ProtectionState.Countdown(0L, 100_000L)
    private val grace = ProtectionState.Grace(0L)

    /** Screens that are not self-protection screens are allowed in every state. */
    @Test
    fun noneIsAllowedInEveryState() {
        listOf(armed, countdown, grace).forEach { state ->
            assertEquals(SelfDecision.Allow, SelfProtectionPolicy.decide(SelfScreen.NONE, state))
        }
    }

    /** Uninstalling another app is allowed in every state. */
    @Test
    fun uninstallOtherIsAllowedInEveryState() {
        listOf(armed, countdown, grace).forEach { state ->
            assertEquals(SelfDecision.Allow, SelfProtectionPolicy.decide(SelfScreen.UNINSTALL_OTHER, state))
        }
    }

    /** Uninstalling Zero during grace ends the grace at once. */
    @Test
    fun uninstallZeroDuringGraceEndsGrace() {
        assertEquals(
            SelfDecision.RedirectHomeAndEndGrace,
            SelfProtectionPolicy.decide(SelfScreen.UNINSTALL_ZERO, grace),
        )
    }

    /** Uninstalling Zero is blocked while Armed or during a countdown, and does not end anything. */
    @Test
    fun uninstallZeroIsBlockedWhileArmedOrCountdown() {
        listOf(armed, countdown).forEach { state ->
            assertEquals(SelfDecision.RedirectHome, SelfProtectionPolicy.decide(SelfScreen.UNINSTALL_ZERO, state))
        }
    }

    /** Zero's App info is blocked in every state, including grace. */
    @Test
    fun appInfoZeroIsBlockedInEveryState() {
        listOf(armed, countdown, grace).forEach { state ->
            assertEquals(SelfDecision.RedirectHome, SelfProtectionPolicy.decide(SelfScreen.APP_INFO_ZERO, state))
        }
    }
}
