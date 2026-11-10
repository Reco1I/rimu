package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.ModSelection
import com.osudroid.ui.v2.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UITextButton

class ModPresetsForm(section: ModMenuPresetsSection) : UIDialog<UILinearContainer>(innerContent = UILinearContainer().apply {
    orientation = UILinearContainer.Orientation.Vertical
    width = Dimension.FillAvailable
}) {

    init {
        title = "New mod preset"
        staticBackdrop = true

        val nameInput = FormInput().apply {
            label = "Name"
            width = Dimension.FillAvailable
            showResetButton = false
        }

        innerContent.apply {
            +nameInput

            +ModsIndicator().apply {
                mods = ModSelection.mods.values
                padding = Padding(24f.px, 24f.px, 24f.px, 12f.px)
            }
        }

        addButton(UITextButton().apply {
            text = "Save"
            variant = UIButton.Variant.Solid
            onActionUp = {
                section.saveModPreset(nameInput.value)
                hide()
            }
        })

        addButton(UITextButton().apply {
            text = "Cancel"
            onActionUp = {
                hide()
            }
        })
    }
}