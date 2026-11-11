package com.osudroid.ui.v2

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.text.UIText
import kotlin.concurrent.Volatile
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

/**
 * The scene shown while the game is loading, created by the host when it starts.
 */
class SplashScene : UIScene() {

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

    companion object {

        /**
         * How far the game's startup is, from 0 to 100, shown below the logo.
         */
        @JvmStatic
        @Volatile
        var progress = 0

        /**
         * What the startup is doing, shown below the progress, nothing if null.
         */
        @JvmStatic
        @Volatile
        var info: String? = null
    }

    private val loading = UISprite(SkinTextures["loading_start"]).also {
        it.anchor = Anchor.Center
        it.origin = Anchor.Center
        it.scale = 0.4f
        it.alpha = 0f
        +it
    }

    private val progressText = UIText().also {
        it.anchor = Anchor.Center
        it.origin = Anchor.TopCenter
        it.y = 100f.dip
        +it
    }

    private val infoText = UIText().also {
        it.anchor = Anchor.BottomCenter
        it.origin = Anchor.BottomCenter
        it.y = (-16f).dip
        +it
    }

    private var isStarting = true


    override fun onUpdate(clock: FrameClock) {
        super.onUpdate(clock)


        if (isStarting) {
            loading.alpha = minOf(1f, loading.alpha + 0.1f)
            loading.rotationZ = (loading.rotationZ + 180f * clock.elapsedFrameTime) % 360f
        }

        progressText.text = "$progress %"
        info?.let { infoText.text = it }
    }

    /**
     * Fades out the loading indicators and plays the welcome animation.
     */
    fun playWelcomeAnimation() = Verktex.withTreeLock {
        isStarting = false

        loading.fadeOut(0.2f)
        progressText.removeSelf()
        infoText.removeSelf()

        SkinSounds["welcome"].play()
        SkinSounds["welcome_piano"].play()

        +UISprite(SkinTextures["welcome"]).apply {
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
