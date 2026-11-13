package ru.nsu.ccfit.zuev.skins

import com.osudroid.ui.v2.hud.HUDSkinData
import com.osudroid.utils.optJsonArray
import com.osudroid.utils.optJsonObject
import com.osudroid.utils.optString
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.Theme
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class SkinJsonReader private constructor() : SkinReader() {
    var currentData = JsonObject(emptyMap())
    private var currentComboColorData = JsonObject(emptyMap())
    private var currentSliderData = JsonObject(emptyMap())
    private var currentUtilsData = JsonObject(emptyMap())
    private var currentLayoutData = JsonObject(emptyMap())
    private var currentColorData = JsonObject(emptyMap())
    private var currentCursorData = JsonObject(emptyMap())
    private var currentFontsData = JsonObject(emptyMap())
    private var currentThemeData = JsonObject(emptyMap())

    fun supplyJson(json: JsonObject) {
        currentData = json
        loadSkin()
    }

    override fun loadSkinBase() {
        load("ComboColor", currentData) { c ->
            currentComboColorData = c
            loadComboColorSetting()
        }
        load("Slider", currentData) { c ->
            currentSliderData = c
            loadSlider()
        }
        load("Utils", currentData) { c ->
            currentUtilsData = c
            loadUtils()
        }
        load("Layout", currentData) { c ->
            currentLayoutData = c
            loadLayout()
        }
        load("Color", currentData) { c ->
            currentColorData = c
            loadColor()
        }
        load("Cursor", currentData) { c ->
            currentCursorData = c
            loadCursor()
        }
        load("Fonts", currentData) { c ->
            currentFontsData = c
            loadFonts()
        }
        loadArray("HUD", currentData) { json ->
            OsuSkin.get().hudSkinData = if (json == null)
                HUDSkinData.Default
            else
                HUDSkinData.readFromJSON(json)
        }
        load("Theme", currentData) { c ->
            currentThemeData = c
            loadTheme()
        }
    }

    override fun loadFonts() {
        val skin = OsuSkin.get()

        skin.hitCirclePrefix.setFromJson(currentFontsData)
        skin.hitCircleOverlapData.setFromJson(currentFontsData)
        skin.scorePrefix.setFromJson(currentFontsData)
        skin.scoreOverlapData.setFromJson(currentFontsData)
        skin.comboPrefix.setFromJson(currentFontsData)
        skin.comboOverlapData.setFromJson(currentFontsData)
    }

    override fun loadComboColorSetting() {
        val skin = OsuSkin.get()
        val data = currentComboColorData
        skin.forceOverrideComboColorData.setFromJson(data)
        skin.comboColorList.clear()
        val array = data.optJsonArray("colors")
        if (array == null || array.size == 0) {
            skin.comboColorList.addAll(skin.DEFAULT_COMBO_COLORS)
        } else {
            for (i in 0 until array.size) {
                val hex = array.optString(i, skin.DEFAULT_COLOR_HEX)

                try {
                    skin.comboColorList.add(Color4(hex))
                } catch (ignored: NumberFormatException) {
                }
            }

            // If no valid colors were found, use the default ones
            if (skin.comboColorList.isEmpty()) {
                skin.comboColorList.addAll(skin.DEFAULT_COMBO_COLORS)
            }
        }
    }

    override fun loadSlider() {
        val skin = OsuSkin.get()
        val data = currentSliderData
        skin.sliderBodyWidthData.setFromJson(data)
        skin.sliderBorderWidthData.setFromJson(data)
        skin.sliderBodyBaseAlphaData.setFromJson(data)
        skin.sliderHintWidthData.setFromJson(data)
        skin.sliderHintShowMinLengthData.setFromJson(data)
        skin.sliderHintAlphaData.setFromJson(data)
        skin.sliderBallFlipData.setFromJson(data)
        skin.sliderFollowComboColorData.setFromJson(data)
        skin.sliderHintEnableData.setFromJson(data)
        skin.sliderBodyColorData.setFromJson(data)
        skin.sliderBorderColorData.setFromJson(data)
        skin.sliderHintColorData.setFromJson(data)
    }

    override fun loadUtils() {
        val data = currentUtilsData
        val skin = OsuSkin.get()
        skin.limitComboTextLengthData.setFromJson(data)
        skin.disableKiaiData.setFromJson(data)
        skin.comboTextScaleData.setFromJson(data)
        skin.animationFramerateData.setFromJson(data)
        skin.layeredHitSoundsData.setFromJson(data)
        skin.spinnerFrequencyModulateData.setFromJson(data)
    }

    override fun loadLayout() {
        val skin = OsuSkin.get()
        val data = currentLayoutData
        skin.useNewLayoutData.setFromJson(data)
        val names = data.keys.toList()
        if (names.isEmpty()) return
        for (name in names) {
            if (name == skin.useNewLayoutData.tag) {
                continue
            }
            val layoutJSON = data.optJsonObject(name)
            if (layoutJSON != null) {
                putLayout(name, SkinLayout.load(layoutJSON))
            }
        }
    }

    override fun loadColor() {
        val skin = OsuSkin.get()
        val data = currentColorData
        val names = data.keys.toList()
        if (names.isEmpty()) return
        for (name in names) {
            try {
                skin.colorData[name] = Color4(data.optString(name))
            } catch (ignored: NumberFormatException) {
            }
        }
    }

    override fun loadCursor() {
        val skin = OsuSkin.get()
        val data = currentCursorData
        skin.rotateCursorData.setFromJson(data)
        skin.rotateCursorTrailData.setFromJson(data)
    }

    override fun loadTheme() {
        val accentColor = Color4(currentThemeData.optString("accentColor", "#C2CAFF"))

        // Applies the accent color of the skin to the theme of the UI.
        Verktex.theme = Theme(palette = Theme.ColorPalette(accentColor))
    }

    fun load(tag: String, data: JsonObject, consumer: (JsonObject) -> Unit) {
        consumer(data.optJsonObject(tag) ?: JsonObject(emptyMap()))
    }

    fun loadArray(tag: String, data: JsonObject, consumer: (JsonArray?) -> Unit) {
        consumer(data.optJsonArray(tag))
    }

    companion object {
        private val reader = SkinJsonReader()

        @JvmStatic
        fun getReader(): SkinJsonReader {
            return reader
        }
    }
}
