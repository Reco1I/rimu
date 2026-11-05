package com.reco1l.verktex.ui

import com.reco1l.verktex.ui.text.buildSpannedText
import com.reco1l.verktex.ui.text.SpannedTextBuilder
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.UIText

fun UIContainer.textButton(builder: UITextButton.() -> Unit) = UITextButton().apply(builder).also { +it }

/**
 * Sets the content of this button's text to the one built by [block], see [SpannedTextBuilder].
 */
inline fun UITextButton.spans(block: SpannedTextBuilder.() -> Unit) {
    spannedText = buildSpannedText(block)
}

/**
 * A button that displays a text.
 */
open class UITextButton : UIButton() {

    /**
     * The text component of the button.
     */
    protected val textComponent = UIText().apply {
        width = fill()
        anchor = Anchor.Center
        origin = Anchor.Center

        color = themed { if (variant == Variant.Solid) palette.contentOnSolid else palette.accent }
        fontSettings = themed { typography.body }
        fontSize = themed { buttonStyle[density].fontSize }
        iconSpacing = themed { buttonStyle[density].iconSpacing }
        alignment = Anchor.Center
    }


    //region Delegation

    var text by textComponent::text
    var spannedText by textComponent::spannedText
    var alignment by textComponent::alignment
    var leadingIcon by textComponent::leadingIcon
    var trailingIcon by textComponent::trailingIcon

    //endregion


    init {
        +textComponent
    }

}