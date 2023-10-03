package com.reco1l.bassbinding.stream

import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_STREAM_AUTOFREE
import com.un4seen.bass.BASS.BASS_STREAM_DECODE
import com.un4seen.bass.BASS.BASS_STREAM_PRESCAN
import com.un4seen.bass.BASS_FX

class AudioStream(

    source: String? = null,

    /**
     * Define the stream flags, by default [BASS_STREAM_DECODE] and [BASS_STREAM_PRESCAN].
     */
    var flags: Int = BASS_STREAM_DECODE or BASS_STREAM_PRESCAN,

    /**
     * Define the FX flags, by default [BASS_STREAM_AUTOFREE].
     */
    var fxFlags: Int = BASS_STREAM_AUTOFREE

) : BaseStream(source)
{
    override fun onSourceLoad(path: String): Int
    {
        val id = BASS.BASS_StreamCreateFile(path, 0, 0, flags)

        return BASS_FX.BASS_FX_TempoCreate(id, fxFlags)
    }
}