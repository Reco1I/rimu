package com.reco1l.bassbinding

import com.reco1l.bassbinding.exceptions.InvalidBassDevice
import com.un4seen.bass.BASS.BASS_CONFIG_BUFFER
import com.un4seen.bass.BASS.BASS_CONFIG_DEV_BUFFER
import com.un4seen.bass.BASS.BASS_CONFIG_DEV_NONSTOP
import com.un4seen.bass.BASS.BASS_CONFIG_DEV_PERIOD
import com.un4seen.bass.BASS.BASS_CONFIG_UPDATEPERIOD
import com.un4seen.bass.BASS.BASS_DEVICE_LATENCY
import com.un4seen.bass.BASS.BASS_DEVICE_REINIT
import com.un4seen.bass.BASS.BASS_Free
import com.un4seen.bass.BASS.BASS_GetDevice
import com.un4seen.bass.BASS.BASS_Init
import com.un4seen.bass.BASS.BASS_SetConfig

/**
 * The BASS device.
 *
 * @author Rian3887
 * @author Reco1l
 */
class BassDevice
{

    /**
     * The device ID.
     */
    var id: Int = -1
        private set

    /**
     * BASS uses a different configuration in background make sure of mark this at the proper state.
     */
    var isBackgroundState = false
        set(value)
        {
            if (value)
                onBackgroundState()
            else
                onForegroundState()

            field = value
        }


    init
    {
        // Initializing in foreground state
        onForegroundState()

        id = BASS_GetDevice()

        if (id == -1)
            throw InvalidBassDevice()
    }


    private fun onBackgroundState() = synchronized(this)
    {
        isBackgroundState = true

        // Reset BASS configurations to their default values.
        // https://www.un4seen.com/doc/#bass/BASS_CONFIG_UPDATEPERIOD.html
        BASS_SetConfig(BASS_CONFIG_UPDATEPERIOD, 100)

        // https://www.un4seen.com/doc/#bass/BASS_CONFIG_DEV_PERIOD.html
        BASS_SetConfig(BASS_CONFIG_DEV_PERIOD, 10)

        // https://www.un4seen.com/doc/#bass/BASS_CONFIG_BUFFER.html
        BASS_SetConfig(BASS_CONFIG_BUFFER, 500)

        // https://www.un4seen.com/doc/#bass/BASS_CONFIG_DEV_NONSTOP.html
        BASS_SetConfig(BASS_CONFIG_DEV_NONSTOP, 0)

        // This is needed for background player so that the music does not get choppy.
        BASS_SetConfig(BASS_CONFIG_DEV_BUFFER, 0)

        // Reinitialize BASS under the current configuration.
        BASS_Init(BASS_GetDevice(), DEFAULT_FREQUENCY, BASS_DEVICE_REINIT)
    }

    private fun onForegroundState() = synchronized(this)
    {
        isBackgroundState = false

        // Initialize BASS if it's not initialized already. This allows us to get the device ID for reinitialization later.
        BASS_Init(-1, DEFAULT_FREQUENCY, BASS_DEVICE_LATENCY)

        // This likely doesn't help, but also doesn't seem to cause any issues or any CPU increase.
        BASS_SetConfig(BASS_CONFIG_UPDATEPERIOD, 5)

        // Reduce latency to a known sane minimum.
        BASS_SetConfig(BASS_CONFIG_DEV_PERIOD, 5)
        BASS_SetConfig(BASS_CONFIG_DEV_BUFFER, 10)
        BASS_SetConfig(BASS_CONFIG_BUFFER, 100)

        // Ensure there are no brief delays on audio operations (causing stream stalls etc.) after periods of silence.
        BASS_SetConfig(BASS_CONFIG_DEV_NONSTOP, 1)

        // Reinitialize BASS under the current configuration.
        BASS_Init(BASS_GetDevice(), DEFAULT_FREQUENCY, BASS_DEVICE_REINIT)
    }


    /**
     * Binding for [BASS_Free].
     */
    fun free() = BASS_Free()


    companion object
    {
        const val DEFAULT_FREQUENCY = 44100
    }
}