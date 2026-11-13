package ru.nsu.ccfit.zuev.skins

import com.osudroid.ui.skinning.BooleanSkinData
import com.osudroid.ui.skinning.ColorSkinData
import com.osudroid.ui.skinning.FloatSkinData
import com.osudroid.ui.skinning.StringSkinData
import com.osudroid.ui.v2.hud.HUDSkinData
import com.reco1l.verktex.Platform
import com.reco1l.verktex.data.Color4
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

class OsuSkin {
    // The values as the skin sets them, which SkinJsonReader loads.
    internal val animationFramerateData = FloatSkinData("animationFramerate", -1f)
    internal val comboTextScaleData = FloatSkinData("comboTextScale", 1f)
    internal val sliderHintWidthData = FloatSkinData("sliderHintWidth", 3f)
    internal val sliderBodyWidthData = FloatSkinData("sliderBodyWidth", 61f)
    internal val sliderBorderWidthData = FloatSkinData("sliderBorderWidth", 5.2f)
    internal val sliderBodyBaseAlphaData = FloatSkinData("sliderBodyBaseAlpha", 0.7f)
    internal val sliderHintAlphaData = FloatSkinData("sliderHintAlpha", 0.3f)
    internal val sliderHintShowMinLengthData = FloatSkinData("sliderHintShowMinLength", 300f)
    internal val hitCircleOverlapData = FloatSkinData("hitCircleOverlap", -2f)

    internal val limitComboTextLengthData = BooleanSkinData("limitComboTextLength")
    internal val disableKiaiData = BooleanSkinData("disableKiai")
    internal val sliderHintEnableData = BooleanSkinData("sliderHintEnable")
    internal val sliderFollowComboColorData = BooleanSkinData("sliderFollowComboColor", true)
    internal val useNewLayoutData = BooleanSkinData("useNewLayout")
    internal val forceOverrideComboColorData = BooleanSkinData("forceOverride")
    internal val rotateCursorData = BooleanSkinData("rotateCursor", true)
    internal val rotateCursorTrailData = BooleanSkinData("rotateCursorTrail", true)
    internal val expandCursorData = BooleanSkinData("expandCursor", true)
    internal val hitCircleOverlayAboveNumberData = BooleanSkinData("hitCircleOverlayAboveNumber", true)
    internal val spinnerNoBlinkData = BooleanSkinData("spinnerNoBlink")
    internal val allowSliderBallTintData = BooleanSkinData("allowSliderBallTint")
    internal val spinnerBackgroundColorData = ColorSkinData("spinnerBackgroundColor", Color4(100 / 255f, 100 / 255f, 100 / 255f))

    // Skins that aren't osu!stable's, without a skin.ini, are made for the latest version.
    internal val versionData = FloatSkinData("version", LATEST_VERSION)
    internal val centreCursorData = BooleanSkinData("centreCursor", true)
    internal val layeredHitSoundsData = BooleanSkinData("layeredHitSounds", true)
    internal val sliderBallFlipData = BooleanSkinData("sliderBallFlip", true)
    internal val spinnerFrequencyModulateData = BooleanSkinData("spinnerFrequencyModulate", true)

    internal val DEFAULT_COLOR_HEX = "#FFFFFF"
    internal val comboColorList = ArrayList<Color4>()

    /**
     * The combo colors of a skin that has none, those of osu!stable's default skin.
     */
    internal val DEFAULT_COMBO_COLORS = listOf(
        Color4(255 / 255f, 192 / 255f, 0f),
        Color4(0f, 202 / 255f, 0f),
        Color4(18 / 255f, 124 / 255f, 255 / 255f),
        Color4(242 / 255f, 24 / 255f, 57 / 255f)
    )

    internal val sliderBorderColorData = ColorSkinData("sliderBorderColor", null)
    internal val sliderBodyColorData = ColorSkinData("sliderBodyColor", null)
    internal val sliderHintColorData = ColorSkinData("sliderHintColor", Color4(DEFAULT_COLOR_HEX))

    val hitCirclePrefix = StringSkinData("hitCirclePrefix", "default")
    val scorePrefix = StringSkinData("scorePrefix", "score")
    internal val scoreOverlapData = FloatSkinData("scoreOverlap", 0f)
    val comboPrefix = StringSkinData("comboPrefix", "score")
    internal val comboOverlapData = FloatSkinData("comboOverlap", 0f)

    internal val layoutData = HashMap<String, SkinLayout>()
    internal val colorData = HashMap<String, Color4>()

    @get:JvmName("getHUDSkinData")
    @set:JvmName("setHUDSkinData")
    var hudSkinData = HUDSkinData.Default


