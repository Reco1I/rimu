package com.reco1l.verktex.android.bass

import android.content.Context
import com.reco1l.verktex.audio.AudioSample
import com.reco1l.verktex.audio.AudioStream
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX


class BASSAudioStreamFileSource(
    val context: Context,
    val filePath: String,
    val isExternal: Boolean
) : AudioStream.Source {

    override fun createStream(): AudioStream {

        // The tempo stream processes a decoding channel, which must not be played on its own.
        val flags = BASS.BASS_STREAM_PRESCAN or BASS.BASS_STREAM_DECODE

        val decodingChannel = if (isExternal) {
            BASS.BASS_StreamCreateFile(filePath, 0, 0, flags)
        } else {
            BASS.BASS_StreamCreateFile(BASS.Asset(context.assets, filePath), 0, 0, flags)
        }

        val channelId = BASS_FX.BASS_FX_TempoCreate(decodingChannel, BASS.BASS_STREAM_AUTOFREE)

        if (channelId == 0) {
            throw IllegalStateException("Failed to create a stream from $filePath, error code: ${BASS.BASS_ErrorGetCode()}")
        }

        BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_BUFFER, 0.1f)

        return BASSAudioStream(channelId)
    }
}

class BASSAudioStreamUrlSource(val url: String) : AudioStream.Source {

    override fun createStream(): AudioStream {
        val channelId = BASS.BASS_StreamCreateURL(url, 0, BASS.BASS_STREAM_AUTOFREE, null, null)

        if (channelId == 0) {
            throw IllegalStateException("Failed to create a stream from $url, error code: ${BASS.BASS_ErrorGetCode()}")
        }

        return BASSAudioStream(channelId)
    }
}

class BASSAudioSampleFileSource(
    val context: Context,
    val filePath: String,
    val isExternal: Boolean
) : AudioSample.Source {
    override fun createSample(): AudioSample {
        var sampleId: Int

        if (isExternal) {
            sampleId = BASS.BASS_SampleLoad(filePath, 0, 0, 1, BASS.BASS_SAMPLE_OVER_POS)
        } else {
            val asset = BASS.Asset(context.assets, filePath)
            sampleId = BASS.BASS_SampleLoad(asset, 0, 0, 1, BASS.BASS_SAMPLE_OVER_POS)
        }

        if (sampleId == 0) {
            throw IllegalStateException("Failed to load a sample from $filePath, error code: ${BASS.BASS_ErrorGetCode()}")
        }

        return BASSAudioSample(sampleId)
    }
}

