package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.reco1l.verktex.*
import com.reco1l.verktex.old.*
import com.reco1l.verktex.component.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.ui.sprite.*
import com.reco1l.verktex.ui.text.*
import com.reco1l.verktex.texture.*
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.data.Color4
import com.osudroid.mods.*
import com.osudroid.ui.ISkinnable
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.UIText
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import javax.microedition.khronos.opengles.*


/**
 * The icon for a mod in the mod menu.
 */
class ModIcon(val mod: Mod) : UIContainer(), ISkinnable {

    private var shouldUpdateTexture = true


    init {
        inheritAncestorsColor = false
    }


    private fun fetchTextureRegion(): TextureRegion? {
        return Textures.getInstance().getTexture(mod.iconTextureName)
            ?.takeUnless { it is BlankTextureRegion }
    }

    private fun setupContent() {
        detachChildren()

        val texture = fetchTextureRegion()

        if (texture != null) {
            backgroundColor = Color4.Transparent

            attachChild(OsuSkinnableSprite(mod.iconTextureName).apply {
                width = fill()
                height = fill()
            })
        } else {
            backgroundColor = Theme.current.accentColor * 0.1f

            attachChild(UIText().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                text = mod.acronym
                fontSize = FontSize.SM
                style = { color = it.accentColor }
            })
        }

        shouldUpdateTexture = false
    }


    override fun onAttached() {
        if (shouldUpdateTexture) {
            setupContent()
        }

        super.onAttached()
    }

    override fun doDraw(gl: GL10, camera: Camera) {

        val acronymText = get<UIComponent>(0)
        if (acronymText is UIText) {
            acronymText.setScale(height * 0.6f / acronymText.contentHeight)
        }

        radius = height * 0.2f

        super.doDraw(gl, camera)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (shouldUpdateTexture) {
            setupContent()
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onSkinChanged() {
        shouldUpdateTexture = true
    }

}