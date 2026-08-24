package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.data.Vec4

class ModPresetsForm(section: ModMenuPresetsSection) : UIDialog<UILinearContainer>(innerContent = UILinearContainer().apply {
    orientation = Orientation.Vertical
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
                mods = ModMenu.enabledMods.values
                padding = Vec4(24f, 24f, 24f, 12f)
            }
        }

        addButton(UITextButton().apply {
            text = "Save"
            isSelected = true
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