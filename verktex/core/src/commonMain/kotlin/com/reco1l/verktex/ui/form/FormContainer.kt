package com.reco1l.verktex.ui.form

import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.control.UIControl
import com.reco1l.verktex.ui.control.UISelect

/**
 * A container that collects the values of every control with a [UIControl.key] inside of it.
 */
open class FormContainer : UILinearContainer() {

    /**
     * Called when [submit] is called and the data has been encoded.
     */
    var onSubmit: ((data: Map<String, Any?>) -> Unit)? = null


    /**
     * Encodes the value of a control, a [List] for a [UISelect]. Controls encoded as `null` are left
     * out of the data.
     */
    open fun onEncodeControlData(control: UIControl<*>): Any? {
        if (control is UISelect<*>) {
            return control.value.toList()
        }
        return control.value
    }

    /**
     * Collects the values of every keyed control, by key, and passes them to [onSubmit].
     */
    fun submit(): Map<String, Any?> {
        val data = LinkedHashMap<String, Any?>()

        fun UIComponent.collect() {
            val control = this as? UIControl<*>
            val key = control?.key

            if (control != null && key != null) {
                val value = onEncodeControlData(control)
                if (value != null) data[key] = value else data.remove(key)
            } else if (this is UIContainer) {
                forEach { it.collect() }
            }
        }

        forEach { it.collect() }
        onSubmit?.invoke(data)
        return data
    }

}
