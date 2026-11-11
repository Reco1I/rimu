package com.reco1l.verktex

import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.video.VideoDecoder
import com.reco1l.verktex.worker.LoopWorker
import kotlin.time.Duration
import okio.FileSystem
import okio.Path

/**
 * What the engine needs from the platform it runs on. Each target has its own actual object
 * (`Platform.android.kt`, `Platform.desktop.kt`), which the host of the platform attaches itself to
 * when it starts the engine.
 *
 * Until then only what needs no host works ([fileSystem], [systemLocale], [application],
 * [resolutionScale] and [onUncaughtException]), which is what code that runs without a platform
 * uses, e.g. unit tests. The rest throws an [IllegalStateException].
 */
expect object Platform {

    /**
     * The execution target of this platform.
     */
    val target: PlatformTarget

    /**
     * The provider used to load physical resources from the platform.
     */
    val provider: Provider

    /**
     * The milliseconds of the host's uptime clock, the time base of [com.reco1l.verktex.input.PointerEvent.time]
     * (e.g. Android's `SystemClock.uptimeMillis`).
     */
    val uptimeMillis: Long

    /**
     * The file system of the device. It doesn't need a host.
     */
    val fileSystem: FileSystem

    /**
     * The folders where the application keeps its files, see [PlatformStorage].
     */
    val storage: PlatformStorage

    /**
     * The driver used to access the audio devices of the platform.
     */
    val audioDriver: AudioDriver

    /**
     * How many pixels of the rendered window there are per pixel of the display, below 1 when
     * rendering at a lower resolution. Sizes in density independent pixels follow it, so the UI
     * keeps its size on the screen.
     */
    var resolutionScale: Float

    /**
     * The configuration of the current display.
     */
    val currentDisplay: PlatformDisplay

    /**
     * Indicates whether the platform uses a virtual keyboard.
     */
    val usesVirtualKeyboard: Boolean

    /**
     * The language the user chose for the system, as a BCP 47 tag such as `pt-BR`. English without
     * a host, e.g. in tests.
     */
    val systemLocale: String

    /**
     * The application running, see [PlatformApplication]. A default one without a host.
     */
    val application: PlatformApplication

    /**
     * Whether the application is in the foreground, visible to the user.
     */
    val isInForeground: Boolean

    /**
     * Whether the device is a low end one, for which applications may lighten their effects.
     */
    val isLowEndDevice: Boolean

    /**
     * The graphics backends the user can choose from (see
     * [com.reco1l.verktex.settings.VerktexSettings.GraphicsBackend]), none where there's no choice.
     */
    val availableGraphicsBackends: List<GraphicsBackend>

    /**
     * The vibrator of the device, or null if it has none.
     */
    val haptics: Haptics?

    /**
     * Whether [forceMaxRefreshRate] can be set.
     */
    val canForceMaxRefreshRate: Boolean

    /**
     * Whether the display is asked to run at its highest refresh rate rather than the one the system
     * picks. Does nothing unless [canForceMaxRefreshRate].
     */
    var forceMaxRefreshRate: Boolean

    /**
     * Whether [pickFiles] is available.
     */
    val canPickFiles: Boolean

    /**
     * What's done with an exception that a background task didn't catch. By default what the
     * platform does with it, which may end the application (thrown again without a host); a process
     * that must stay alive (e.g. a test browser) can log it instead.
     */
    var onUncaughtException: (Throwable) -> Unit

    /**
     * Whether videos can be played, see [createVideoDecoder].
     */
    val canPlayVideos: Boolean


    /**
     * Opens a web page, in a web view inside the app where the platform has one (Android) or in the
     * system's browser otherwise.
     *
     * @param userAgent The user agent the page is requested with, the platform's default if null. Only
     * the in-app web view can change it.
     */
    fun openWebPage(url: String, userAgent: String? = null)

    /**
     * Opens [url] outside the app, with what the system picks for it: the browser for a web page.
     */
    fun openUrl(url: String)

    /**
     * Creates a new instance of a loop worker, calling [onLoop] on its thread in a loop. The worker
     * waits before calling it if the previous call started less than [minimumInterval] ago, so a
     * zero interval loops as fast as it can.
     */
    fun createLoopWorker(name: String, minimumInterval: Duration = Duration.ZERO, onLoop: () -> Unit): LoopWorker

    /**
     * Runs [block] on the platform's main thread (Android's UI thread), right away if called from it.
     */
    fun runOnMainThread(block: () -> Unit)

    /**
     * Closes the application.
     */
    fun exit()

    /**
     * Copies [text] to the clipboard, under [label] where the platform names its entries.
     */
    fun copyToClipboard(text: String, label: String = "")

    /**
     * Lets the user pick files of [mimeType] (e.g. `application/octet-stream`), and calls [onPicked]
     * with copies of them in the cache, on a background thread, which it may delete. Nothing is picked
     * if the user cancels. Does nothing unless [canPickFiles].
     */
    fun pickFiles(mimeType: String, onPicked: (List<Path>) -> Unit)

    /**
     * Creates a decoder for the video file at [path], or null where videos can't be played.
     */
    fun createVideoDecoder(path: String): VideoDecoder?
}


/**
 * The folders where the application keeps its files, in [Platform.fileSystem].
 */
class PlatformStorage(

    /**
     * The application's private folder, for its settings, databases and other files the user
     * doesn't handle (e.g. Android's `filesDir`, `%APPDATA%/<name>` on Windows).
     */
    val dataDirectory: Path,

    /**
     * A folder for files that can be recreated, which the system may clear.
     */
    val cacheDirectory: Path,

    /**
     * The root of the storage the user sees and handles (Android's external storage, the home
     * folder on desktop), or null when it can't be used right now (e.g. removable storage that
     * isn't mounted).
     */
    val externalDirectory: Path?,
)


/**
 * The application running on the platform.
 */
data class PlatformApplication(

    /**
     * The name of its version, e.g. `1.8.2`.
     */
    val versionName: String = "",

    /**
     * Whether it's a debug build.
     */
    val isDebug: Boolean = false,
)


/**
 * The vibrator of a device.
 */
interface Haptics {

    /**
     * Vibrates for [milliseconds], with an [amplitude] from 1 to 255 where the device can tell
     * them apart.
     */
    fun vibrate(milliseconds: Long, amplitude: Int = 255)

    /**
     * Vibrates following [timings], which alternate off and on durations in milliseconds starting
     * with an off one, at the [amplitudes] of each, from 0 to 255.
     */
    fun vibrate(timings: LongArray, amplitudes: IntArray)

    /**
     * Stops the current vibration.
     */
    fun cancel()
}


/**
 * Represents the configuration of the display.
 */
data class PlatformDisplay(

    /**
     * The density of the display, which is a scaling factor for the density-independent pixel unit.
     */
    val density: Float,

    /**
     * The width of the display in pixels.
     */
    val width: Int = 0,

    /**
     * The height of the display in pixels.
     */
    val height: Int = 0
)


/**
 * The platforms the engine runs on.
 */
enum class PlatformTarget {
    Android,
    Desktop
}
