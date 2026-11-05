package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.themed

/**
 * A dialog asking for a text with [input], below an optional message.
 */
open class UIPromptDialog<T : UITextInput>(val input: T) : UIDialog<UILinearContainer>(
    innerContent = UILinearContainer().apply {
        orientation = UILinearContainer.Orientation.Vertical
        width = fill()
        padding = themed { Padding(gap.lg) }
        spacing = themed { gap.md }
    }
) {

    private val messageText = UIText().apply {
        width = fill()
        alignment = Anchor.TopCenter
        isVisible = false
    }

    /**
     * The message shown above the input, hidden while it's empty.
     */
    var text: String
        get() = messageText.text
        set(value) {
            messageText.text = value
            messageText.isVisible = value.isNotEmpty()
        }

    /**
     * The text entered.
     */
    var value by input::value


    init {
        input.width = fill()

        innerContent += messageText
        innerContent += input
    }


    override fun onShown() {
        super.onShown()
        input.focus()
    }
}
