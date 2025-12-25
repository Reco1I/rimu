package com.reco1l.verktex.ui.form

import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.control.FloatTextInput
import com.reco1l.verktex.ui.control.IntegerTextInput
import com.reco1l.verktex.ui.control.UITextInput

/**
 * A form control with a text input below its label.
 *
 * The input is created by [createControl], passed to the constructor so subclasses can configure
 * it with their own arguments before their properties are initialized.
 */
@Suppress("LeakingThis")
open class FormInput(
    initialValue: String = "",
    createControl: (initialValue: String) -> UITextInput = ::UITextInput
) : FormControl<String, UITextInput>(initialValue) {

    final override val control = createControl(initialValue).apply {
        width = fill()
    }


    init {
        orientation = Orientation.Vertical

        +createHeader()
        +control
    }
}

/**
 * A [FormInput] that only allows [Int]s between [minValue] and [maxValue].
 */
open class IntegerFormInput(
    initialValue: Int?,
    minValue: Int? = -Int.MAX_VALUE,
    maxValue: Int? = Int.MAX_VALUE
) : FormInput(initialValue?.toString() ?: "", { IntegerTextInput(it.toIntOrNull(), minValue, maxValue) })

/**
 * A [FormInput] that only allows [Float]s between [minValue] and [maxValue].
 */
open class FloatFormInput(
    initialValue: Float?,
    minValue: Float? = -Float.MAX_VALUE,
    maxValue: Float? = Float.MAX_VALUE
) : FormInput(initialValue?.toString() ?: "", { FloatTextInput(it.toFloatOrNull(), minValue, maxValue) })
