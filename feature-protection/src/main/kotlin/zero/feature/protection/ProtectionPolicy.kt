/*
 * MODULE: feature-protection
 * PURPOSE: Decides whether a foreground app should be redirected Home.
 * TAKES: Foreground package, protection state, protected packages, own package, last redirect time, and now.
 * GIVES: A ProtectionAction.
 * OWNS: No state.
 * DOES NOT OWN: Recording redirect events or performing the redirect.
 * DEPENDENCIES: ProtectionState, ProtectionAction, ProtectionConstants.
 * CONTRACT: Our own package is always allowed. Only Countdown blocks.
 *           A redirect within the guard window is allowed to break loops.
 *           A last-redirect time later than now is treated as no recent redirect.
 * FAILURE BEHAVIOR: Never throws. Empty inputs produce Allow.
 */
package zero.feature.protection

/** Pure protection decision. */
object ProtectionPolicy {
    /** Returns the action for the app that just came to the foreground. */
    fun decide(
        foregroundPackage: String,
        state: ProtectionState,
        protectedPackages: Set<String>,
        ownPackage: String,
        lastRedirectMillis: Long?,
        nowMillis: Long,
    ): ProtectionAction {
        if (foregroundPackage == ownPackage) return ProtectionAction.Allow
        if (state !is ProtectionState.Countdown) return ProtectionAction.Allow
        if (foregroundPackage !in protectedPackages) return ProtectionAction.Allow
        if (isWithinGuard(lastRedirectMillis, nowMillis)) return ProtectionAction.Allow
        return ProtectionAction.RedirectHome
    }

    private fun isWithinGuard(lastRedirectMillis: Long?, nowMillis: Long): Boolean {
        if (lastRedirectMillis == null) return false
        val elapsed = nowMillis - lastRedirectMillis
        return elapsed >= 0 && elapsed < ProtectionConstants.REDIRECT_GUARD_MILLIS
    }
}
