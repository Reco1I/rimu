package com.reco1l.verktex.ui.form

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.control.UISlider
import com.reco1l.verktex.ui.text.UIText
import kotlin.math.abs
import com.reco1l.verktex.ui.themed

/**
 * A form control with a slider below its label and its formatted value on the right.
 */
@Suppress("LeakingThis")
open class FormSlider(initialValue: Float = 0f) : FormControl<Float, UISlider>(initialValue) {

    override val control = UISlider(initialValue).apply {
        width = fill()
    }

    override val isDefault
        get() = abs(value - defaultValue) < 1e-6f

    override val valueText = UIText().apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
        text = initialValue.toString()
        fontSettings = themed { typography.caption }
        padding = themed { Padding(gap.sm, gap.xs) }
        backgroundColor = themed { palette.controlInactiveBackground }
        radius = themed { radius.md }
    }


    init {
        orientation = Orientation.Vertical

        +createHeader { +valueText }
        +control
    }
}
