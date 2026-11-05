package com.reco1l.verktex.desktop.testing

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.DesktopGameHost
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import com.reco1l.verktex.testing.UITestScene
import com.reco1l.verktex.testing.UITestSuite
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.prefs.Preferences
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImageWrite

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
            captureScreenshot(screenshot)
        }
    }

    private fun captureScreenshot(file: File) {
        val start = glfwGetTime()

        // Called on the draw thread, the frame is complete in the back buffer before it's presented.
        context.onBeforePresent = callback@{
            if (glfwGetTime() - start < SCREENSHOT_DELAY_SECONDS) return@callback
            context.onBeforePresent = null

            val window = context.window
            val width = window.windowWidth
            val height = window.windowHeight

            val pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            glPixelStorei(GL_PACK_ALIGNMENT, 1)
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

            // GL reads from the bottom row, images start at the top.
            val flipped = ByteBuffer.allocateDirect(pixels.capacity())
            val row = ByteArray(width * 4)
            for (y in height - 1 downTo 0) {
                pixels.position(y * width * 4)
                pixels.get(row)
                flipped.put(row)
            }
            flipped.flip()

            file.absoluteFile.parentFile?.mkdirs()
            if (STBImageWrite.stbi_write_png(file.absolutePath, width, height, 4, flipped, width * 4)) {
                Logger.i("UITestApplication", "Saved a screenshot to ${file.absolutePath}")
            } else {
                Logger.e("UITestApplication", "Couldn't write the screenshot to ${file.absolutePath}")
            }
            close()
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
