package com.osudroid.ui.skinning

import com.reco1l.iniserialization.Ini
import com.reco1l.iniserialization.IniNames
import com.reco1l.verktex.data.Color4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The keys of an osu!stable `skin.ini` rimu! reads, with osu!stable's defaults, and how they're
 * converted to a [SkinConfiguration] ([toConfiguration]).
 */
@Serializable
data class LegacySkinIni(
    @SerialName("General") val general: General = General(),
    @SerialName("Colours") val colours: Colours = Colours(),
    @SerialName("Fonts") val fonts: Fonts = Fonts(),
) {

    @Serializable
    data class General(
        /**
         * The version the skin is made for: a number, `latest`, or nothing for 1.0.
         */
        @SerialName("Version") val version: String? = null,
        @SerialName("CursorRotate") val cursorRotate: Boolean = true,
        @SerialName("CursorTrailRotate") val cursorTrailRotate: Boolean = true,
        @SerialName("CursorExpand") val cursorExpand: Boolean = true,
        @SerialName("CursorCentre") val cursorCentre: Boolean = true,
        // osu!stable also reads it misspelled, as some skins have it.
        @SerialName("HitCircleOverlayAboveNumber") @IniNames("HitCircleOverlayAboveNumer")
        val hitCircleOverlayAboveNumber: Boolean = true,
        @SerialName("SpinnerNoBlink") val spinnerNoBlink: Boolean = false,
        @SerialName("SpinnerFrequencyModulate") val spinnerFrequencyModulate: Boolean = true,
        @SerialName("AllowSliderBallTint") val allowSliderBallTint: Boolean = false,
        @SerialName("SliderBallFlip") val sliderBallFlip: Boolean = true,
        @SerialName("ComboBurstRandom") val comboBurstRandom: Boolean = false,
        @SerialName("LayeredHitSounds") val layeredHitSounds: Boolean = true,
        @SerialName("AnimationFramerate") val animationFramerate: Float = -1f,
    )

    /**
     * Colours as osu!stable writes them, `r,g,b` from 0 to 255 (an alpha after them is ignored).
     */
    @Serializable
    data class Colours(
        @SerialName("Combo1") val combo1: List<Int>? = null,
        @SerialName("Combo2") val combo2: List<Int>? = null,
        @SerialName("Combo3") val combo3: List<Int>? = null,
        @SerialName("Combo4") val combo4: List<Int>? = null,
        @SerialName("Combo5") val combo5: List<Int>? = null,
        @SerialName("Combo6") val combo6: List<Int>? = null,
        @SerialName("Combo7") val combo7: List<Int>? = null,
        @SerialName("Combo8") val combo8: List<Int>? = null,
        @SerialName("SliderTrackOverride") val sliderTrackOverride: List<Int>? = null,
        @SerialName("SliderBorder") val sliderBorder: List<Int>? = null,
        @SerialName("SpinnerBackground") val spinnerBackground: List<Int>? = null,
    ) {

        /**
         * The combo colours the skin gives, in order.
         */
        val comboColors: List<Color4>
            get() = listOf(combo1, combo2, combo3, combo4, combo5, combo6, combo7, combo8).mapNotNull { it.toColor() }
    }

    @Serializable
    data class Fonts(
        @SerialName("HitCirclePrefix") val hitCirclePrefix: String = "default",
        @SerialName("HitCircleOverlap") val hitCircleOverlap: Float = -2f,
        @SerialName("ScorePrefix") val scorePrefix: String = "score",
        @SerialName("ScoreOverlap") val scoreOverlap: Float = 0f,
        @SerialName("ComboPrefix") val comboPrefix: String = "score",
        @SerialName("ComboOverlap") val comboOverlap: Float = 0f,
    )

    /**
     * The configuration this skin.ini gives.
     */
    fun toConfiguration(): SkinConfiguration {
        val comboColors = colours.comboColors
        val trackColor = colours.sliderTrackOverride.toColor()
        val defaultSlider = SliderConfiguration()

        return SkinConfiguration(
            comboColors = ComboColors(forceOverride = comboColors.isNotEmpty(), colors = comboColors),
            slider = SliderConfiguration(
                ballFlip = general.sliderBallFlip,
                // A track colour replaces the combo colour of the body, opaque.
                followComboColor = trackColor == null,
                bodyColor = trackColor,
                bodyBaseAlpha = if (trackColor != null) 1f else defaultSlider.bodyBaseAlpha,
                borderColor = colours.sliderBorder.toColor(),
            ),
            utils = UtilsConfiguration(
                version = parseVersion(general.version),
                hitCircleOverlayAboveNumber = general.hitCircleOverlayAboveNumber,
                spinnerNoBlink = general.spinnerNoBlink,
                allowSliderBallTint = general.allowSliderBallTint,
                comboBurstRandom = general.comboBurstRandom,
                spinnerBackgroundColor = colours.spinnerBackground.toColor() ?: UtilsConfiguration().spinnerBackgroundColor,
                comboTextScale = 0.8f,
                animationFramerate = general.animationFramerate,
                layeredHitSounds = general.layeredHitSounds,
                spinnerFrequencyModulate = general.spinnerFrequencyModulate,
            ),
            cursor = CursorConfiguration(
                rotateCursor = general.cursorRotate,
                rotateCursorTrail = general.cursorTrailRotate,
                expandCursor = general.cursorExpand,
                centreCursor = general.cursorCentre,
            ),
            fonts = FontsConfiguration(
                hitCirclePrefix = fonts.hitCirclePrefix,
                hitCircleOverlap = fonts.hitCircleOverlap,
                scorePrefix = fonts.scorePrefix,
                scoreOverlap = fonts.scoreOverlap,
                comboPrefix = fonts.comboPrefix,
                comboOverlap = fonts.comboOverlap,
            ),
        )
    }

    companion object {

        /**
         * Reads a skin.ini.
         */
        fun decode(text: String): LegacySkinIni = Ini.decodeFromString(serializer(), text)

        /**
         * The version a skin.ini says the skin is made for: 1.0 when it doesn't say, as osu!stable
         * assumes, and [SkinConfiguration.LATEST_VERSION] for `latest`.
         */
        private fun parseVersion(value: String?): Float =
            if (value == null) 1f else value.toFloatOrNull() ?: SkinConfiguration.LATEST_VERSION

        private fun List<Int>?.toColor(): Color4? {
            if (this == null || size < 3) {
                return null
            }
            return Color4(this[0].coerceIn(0, 255), this[1].coerceIn(0, 255), this[2].coerceIn(0, 255))
        }
    }
}
