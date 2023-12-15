package com.rian.osu.beatmap.sections

import com.reco1l.rimu.data.Color4
import com.rian.osu.beatmap.ComboColor

/**
 * Contains information about combo and skin colors of a beatmap.
 */
class BeatmapColor : Cloneable {
    /**
     * The combo colors of this beatmap.
     */
    @JvmField
    var comboColors = mutableListOf<ComboColor>()

    /**
     * The color of the slider border.
     */
    @JvmField
    var sliderBorderColor: Color4? = null

    public override fun clone() =
        (super.clone() as BeatmapColor).apply {
            this@BeatmapColor.comboColors.forEach { comboColors.add(it.clone()) }
            sliderBorderColor = this@BeatmapColor.sliderBorderColor?.let { Color4(it) }
        }
}
