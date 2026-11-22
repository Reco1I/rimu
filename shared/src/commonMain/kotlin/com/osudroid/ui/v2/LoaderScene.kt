package com.osudroid.ui.v2

import com.reco1l.verktex.data.Background
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.LoadingIndicator
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

class LoaderScene : Scene() {

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
        backgroundComponent = Container().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                color = Verktex.theme.palette.accent.shade(0.1f)
                alpha = 0.9f
            }
        }

        +LoadingIndicator().apply {
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
