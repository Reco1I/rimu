package com.reco1l.verktex.android.bass

import com.reco1l.verktex.Logger
import com.reco1l.verktex.audio.AudioDevice
import com.un4seen.bass.BASS

/**
 * An audio device based on BASS.
 */
class BASSAudioDevice(

    /**
     * The device ID used by BASS.
     */
    val id: Int,

    /**
     * The flags used by BASS.
     */
    val flags: Int,

    override val name: String

) : AudioDevice() {

    override val isDefault: Boolean
        get() = flags and BASS.BASS_DEVICE_DEFAULT != 0


    override fun onInitialize() {
        if (flags and BASS.BASS_DEVICE_INIT != 0) {
            Logger.w("BASSAudioDevice", "Device $id ($name) is already initialized!")
            return
        }

        BASS.BASS_Init(id, 44100, 0)
    }
}