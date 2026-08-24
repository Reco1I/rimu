package com.reco1l.verktex.android.bass

import com.reco1l.verktex.Logger
import com.reco1l.verktex.audio.AudioPlayback
import com.reco1l.verktex.audio.AudioSample
import com.un4seen.bass.BASS

class BASSAudioSample(
    val sampleId: Int
) : AudioSample() {

    private val baseFrequency: Int


    init {
        BASS.BASS_SampleGetInfo(sampleId, SAMPLE_INFO_FETCHER)
        baseFrequency = SAMPLE_INFO_FETCHER.freq
    }


    override fun createPlayback(): AudioPlayback {
        val channelId = BASS.BASS_SampleGetChannel(sampleId, BASS.BASS_SAMCHAN_STREAM)
        return Playback(channelId)
    }

    override fun dispose() {
        BASS.BASS_SampleFree(sampleId)
    }


    inner class Playback(val channelId: Int) : AudioPlayback {

        override val isPlaying: Boolean
            get() = BASS.BASS_ChannelIsActive(channelId) == BASS.BASS_ACTIVE_PLAYING

        override val length: Double
            get() {
                val length = BASS.BASS_ChannelGetLength(channelId, BASS.BASS_POS_BYTE)
                if (length == -1L) return 0.0
                return BASS.BASS_ChannelBytes2Seconds(channelId, length)
            }


        //region Settings

        override var position: Double
            get() {
                val pos = BASS.BASS_ChannelGetPosition(channelId, BASS.BASS_POS_BYTE)
                if (pos == -1L) return 0.0

                return BASS.BASS_ChannelBytes2Seconds(channelId, pos) * 1000
            }
            set(value) {
                if (value < 0.0) throw IllegalArgumentException("Position cannot be negative")

                val pos = BASS.BASS_ChannelSeconds2Bytes(channelId, value / 1000)
                BASS.BASS_ChannelSetPosition(channelId, pos, BASS.BASS_POS_BYTE)
            }

        override var volume: Float
            get() {
                BASS.BASS_ChannelGetAttribute(channelId, BASS.BASS_ATTRIB_VOL, FLOAT_VALUE_FETCHER)
                return FLOAT_VALUE_FETCHER.value
            }
            set(value) {
                BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_VOL, value)
            }

        override var pitch = 1f
            set(value) {
                if (field == value) return
                field = value
                updateFrequency()
            }

        override var looping = false
            set(value) {
                if (field == value) return
                field = value

                BASS.BASS_ChannelFlags(channelId, if (value) BASS.BASS_SAMPLE_LOOP else 0, BASS.BASS_SAMPLE_LOOP)
            }

        //region Unsupported

        override var speed = 1f
            @Suppress("SetterBackingFieldAssignment")
            set(value) {
                Logger.w("BASSAudioSample", "Due to limitations, speed is always 1 for BASS audio samples")
            }

        override var adjustPitch = true
            @Suppress("SetterBackingFieldAssignment")
            set(value) {
                Logger.w("BASSAudioSample", "Due to limitations, adjustPitch is always true for BASS audio samples")
            }

        //endregion

        //endregion


        private fun updateFrequency() {
            var frequency = baseFrequency * pitch

            if (adjustPitch) {
                frequency *= speed
            }

            BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_FREQ, baseFrequency * frequency)
        }


        //region Controls

        override fun play() {
            if (isPlaying) return
            BASS.BASS_ChannelPlay(channelId, false)
        }

        override fun pause() {
            if (!isPlaying) return
            BASS.BASS_ChannelPause(channelId)
        }

        override fun stop() {
            BASS.BASS_ChannelStop(channelId)
        }

        //endregion


    }


    companion object {
        private val SAMPLE_INFO_FETCHER = BASS.BASS_SAMPLE()
        private val FLOAT_VALUE_FETCHER = BASS.FloatValue()
    }
}