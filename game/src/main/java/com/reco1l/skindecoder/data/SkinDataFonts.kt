package com.reco1l.skindecoder.data

import kotlinx.serialization.Serializable

/**
 * Skin `Fonts` section.
 *
 * [See docs...](https://osu.ppy.sh/wiki/en/Skinning/skin.ini#[fonts])
 */
@Serializable
data class SkinDataFonts(

    val hitCirclePrefix: String = "default",

    val hitCircleOverlap: Int = -2,

    val scorePrefix: String = "score",

    val scoreOverlap: Int = 0,

    val comboPrefix: String = "score",

    val comboOverlap: Int = 0
)
