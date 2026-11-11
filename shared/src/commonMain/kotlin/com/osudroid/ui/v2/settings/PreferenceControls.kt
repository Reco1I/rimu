package com.osudroid.ui.v2.settings

import com.reco1l.verktex.settings.SettingKey
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.form.FormCheckbox
import com.reco1l.verktex.ui.form.FormInput
import com.reco1l.verktex.ui.form.FormSelect
import com.reco1l.verktex.ui.form.FormSlider
import kotlin.math.roundToInt

/*
 * Form controls that store their value in a setting, whose default is the one the reset button
 * restores.
 */

class PreferenceCheckbox(private val setting: SettingKey<Boolean>) : FormCheckbox(Settings[setting]) {

    init {
        defaultValue = setting.default
    }

    override fun onControlValueChanged() {
        Settings[setting] = value
        super.onControlValueChanged()
    }
}

class PreferenceInput(private val setting: SettingKey<String>) : FormInput(Settings[setting]) {

    init {
        defaultValue = setting.default
    }

    override fun onControlValueChanged() {
        Settings[setting] = value
        super.onControlValueChanged()
    }
}

/**
 * A select storing its single selected value as a string, the format the legacy list preferences use.
 */
class PreferenceSelect(private val setting: SettingKey<String>) :
    FormSelect<String>(Settings[setting].let { if (it.isEmpty()) emptyList() else listOf(it) }) {

    init {
        defaultValue = if (setting.default.isEmpty()) emptyList() else listOf(setting.default)
    }

    override fun onControlValueChanged() {
        Settings[setting] = value.firstOrNull() ?: ""
        super.onControlValueChanged()
    }
}

/**
 * A slider storing its value as a float.
 */
class FloatPreferenceSlider(private val setting: SettingKey<Float>) : FormSlider(Settings[setting]) {

    init {
        defaultValue = setting.default
    }

    override fun onControlValueChanged() {
        Settings[setting] = value
        super.onControlValueChanged()
    }
}

/**
 * A slider storing its value as a whole number.
 */
class IntPreferenceSlider(private val setting: SettingKey<Int>) : FormSlider(Settings[setting].toFloat()) {

    init {
        defaultValue = setting.default.toFloat()
    }

    override fun onControlValueChanged() {
        Settings[setting] = value.roundToInt()
        super.onControlValueChanged()
    }
}
