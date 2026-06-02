package com.osudroid.debug

import com.reco1l.andengine.*
import com.reco1l.andengine.theme.Size

object DebugPlaygroundScene : UIScene() {
    init {
        container {
            width = Size.Full
            height = Size.Full

            box {
                width = 100f
                height = 100f
                radius = 25f
                anchor = Anchor.Center
                origin = Anchor.Center

            }
        }
    }
}