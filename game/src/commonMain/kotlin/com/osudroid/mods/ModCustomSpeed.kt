package com.osudroid.mods

import com.reco1l.verktex.math.toFixed
import kotlin.jvm.JvmOverloads

/**
 * Represents the Custom Speed mod.
 *
 * @param trackRateMultiplier The multiplier to apply to the track's playback rate.
 */
class ModCustomSpeed @JvmOverloads constructor(trackRateMultiplier: Float = 1f) : ModRateAdjust(trackRateMultiplier) {
    override val name = "Custom Speed"
    override val acronym = "CS"
    override val description = "Play at any speed you want - slow or fast."
    override val type = ModType.Conversion
    override val isRanked = true
    override val requiresConfiguration = true

    override val extraInformation
        get() = trackRateMultiplier.toFixed(2) + "x"
}