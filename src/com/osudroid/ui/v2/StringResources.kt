package com.osudroid.ui.v2

import androidx.annotation.StringRes
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.text.UIText
import ru.nsu.ccfit.zuev.osu.helper.StringTable

/**
 * Sets the text of this [UIText] from a string resource.
 */
fun UIText.setText(@StringRes resource: Int) {
    text = StringTable.get(resource)
}

/**
 * Sets the text of this [UITextButton] from a string resource.
 */
fun UITextButton.setText(@StringRes resource: Int) {
    text = StringTable.get(resource)
}
