package com.reco1l.verktex.ui.form

import com.reco1l.verktex.*
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.theme.Radius
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.control.UISlider
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.fillContainer
import com.reco1l.verktex.ui.linearContainer
import ru.nsu.ccfit.zuev.osu.Config
import kotlin.math.abs

@Suppress("LeakingThis")
open class FormSlider(initialValue: Float = 0f) : FormControl<Float, UISlider>(initialValue) {

    override val control = UISlider(initialValue).apply {
        width = Dimension.FillAvailable
    }

    override val isDefault
        get() = abs(value - defaultValue) < 1e-6f

    override val valueText = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        alignment = Anchor.Center
        style = {
            fontSize = FontSize.XS
            padding = Vec4(2f.srem, 1f.srem)
            color = it.accentColor
            backgroundColor = Color4.Black.copy(alpha = 0.1f)
            radius = Radius.MD
        }
    }


    init {
        orientation = Orientation.Vertical
        style + {
            spacing = 2f.srem
        }

        fillContainer {
            width = Dimension.FillAvailable

            linearContainer {
                width = Dimension.FillAvailable
                style = {
                    spacing = 2f.srem
                }
                +labelText
                +resetButton
            }

            +valueText
        }

        +control
    }
}

class FloatPreferenceSlider(private val preferenceKey: String, fallbackValue: Float = 0f) : FormSlider(Config.getFloat(preferenceKey, fallbackValue)) {
    override fun onControlValueChanged() {
        Config.setFloat(preferenceKey, control.value)
        super.onControlValueChanged()
    }
}

class IntPreferenceSlider(private val preferenceKey: String, fallbackValue: Int = 0) : FormSlider(Config.getInt(preferenceKey, fallbackValue).toFloat()) {
    override fun onControlValueChanged() {
        Config.setInt(preferenceKey, control.value.toInt())
        super.onControlValueChanged()
    }
}