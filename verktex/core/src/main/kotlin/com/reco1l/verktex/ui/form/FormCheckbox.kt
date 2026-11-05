package com.reco1l.verktex.ui.form

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.control.UICheckbox

/**
 * A form control with a checkbox. Tapping anywhere on the row toggles it.
 */
@Suppress("LeakingThis")
open class FormCheckbox(initialValue: Boolean = false) : FormControl<Boolean, UICheckbox>(initialValue) {

    override val control = UICheckbox(initialValue).apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
    }


    // The finger that pressed the row outside of the checkbox and the reset button.
    private var rowPointerId = -1


    init {
        +createHeader { +control }
    }


    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event !is PointerEvent) {
            return false
        }

        return when (event.action) {
            PointerEvent.Action.Down -> {
                rowPointerId = event.pointerId
                true
            }

            PointerEvent.Action.Up -> {
                if (event.pointerId != rowPointerId) return false
                rowPointerId = -1
                if (containsScenePoint(event.x, event.y)) {
                    value = !value
                }
                true
            }

            PointerEvent.Action.Unknown, PointerEvent.Action.Cancel -> {
                rowPointerId = -1
                false
            }

            PointerEvent.Action.Move -> event.pointerId == rowPointerId
        }
    }

}
