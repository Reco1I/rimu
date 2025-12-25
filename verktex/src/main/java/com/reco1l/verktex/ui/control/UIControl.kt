package com.reco1l.verktex.ui.control

import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.form.FormControl

/**
 * Interface for a control that has a value and can notify when the value changes.
 */
abstract class UIControl<T : Any>(initialValue: T) : UIContainer() {

    /**
     * The key of the control, used for identification in [FormContainer] during submission.
     */
    var key: String? = null

    /**
     * The value of the control.
     */
    var value = initialValue
        set(value) {
            if (field != value) {
                field = onProcessValue(value)
                onValueChanged()
            }
        }

    /**
     * A callback that is invoked when the value of the control changes.
     */
    var onValueChange: ((T) -> Unit)? = null


    //region Callbacks

    /**
     * Processes the value of the control. This is called when the value is set to a new value.
     */
    open fun onProcessValue(value: T) = value

    /**
     * The callback that is called when the value of the control changes.
     */
    open fun onValueChanged() {
        onValueChange?.invoke(value)

        // The form control wrapping this control, if any, reacts to every change too.
        var parent = parent
        while (parent != null) {
            if (parent is FormControl<*, *> && parent.control === this) {
                parent.onControlValueChanged()
                break
            }
            parent = parent.parent
        }
    }

    //endregion
}