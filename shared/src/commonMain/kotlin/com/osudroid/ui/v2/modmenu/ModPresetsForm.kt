package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.dialog.Dialog
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.TextButton

class ModPresetsForm(section: ModMenuPresetsSection) : Dialog<LinearContainer>(innerContent = LinearContainer().apply {
    orientation = LinearContainer.Orientation.Vertical
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
                mods = section.menu.mods.values
                padding = Padding(24f.px, 24f.px, 24f.px, 12f.px)
            }
        }

        addButton(TextButton().apply {
            text = "Save"
            variant = Button.Variant.Solid
            onActionUp = {
                section.saveModPreset(nameInput.value)
                hide()
            }
        })

        addButton(TextButton().apply {
            text = "Cancel"
            onActionUp = {
                hide()
            }
        })
    }
}