package com.osudroid.ui.v2

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.LoadingIndicator
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

/**
 * The scene shown while the game starts: a loading indicator until the database and the skin are
 * loaded, then the welcome animation.
 */
class SplashScene : Scene() {

    // Back does nothing while the game starts, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            return true
        }
        return false
    }

    // Shown until the database and the skin are loaded.
    private val loading = LoadingIndicator().also {
        it.anchor = Anchor.Center
        it.origin = Anchor.Center
        it.width = 48f.dip
        it.height = 48f.dip
        +it
    }

    /**
     * Fades out the loading indicator and plays the welcome animation.
     */
    fun playWelcomeAnimation() = Verktex.withTreeLock {
        loading.fadeOut(0.2f)

        SkinSounds["welcome"].play()
        SkinSounds["welcome_piano"].play()

        +Sprite(SkinTextures["welcome"]).apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            alpha = 0f
            scaleY = 0f

            fadeIn(2.5f)

            choreograph {
                at(currentClockTime)
                addScaleY(1f, 0.25f)
                then()
                scaleTo(1.1f, 2.25f)
            }
        }
    }
}
