package com.reco1l.verktex.ui.form

import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.control.UISelect

/**
 * A form control with a select below its label.
 */
open class FormSelect<T : Any>(initialValues: List<T> = emptyList()) : FormControl<List<T>, UISelect<T>>(initialValues) {

    final override val control = UISelect(initialValues).apply {
        width = fill()
    }


    //region Shortcuts

    var selectionMode by control::selectionMode
    var placeholder by control::placeholder
    var options by control::options

    //endregion


    init {
        orientation = Orientation.Vertical

        +createHeader()
        +control
    }

}
