package com.osudroid.ui.skinning

import com.osudroid.ui.v2.hud.HUDSkinData
import com.reco1l.verktex.data.Color4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The configuration of a skin, as its `skin.json` has it: osu!droid's format, whose sections and keys
 * these keep the names of. A skin with a `skin.ini` alone has the one [LegacySkinIni] converts it
 * to, and what a `skin.json` doesn't say is taken from its `skin.ini`, see [SkinConfigurationLoader].
 *
 * Keys rimu! doesn't read (the layout of osu!droid's old song select, its menu colours, its slider
 * hints) are ignored.
 */
@Serializable
data class SkinConfiguration(
    @SerialName("ComboColor") val comboColors: ComboColors = ComboColors(),
    @SerialName("Slider") val slider: SliderConfiguration = SliderConfiguration(),
    @SerialName("Utils") val utils: UtilsConfiguration = UtilsConfiguration(),
    @SerialName("Cursor") val cursor: CursorConfiguration = CursorConfiguration(),
    @SerialName("Fonts") val fonts: FontsConfiguration = FontsConfiguration(),
    @SerialName("HUD") val hud: HUDSkinData = HUDSkinData.Default,
    @SerialName("Theme") val theme: ThemeConfiguration = ThemeConfiguration(),
) {

    companion object {

        /**
         * The version of a skin made for the latest version of osu!stable.
         */
        const val LATEST_VERSION = 999f

        /**
         * The configuration of a skin that has none.
         */
        val Default = SkinConfiguration()
    }
}

@Serializable
data class ComboColors(

    /**
     * Whether these colours are used over the beatmap's.
     */
    val forceOverride: Boolean = false,

    /**
     * The colours as the skin gives them, [resolved] gives the ones to use.
     */
    val colors: List<@Serializable(HexColorSerializer::class) Color4> = emptyList(),
) {

    /**
     * The skin's combo colours, or those of osu!stable's default skin when it has none.
     */
    val resolved: List<Color4>
        get() = colors.ifEmpty { DEFAULT }

    companion object {

        /**
         * The combo colours of a skin that has none, those of osu!stable's default skin.
         */
        val DEFAULT = listOf(
            Color4(255 / 255f, 192 / 255f, 0f),
            Color4(0f, 202 / 255f, 0f),
            Color4(18 / 255f, 124 / 255f, 255 / 255f),
            Color4(242 / 255f, 24 / 255f, 57 / 255f),
        )
    }
}

@Serializable
data class SliderConfiguration(
    @SerialName("sliderBodyWidth") val bodyWidth: Float = 61f,
    @SerialName("sliderBorderWidth") val borderWidth: Float = 5.2f,
    @SerialName("sliderBodyBaseAlpha") val bodyBaseAlpha: Float = 0.7f,

    /**
     * Whether the slider ball is flipped when it goes back.
     */
    @SerialName("sliderBallFlip") val ballFlip: Boolean = true,

    /**
     * Whether the body takes the combo colour, rather than [bodyColor].
     */
    @SerialName("sliderFollowComboColor") val followComboColor: Boolean = true,

    @SerialName("sliderBodyColor") val bodyColor: @Serializable(HexColorSerializer::class) Color4? = null,

    /**
     * The colour of the border, over the beatmap's, when given.
     */
    @SerialName("sliderBorderColor") val borderColor: @Serializable(HexColorSerializer::class) Color4? = null,
)

@Serializable
data class UtilsConfiguration(

    /**
     * The osu!stable version the skin is made for, e.g. 1.0 for skins made before the new spinner.
     * Skins that aren't osu!stable's, without a skin.ini, are made for the latest version.
     */
    val version: Float = SkinConfiguration.LATEST_VERSION,

    val limitComboTextLength: Boolean = false,
    val disableKiai: Boolean = false,
    val comboTextScale: Float = 1f,

    /**
     * The frame rate of the skin's animations, or 0 or less for each animation to last a second.
     */
    val animationFramerate: Float = -1f,

    /**
     * Whether the overlay of hit circles is drawn over their number rather than under it.
     */
    val hitCircleOverlayAboveNumber: Boolean = true,

    /**
     * Whether the highest bar of the old style spinner's metre stays shown, instead of blinking
     * while it fills.
     */
    val spinnerNoBlink: Boolean = false,

    /**
     * Whether the slider ball is tinted with the combo colour.
     */
    val allowSliderBallTint: Boolean = false,

    /**
     * Whether combo bursts are shown in a random order instead of one after the other.
     */
    val comboBurstRandom: Boolean = false,

    /**
     * The colour the old style spinner's background is tinted with.
     */
    val spinnerBackgroundColor: @Serializable(HexColorSerializer::class) Color4 = Color4(100 / 255f, 100 / 255f, 100 / 255f),

    val layeredHitSounds: Boolean = true,
    val spinnerFrequencyModulate: Boolean = true,
)

@Serializable
data class CursorConfiguration(
    val rotateCursor: Boolean = true,
    val rotateCursorTrail: Boolean = true,

    /**
     * Whether the cursor grows when pressing and while following a slider.
     */
    val expandCursor: Boolean = true,

    /**
     * Whether the cursor's texture is centred on its position, rather than starting at it.
     */
    val centreCursor: Boolean = true,
)

@Serializable
data class FontsConfiguration(
    val hitCirclePrefix: String = "default",
    val hitCircleOverlap: Float = -2f,
    val scorePrefix: String = "score",
    val scoreOverlap: Float = 0f,
    val comboPrefix: String = "score",
    val comboOverlap: Float = 0f,
)

@Serializable
data class ThemeConfiguration(

    /**
     * The accent colour of the UI's theme.
     */
    val accentColor: @Serializable(HexColorSerializer::class) Color4 = Color4("#C2CAFF"),
)

/**
 * The configuration of the current skin, which [com.osudroid.ui.v2.Skin.load] loads.
 */
object OsuSkin {

    var current = SkinConfiguration.Default
}

/**
 * A font of the skin, its textures named with a prefix ([FontsConfiguration]): textures refer to
 * one of these rather than to its prefix, so they follow it as the skin changes.
 */
enum class SkinFont(private val prefixIn: (FontsConfiguration) -> String) {
    HitCircle({ it.hitCirclePrefix }),
    Score({ it.scorePrefix }),
    Combo({ it.comboPrefix });

    /**
     * The prefix the current skin gives.
     */
    val prefix: String
        get() = prefixIn(OsuSkin.current.fonts)

    /**
     * The prefix of the default skin, whose textures are used when the skin doesn't have them.
     */
    val defaultPrefix: String
        get() = prefixIn(DEFAULT_FONTS)

    private companion object {
        val DEFAULT_FONTS = FontsConfiguration()
    }
}
