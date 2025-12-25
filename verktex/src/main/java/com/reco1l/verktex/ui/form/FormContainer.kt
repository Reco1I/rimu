package com.reco1l.verktex.ui.form

import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.control.UIControl
import com.reco1l.verktex.ui.control.UISelect
import org.json.JSONArray
import org.json.JSONObject

/**
 * A container that collects the values of every control with a [UIControl.key] inside of it.
 */
open class FormContainer : UILinearContainer() {

    /**
     * Called when [submit] is called and the data has been encoded.
     */
    var onSubmit: ((data: JSONObject) -> Unit)? = null


    /**
     * Encodes the value of a control into a JSON compatible value.
     */
    open fun onEncodeControlData(control: UIControl<*>): Any? {
        if (control is UISelect<*>) {
            return JSONArray(control.value)
        }
        return control.value
    }

    /**
     * Collects the values of every keyed control and passes them to [onSubmit].
     */
    fun submit(): JSONObject {
        val json = JSONObject()

        fun UIComponent.collect() {
            val control = this as? UIControl<*>
            val key = control?.key

            if (control != null && key != null) {
                json.put(key, onEncodeControlData(control))
            } else if (this is UIContainer) {
                forEach { it.collect() }
            }
        }

        forEach { it.collect() }
        onSubmit?.invoke(json)
        return json
    }

}
