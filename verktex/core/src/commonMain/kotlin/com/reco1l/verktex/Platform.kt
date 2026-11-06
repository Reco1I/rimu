package com.reco1l.verktex

import kotlin.time.Duration
import kotlin.concurrent.Volatile
import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.io.systemFileSystem
import okio.FileSystem
import okio.Path
import com.reco1l.verktex.worker.LoopWorker
import io.ktor.client.engine.HttpClientEngineFactory
import com.reco1l.verktex.graphics.GraphicsBackend

/**
 * An interface that defines the platform-specific functionality required by the engine.
 */
object Platform {

    lateinit var implementation: Implementation


    /**
     * The provider used to load physical resources from the platform.
     */
    val provider: Provider
        get() {
            checkImplementation()
            return implementation.provider
        }

    /**
     * The milliseconds of the host's uptime clock, the time base of [com.reco1l.verktex.input.PointerEvent.time]
     * (e.g. Android's `SystemClock.uptimeMillis`).
     */
    val uptimeMillis: Long
        get() {
            checkImplementation()
            return implementation.uptimeMillis
        }

    /**
     * The file system of the device. It doesn't need the [implementation], so code that runs
     * without a platform (e.g. tests) can use it too.
     */
    val fileSystem: FileSystem
        get() = systemFileSystem

    /**
     * The folders where the application keeps its files, see [Storage].
     */
    val storage: Storage
        get() {
            checkImplementation()
            return implementation.storage
        }

    /**
     * The driver used to access the audio devices of the platform.
     */
    val audioDriver: AudioDriver
        get() {
            checkImplementation()
            return implementation.audioDriver
        }

    /**
     * How many pixels of the rendered window there are per pixel of the display, below 1 when
     * rendering at a lower resolution. Sizes in density independent pixels follow it, so the UI
     * keeps its size on the screen.
     */
    @Volatile
    var resolutionScale = 1f

    /**
     * The configuration of the current display.
     */
    val currentDisplay: Display
        get() {
            checkImplementation()
            return implementation.currentDisplay
        }

    /**
     * Indicates whether the platform uses a virtual keyboard.
     */
    val usesVirtualKeyboard: Boolean
        get() {
            checkImplementation()
            return implementation.usesVirtualKeyboard
        }

    /**
     * The language the user chose for the system, as a BCP 47 tag such as `pt-BR`. English without
     * a platform, e.g. in tests.
     */
    val systemLocale: String
        get() = if (::implementation.isInitialized) implementation.systemLocale else "en"


    /**
     * Opens a web page, in a web view inside the app where the platform has one (Android) or in the
     * system's browser otherwise.
     *
     * @param userAgent The user agent the page is requested with, the platform's default if null. Only
     * the in-app web view can change it.
     */
    fun openWebPage(url: String, userAgent: String? = null) {
        checkImplementation()
        implementation.openWebPage(url, userAgent)
    }

    /**
     * Opens [url] outside the app, with what the system picks for it: the browser for a web page.
     */
    fun openUrl(url: String) {
        checkImplementation()
        implementation.openUrl(url)
    }

    /**
     * Creates a new instance of a loop worker, see [Implementation.createLoopWorker].
     */
    fun createLoopWorker(name: String, minimumInterval: Duration = Duration.ZERO, onLoop: () -> Unit): LoopWorker {
        checkImplementation()
        return implementation.createLoopWorker(name, minimumInterval, onLoop)
    }


    /**
     * The application running, see [Application].
     */
    val application: Application
        get() = if (::implementation.isInitialized) implementation.application else Application()

    /**
     * Whether the application is in the foreground, visible to the user.
     */
    val isInForeground: Boolean
        get() {
            checkImplementation()
            return implementation.isInForeground
        }

    /**
     * Whether the device is a low end one, for which applications may lighten their effects.
     */
    val isLowEndDevice: Boolean
        get() {
            checkImplementation()
            return implementation.isLowEndDevice
        }

    /**
     * The graphics backends the user can choose from (see
     * [com.reco1l.verktex.settings.VerktexSettings.GraphicsBackend]), none where there's no choice.
     */
    val availableGraphicsBackends: List<GraphicsBackend>
        get() {
            checkImplementation()
            return implementation.availableGraphicsBackends
        }

    /**
     * The vibrator of the device, or null if it has none.
     */
    val haptics: Haptics?
        get() {
            checkImplementation()
            return implementation.haptics
        }

    /**
     * Whether [forceMaxRefreshRate] can be set.
     */
    val canForceMaxRefreshRate: Boolean
        get() {
            checkImplementation()
            return implementation.canForceMaxRefreshRate
        }

    /**
     * Whether the display is asked to run at its highest refresh rate rather than the one the system
     * picks. Does nothing unless [canForceMaxRefreshRate].
     */
    var forceMaxRefreshRate: Boolean = false
        set(value) {
            field = value
            checkImplementation()
            implementation.setForceMaxRefreshRate(value)
        }

    /**
     * Whether [pickFiles] is available.
     */
    val canPickFiles: Boolean
        get() {
            checkImplementation()
            return implementation.canPickFiles
        }

