package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.IMigratableMod
import com.osudroid.mods.ModType
import com.osudroid.utils.allModsInstances
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.ui.container.linearContainer

/**
 * The mods tab of the [ModMenu]: the presets and a section of toggles for each type of mod, side by side.
 */
class ModSectionsView(menu: ModMenu) : ScrollableContainer() {

    /**
     * The sections of mods, one for each [ModType] with mods the user can play.
     */
    val modSections = mutableListOf<ModMenuSection>()

    /**
     * The toggles of every section.
     */
    val modToggles = mutableListOf<ModMenuToggle>()

    /**
     * The section of the user's mod presets.
     */
    val presetsSection: ModMenuPresetsSection


    init {
        width = fill()
        height = fill()
        scrollAxes = Axis.X

        lateinit var presets: ModMenuPresetsSection

        linearContainer {
            orientation = Orientation.Horizontal
            width = wrap()
            height = fill()
            padding = horizontalPadding(16f.dip)
            spacing = 16f.dip

            presets = ModMenuPresetsSection(menu)
            +presets

            ModType.entries.forEach { type ->
                val sectionToggles = allModsInstances
                    .filter { it !is IMigratableMod && it.isUserPlayable && it.type == type }
                    .map { ModMenuToggle(it, menu.mods) }

                if (sectionToggles.isEmpty()) {
                    return@forEach
                }

                modToggles.addAll(sectionToggles)

                val section = ModMenuSection(type.stringId.toString(), sectionToggles)
                +section
                modSections.add(section)
            }
        }

        presetsSection = presets
    }


    /**
     * Filters the toggles and the presets by [searchTerm].
     */
    fun onSearchTermUpdate(searchTerm: String) {
        modSections.forEach { it.onSearchTermUpdate(searchTerm) }

        if (presetsSection.isVisible) {
            presetsSection.onSearchTermUpdate(searchTerm)
        }
    }

    /**
     * Shows the toggles matching [searchTerm] and whether they can be selected with the selected mods.
     */
    fun updateModButtonVisibility(searchTerm: String) {
        modToggles.forEach {
            it.updateVisibility(searchTerm)
            it.applyCompatibilityState()
        }

        // Update section visibility after toggles have been updated since it may not need to be visible anymore.
        modSections.forEach { it.updateVisibility() }
    }
}
