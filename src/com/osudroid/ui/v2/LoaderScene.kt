package com.osudroid.ui.v2

import com.reco1l.verktex.*
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.sprite.ScaleType
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

class LoaderScene() : Scene() {

    init {
        sprite {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            scaleType = ScaleType.Crop
            textureRegion = ResourceManager.getInstance().getTexture("menu-background")

            if (!Config.isSafeBeatmapBg()) {
                textureRegion = ResourceManager.getInstance().getTexture("::background") ?: textureRegion
            }
        }

        box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            style = {
                color = it.accentColor * 0.1f
                alpha = 0.9f
            }
        }

        attachChild(UILoadingIndicator().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
        })
    }

}