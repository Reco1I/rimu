package com.rian.osu.beatmap.sections

/**
 * Contains difficulty settings of a beatmap.
 */
class BeatmapDifficulty : Cloneable {
    /**
     * The circle size of this beatmap.
     */
    @JvmField
    var cs: Float = 5f

    /**
     * The approach rate of this beatmap.
     */
    var ar = Float.NaN
        get() = field.takeUnless { it.isNaN() } ?: od

    /**
     * The overall difficulty of this beatmap.
     */
    @JvmField
    var od: Float = 5f

    /**
     * The health drain rate of this beatmap.
     */
    @JvmField
    var hp: Float = 5f

    /**
     * The base slider velocity in hundreds of osu! pixels per beat.
     */
    @JvmField
    var sliderMultiplier: Double = 1.0

    /**
     * The amount of slider ticks per beat.
     */
    @JvmField
    var sliderTickRate: Double = 1.0

    fun apply(other: BeatmapDifficulty) = run {
        cs = other.cs
        ar = other.ar
        od = other.od
        hp = other.hp
        sliderMultiplier = other.sliderMultiplier
        sliderTickRate = other.sliderTickRate
    }

    public override fun clone() = super.clone() as BeatmapDifficulty
}
