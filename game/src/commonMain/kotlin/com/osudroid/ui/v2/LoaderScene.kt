package com.osudroid.ui.v2

import com.osudroid.settings.OsuSettings
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

class LoaderScene : UIScene() {

    init {
        background = UIContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            sprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = UISprite.ScaleType.Crop
                textureRegion = SkinTextures["menu-background"]

                if (!Settings[OsuSettings.SafeBeatmapBackground]) {
                    textureRegion = Textures["::background"] ?: textureRegion
                }
            }

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                alpha = 0.9f
                backgroundColor = Verktex.theme.palette.accent.shade(0.1f)
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
