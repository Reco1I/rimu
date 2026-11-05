package com.reco1l.verktex.desktop.bass

import com.reco1l.verktex.Logger
import com.reco1l.verktex.audio.AudioPlayback
import com.reco1l.verktex.audio.AudioSample
import com.reco1l.verktex.desktop.bass.BassNative as BASS

/**
 * A BASS sample, adapted from the Android one.
 */
class BASSAudioSample(val sampleId: Int) : AudioSample() {

    override fun createPlayback(): AudioPlayback {
        val channelId = BASS.BASS_SampleGetChannel(sampleId, BASS.BASS_SAMCHAN_STREAM)
        BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_NOBUFFER, 1f)
        return Playback(channelId)
    }

    override fun dispose() {
        BASS.BASS_SampleFree(sampleId)
    }


    inner class Playback(val channelId: Int) : AudioPlayback {

        // The channel's frequency when created is the sample's, read instead of BASS_SAMPLE.
        private val baseFrequency = readAttribute(channelId, BASS.BASS_ATTRIB_FREQ)

        override val isPlaying: Boolean
            get() = BASS.BASS_ChannelIsActive(channelId) == BASS.BASS_ACTIVE_PLAYING

        override val isPaused: Boolean
            get() = BASS.BASS_ChannelIsActive(channelId) == BASS.BASS_ACTIVE_PAUSED

        override val isStalled: Boolean
            get() = BASS.BASS_ChannelIsActive(channelId) == BASS.BASS_ACTIVE_STALLED

        override val length: Double
            get() {
                val length = BASS.BASS_ChannelGetLength(channelId, BASS.BASS_POS_BYTE)
                if (length == -1L) return 0.0
                return BASS.BASS_ChannelBytes2Seconds(channelId, length)
            }

        override var position: Double
            get() {
                val pos = BASS.BASS_ChannelGetPosition(channelId, BASS.BASS_POS_BYTE)
                if (pos == -1L) return 0.0
                return BASS.BASS_ChannelBytes2Seconds(channelId, pos)
            }
            set(value) {
                if (value < 0.0) throw IllegalArgumentException("Position cannot be negative")
                BASS.BASS_ChannelSetPosition(channelId, BASS.BASS_ChannelSeconds2Bytes(channelId, value), BASS.BASS_POS_BYTE)
            }

        override var volume: Float
            get() = readAttribute(channelId, BASS.BASS_ATTRIB_VOL)
            set(value) {
                BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_VOL, value)
            }

        override var pitch = 1f
            set(value) {
                if (field == value) return
                field = value
                BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_FREQ, baseFrequency * pitch * speed)
            }

        override var looping = false
            set(value) {
                if (field == value) return
                field = value
                BASS.BASS_ChannelFlags(channelId, if (value) BASS.BASS_SAMPLE_LOOP else 0, BASS.BASS_SAMPLE_LOOP)
            }

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

        override fun play(restart: Boolean) {
            if (isPlaying && !restart) return
            BASS.BASS_ChannelPlay(channelId, restart)
        }

        override fun pause() {
            if (!isPlaying) return
            BASS.BASS_ChannelPause(channelId)
        }

        override fun stop() {
            BASS.BASS_ChannelStop(channelId)
        }
    }
}

internal fun readAttribute(channel: Int, attribute: Int): Float {
    val value = FloatArray(1)
    BASS.BASS_ChannelGetAttribute(channel, attribute, value)
    return value[0]
}
