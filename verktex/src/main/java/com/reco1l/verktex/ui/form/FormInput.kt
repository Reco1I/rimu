package com.reco1l.verktex.ui.form

import com.reco1l.verktex.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.control.FloatTextInput
import com.reco1l.verktex.ui.control.IntegerTextInput
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.linearContainer

@Suppress("LeakingThis")
open class FormInput(private val initialValue: String = "") : FormControl<String, UITextInput>(initialValue) {

    final override val control = createControl().apply {
        width = Dimension.FillAvailable
    }

    override val valueText = null


    init {
        orientation = Orientation.Vertical
        style + {
            spacing = 2f.srem
        }

        linearContainer {
            width = Dimension.FillAvailable
            style = {
                spacing = 2f.srem
            }
            +labelText
            +resetButton
        }

        +control
    }

    open fun createControl() = UITextInput(initialValue)
}

open class IntegerFormInput(
    initialValue: Int?,
    val minValue: Int? = -Int.MAX_VALUE,
    val maxValue: Int? = Int.MAX_VALUE
) : FormInput(initialValue?.toString() ?: "") {
    override fun createControl() = IntegerTextInput(defaultValue.toIntOrNull(), minValue, maxValue)
}

open class FloatFormInput(
    initialValue: Float?,
    val minValue: Float? = -Float.MAX_VALUE,
    val maxValue: Float? = Float.MAX_VALUE
) : FormInput(initialValue?.toString() ?: "") {
    override fun createControl() = FloatTextInput(defaultValue.toFloatOrNull(), minValue, maxValue)
}