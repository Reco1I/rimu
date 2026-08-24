package com.reco1l.verktex.android.bass

import com.reco1l.verktex.audio.AudioStream
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX

class BASSAudioStream(
    /**
     * The channel ID of the audio stream.
     */
    val id: Int
) : AudioStream() {

    override val isPlaying: Boolean
        get() = BASS.BASS_ChannelIsActive(id) == BASS.BASS_ACTIVE_PLAYING

    override val length: Double
        get() {
            val length = BASS.BASS_ChannelGetLength(id, BASS.BASS_POS_BYTE)
            if (length == -1L) return 0.0
            return BASS.BASS_ChannelBytes2Seconds(id, length)
        }


    override var position: Double
        get() {
            val pos = BASS.BASS_ChannelGetPosition(id, BASS.BASS_POS_BYTE)
            if (pos == -1L) return 0.0

            return BASS.BASS_ChannelBytes2Seconds(id, pos) * 1000
        }
        set(value) {
            if (value < 0.0) throw IllegalArgumentException("Position cannot be negative")

            val pos = BASS.BASS_ChannelSeconds2Bytes(id, value / 1000)
            BASS.BASS_ChannelSetPosition(id, pos, BASS.BASS_POS_BYTE)
        }

    override var volume: Float
        get() {
            BASS.BASS_ChannelGetAttribute(id, BASS.BASS_ATTRIB_VOL, FLOAT_VALUE_FETCHER)
            return FLOAT_VALUE_FETCHER.value
        }
        set(value) {
            BASS.BASS_ChannelSetAttribute(id, BASS.BASS_ATTRIB_VOL, value)
        }

    override var pitch = 1f
        set(value) {
            if (field == value) return
            field = value
            updateFrequency()
        }

    override var adjustPitch = false
        set(value) {
            if (field == value) return
            field = value
            updateFrequency()
        }

    override var speed = 1f
        set(value) {
            if (field == value) return
            field = value
            updateFrequency()
        }


    override var looping: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}


    private val baseFrequency: Int


    init {
        BASS.BASS_ChannelGetInfo(id, CHANNEL_INFO_FETCHER)
        baseFrequency = CHANNEL_INFO_FETCHER.freq
    }


    private fun updateFrequency() {
        var frequency = baseFrequency * pitch

        if (adjustPitch) {
            frequency *= speed
        }

        BASS.BASS_ChannelSetAttribute(id, BASS_FX.BASS_ATTRIB_TEMPO_FREQ, frequency)
        BASS.BASS_ChannelSetAttribute(id, BASS_FX.BASS_ATTRIB_TEMPO, if (adjustPitch) 0f else (speed - 1f) * 100f)
    }


    //region Controls

    override fun play() {
        if (isPlaying) return
        BASS.BASS_ChannelPlay(id, false)
    }

    override fun pause() {
        if (!isPlaying) return
        BASS.BASS_ChannelPause(id)
    }

    override fun stop() {
        BASS.BASS_ChannelStop(id)
    }

    override fun dispose() {
        BASS.BASS_ChannelStop(id)
        BASS.BASS_ChannelFree(id)
    }

    //endregion


    companion object {
        private val CHANNEL_INFO_FETCHER = BASS.BASS_CHANNELINFO()
        private val FLOAT_VALUE_FETCHER = BASS.FloatValue()
    }

}