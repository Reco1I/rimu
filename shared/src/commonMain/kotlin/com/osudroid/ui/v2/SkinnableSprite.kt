package com.osudroid.ui.v2

import com.osudroid.ui.ISkinnable
import com.reco1l.verktex.ui.Sprite

class OsuSkinnableSprite(val textureLookup: String) : Sprite(), ISkinnable {

    init {
        onSkinChanged()
    }

    override fun onSkinChanged() {
        textureRegion = SkinTextures[textureLookup]
    }

}
