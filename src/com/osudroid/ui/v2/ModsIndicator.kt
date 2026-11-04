package com.osudroid.ui.v2

import com.osudroid.mods.*
import com.osudroid.ui.v2.modmenu.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.container.UILinearContainer

class ModsIndicator : UILinearContainer() {

    /**
     * Whether to show mods that are not playable. Defined by [Mod.isUserPlayable].
     */
    var showNonPlayableMods: Boolean = false
        set(value) {
            if (field != value) {
                field = value

                // Reapply mods to update visibility
                val previousMods = mods
                mods = null
                mods = previousMods
            }
        }

    /**
     * The list of mods to display.
     */
    var mods: Iterable<Mod>? = null
        set(value) {
            if (field != value) {
                field = value

                clearChildren()

                value?.forEach { mod ->
                    if (!showNonPlayableMods && !mod.isUserPlayable) {
                        return@forEach
                    }

                    +ModIcon(mod).apply {
                        width = resolvedIconSize
                        height = resolvedIconSize
                    }
                }
            }
        }

    /**
     * The size of the mod icons in this indicator, the size of the theme's body text if null.
     */
    var iconSize: Dimension.Fixed? = null
        set(value) {
            if (field != value) {
                field = value
                applyIconSize()
            }
        }

    // The style runs after the indicator is set up, so it must not overwrite a size set by its user.
    private val resolvedIconSize
        get() = iconSize ?: Verktex.theme.typography.body.size


    init {
        orientation = Orientation.Horizontal
        style = Style {
            spacing = (-5f).px
            applyIconSize()
        }
    }


    private fun applyIconSize() {
        val size = resolvedIconSize

        forEach { icon ->
            icon.width = size
            icon.height = size
        }
    }

}
