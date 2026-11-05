package com.osudroid.ui.v2

import com.osudroid.resources.StringResource
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.text.UIText
import ru.nsu.ccfit.zuev.osu.helper.StringTable

/**
 * Sets the text of this [UIText] from a string resource.
 */
fun UIText.setText(resource: StringResource) {
    text = StringTable.get(resource)
}

/**
 * Sets the text of this [UITextButton] from a string resource.
 */
fun UITextButton.setText(resource: StringResource) {
    text = StringTable.get(resource)
}
