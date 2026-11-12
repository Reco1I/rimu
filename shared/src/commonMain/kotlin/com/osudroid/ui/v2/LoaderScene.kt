package com.osudroid.ui.v2

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

class LoaderScene : UIScene() {

    // Back does nothing while loading, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            return true
        }
        return false
    }

    init {
        backgroundComponent = UIContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            sprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = ScaleType.Crop
                textureRegion = SkinTextures["menu-background"]

                if (!Settings[OsuSettings.SafeBeatmapBackground]) {
                    textureRegion = Textures["::background"] ?: textureRegion
                }
            }

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                alpha = 0.9f
                background = Background.Color(Verktex.theme.palette.accent.shade(0.1f))
            }
        }

        +UILoadingIndicator().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
        }
    }


    /**
     * Makes this scene the current one.
     */
    fun show() {
        Verktex.withTreeLock { Verktex.pushScene(this) }
    }

}
