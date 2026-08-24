package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.UIText

fun UIContainer.iconButton(builder: UIIconButton.() -> Unit) = UIIconButton().apply(builder).also { +it }

/**
 * A button that displays a text.
 */
open class UIIconButton : UIButton() {

    /**
     * The text component of the button.
     */
    protected val iconComponent = UIIcon().apply {
        width = fill()
        anchor = Anchor.Center
        origin = Anchor.Center

        style = Style { theme ->
            iconSize = when (density) {
                Density.Small -> theme.typography.smallBody.size
                Density.Medium -> theme.typography.body.size
                Density.Large -> theme.typography.body.size
            }
        }
    }


    //region Delegation
    var icon by iconComponent::iconCode
    var family by iconComponent::iconFamily
    //endregion


    init {
        +iconComponent
    }

}