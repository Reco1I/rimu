package com.osudroid.ui.v2.modmenu

import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.modmenu.ModMenu.addMod
import com.osudroid.ui.v2.modmenu.ModMenu.removeMod
import com.osudroid.utils.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text

class ModMenuPresetsSection : ModMenuSection("Presets") {


    private val addButton: UITextButton

    private var modPresets = listOf<ModPreset>()


    init {
        minWidth = 192f.dip

        addButton = UITextButton().apply {
            width = fill()
            leadingIcon = faSolidIcon(FAIcon.Plus)
            text = "Add preset"
            onActionUp = {
                ModPresetsForm(this@ModMenuPresetsSection).show()
            }
        }
        toggleContainer += addButton
    }


    fun saveModPreset(name: String) {

        if (name.isEmpty()) {
            return
        }

        val modPreset = ModPreset(
            name = name,
            serializedMods = ModMenu.enabledMods.serializeMods(includeIrrelevantMods = true)
        )

        DatabaseManager.modPresetTable.insert(modPreset)
        loadPresets()
        onModsChanged()
    }

    fun loadPresets() {
        toggleContainer.filter { it is ModPresetToggle }.forEach { it.removeSelf() }

        modPresets = DatabaseManager.modPresetTable.getAll()

        for (preset in modPresets) {

            preset.mods = deserializeMods(preset.serializedMods)
            toggleContainer += ModPresetToggle(preset)
        }
    }


    fun onModsChanged() {
        val enabledMods = ModMenu.enabledMods

        addButton.isEnabled = enabledMods.isNotEmpty()

        toggleContainer.forEach { toggle ->
            if (toggle is ModPresetToggle) {
                toggle.isSelected = toggle.preset.mods == enabledMods
            }
        }
    }

    override fun onSearchTermUpdate(searchTerm: String) {
        toggleContainer.forEach { toggle ->
            if (toggle is ModPresetToggle) {
                toggle.isVisible = toggle.preset.name.searchContiguously(searchTerm)
            }
        }
    }

    inner class ModPresetToggle(val preset: ModPreset) : UIButton() {

        init {
            width = fill()
            height = wrap()
            padding = Padding(12f.dip)

            onActionUp = {
                if (isSelected) {
                    ModMenu.clear()
                } else {
                    ModMenu.modToggles.forEach { toggle ->
                        val presetMod = preset.mods[toggle.mod::class]

                        if (presetMod != null) {
                            // Prevent operations from modifying mods in the preset.
                            addMod(presetMod.deepCopy())
                        } else {
                            removeMod(toggle.mod)
                        }
                    }
                }
            }

            onActionLongPress = {
                UIMessageDialog().apply {
                    title = "Delete preset"
                    text = "Delete preset \"${preset.name}\"?"

                    addButton(UITextButton().apply {
                        text = "Delete"
                        variant = Variant.Solid
                        onActionUp = {
                            hide()
                            DatabaseManager.modPresetTable.delete(preset)
                            loadPresets()
                        }
                    })

                    addButton(UITextButton().apply {
                        text = "Cancel"
                        onActionUp = {
                            hide()
                        }
                    })
                    show()
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                spacing = 8f.px

                text {
                    text = preset.name
                    fontSize = 14f.dip
                }

                +ModsIndicator().apply {
                    iconSize = 16f.dip
                    spacing = (-5f).px
                    mods = preset.mods.values
                }
            }
        }

    }
}