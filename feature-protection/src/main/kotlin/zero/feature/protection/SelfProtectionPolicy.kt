/*
 * MODULE: feature-protection
 * PURPOSE: Decides how to respond to uninstall, install, and Zero App info screens.
 * TAKES: A SelfScreen and the current ProtectionState.
 * GIVES: A SelfDecision.
 * OWNS: No state.
 * DOES NOT OWN: Performing the redirect or ending the grace window. The coordinator does that.
 * DEPENDENCIES: SelfScreen, ProtectionState.
 * CONTRACT: Uninstalling any other app is always allowed. Uninstalling Zero and opening Zero's App info are blocked
 *           in every state; uninstalling Zero during Grace ends the grace at once.
 *           Install screens, including the Play Store, are blocked outside Grace and allowed during Grace.
 *           No redirect guard applies, since Home never reopens these screens.
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
        SelfScreen.UNINSTALL_OTHER -> SelfDecision.Allow
        SelfScreen.UNINSTALL_ZERO -> when (state) {
            is ProtectionState.Grace -> SelfDecision.RedirectHomeAndEndGrace
            else -> SelfDecision.RedirectHome
        }
        SelfScreen.APP_INFO_ZERO -> SelfDecision.RedirectHome
        SelfScreen.INSTALL_SCREEN -> when (state) {
            is ProtectionState.Grace -> SelfDecision.Allow
            else -> SelfDecision.RedirectHome
        }
    }
}
