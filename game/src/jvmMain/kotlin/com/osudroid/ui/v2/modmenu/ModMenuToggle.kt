package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.SkinSounds
import com.osudroid.mods.*
import com.osudroid.multiplayer.*
import com.osudroid.utils.searchContiguously
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.spans
import com.reco1l.verktex.ui.text.text
import ru.nsu.ccfit.zuev.osu.*


class ModMenuToggle(var mod: Mod) : UIButton() {

    /**
     * Whether the [Mod] represented by this [ModMenuToggle] is incompatible with one or more enabled [Mod]s.
     */
    var hasIncompatibility = false
        set(value) {
            if (field != value) {
                field = value
                applyCompatibilityState()
            }
        }


    init {
        width = fill()
        height = wrap()
        padding = Padding(14f.dip)
        tintChildren = false

        linearContainer {
            width = fill()
            spacing = 12f.dip

            +ModIcon(mod).apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                width = 21.6f.dip
                height = 21.6f.dip
            }

            text {
                text = mod.name
                width = fill()
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                spans {
                    font(BuiltIn.Fonts.Torus) { +mod.name }
                    +"\n"
                    size(Verktex.theme.typography.smallCaption.size) { +mod.description }
                }
            }
        }

        onActionUp = {
            if (isSelected) {
                ModMenu.removeMod(mod)
                SkinSounds["check-off"].play()
            } else {
                ModMenu.addMod(mod)
                SkinSounds["check-on"].play()
            }
        }

        updateVisibility()
    }

    @JvmOverloads
    fun updateVisibility(searchTerm: String = "") {
        var shouldBeVisible = if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
            mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
        } else {
            true
        }

        if (searchTerm.isNotBlank()) {
            shouldBeVisible = shouldBeVisible &&
                (mod.acronym.equals(searchTerm, true) ||
                    mod.name.searchContiguously(searchTerm, true))
        }

        isVisible = shouldBeVisible
    }

    fun applyCompatibilityState() {
        // Intentionally not using isEnabled here, otherwise the button will not be clickable.
        clearTransforms()
        fadeTo(if (hasIncompatibility) 0.5f else 1f, 0.2f)
    }

}
