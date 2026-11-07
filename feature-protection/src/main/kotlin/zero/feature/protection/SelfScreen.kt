/*
 * MODULE: feature-protection
 * PURPOSE: Recognizes screens that could remove Zero, change installed apps, or change its data, and whether text may be read.
 * TAKES: A package name, optional activity class name, window text, and Zero's app label.
 * GIVES: A SelfScreen value.
 * OWNS: Installer, Play Store, and Settings identifiers, and the App info marker words.
 * DOES NOT OWN: Reading the screen. The Android adapter reads text and passes it in.
 * DEPENDENCIES: None.
 * CONTRACT: Text is only inspected for installer and Settings windows. The Play Store is recognized by package alone.
 *           Installers are recognized by package or activity class. Uninstall screens are checked before install
 *           screens, because "uninstall" contains "install". Other packages are always NONE. Text is never stored.
 * FAILURE BEHAVIOR: Never throws. Empty text gives NONE.
 */
package zero.feature.protection

/** Screens that matter for self-protection. */
enum class SelfScreen { NONE, UNINSTALL_OTHER, UNINSTALL_ZERO, APP_INFO_ZERO, INSTALL_SCREEN }

/** Classifies a window by its package, class name, and visible text. */
object SelfScreenClassifier {
    val INSTALLER_PACKAGES: Set<String> = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
    )
    const val SETTINGS_PACKAGE: String = "com.android.settings"
    const val PLAY_STORE_PACKAGE: String = "com.android.vending"

    private val APP_INFO_MARKERS = listOf("force stop", "clear storage", "clear data", "uninstall")

    /** True when the window is a system installer, by package name, keyword, or activity class name. */
    fun isInstaller(packageName: String, className: String?): Boolean =
        packageName in INSTALLER_PACKAGES ||
            packageName.contains("packageinstaller", ignoreCase = true) ||
            packageName.contains("installer", ignoreCase = true) ||
            (className ?: "").contains("uninstall", ignoreCase = true)

    /** True when the adapter may read this window's text. */
    fun readsContentFor(packageName: String, className: String? = null): Boolean =
        isInstaller(packageName, className) || packageName == SETTINGS_PACKAGE

    /** Classifies a window. [selfLabel] is Zero's app label on its own uninstall and App info screens. */
    fun classify(
        packageName: String,
        texts: List<String>,
        selfLabel: String,
        className: String? = null,
    ): SelfScreen {
        val namesZero = texts.any { it.trim() == selfLabel }
        val mentionsUninstall = texts.any { it.contains("uninstall", ignoreCase = true) }
        val mentionsInstall = texts.any { it.contains("install", ignoreCase = true) }
        return when {
            packageName == PLAY_STORE_PACKAGE -> SelfScreen.INSTALL_SCREEN
            isInstaller(packageName, className) && mentionsUninstall ->
                if (namesZero) SelfScreen.UNINSTALL_ZERO else SelfScreen.UNINSTALL_OTHER
            isInstaller(packageName, className) && mentionsInstall -> SelfScreen.INSTALL_SCREEN
            packageName == SETTINGS_PACKAGE &&
                texts.any { it.contains("install unknown apps", ignoreCase = true) } -> SelfScreen.INSTALL_SCREEN
            packageName == SETTINGS_PACKAGE && namesZero &&
                texts.any { text -> APP_INFO_MARKERS.any { text.contains(it, ignoreCase = true) } } ->
                SelfScreen.APP_INFO_ZERO
            else -> SelfScreen.NONE
        }
    }
}
