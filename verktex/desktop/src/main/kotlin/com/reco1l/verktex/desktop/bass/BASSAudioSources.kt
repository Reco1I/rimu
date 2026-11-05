package com.reco1l.verktex.desktop.bass

import com.reco1l.verktex.audio.AudioSample
import com.reco1l.verktex.audio.AudioStream
import com.reco1l.verktex.desktop.DesktopProvider
import com.reco1l.verktex.desktop.bass.BassNative as BASS
import com.sun.jna.Memory

// External files are opened by BASS by path, internal ones (classpath resources) are read into
// memory first, as Android's are opened from its assets.

private fun readIntoMemory(provider: DesktopProvider, path: String): Memory {
    val bytes = provider.read(path, isExternal = false)
        ?: throw IllegalStateException("There's no resource $path")

    return Memory(bytes.size.toLong()).apply { write(0, bytes, 0, bytes.size) }
}

class BASSAudioStreamSource(
    private val provider: DesktopProvider,
    val filePath: String,
    val isExternal: Boolean
) : AudioStream.Source {

    override fun createStream(): AudioStream {
        // The tempo stream processes a decoding channel, which must not be played on its own.
        val flags = BASS.BASS_STREAM_PRESCAN or BASS.BASS_STREAM_DECODE

        var data: Memory? = null
        val decodingChannel = if (isExternal) {
            val (path, encoding) = BASS.path(filePath)
            BASS.BASS_StreamCreateFile(false, path, 0, 0, flags or encoding)
        } else {
            data = readIntoMemory(provider, filePath)
            BASS.BASS_StreamCreateFile(true, data, 0, data.size(), flags)
        }

        val channelId = BassFxNative.BASS_FX_TempoCreate(decodingChannel, BASS.BASS_STREAM_AUTOFREE)

        if (channelId == 0) {
            throw IllegalStateException("Failed to create a stream from $filePath, error code: ${BASS.BASS_ErrorGetCode()}")
        }

        BASS.BASS_ChannelSetAttribute(channelId, BASS.BASS_ATTRIB_BUFFER, 0.1f)

        return BASSAudioStream(channelId, data)
    }
}

class BASSAudioStreamUrlSource(val url: String) : AudioStream.Source {

    override fun createStream(): AudioStream {
        val channelId = BASS.BASS_StreamCreateURL(BASS.utf8(url), 0, BASS.BASS_STREAM_AUTOFREE, null, null)

        if (channelId == 0) {
            throw IllegalStateException("Failed to create a stream from $url, error code: ${BASS.BASS_ErrorGetCode()}")
        }

        return BASSAudioStream(channelId)
    }
}

class BASSAudioSampleSource(
    private val provider: DesktopProvider,
    val filePath: String,
    val isExternal: Boolean
) : AudioSample.Source {

    override fun createSample(): AudioSample {
        val sampleId = if (isExternal) {
            val (path, encoding) = BASS.path(filePath)
            BASS.BASS_SampleLoad(false, path, 0, 0, 1, BASS.BASS_SAMPLE_OVER_POS or encoding)
        } else {
            // BASS copies the sample's data, the memory can go once it's loaded.
            val data = readIntoMemory(provider, filePath)
            BASS.BASS_SampleLoad(true, data, 0, data.size().toInt(), 1, BASS.BASS_SAMPLE_OVER_POS)
        }

        if (sampleId == 0) {
            throw IllegalStateException("Failed to load a sample from $filePath, error code: ${BASS.BASS_ErrorGetCode()}")
        }

        return BASSAudioSample(sampleId)
    }
}
