package com.osudroid.ui.v2.settings

data class SettingsCategory(

    /**
     * The title of the category.
     */
    val title: CharSequence,

    /**
     * The options that will be displayed in the category.
     */
    val options: List<SettingsOption>,

    /**
     * Whether the category is shown, false for features the platform doesn't have.
     */
    val isVisible: Boolean = true,
)