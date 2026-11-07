/*
 * MODULE: feature-protection tests
 * PURPOSE: Verifies each screen classification and that other packages are never read.
 * TAKES: Explicit package names and text lists.
 * GIVES: Pass/fail results for the classifier.
 * OWNS: No production state.
 * DOES NOT OWN: Android window access.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: Each test asserts one classification rule.
 * FAILURE BEHAVIOR: Assertion failures identify the wrong classification.
 */
package zero.feature.protection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfScreenClassifierTest {
    private val installer = "com.google.android.packageinstaller"
    private val settings = "com.android.settings"
    private val label = "Zero"

    /** An uninstall dialog that names Zero is UNINSTALL_ZERO. */
    @Test
    fun installerUninstallNamingZeroIsUninstallZero() {
        val texts = listOf("Do you want to uninstall this app?", "Zero")

        assertEquals(SelfScreen.UNINSTALL_ZERO, SelfScreenClassifier.classify(installer, texts, label))
    }

    /** An uninstall dialog for another app is UNINSTALL_OTHER. */
    @Test
    fun installerUninstallOtherAppIsUninstallOther() {
        val texts = listOf("Do you want to uninstall this app?", "Calculator")

        assertEquals(SelfScreen.UNINSTALL_OTHER, SelfScreenClassifier.classify(installer, texts, label))
    }

    /** An installer prompt to install an app is an install screen, not an uninstall. */
    @Test
    fun installerInstallPromptIsInstallScreen() {
        val texts = listOf("Do you want to install this app?", "Zero")

        assertEquals(SelfScreen.INSTALL_SCREEN, SelfScreenClassifier.classify(installer, texts, label))
    }

    /** Zero's App info page with a destructive action is APP_INFO_ZERO. */
    @Test
    fun settingsZeroAppInfoIsAppInfoZero() {
        val texts = listOf("Zero", "Force stop", "Clear data", "Uninstall")

        assertEquals(SelfScreen.APP_INFO_ZERO, SelfScreenClassifier.classify(settings, texts, label))
    }

    /** Another app's App info page is NONE, even with the same buttons. */
    @Test
    fun settingsOtherAppInfoIsNone() {
        val texts = listOf("Calculator", "Force stop", "Clear data", "Uninstall")

        assertEquals(SelfScreen.NONE, SelfScreenClassifier.classify(settings, texts, label))
    }

    /** Packages other than installers and Settings are never classified. */
    @Test
    fun otherPackagesAreNeverClassified() {
        val texts = listOf("Do you want to uninstall this app?", "Zero", "Force stop")

        assertEquals(SelfScreen.NONE, SelfScreenClassifier.classify("com.example.notes", texts, label))
    }

    /** Only installer and Settings windows are read for their text. */
    @Test
    fun onlyInstallerAndSettingsAreReadForContent() {
        assertTrue(SelfScreenClassifier.readsContentFor(installer))
        assertTrue(SelfScreenClassifier.readsContentFor(settings))
        assertTrue(SelfScreenClassifier.readsContentFor("com.example.installer"))
        assertTrue(SelfScreenClassifier.readsContentFor("com.example.app", "com.example.UninstallActivity"))
        assertFalse(SelfScreenClassifier.readsContentFor("com.example.notes"))
    }

    /** The Play Store is an install screen by package alone. */
    @Test
    fun playStoreIsInstallScreen() {
        assertEquals(
            SelfScreen.INSTALL_SCREEN,
            SelfScreenClassifier.classify(SelfScreenClassifier.PLAY_STORE_PACKAGE, emptyList(), label),
        )
    }

    /** The "Install unknown apps" page in Settings is an install screen. */
    @Test
    fun settingsInstallUnknownAppsIsInstallScreen() {
        val texts = listOf("Install unknown apps", "Chrome")

        assertEquals(SelfScreen.INSTALL_SCREEN, SelfScreenClassifier.classify(settings, texts, label))
    }

    /** Installer activity class names identify uninstall screens even when the package is not in the known list. */
    @Test
    fun uninstallActivityClassIdentifiesOtherUninstall() {
        val texts = listOf("Do you want to uninstall this app?", "Calculator")

        assertEquals(
            SelfScreen.UNINSTALL_OTHER,
            SelfScreenClassifier.classify("com.example.systemui", texts, label, "com.example.UninstallActivity"),
        )
    }
}