    val isRotateCursor: Boolean
        get() = rotateCursorData.currentValue

    val isRotateCursorTrail: Boolean
        get() = rotateCursorTrailData.currentValue

    /**
     * The osu!stable version the skin is made for, e.g. 1.0 for skins made before the new spinner.
     */
    val version: Float
        get() = versionData.currentValue

    /**
     * Whether the highest bar of the old style spinner's metre stays shown, instead of blinking while it fills.
     */
    val isSpinnerNoBlink: Boolean
        get() = spinnerNoBlinkData.currentValue

    /**
     * Whether the slider ball is tinted with the combo colour.
     */
    val isAllowSliderBallTint: Boolean
        get() = allowSliderBallTintData.currentValue

    /**
     * The colour the old style spinner's background is tinted with.
     */
    val spinnerBackgroundColor: Color4
        get() = spinnerBackgroundColorData.currentValue ?: Color4(100 / 255f, 100 / 255f, 100 / 255f)

    /**
     * Whether the overlay of hit circles is drawn over their number rather than under it.
     */
    val isHitCircleOverlayAboveNumber: Boolean
        get() = hitCircleOverlayAboveNumberData.currentValue

    /**
     * Whether the cursor grows when pressing and while following a slider.
     */
    val isExpandCursor: Boolean
        get() = expandCursorData.currentValue

    /**
     * Whether the cursor's texture is centred on its position, rather than starting at it.
     */
    val isCentreCursor: Boolean
        get() = centreCursorData.currentValue

    val comboTextScale: Float
        get() = comboTextScaleData.currentValue

    val isUseNewLayout: Boolean
        get() = useNewLayoutData.currentValue

    val isSliderHintEnable: Boolean
        get() = sliderHintEnableData.currentValue

    val sliderHintAlpha: Float
        get() = sliderHintAlphaData.currentValue

    val sliderHintWidth: Float
        get() = sliderHintWidthData.currentValue

    val sliderHintColor: Color4?
        get() = sliderHintColorData.currentValue

    val sliderHintShowMinLength: Float
        get() = sliderHintShowMinLengthData.currentValue

    val sliderBodyWidth: Float
        get() = sliderBodyWidthData.currentValue

    val sliderBorderWidth: Float
        get() = sliderBorderWidthData.currentValue

    val isDisableKiai: Boolean
        get() = disableKiaiData.currentValue

    val isLimitComboTextLength: Boolean
        get() = limitComboTextLengthData.currentValue

    val sliderBodyBaseAlpha: Float
        get() = sliderBodyBaseAlphaData.currentValue

    val isForceOverrideComboColor: Boolean
        get() = forceOverrideComboColorData.currentValue

    val comboColor: ArrayList<Color4>
        get() {
            if (comboColorList.isEmpty()) {
                comboColorList.addAll(DEFAULT_COMBO_COLORS)
            }
            return comboColorList
        }

    val isForceOverrideSliderBorderColor: Boolean
        get() = !sliderBorderColorData.isDefault

    val sliderBorderColor: Color4?
        get() = sliderBorderColorData.currentValue

    val isSliderFollowComboColor: Boolean
        get() = sliderFollowComboColorData.currentValue

    val sliderBodyColor: Color4?
        get() = sliderBodyColorData.currentValue

    fun getLayout(name: String): SkinLayout? {
        return layoutData[name]
    }

    fun getColor(name: String, fallback: Color4): Color4 {
        val color = colorData[name]
        return color ?: fallback
    }

    val scoreOverlap: Float
        get() = scoreOverlapData.currentValue

    val comboOverlap: Float
        get() = comboOverlapData.currentValue

    val hitCircleOverlap: Float
        get() = hitCircleOverlapData.currentValue

    val animationFramerate: Float
        get() = animationFramerateData.currentValue

    val isLayeredHitSounds: Boolean
        get() = layeredHitSoundsData.currentValue

    val isSliderBallFlip: Boolean
        get() = sliderBallFlipData.currentValue

    val isSpinnerFrequencyModulate: Boolean
        get() = spinnerFrequencyModulateData.currentValue

    fun reset() {
        layoutData.clear()
        colorData.clear()
        hudSkinData = HUDSkinData.Default
    }

    companion object {

        /**
         * The version of a skin made for the latest version of osu!stable.
         */
        const val LATEST_VERSION = 999f
        private val skinJson = OsuSkin()

        @JvmStatic
        fun get(): OsuSkin {
            return skinJson
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFull(path: String): String {
            return Platform.fileSystem.source(path.toPath()).buffer().use { it.readUtf8() }
        }
    }
}
