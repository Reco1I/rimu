package com.reco1l.verktex

import kotlin.time.Duration
import kotlin.concurrent.Volatile
import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.worker.LoopWorker

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
     * Creates a new instance of a loop worker, see [Implementation.createLoopWorker].
     */
    fun createLoopWorker(name: String, minimumInterval: Duration = Duration.ZERO, onLoop: () -> Unit): LoopWorker {
        checkImplementation()
        return implementation.createLoopWorker(name, minimumInterval, onLoop)
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
         * Creates a new instance of a loop worker, calling [onLoop] on its thread in a loop. The
         * worker waits before calling it if the previous call started less than [minimumInterval]
         * ago, so a zero interval loops as fast as it can.
         */
        fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker

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