package com.reco1l.basskt

import com.reco1l.framework.android.logE
import com.reco1l.framework.android.logI
import com.reco1l.framework.lang.klass
import com.un4seen.bass.BASS.BASS_CONFIG_DEV_BUFFER
import com.un4seen.bass.BASS.BASS_CONFIG_DEV_NONSTOP
import com.un4seen.bass.BASS.BASS_CONFIG_DEV_PERIOD
import com.un4seen.bass.BASS.BASS_CONFIG_UPDATEPERIOD
import com.un4seen.bass.BASS.BASS_DEVICE_LATENCY
import com.un4seen.bass.BASS.BASS_Free
import com.un4seen.bass.BASS.BASS_GetConfig
import com.un4seen.bass.BASS.BASS_GetDevice
import com.un4seen.bass.BASS.BASS_Init
import com.un4seen.bass.BASS.BASS_SetConfig
import com.un4seen.bass.BASS.BASS_Start
import com.un4seen.bass.BASS.BASS_Stop

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
    val id
        get() = BASS_GetDevice()

    /**
     * The device update period.
     */
    var updatePeriod
        get() = getConfig(BASS_CONFIG_UPDATEPERIOD)
        set(value) { setConfig(BASS_CONFIG_UPDATEPERIOD, value) }


    init
    {
        updatePeriod = 5

        setConfig(BASS_CONFIG_DEV_PERIOD, 5)
        setConfig(BASS_CONFIG_DEV_BUFFER, 10)
        setConfig(BASS_CONFIG_DEV_NONSTOP, 1)

        if (!BASS_Init(-1, DEFAULT_FREQUENCY, BASS_DEVICE_LATENCY))
            throw InvalidBassDevice()

        klass logI "Initialized with ID: $id"
    }


    fun free() = BASS_Free().also {

        if (!it)
            klass logE "Failed to free device: ${BASS_ErrorGetName()}"
    }

    fun start() = BASS_Start().also {

        if (!it)
            klass logE "Failed to start device: ${BASS_ErrorGetName()}"
    }

    fun stop() = BASS_Stop().also {

        if (!it)
            klass logE "Failed to stop device: ${BASS_ErrorGetName()}"
    }


    /**
     * Binding for [BASS_SetConfig].
     */
    fun setConfig(option: Int, value: Int) = BASS_SetConfig(option, value).also {

        if (!it)
            klass logE "Failed to set ${BASS_GetConfigName(option)} to \"$value\": ${BASS_ErrorGetName()}"
    }

    /**
     * Binding for [BASS_GetConfig].
     */
    fun getConfig(option: Int) = BASS_GetConfig(option)


    companion object
    {
        /**
         * The default frequency.
         */
        const val DEFAULT_FREQUENCY = 44100
    }
}

class InvalidBassDevice : Exception("Failed to initialize BassDevice: ${BASS_ErrorGetName()}")