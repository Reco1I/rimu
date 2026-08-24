package com.reco1l.verktex.audio

/**
 * Represents an audio device that can be used to play audio.
 */
abstract class AudioDevice {

    /**
     * Whether the device has been initialized.
     */
    var isInitialized = false
        private set


    /**
     * The name of the audio device.
     */
    abstract val name: String

    /**
     * Whether the device is the default one.
     */
    abstract val isDefault: Boolean


    /**
     * Called when the device is no longer used as active device.
     *
     * @see AudioDriver.activeDevice
     */
    open fun onDeactivate() {}

    /**
     * Called when the device is made active.
     *
     * @see AudioDriver.activeDevice
     */
    open fun onActivate() {}


    /**
     * Called when the device is initialized.
     */
    protected abstract fun onInitialize()


    /**
     * Initializes the device.
     */
    fun initialize() {
        if (isInitialized) return
        onInitialize()
        isInitialized = true
    }


}