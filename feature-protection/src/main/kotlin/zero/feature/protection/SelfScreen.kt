/*
 * MODULE: feature-protection
 * PURPOSE: Recognizes the two screens that could remove Zero or its data, and whether a screen is worth reading.
 * TAKES: A foreground package name, the texts on that window, and Zero's app label.
 * GIVES: A SelfScreen value.
 * OWNS: The installer and Settings package names, and the App info marker words.
 * DOES NOT OWN: Reading the screen. The Android adapter reads text and passes it in.
 * DEPENDENCIES: None.
 * CONTRACT: Text is only inspected for installer and Settings packages. Other packages are always NONE.
 *           The text is never stored or returned; only the classification is returned.
 * FAILURE BEHAVIOR: Never throws. Empty text gives NONE.
 */
package zero.feature.protection

/** Screens that matter for self-protection. */
enum class SelfScreen { NONE, UNINSTALL_OTHER, UNINSTALL_ZERO, APP_INFO_ZERO }

/** Classifies a window by its package and visible text. */
object SelfScreenClassifier {
    val INSTALLER_PACKAGES: Set<String> = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
    )
    const val SETTINGS_PACKAGE: String = "com.android.settings"

    private val APP_INFO_MARKERS = listOf("force stop", "clear storage", "clear data", "uninstall")

    /** True when the adapter may read this window's text. */
    fun readsContentFor(packageName: String): Boolean =
        packageName in INSTALLER_PACKAGES || packageName == SETTINGS_PACKAGE

    /** Classifies the window. [selfLabel] is Zero's app label, shown on its own uninstall and App info screens. */
    fun classify(packageName: String, texts: List<String>, selfLabel: String): SelfScreen {
        val namesZero = texts.any { it.trim() == selfLabel }
        return when {
            packageName in INSTALLER_PACKAGES && texts.any { it.contains("uninstall", ignoreCase = true) } ->
                if (namesZero) SelfScreen.UNINSTALL_ZERO else SelfScreen.UNINSTALL_OTHER
            packageName == SETTINGS_PACKAGE && namesZero &&
                texts.any { text -> APP_INFO_MARKERS.any { text.contains(it, ignoreCase = true) } } ->
                SelfScreen.APP_INFO_ZERO
            else -> SelfScreen.NONE
        }
    }
}
