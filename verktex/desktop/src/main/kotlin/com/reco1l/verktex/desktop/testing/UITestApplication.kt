package com.reco1l.verktex.desktop.testing

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.DesktopGameHost
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import com.reco1l.verktex.testing.UITestScene
import com.reco1l.verktex.testing.UITestSuite
import java.io.File
import java.util.prefs.Preferences

/**
 * A desktop window to browse and try UI components in isolation, the counterpart of the Android
 * platform's UITestActivity.
 *
 * Projects extend it and return their suites from [createSuites] (usually with
 * [com.reco1l.verktex.testing.VerktexUITests] first).
 *
 * @param initialTest The test to open first, as `suite/test` or just the test name. Without it,
 * the last opened test is shown again.
 * @param screenshot When set, the frame shown [SCREENSHOT_DELAY_SECONDS] after starting is saved
 * to this PNG and the window closes, e.g. for smoke checks.
 * @param vsync Whether frames wait for the display's refresh, see [DesktopGameHost].
 */
abstract class UITestApplication(
    private val initialTest: String? = null,
    private val screenshot: File? = null,
    vsync: Boolean = false,
) : DesktopGameHost(title = "Verktex UI tests", vsync = vsync) {

    private val preferences = Preferences.userRoot().node("com/reco1l/verktex/uitests")

    /**
     * Creates the suites shown in the browser. Called once the engine is initialized, so textures and
     * fonts can be loaded.
     */
    abstract fun createSuites(): List<UITestSuite>


    override fun onEngineInitialized() {}

    override fun onEngineStarted() {
        // Always shown while trying components.
        Settings[VerktexSettings.ShowPerformanceOverlay] = true

        val test = initialTest ?: preferences.get(KEY_LAST_TEST, null)

        Verktex.withTreeLock {
            Verktex.pushScene(UITestScene(createSuites(), test) { id ->
                preferences.put(KEY_LAST_TEST, id)
            })
        }

        if (screenshot != null) {
            captureScreenshot(screenshot, SCREENSHOT_DELAY_SECONDS)
        }
    }

    companion object {
        /**
         * How long after starting the screenshot is taken, so fonts and textures are loaded and the
         * test's entrance animations ended.
         */
        const val SCREENSHOT_DELAY_SECONDS = 1.5

        private const val KEY_LAST_TEST = "last_test"
    }
}
