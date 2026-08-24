package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.*
import com.osudroid.utils.searchContiguously
import com.reco1l.verktex.*
import com.reco1l.verktex.component.*
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.data.Vec4
import com.rian.andengine.modifier.ModifierType
import com.osudroid.mods.*
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.container.Orientation
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
        width = Density.Full
        style + {
            padding = Vec4(3f.srem, 2f.srem)
        }

        fillContainer {
            width = Density.Full
            cullingMode = CullingMode.ScreenBounds
            style = {
                spacing = 3f.srem
            }

            +ModIcon(mod).apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = {
                    width = 1.35f.rem
                    height = 1.35f.rem
                }
            }

            linearContainer {
                width = Density.Full
                orientation = Orientation.Vertical
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                text {
                    text = mod.name
                }

                text {
                    width = Density.Full
                    text = mod.description
                    wrapText = true
                    style = {
                        fontSize = FontSize.XXS
                        alpha = 0.75f
                    }
                }
            }

            onActionUp = {
                if (isSelected) {
                    ModMenu.removeMod(mod)
                    Textures.getInstance().getSound("check-off")?.play()
                } else {
                    ModMenu.addMod(mod)
                    Textures.getInstance().getSound("check-on")?.play()
                }
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
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (hasIncompatibility) 0.5f else 1f, 0.2f)
    }

}