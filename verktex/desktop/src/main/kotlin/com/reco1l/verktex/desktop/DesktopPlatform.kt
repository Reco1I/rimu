package com.reco1l.verktex.desktop

import kotlin.time.Duration
import com.reco1l.verktex.Platform
import com.reco1l.verktex.desktop.bass.BASSAudioDriver
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker
import okio.Path
import okio.Path.Companion.toPath
import org.lwjgl.glfw.GLFW.glfwGetTime

/**
 * The desktop implementation of [Platform.Implementation].
 *
 * @param density The scale of the window's content set by the system (e.g. 1.5 at 150%), which
 * density independent sizes follow.
 * @param applicationName The name of the folders the application keeps its files in, see [storage].
 * @param onExit Closes the application, see [exit].
 */
class DesktopPlatform(
    density: Float,
    width: Int,
    height: Int,
    applicationName: String = "Verktex",
    override val application: Platform.Application = Platform.Application(),
    private val onExit: () -> Unit = {},
) : Platform.Implementation {

    override val target = Platform.Target.Desktop
    override val provider = DesktopProvider()
    override val audioDriver by lazy { BASSAudioDriver() }
    override val currentDisplay = Platform.Display(density = density, width = width, height = height)

    override val storage = Platform.Storage(
        dataDirectory = dataDirectory(applicationName),
        cacheDirectory = System.getProperty("java.io.tmpdir").toPath() / applicationName,
        externalDirectory = System.getProperty("user.home").toPath(),
    )

    init {
        Platform.fileSystem.createDirectories(storage.dataDirectory)
        Platform.fileSystem.createDirectories(storage.cacheDirectory)
    }

    // A physical keyboard, text is typed into the focused input directly.
    override val usesVirtualKeyboard = false

    override val systemLocale: String
        get() = java.util.Locale.getDefault().toLanguageTag()


    // The same clock as the time of the input events.
    override val uptimeMillis: Long
        get() = (glfwGetTime() * 1000).toLong()


    // There are no platform views, what runs on the main thread goes to the update thread with
    // the rest of the UI.
    override fun runOnMainThread(block: () -> Unit) {
        com.reco1l.verktex.Verktex.scheduleOnUpdate(block)
    }

    override fun exit() = onExit()

    override fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker {
        return object : ThreadLoopWorker(name, minimumInterval) {
            override fun onLoop() = onLoop()
        }
    }

    // A desktop has no web view of its own, the page opens in the browser.
    override fun openWebPage(url: String, userAgent: String?) = openUrl(url)

    override fun openUrl(url: String) {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                return
            }

            val os = System.getProperty("os.name").lowercase()
            val command = when {
                "win" in os -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
                "mac" in os -> listOf("open", url)
                else -> listOf("xdg-open", url)
            }
            ProcessBuilder(command).start()
        } catch (e: Exception) {
            com.reco1l.verktex.Logger.e("DesktopPlatform", "Couldn't open $url", e)
        }
    }
}


/**
 * Where each system keeps the files of applications: `%APPDATA%` on Windows, `Application Support`
 * on macOS, and `$XDG_DATA_HOME` (`~/.local/share` by default) elsewhere.
 */
private fun dataDirectory(applicationName: String): Path {
    val home = System.getProperty("user.home").toPath()
    val os = System.getProperty("os.name").lowercase()

    val root = when {
        "win" in os -> System.getenv("APPDATA")?.toPath() ?: (home / "AppData" / "Roaming")
        "mac" in os -> home / "Library" / "Application Support"
        else -> System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotEmpty() }?.toPath() ?: (home / ".local" / "share")
    }
    return root / applicationName
}
