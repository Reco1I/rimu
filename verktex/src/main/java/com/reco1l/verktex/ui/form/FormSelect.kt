package com.reco1l.verktex.ui.form

import com.reco1l.verktex.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.control.UISelect
import com.reco1l.verktex.ui.linearContainer

open class FormSelect<T : Any>(initialValues: List<T> = emptyList()) : FormControl<List<T>, UISelect<T>>(initialValues) {

    final override val control = UISelect<T>().apply {
        width = Dimension.FillAvailable
    }

    override val valueText = null


    //region Shortcuts

    var selectionMode by control::selectionMode
    var placeholder by control::placeholder
    var options by control::options

    //endregion


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

}