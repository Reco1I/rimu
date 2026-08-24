package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Anchor
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
            when (density) {
                Density.Small -> {
                    fontSettings = theme.typography.smallBody
                    iconSpacing = theme.gap.sm
                }
                Density.Medium -> {
                    fontSettings = theme.typography.body
                    iconSpacing = theme.gap.md
                }
                Density.Large -> {
                    fontSettings = theme.typography.body
                    iconSpacing = theme.gap.lg
                }
            }
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