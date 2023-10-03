package com.reco1l.skindecoder.data

import com.reco1l.skindecoder.data.SkinDataGeneral.Companion.LATEST_VERSION
import kotlinx.serialization.Serializable


/**
 * Contains the decoded data from an `skin.ini` file.
 *
 * Note: This only contains data from used properties in rimu!
 */
@Serializable
data class SkinData(

    /**
     * If this skin data was created from this constructor means the `skin.ini` file was missing and
     * as specified in the [osu!wiki](https://osu.ppy.sh/wiki/en/Skinning/skin.ini#latest) it'll use
     * [LATEST_VERSION]
     */
    val general: SkinDataGeneral = SkinDataGeneral(version = LATEST_VERSION),

    val colours: SkinDataColours = SkinDataColours(),

    val fonts: SkinDataFonts = SkinDataFonts()

)
