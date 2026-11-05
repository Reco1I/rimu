package com.reco1l.verktex.desktop.bass

import com.reco1l.verktex.Logger
import com.reco1l.verktex.audio.AudioDevice
import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.desktop.bass.BassNative as BASS

/**
 * The BASS audio driver of the desktop platform, adapted from the Android one.
 */
class BASSAudioDriver : AudioDriver() {

    init {
        // Strings (device names) in UTF-8 on Windows too.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UNICODE, 1)

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 100)

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 5)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_NONSTOP, 1)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_TIMEOUT, 0)
    }


    override fun dispose() {
        super.dispose()
        BASS.BASS_Free()
    }

    override fun onEnumerateDevices(): List<AudioDevice> {
        // Device 0 is "no sound", so devices start from 1.
        // https://www.un4seen.com/doc/#bass/BASS_GetDeviceInfo.html
        var deviceIndex = 1

        val info = BASS.DeviceInfo()
        val devices = mutableListOf<BASSAudioDevice>()

        while (BASS.BASS_GetDeviceInfo(deviceIndex, info)) {
            devices += BASSAudioDevice(
                id = deviceIndex,
                name = info.name?.getString(0, "UTF-8") ?: "Device $deviceIndex",
                flags = info.flags
            )
            deviceIndex++
        }

        if (devices.isEmpty()) {
            Logger.w("BASSAudioDriver", "No audio output device found.")
        }

        return devices
    }
}

/**
 * An audio device of BASS.
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

        if (!BASS.BASS_Init(id, 44100, 0, null, null)) {
            Logger.w("BASSAudioDevice", "Couldn't initialize device $id ($name), error code: ${BASS.BASS_ErrorGetCode()}")
        }
    }
}
