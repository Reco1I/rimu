package com.reco1l.verktex.ui.form

import com.reco1l.verktex.*
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.container
import com.reco1l.verktex.ui.control.UICheckbox
import com.reco1l.verktex.ui.linearContainer
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class FormCheckbox(initialValue: Boolean = false) : FormControl<Boolean, UICheckbox>(initialValue) {

    override val control = UICheckbox(initialValue).apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
    }


    init {
        container {
            width = Dimension.FillAvailable

            linearContainer {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = {
                    spacing = 2f.srem
                }

                +labelText
                +resetButton
            }

            +control
        }
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!super.onAreaTouched(event, localX, localY) && event.isActionUp) {
            control.value = !control.value
        }
        return true
    }

}

class PreferenceCheckbox(private val preferenceKey: String, fallback: Boolean = false) : FormCheckbox(Config.getBoolean(preferenceKey, fallback)) {
    override fun onControlValueChanged() {
        Config.setBoolean(preferenceKey, value)
        super.onControlValueChanged()
    }
}