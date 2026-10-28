/*
 * MODULE: foundation-design
 * PURPOSE: Font sizes in sp for the app's type scale.
 * TAKES: Nothing; constants.
 * GIVES: Display, headline, title, body, and label sizes.
 * OWNS: No state.
 * DOES NOT OWN: Font family or weight. The app theme sets those.
 * DEPENDENCIES: None.
 * CONTRACT: Sizes strictly decrease from display to label.
 * FAILURE BEHAVIOR: Not applicable.
 */
package zero.foundation.design

/** Type scale in sp. */
object TypeScale {
    const val DISPLAY_SP: Int = 56
    const val HEADLINE_SP: Int = 28
    const val TITLE_SP: Int = 20
    const val BODY_SP: Int = 16
    const val LABEL_SP: Int = 12
}
