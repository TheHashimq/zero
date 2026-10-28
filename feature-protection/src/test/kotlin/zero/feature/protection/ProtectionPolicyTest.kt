/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies every rule in ProtectionPolicy.decide.
 * TAKES: Explicit packages, states, and times.
 * GIVES: Pass/fail results for the decision policy.
 * OWNS: No production state.
 * DOES NOT OWN: State transitions.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one decision rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong decision.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals

class ProtectionPolicyTest {
    private val own = "com.TheHashimq.zero"
    private val social = "com.example.social"
    private val now = 10_000L
    private val countdown = ProtectionState.Countdown(0L, 100_000L)

    private fun decide(
        pkg: String = social,
        state: ProtectionState = countdown,
        protected: Set<String> = setOf(social),
        last: Long? = null,
        at: Long = now,
    ) = ProtectionPolicy.decide(pkg, state, protected, own, last, at)

    /** Our own package is always allowed, even if it were in the protected set. */
    @Test
    fun ownPackageIsAlwaysAllowed() {
        assertEquals(ProtectionAction.Allow, decide(pkg = own, protected = setOf(own)))
    }

    /** Armed never blocks, even a protected app. */
    @Test
    fun armedAllowsProtectedApp() {
        assertEquals(ProtectionAction.Allow, decide(state = ProtectionState.Armed))
    }

    /** Grace never blocks, even a protected app. */
    @Test
    fun graceAllowsProtectedApp() {
        assertEquals(ProtectionAction.Allow, decide(state = ProtectionState.Grace(0L)))
    }

    /** During a countdown, an app that is not protected is allowed. */
    @Test
    fun countdownAllowsUnprotectedApp() {
        assertEquals(ProtectionAction.Allow, decide(pkg = "com.example.notes", protected = setOf(social)))
    }

    /** During a countdown, a protected app with no recent redirect is redirected Home. */
    @Test
    fun countdownRedirectsProtectedApp() {
        assertEquals(ProtectionAction.RedirectHome, decide())
    }

    /** A redirect 1499 ms ago is inside the guard, so the app is allowed. */
    @Test
    fun redirectWithinGuardIsAllowed() {
        assertEquals(ProtectionAction.Allow, decide(last = now - 1_499L))
    }

    /** A redirect exactly 1500 ms ago is outside the guard, so the app is redirected again. */
    @Test
    fun redirectExactlyAtGuardRedirectsAgain() {
        assertEquals(ProtectionAction.RedirectHome, decide(last = now - 1_500L))
    }

    /** A last-redirect time later than now is treated as no recent redirect. */
    @Test
    fun futureRedirectTimeRedirects() {
        assertEquals(ProtectionAction.RedirectHome, decide(last = now + 5L))
    }

    /** An empty protected set allows everything during a countdown. */
    @Test
    fun emptyProtectedSetAllowsEverything() {
        assertEquals(ProtectionAction.Allow, decide(protected = emptySet()))
    }

    /** Android Settings, when protected, is redirected during a countdown. */
    @Test
    fun settingsIsRedirectedDuringCountdown() {
        assertEquals(
            ProtectionAction.RedirectHome,
            decide(pkg = "com.android.settings", protected = setOf("com.android.settings")),
        )
    }
}
