package com.osudroid.ui.v2.settings

import com.reco1l.verktex.localization.LocalizedStringArray
import com.reco1l.verktex.settings.SettingKey
import com.reco1l.verktex.ui.UIComponent

data class SettingsOption(
    /**
     * The type of the option.
     */
    val type: Type,

    // General props

    /**
     * The key that will be used to store the value of this option.
     */
    val key: String,

    /**
     * The title of the option.
     */
    val title: CharSequence,

    /**
     * The summary of the option.
     */
    val summary: CharSequence? = null,

    /**
     * The setting this option shows and changes, whose type matches [type]: a `Boolean` for a
     * checkbox, a `String` for an input or a select, an `Int` or a `Float` for a slider. Null for
     * buttons and the options with an [initialValue].
     */
    val setting: SettingKey<*>? = null,

    /**
     * The action that will be performed when the value of this option changes.
     */
    val onChange: ((value: Any) -> Unit)? = null,

    // Button props

    /**
     * The action that will be performed when the button is clicked.
     *
     * This property is only used in [Type.Button] preferences.
     *
     */
    val onClick: (() -> Unit)? = null,


    // FormSelect props

    /**
     * The entries of the select.
     *
     * This property is only used in [Type.Select] preferences.
     */
    val entries: LocalizedStringArray? = null,

    /**
     * The entry values of the select.
     *
     * This property is only used in [Type.Select] preferences.
     */
    val entryValues: LocalizedStringArray? = null,

    // FormSlider props

    /**
     * The minimum value of the slider.
     *
     * This property is only used in [Type.Slider] preferences.
     */
    val min: Float? = null,

    /**
     * The maximum value of the slider.
     *
     * This property is only used in [Type.Slider] preferences.
     */
    val max: Float? = null,

    /**
     * The step of the slider.
     *
     * This property is only used in [Type.Slider] preferences.
     */
    val step: Float? = 0.01f,


    /**
     * The value formatter that will be used to format the value.
     *
     * This property is only used in [Type.Slider] preferences.
     */
    val valueFormatter: ((value: Float) -> String)? = null,


    /**
     * Called when the component is attached to the scene.
     */
    val onAttach: (component: UIComponent) -> Unit = { },

    /**
     * The current value of an option that isn't a preference, e.g. a setting of the multiplayer
     * room. When set the value is taken from it and not stored, [onChange] applies it.
     */
    val initialValue: (() -> Any)? = null
) {
    enum class Type {
        Checkbox,
        Input,
        Select,
        Button,
        Slider,
    }
}

