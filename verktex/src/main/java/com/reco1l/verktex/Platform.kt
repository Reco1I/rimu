package com.reco1l.verktex

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
     * Creates a new instance of a loop worker.
     */
    fun createLoopWorker(name: String, onLoop: () -> Unit): LoopWorker {
        checkImplementation()
        return implementation.createLoopWorker(name, onLoop)
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
         * The configuration of the current display.
         */
        val currentDisplay: Display

        /**
         * Indicates whether the platform uses a virtual keyboard.
         */
        val usesVirtualKeyboard: Boolean


        /**
         * Creates a new instance of a loop worker.
         */
        fun createLoopWorker(name: String, onLoop: () -> Unit): LoopWorker

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