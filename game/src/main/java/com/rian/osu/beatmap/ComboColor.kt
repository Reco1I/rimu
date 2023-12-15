package com.rian.osu.beatmap

import com.reco1l.rimu.data.Color4

/**
 * An extension to [RGBColor] specifically for combo colors.
 */
class ComboColor(
    /**
     * The index of this combo color.
     */
    @JvmField val index: Int,

    /**
     * The underlying [RGBColor].
     */
    color: Color4
) : Color4(color), Cloneable {
    public override fun clone() = super.clone() as ComboColor
}
