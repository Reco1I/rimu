package com.reco1l.skindecoder.data

import com.reco1l.framework.graphics.Color4
import kotlinx.serialization.Serializable
import kotlin.reflect.full.memberProperties

/**
 * Skin `Colours` section.
 *
 * [See docs...](https://osu.ppy.sh/wiki/en/Skinning/skin.ini#[colours])
 */
@Serializable
data class SkinDataColours(


    val combo1: Color4 = Color4(255, 192, 0),

    val combo2: Color4 = Color4(0, 202, 0),

    val combo3: Color4 = Color4(18, 124, 255),

    val combo4: Color4 = Color4(242, 24, 57),

    val combo5: Color4? = null,

    val combo6: Color4? = null,

    val combo7: Color4? = null,

    val combo8: Color4? = null,


    val sliderBallColor: Color4 = Color4(2, 170, 255),

    val sliderBorderColor: Color4 = Color4(255, 255, 255),

    val sliderTrackColor: Color4? = null,

    val spinnerBackgroundColor: Color4 = Color4(100, 100, 100),


    // rimu! exclusive

    val accentColor: Color4 = Color4(0xFF9AC2FF)
)
{

    /**
     * List of defined colors.
     */
    val comboColors by lazy { listOfNotNull(combo1, combo2, combo3, combo4, combo5, combo6, combo7, combo8) }

    /**
     * Map of colors stored in this data class.
     */
    val map by lazy {

         SkinDataColours::class.memberProperties
             .filter { it.returnType.classifier == Color4::class }
             .associate { it.name to it.get(this) as Color4? }
    }
}
