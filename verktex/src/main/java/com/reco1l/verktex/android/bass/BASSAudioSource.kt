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

        var channelId: Int

        if (isExternal) {
            channelId = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_PRESCAN or BASS.BASS_STREAM_DECODE)
            channelId = BASS_FX.BASS_FX_TempoCreate(channelId, BASS.BASS_STREAM_AUTOFREE)
        } else {
            val asset = BASS.Asset(context.assets, filePath)
            channelId = BASS.BASS_StreamCreateFile(asset, 0, 0, BASS.BASS_STREAM_PRESCAN)
            channelId = BASS_FX.BASS_FX_TempoCreate(channelId, BASS.BASS_STREAM_AUTOFREE)
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
        return BASSAudioSample(sampleId)
    }
}

