package com.reco1l.verktex.desktop

import com.reco1l.verktex.desktop.bass.BassFxNative
import com.reco1l.verktex.desktop.bass.BassNative as BASS
import com.sun.jna.Memory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * The bundled BASS libraries load and work, on the "no sound" device so no audio hardware is needed.
 */
class BassTest {

    @Test
    fun loadsASampleFromMemory() {
        val wav = wav(seconds = 0.5)
        val data = Memory(wav.size.toLong()).apply { write(0, wav, 0, wav.size) }

        val sample = BASS.BASS_SampleLoad(true, data, 0, wav.size, 1, 0)
        assertNotEquals("BASS error ${BASS.BASS_ErrorGetCode()}", 0, sample)

        val channel = BASS.BASS_SampleGetChannel(sample, BASS.BASS_SAMCHAN_STREAM)
        assertNotEquals("BASS error ${BASS.BASS_ErrorGetCode()}", 0, channel)

        val length = BASS.BASS_ChannelBytes2Seconds(channel, BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE))
        assertEquals(0.5, length, 0.01)

        val frequency = FloatArray(1)
        assertTrue(BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_FREQ, frequency))
        assertEquals(RATE.toFloat(), frequency[0], 0f)

        assertTrue(BASS.BASS_SampleFree(sample))
    }

    @Test
    fun createsATempoStreamFromMemory() {
        val wav = wav(seconds = 1.0)
        val data = Memory(wav.size.toLong()).apply { write(0, wav, 0, wav.size) }

        val decoding = BASS.BASS_StreamCreateFile(true, data, 0, wav.size.toLong(), BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN)
        assertNotEquals("BASS error ${BASS.BASS_ErrorGetCode()}", 0, decoding)

        val tempo = BassFxNative.BASS_FX_TempoCreate(decoding, 0)
        assertNotEquals("BASS_FX error ${BASS.BASS_ErrorGetCode()}", 0, tempo)

        assertTrue(BASS.BASS_ChannelSetAttribute(tempo, BassFxNative.BASS_ATTRIB_TEMPO, 50f))
        assertTrue(BASS.BASS_ChannelFree(tempo))
    }


    companion object {
        private const val RATE = 44100

        @BeforeClass
        @JvmStatic
        fun initialize() {
            // Device 0 is "no sound".
            assertTrue("BASS error ${BASS.BASS_ErrorGetCode()}", BASS.BASS_Init(0, RATE, 0, null, null))
        }

        @AfterClass
        @JvmStatic
        fun free() {
            BASS.BASS_Free()
        }

        // A mono 16-bit PCM WAV of a 440 Hz sine.
        private fun wav(seconds: Double): ByteArray {
            val samples = (RATE * seconds).toInt()
            val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)

            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVE".toByteArray())
            buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
            buffer.put("data".toByteArray()).putInt(samples * 2)

            for (i in 0 until samples) {
                buffer.putShort((Math.sin(2 * Math.PI * 440 * i / RATE) * 8000).toInt().toShort())
            }
            return buffer.array()
        }
    }
}
