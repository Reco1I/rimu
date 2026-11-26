package com.osudroid.ui.v2

import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.mainmenu.RimuLogo
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.LoadingIndicator
import com.reco1l.verktex.ui.transformSequence
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

/**
 * The scene shown while the game starts: a loading indicator until the database and the skin are
 * loaded, then the welcome animation, the rimu! logo of the menu growing in at the center, bigger
 * than the menu shows it ([MainScene.SPLASH_LOGO_SCALE]). The menu then takes the logo from where
 * it's left and stamps it down to its own scale.
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

        // Where the menu's logo is when it shows: in the center, with the menu folded.
        +RimuLogo().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            scaleOrigin = Anchor.Center
            alpha = 0f
            scale = MainScene.SPLASH_LOGO_SCALE * 0.85f

            transformSequence(0.2f) {
                fadeIn(0.8f, Easing.OutCubic)
                scaleTo(MainScene.SPLASH_LOGO_SCALE, 1.6f, Easing.OutCubic)
            }
        }
    }
}
