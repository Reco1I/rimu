package com.osudroid.ui.v2

import com.osudroid.ui.ISkinnable
import com.reco1l.verktex.ui.UISprite

class OsuSkinnableSprite(val textureLookup: String) : UISprite(), ISkinnable {

    init {
        onSkinChanged()
    }

    override fun onSkinChanged() {
        textureRegion = SkinTextures[textureLookup]
    }

}
