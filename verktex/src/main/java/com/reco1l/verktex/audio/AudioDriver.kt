package com.reco1l.verktex.audio

import com.reco1l.verktex.Logger

/**
 * Represents an audio driver that can be used to play audio.
 */
abstract class AudioDriver {

    /**
     * List of all available audio devices.
     */
    var devices: List<AudioDevice> = listOf()
        private set

    /**
     * The currently active [AudioDevice] for playback.
     */
    var activeDevice: AudioDevice? = null
        set(value) {
            if (field == value) return
            field?.onDeactivate()
            field = value

            if (value != null) {
                if (!value.isInitialized) {
                    value.initialize()
                }
                value.onActivate()
            }
            onDeviceChanged()
        }


    /**
     * Called when the active device has changed.
     */
    protected open fun onDeviceChanged() {}


    /**
     * Enumerates all available audio devices.
     */
    fun enumerateDevices(): List<AudioDevice> {
        devices = onEnumerateDevices()
        if (devices.isEmpty()) {
            Logger.w("AudioBackend", "No audio devices found! Working without sound.")
        }

        activeDevice = devices.firstOrNull()
        return devices
    }


    /**
     * Called when devices are enumerated after calling [enumerateDevices].
     */
    abstract fun onEnumerateDevices(): List<AudioDevice>

}