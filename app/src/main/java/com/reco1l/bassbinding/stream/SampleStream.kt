package com.reco1l.bassbinding.stream

import com.un4seen.bass.BASS.BASS_SAMPLE_OVER_POS
import com.un4seen.bass.BASS.BASS_STREAM_AUTOFREE
import com.un4seen.bass.BASS.BASS_SampleFree
import com.un4seen.bass.BASS.BASS_SampleGetChannel
import com.un4seen.bass.BASS.BASS_SampleLoad
import com.un4seen.bass.BASS.BASS_StreamFree
import com.un4seen.bass.BASS_FX

open class SampleStream(

    source: String? = null,

    /**
     * Define the sample flags, by default [BASS_SAMPLE_OVER_POS].
     */
    var flags: Int = BASS_SAMPLE_OVER_POS,

    /**
     * Define the FX flags, by default [BASS_STREAM_AUTOFREE].
     */
    var fxFlags: Int = BASS_STREAM_AUTOFREE,

    /**
     * The max amount of simultaneous playbacks of the same sample, by default `8`.
     */
    var simultaneousPlaybacks: Int = 8

) : BaseStream(source)
{

    /**
     * The sample ID.
     */
    var sampleID = 0


    override fun onSourceLoad(path: String): Int
    {
        sampleID = BASS_SampleLoad(path, 0, 0, simultaneousPlaybacks, flags)
        id = BASS_SampleGetChannel(sampleID, false)

        return BASS_FX.BASS_FX_TempoCreate(id, fxFlags)
    }

    /**
     * Binding for [BASS_SampleFree] and [BASS_StreamFree].
     */
    override fun free() = BASS_SampleFree(sampleID) && super.free()
}