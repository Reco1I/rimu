package com.osudroid.ui.v2.settings

import com.reco1l.verktex.ui.form.FormCheckbox
import com.reco1l.verktex.ui.form.FormInput
import com.reco1l.verktex.ui.form.FormSelect
import com.reco1l.verktex.ui.form.FormSlider
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config

/*
 * Form controls that persist their value in the shared preferences. These live outside of the
 * Verktex module since the engine does not know about Config.
 */

class PreferenceCheckbox(private val preferenceKey: String, fallback: Boolean = false) :
    FormCheckbox(Config.getBoolean(preferenceKey, fallback)) {

    init {
        defaultValue = fallback
    }

    override fun onControlValueChanged() {
        Config.setBoolean(preferenceKey, value)
        super.onControlValueChanged()
    }
}

class PreferenceInput(private val preferenceKey: String, fallback: String = "") :
    FormInput(Config.getString(preferenceKey, fallback)) {

    init {
        defaultValue = fallback
    }

    override fun onControlValueChanged() {
        Config.setString(preferenceKey, value)
        super.onControlValueChanged()
    }
}

/**
 * A select storing its single selected value as a string, the format the legacy list preferences use.
 */
class PreferenceSelect(private val preferenceKey: String, fallback: String = "") :
    FormSelect<String>(Config.getString(preferenceKey, fallback).let { if (it.isEmpty()) emptyList() else listOf(it) }) {

    init {
        defaultValue = if (fallback.isEmpty()) emptyList() else listOf(fallback)
    }

    override fun onControlValueChanged() {
        Config.setString(preferenceKey, value.firstOrNull() ?: "")
        super.onControlValueChanged()
    }
}

/**
 * A slider storing its value as a float, for the preferences in [Config.FLOAT_PREFERENCES].
 */
class FloatPreferenceSlider(private val preferenceKey: String, fallback: Float = 0f) :
    FormSlider(Config.getNumber(preferenceKey, fallback)) {

    init {
        defaultValue = fallback
    }

    override fun onControlValueChanged() {
        Config.setFloat(preferenceKey, value)
        super.onControlValueChanged()
    }
}

/**
 * A slider storing its value as an integer, for the numeric preferences that are whole numbers
 * (see [Config.PREFERENCE_BOUNDS]).
 */
class IntPreferenceSlider(private val preferenceKey: String, fallback: Int = 0) :
    FormSlider(Config.getInt(preferenceKey, fallback).toFloat()) {

    init {
        defaultValue = fallback.toFloat()
    }

    override fun onControlValueChanged() {
        Config.setInt(preferenceKey, value.roundToInt())
        super.onControlValueChanged()
    }
}