    /**
     * What's done with an exception that a background task didn't catch. By default the platform's
     * [Implementation.handleUncaughtException], which may end the application; a process that must
     * stay alive (e.g. a test browser) can log it instead.
     */
    var onUncaughtException: (Throwable) -> Unit = { throwable ->
        if (::implementation.isInitialized) {
            implementation.handleUncaughtException(throwable)
        } else {
            throw throwable
        }
    }


    /**
     * Runs [block] on the platform's main thread (Android's UI thread), right away if called from it.
     */
    fun runOnMainThread(block: () -> Unit) {
        checkImplementation()
        implementation.runOnMainThread(block)
    }

    /**
     * Closes the application.
     */
    fun exit() {
        checkImplementation()
        implementation.exit()
    }

    /**
     * Copies [text] to the clipboard, under [label] where the platform names its entries.
     */
    fun copyToClipboard(text: String, label: String = "") {
        checkImplementation()
        implementation.copyToClipboard(text, label)
    }

    /**
     * Lets the user pick files of [mimeType] (e.g. `application/octet-stream`), and calls [onPicked]
     * with copies of them in the cache, on a background thread, which it may delete. Nothing is picked
     * if the user cancels. Does nothing unless [canPickFiles].
     */
    fun pickFiles(mimeType: String, onPicked: (List<Path>) -> Unit) {
        checkImplementation()
        implementation.pickFiles(mimeType, onPicked)
    }


    private fun checkImplementation() {
        if (!::implementation.isInitialized) {
            throw IllegalStateException("Platform implementation not initialized")
        }
    }


    interface Implementation {
        /**
         * The source provider used to load physical resources from the platform.
         */
        val provider: Provider

        /**
         * The driver used to access the audio devices of the platform.
         */
        val audioDriver: AudioDriver

        /**
         * The folders where the application keeps its files.
         */
        val storage: Storage

        /**
         * The configuration of the current display.
         */
        val currentDisplay: Display

        /**
         * Indicates whether the platform uses a virtual keyboard.
         */
        val usesVirtualKeyboard: Boolean

        /**
         * The milliseconds of the host's uptime clock, the time base of the input events.
         */
        val uptimeMillis: Long

        /**
         * The language the user chose for the system, as a BCP 47 tag such as `pt-BR`.
         */
        val systemLocale: String
            get() = "en"

        /**
         * The engine HTTP clients make their requests with, see [com.reco1l.verktex.network.Network].
         * None by default, for platforms without networking.
         */
        val httpEngine: HttpClientEngineFactory<*>?
            get() = null

        /**
         * The application running.
         */
        val application: Application
            get() = Application()

        /**
         * Whether the application is in the foreground, visible to the user.
         */
        val isInForeground: Boolean
            get() = true

        /**
         * Whether the device is a low end one.
         */
        val isLowEndDevice: Boolean
            get() = false

        /**
         * The graphics backends the user can choose from, none where there's no choice.
         */
        val availableGraphicsBackends: List<GraphicsBackend>
            get() = emptyList()

        /**
         * The vibrator of the device, or null if it has none.
         */
        val haptics: Haptics?
            get() = null

        /**
         * Whether [setForceMaxRefreshRate] is supported.
         */
        val canForceMaxRefreshRate: Boolean
            get() = false

        /**
         * Asks the display to run at its highest refresh rate, or at the one the system picks.
         */
        fun setForceMaxRefreshRate(enabled: Boolean) = Unit

        /**
         * Whether [pickFiles] is supported.
         */
        val canPickFiles: Boolean
            get() = false

        /**
         * Lets the user pick files, see [Platform.pickFiles].
         */
        fun pickFiles(mimeType: String, onPicked: (List<Path>) -> Unit) = Unit

        /**
         * Handles an exception that a background task didn't catch. Logged by default.
         */
        fun handleUncaughtException(throwable: Throwable) {
            Logger.e("Platform", "Uncaught exception in a task", throwable)
        }

        /**
         * Runs [block] on the platform's main thread, right away if called from it. Right away by
         * default, for platforms whose main thread is any.
         */
        fun runOnMainThread(block: () -> Unit) = block()

        /**
         * Closes the application. Does nothing by default.
         */
        fun exit() = Unit

        /**
         * Copies [text] to the clipboard. Does nothing by default.
         */
        fun copyToClipboard(text: String, label: String) = Unit


        /**
         * Creates a new instance of a loop worker, calling [onLoop] on its thread in a loop. The
         * worker waits before calling it if the previous call started less than [minimumInterval]
         * ago, so a zero interval loops as fast as it can.
         */
        fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker

        /**
         * Opens a web page, see [Platform.openWebPage].
         */
        fun openWebPage(url: String, userAgent: String?)

        /**
         * Opens a URL outside the app, see [Platform.openUrl].
         */
        fun openUrl(url: String)

    }


    /**
     * The folders where the application keeps its files, in [fileSystem].
     */
    class Storage(

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
    data class Application(

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
    data class Display(

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
}