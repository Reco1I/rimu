package com.reco1l.bassbinding.stream

import android.content.Context
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX

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