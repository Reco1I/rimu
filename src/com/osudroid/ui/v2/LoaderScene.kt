package com.osudroid.ui.v2

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import ru.nsu.ccfit.zuev.osu.Config

class LoaderScene : UIScene() {

    init {
        sprite {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            scaleType = UISprite.ScaleType.Crop
            textureRegion = SkinTextures["menu-background"]

            if (!Config.isSafeBeatmapBg()) {
                textureRegion = Textures["::background"] ?: textureRegion
            }
        }

        box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            alpha = 0.9f
            style = Style { theme ->
                backgroundColor = theme.palette.accent * 0.1f
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
