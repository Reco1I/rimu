package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.box
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIFillContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.fillContainer
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.theme.Radius
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.pct
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIModal
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Vec4

abstract class UIDialog<T : UIComponent>(val innerContent: T) : UIModal(card = UILinearContainer().apply {
    orientation = Orientation.Vertical
    style = {
        backgroundColor = it.accentColor * 0.15f
        radius = Radius.XL
    }
}) {

    val titleEntity = UIText().apply {
        width = Dimension.FillAvailable
        alignment = Anchor.Center

        style = {
            color = it.accentColor * 0.7f
            fontSize = FontSize.SM
            padding = Vec4(3f.srem)
        }
    }

    val buttonLayout: UIFillContainer


    /**
     * The title of the dialog.
     */
    var title by titleEntity::text


    init {
        detachOnHide = true

        card.apply {
            width = 0.5f.pct
            anchor = Anchor.Center
            origin = Anchor.Center

            +titleEntity

            box {
                width = Dimension.FillAvailable
                height = 2f
                style = {
                    color = it.accentColor.copy(alpha = 0.1f)
                }
            }

            +innerContent

            box {
                width = Dimension.FillAvailable
                height = 2f
                style = {
                    color = it.accentColor.copy(alpha = 0.1f)
                }
            }

            buttonLayout = fillContainer {
                width = Dimension.FillAvailable
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = {
                    padding = Vec4(3f.srem)
                    spacing = 3f.srem
                }
            }
        }
    }


    fun addButton(block: UITextButton.() -> Unit) {
        addButton(UITextButton().apply(block))
    }

    fun addButton(button: UIButton) {
        buttonLayout.apply {
            attachChild(button.apply {
                width = Dimension.FillAvailable
            })
        }

    }
}