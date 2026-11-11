package com.osudroid.game.replay

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.UICard
import com.reco1l.verktex.ui.form.FormSlider
import kotlin.math.roundToInt

/**
 * Controls visual settings of gameplay when replaying.
 */
class ReplayVisualSettingsControl : UICard() {
    /**
     * The default background brightness.
     *
     * Note that setting this value will also override the control's value.
     */
    var defaultBackgroundBrightness = OsuSettings.backgroundBrightness
        set(value) {
            field = value
            val sliderValue = value * 100

            brightnessSlider.defaultValue = sliderValue
            brightnessSlider.value = sliderValue
        }

    private val brightnessSlider = FormSlider(defaultBackgroundBrightness * 100).apply {
        label = com.osudroid.resources.Res.string.opt_bgbrightness_title
        control.min = 0f
        control.max = 100f
        valueFormatter = { "${it.roundToInt()}%"}
        onValueChanged = { onBackgroundBrightnessChanged?.invoke(it / 100f) }
    }

    /**
     * Called when the background brightness was changed. The argument is the background brightness (from 0 to 1).
     */
    var onBackgroundBrightnessChanged: ((Float) -> Unit)? = null

    init {
        width = Dimension.FillAvailable
        title = "Visual Settings"

        content {
            +brightnessSlider
        }
    }
}