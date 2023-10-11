package com.reco1l.basskt.stream

import android.content.Context
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX

open class SampleStream(

    source: String? = null,

    /**
     * Define the sample flags, by default [BASS_SAMPLE_OVER_POS].
     */
    var flags: Int = BASS.BASS_SAMPLE_OVER_POS,

    /**
     * Define the FX flags, by default [BASS_STREAM_AUTOFREE].
     */
    var fxFlags: Int = BASS.BASS_STREAM_AUTOFREE,

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
        sampleID = BASS.BASS_SampleLoad(path, 0, 0, simultaneousPlaybacks, flags)
        id = BASS.BASS_SampleGetChannel(sampleID, false)

        return BASS_FX.BASS_FX_TempoCreate(id, fxFlags)
    }

    /**
     * Binding for [BASS_SampleFree] and [BASS_StreamFree].
     */
    override fun free() = BASS.BASS_SampleFree(sampleID) && super.free()
}


/**
 * A sample from an Android asset using its path relative to `/assets` directory as source.
 */
class AssetSampleStream(private val context: Context, asset: String? = null) : SampleStream(asset)
{
    override fun onSourceLoad(path: String): Int
    {
        val asset = BASS.Asset(context.assets, path)

        sampleID = BASS.BASS_SampleLoad(asset, 0, 0, simultaneousPlaybacks, flags)
        id = BASS.BASS_SampleGetChannel(sampleID, false)

        return BASS_FX.BASS_FX_TempoCreate(id, fxFlags)
    }
}