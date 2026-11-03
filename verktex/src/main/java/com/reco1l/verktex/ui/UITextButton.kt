package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.UIText

fun UIContainer.textButton(builder: UITextButton.() -> Unit) = UITextButton().apply(builder).also { +it }

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

        style = Style { theme ->
            color = if (variant == Variant.Solid) theme.palette.contentOnSolid else Color4.White

            val size = theme.buttonStyle[density]
            fontSettings = theme.typography.body
            fontSize = size.fontSize
            iconSpacing = size.iconSpacing
        }
        alignment = Anchor.Center
    }


    //region Delegation

    var text by textComponent::text
    var alignment by textComponent::alignment
    var leadingIcon by textComponent::leadingIcon
    var trailingIcon by textComponent::trailingIcon

    //endregion


    init {
        +textComponent
    }

}