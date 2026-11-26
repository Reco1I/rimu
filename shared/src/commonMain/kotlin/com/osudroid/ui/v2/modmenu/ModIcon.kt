package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.*
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.ui.ISkinnable
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.ui.Sprite
import com.osudroid.ui.v2.*
import com.osudroid.utils.allModsInstances
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.themed


/**
 * The icon for a mod in the mod menu.
 */
class ModIcon(val mod: Mod) : Container(), ISkinnable {

    private var shouldUpdateTexture = true

    /**
     * Whether the icon is the skin's texture of the mod when it has one, otherwise always the acronym drawn as text.
     */
    var useSkinTexture = true
        set(value) {
            if (field == value) return
            field = value
            shouldUpdateTexture = true
        }

    constructor(acronym: String): this(allModsInstances.find { it.acronym.equals(acronym, ignoreCase = true) }!!)


    private fun setupContent() {
        clearChildren()

        val texture = if (useSkinTexture) SkinTextures[mod.iconTextureName] else null

        if (texture != null) {
            background = Background.Color(Color4.Transparent)

            +Sprite(skinTexture(mod.iconTextureName)).apply {
                width = fill()
                height = fill()
            }
        } else {
            background = Background.Color(Verktex.theme.palette.accent.shade(0.1f))

            +Text().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                text = mod.acronym
                fontSize = themed { typography.smallBody.size }
                color = themed { palette.accent }
            }
        }

        shouldUpdateTexture = false
    }


    override fun onAttached() {
        if (shouldUpdateTexture) {
            setupContent()
        }

        super.onAttached()
    }

    override fun onUpdate(clock: FrameClock) {
        if (shouldUpdateTexture) {
            setupContent()
        }

        val acronymText = if (childCount > 0) this[0] else null
        if (acronymText is Text && acronymText.measuredSize.y > 0f) {
            acronymText.scale = bounds.height * 0.6f / acronymText.measuredSize.y
        }

        radius = CornerRadius((bounds.height * 0.2f).px)

        super.onUpdate(clock)
    }

    override fun onSkinChanged() {
        shouldUpdateTexture = true
    }

}
