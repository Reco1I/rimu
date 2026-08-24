package com.reco1l.verktex.android.bass

import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.audio.AudioDevice
import com.un4seen.bass.BASS

class BASSAudioDriver : AudioDriver() {

    init {
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 100)

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 5)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_NONSTOP, 1)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_TIMEOUT, 0)
    }


    override fun onEnumerateDevices(): List<AudioDevice> {
        // Device ID 0 is "no sound" so we start from 1.
        // https://www.un4seen.com/doc/#bass/BASS_GetDeviceInfo.html
        var deviceIndex = 1

        val deviceInfo = BASS.BASS_DEVICEINFO()
        val devices = mutableListOf<BASSAudioDevice>()

        while (BASS.BASS_GetDeviceInfo(deviceIndex, deviceInfo)) {
            val device = BASSAudioDevice(
                id = deviceIndex,
                name = deviceInfo.name,
                flags = deviceInfo.flags
            )
            devices.add(device)
            deviceIndex++
        }

        return devices
    }

}