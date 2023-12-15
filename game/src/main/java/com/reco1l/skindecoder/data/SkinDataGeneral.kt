package com.reco1l.skindecoder.data

import com.reco1l.skindecoder.serializers.IntAsBooleanSerializer
import kotlinx.serialization.Serializable

/**
 * Skin `General` section.
 *
 * [See docs...](https://osu.ppy.sh/wiki/en/Skinning/skin.ini#[general])
 */
@Serializable
data class SkinDataGeneral(

    var name: String? = null,

    val author: String? = null,

    val version: Double = 1.0,

    val animationFramerate: Int = -1,

    val customComboBurstSounds: IntArray? = null,


    // Because skin.ini stores booleans in numeric format (0 for false and 1 for true) we need to
    // specify a custom serializer.

    @Serializable(IntAsBooleanSerializer::class)
    val allowSliderBallTint: Boolean = false,

    @Serializable(IntAsBooleanSerializer::class)
    val comboBurstRandom: Boolean = false,

    @Serializable(IntAsBooleanSerializer::class)
    val cursorCentre: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val cursorExpand: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val cursorRotate: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val cursorTrailRotate: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val hitCircleOverlayAboveNumber: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val layeredHitSounds: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val sliderBallFlip: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val spinnerFadePlayfield: Boolean = false,

    @Serializable(IntAsBooleanSerializer::class)
    val spinnerFrequencyModulate: Boolean = true,

    @Serializable(IntAsBooleanSerializer::class)
    val spinnerNoBlink: Boolean = false
)
{

    // Generated
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkinDataGeneral

        if (name != other.name) return false
        if (author != other.author) return false
        if (version != other.version) return false
        if (animationFramerate != other.animationFramerate) return false
        if (allowSliderBallTint != other.allowSliderBallTint) return false
        if (comboBurstRandom != other.comboBurstRandom) return false
        if (cursorCentre != other.cursorCentre) return false
        if (cursorExpand != other.cursorExpand) return false
        if (cursorRotate != other.cursorRotate) return false
        if (cursorTrailRotate != other.cursorTrailRotate) return false
        if (customComboBurstSounds != null)
        {
            if (other.customComboBurstSounds == null) return false
            if (!customComboBurstSounds.contentEquals(other.customComboBurstSounds)) return false
        }
        else if (other.customComboBurstSounds != null) return false
        if (hitCircleOverlayAboveNumber != other.hitCircleOverlayAboveNumber) return false
        if (layeredHitSounds != other.layeredHitSounds) return false
        if (sliderBallFlip != other.sliderBallFlip) return false
        if (spinnerFadePlayfield != other.spinnerFadePlayfield) return false
        if (spinnerFrequencyModulate != other.spinnerFrequencyModulate) return false
        if (spinnerNoBlink != other.spinnerNoBlink) return false

        return true
    }

    // Generated
    override fun hashCode(): Int
    {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + version.hashCode()
        result = 31 * result + animationFramerate
        result = 31 * result + allowSliderBallTint.hashCode()
        result = 31 * result + comboBurstRandom.hashCode()
        result = 31 * result + cursorCentre.hashCode()
        result = 31 * result + cursorExpand.hashCode()
        result = 31 * result + cursorRotate.hashCode()
        result = 31 * result + cursorTrailRotate.hashCode()
        result = 31 * result + (customComboBurstSounds?.contentHashCode() ?: 0)
        result = 31 * result + hitCircleOverlayAboveNumber.hashCode()
        result = 31 * result + layeredHitSounds.hashCode()
        result = 31 * result + sliderBallFlip.hashCode()
        result = 31 * result + spinnerFadePlayfield.hashCode()
        result = 31 * result + spinnerFrequencyModulate.hashCode()
        result = 31 * result + spinnerNoBlink.hashCode()
        return result
    }


    companion object
    {
        /**
         * Identifier for latest skin version.
         */
        const val LATEST_VERSION = -1.0
    }
}
