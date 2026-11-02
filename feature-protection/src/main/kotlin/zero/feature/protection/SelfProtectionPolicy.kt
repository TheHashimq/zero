/*
 * MODULE: feature-protection
 * PURPOSE: Decides how to respond to uninstall and Zero App info screens.
 * TAKES: A SelfScreen and the current ProtectionState.
 * GIVES: A SelfDecision.
 * OWNS: No state.
 * DOES NOT OWN: Performing the redirect or ending the grace window. The coordinator does that.
 * DEPENDENCIES: SelfScreen, ProtectionState.
 * CONTRACT: Uninstall dialogs for any app and Zero's App info are blocked in Armed and Countdown.
 *           In Grace, other apps' uninstall dialogs are allowed. Opening Zero's uninstall dialog during Grace
 *           ends the grace at once. Zero's App info is blocked in every state.
 *           No redirect guard applies: Home never reopens these screens, so a guard only opens a bypass window.
 * FAILURE BEHAVIOR: Never throws.
 */
package zero.feature.protection

/** Decision for a self-protection screen. */
sealed interface SelfDecision {
    /** Leave the screen alone. */
    data object Allow : SelfDecision

    /** Send the user Home. */
    data object RedirectHome : SelfDecision

    /** Send the user Home and end the grace window immediately. */
    data object RedirectHomeAndEndGrace : SelfDecision
}

/** Pure self-protection decision. */
object SelfProtectionPolicy {
    fun decide(screen: SelfScreen, state: ProtectionState): SelfDecision = when (screen) {
        SelfScreen.NONE -> SelfDecision.Allow
        SelfScreen.UNINSTALL_OTHER -> when (state) {
            is ProtectionState.Grace -> SelfDecision.Allow
            else -> SelfDecision.RedirectHome
        }
        SelfScreen.UNINSTALL_ZERO -> when (state) {
            is ProtectionState.Grace -> SelfDecision.RedirectHomeAndEndGrace
            else -> SelfDecision.RedirectHome
        }
        SelfScreen.APP_INFO_ZERO -> SelfDecision.RedirectHome
    }
}
