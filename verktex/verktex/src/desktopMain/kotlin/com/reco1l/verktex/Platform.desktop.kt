package com.reco1l.verktex

import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.desktop.DesktopProvider
import com.reco1l.verktex.desktop.bass.BASSAudioDriver
import com.reco1l.verktex.desktop.video.FFmpeg
import com.reco1l.verktex.desktop.video.FFmpegVideoDecoder
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.io.systemFileSystem
import com.reco1l.verktex.video.VideoDecoder
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker
import java.util.Locale
import kotlin.time.Duration
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.lwjgl.glfw.GLFW.glfwGetTime

/**
 * The desktop platform, over the window that hosts the engine, see [attach].
 */
actual object Platform {

    @Volatile
    private var host: Host? = null

    private val attached: Host
        get() = host ?: throw IllegalStateException("The platform isn't attached to a host yet")


    /**
     * Attaches the platform to the window that hosts the engine.
     *
     * @param density The scale of the window's content set by the system (e.g. 1.5 at 150%), which
     * density independent sizes follow.
     * @param applicationName The name of the folders the application keeps its files in, see [storage].
     * @param onExit Closes the application, see [exit].
     */
    internal fun attach(
        density: Float,
        width: Int,
        height: Int,
        applicationName: String = "Verktex",
        application: PlatformApplication = PlatformApplication(),
        onExit: () -> Unit = {},
    ) {
        val storage = PlatformStorage(
            dataDirectory = dataDirectory(applicationName),
            cacheDirectory = System.getProperty("java.io.tmpdir").toPath() / applicationName,
            externalDirectory = System.getProperty("user.home").toPath(),
        )

        fileSystem.createDirectories(storage.dataDirectory)
        fileSystem.createDirectories(storage.cacheDirectory)

        host = Host(
            display = PlatformDisplay(density = density, width = width, height = height),
            storage = storage,
            application = application,
            onExit = onExit,
        )
    }


    actual val target = PlatformTarget.Desktop

    actual val provider: Provider
        get() = attached.provider

    // The same clock as the time of the input events.
    actual val uptimeMillis: Long
        get() {
            attached
            return (glfwGetTime() * 1000).toLong()
        }

    actual val fileSystem: FileSystem
        get() = systemFileSystem

    actual val storage: PlatformStorage
        get() = attached.storage

    actual val audioDriver: AudioDriver by lazy { BASSAudioDriver() }

    @Volatile
    actual var resolutionScale = 1f

    actual val currentDisplay: PlatformDisplay
        get() = attached.display

    // A physical keyboard, text is typed into the focused input directly.
    actual val usesVirtualKeyboard = false

    actual val systemLocale: String
        get() = if (host != null) Locale.getDefault().toLanguageTag() else "en"

    actual val application: PlatformApplication
        get() = host?.application ?: PlatformApplication()

    // A window is taken as visible while it's open.
    actual val isInForeground: Boolean
        get() {
            attached
            return true
        }

    actual val isLowEndDevice: Boolean
        get() {
            attached
            return false
        }

    // OpenGL is the only backend, there's nothing to choose.
    actual val availableGraphicsBackends: List<GraphicsBackend>
        get() {
            attached
            return emptyList()
        }

    actual val haptics: Haptics?
        get() {
            attached
            return null
        }

    actual val canForceMaxRefreshRate = false

    actual var forceMaxRefreshRate = false
        set(value) {
            attached
            field = value
        }

    actual val canPickFiles: Boolean
        get() {
            attached
            return false
        }

    // Logged, a failed task doesn't end the application.
    actual var onUncaughtException: (Throwable) -> Unit = { throwable ->
        if (host != null) {
            Logger.e("Platform", "Uncaught exception in a task", throwable)
        } else {
            throw throwable
        }
    }

    // Videos are decoded with FFmpeg, when its libraries could be loaded.
    actual val canPlayVideos: Boolean
        get() {
            attached
            return FFmpeg.libraries != null
        }


    // A desktop has no web view of its own, the page opens in the browser.
    actual fun openWebPage(url: String, userAgent: String?) = openUrl(url)

    actual fun openUrl(url: String) {
        attached

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
            Logger.e("Platform", "Couldn't open $url", e)
        }
    }

    actual fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker {
        attached
        return object : ThreadLoopWorker(name, minimumInterval) {
            override fun onLoop() = onLoop()
        }
    }

    // There are no platform views, what runs on the main thread goes to the update thread with
    // the rest of the UI.
    actual fun runOnMainThread(block: () -> Unit) {
        attached
        Verktex.scheduleOnUpdate(block)
    }

    actual fun exit() = attached.onExit()

    // Not supported yet.
    actual fun copyToClipboard(text: String, label: String) {
        attached
    }

    actual fun pickFiles(mimeType: String, onPicked: (List<Path>) -> Unit) {
        attached
    }

    actual fun createVideoDecoder(path: String): VideoDecoder? {
        return if (canPlayVideos) FFmpegVideoDecoder(path) else null
    }


    /**
     * What's known from the window hosting the engine.
     */
    private class Host(
        val display: PlatformDisplay,
        val storage: PlatformStorage,
        val application: PlatformApplication,
        val onExit: () -> Unit,
    ) {
        val provider = DesktopProvider()
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
