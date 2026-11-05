package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.*
import com.osudroid.ui.ISkinnable
import com.osudroid.ui.v2.*
import com.osudroid.utils.allModsInstances
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.themed


/**
 * The icon for a mod in the mod menu.
 */
class ModIcon(val mod: Mod) : UIContainer(), ISkinnable {

    private var shouldUpdateTexture = true

    constructor(acronym: String): this(allModsInstances.find { it.acronym.equals(acronym, ignoreCase = true) }!!)


    private fun setupContent() {
        clearChildren()

        val texture = SkinTextures[mod.iconTextureName]

        if (texture != null) {
            backgroundColor = Color4.Transparent

            +OsuSkinnableSprite(mod.iconTextureName).apply {
                width = fill()
                height = fill()
            }
        } else {
            backgroundColor = Verktex.theme.palette.accent.shade(0.1f)

            +UIText().apply {
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
        if (acronymText is UIText && acronymText.measuredSize.y > 0f) {
            acronymText.scale = bounds.height * 0.6f / acronymText.measuredSize.y
        }

        radius = (bounds.height * 0.2f).px

        super.onUpdate(clock)
    }

    override fun onSkinChanged() {
        shouldUpdateTexture = true
    }

}
