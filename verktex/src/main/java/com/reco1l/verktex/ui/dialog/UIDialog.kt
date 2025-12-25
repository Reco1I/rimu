package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UIModal
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText

/**
 * A modal with a title, a content and a row of buttons below it.
 */
abstract class UIDialog<T : UIComponent>(val innerContent: T) : UIModal(card = UILinearContainer().apply {
    orientation = UILinearContainer.Orientation.Vertical
    width = fill(0.5f)
    anchor = Anchor.Center
    origin = Anchor.Center
    style = Style { theme ->
        backgroundColor = theme.palette.surface
        radius = theme.radius.xl
    }
}) {

    private val titleText = UIText().apply {
        width = fill()
        alignment = Anchor.Center
        style = Style { theme ->
            fontSettings = theme.typography.caption
            color = theme.palette.accent
            padding = Padding(theme.gap.md)
        }
    }

    private val buttonLayout = UILinearContainer().apply {
        orientation = UILinearContainer.Orientation.Horizontal
        width = fill()
        style = Style { theme ->
            padding = Padding(theme.gap.md)
            spacing = theme.gap.sm
        }
    }

    /**
     * The title of the dialog.
     */
    var title by titleText::text


    init {
        detachOnHide = true

        card += titleText
        card += divider()
        card += innerContent
        card += divider()
        card += buttonLayout
    }


    /**
     * Adds a text button to the bottom of the dialog.
     */
    fun addButton(block: UITextButton.() -> Unit) {
        addButton(UITextButton().apply(block))
    }

    /**
     * Adds a button to the bottom of the dialog, buttons share the dialog width equally.
     */
    fun addButton(button: UIButton) {
        button.width = fill()
        buttonLayout += button
    }


    private fun divider() = UIBox().apply {
        width = fill()
        height = 2f.px
        style = Style { theme -> backgroundColor = theme.palette.divider }
    }
}
