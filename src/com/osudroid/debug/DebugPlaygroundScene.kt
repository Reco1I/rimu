package com.osudroid.debug

import com.reco1l.andengine.*
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.utils.ComponentTestScene
import com.reco1l.framework.Color4

object DebugPlaygroundScene : UIScene() {
    init {
        //attachChild(ComponentTestScene)
        container {
            width = Size.Full
            height = Size.Full

            linearContainer {
                anchor = Anchor.Center
                origin = Anchor.Center
                spacing = 10f
                orientation = Orientation.Vertical

                text {
                    text = "Hello, World!"
                }

                box {
                    width = 100f
                    height = 100f
                    radius = 20f
                    color = Color4.White
                }

                triangle {
                    width = 100f
                    height = 100f
                    color = Color4.White
                }

                circle {
                    width = 100f
                    height = 100f
                    color = Color4.White
                    endAngle = 270f
                }

            }
        }
    }
}