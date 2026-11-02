package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.transformSequence

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.*


class HUDComboCounter : HUDElement() {


    private val popOutCount = if (Config.isAnimateComboText()) SpriteFont(OsuSkin.get().comboPrefix).also {

        it.alpha = 0f
        it.text = "0x"
        it.spacing = -OsuSkin.get().comboOverlap

        +it

    } else null

    private val displayedCountTextSprite = SpriteFont(OsuSkin.get().comboPrefix).also {

        it.text = "0x"
        it.spacing = -OsuSkin.get().comboOverlap

        add(0, it)
    }

    private val updateDisplayedCount: (UIComponent) -> Unit = {
        displayedCountTextSprite.text = "${current}x"
    }


    private var current = 0


    fun setCombo(value: Int) {

        if (current == value) {
            return
        }

        // Means the animation is disabled by the user.
        if (popOutCount == null) {
            current = value
            displayedCountTextSprite.text = "${current}x"
            return
        }

        popOutCount.clearTransforms()
        displayedCountTextSprite.clearTransforms()

        // Force update the text to the current combo if the previous
        // modifier haven't finished yet.
        current = value
        updateDisplayedCount(popOutCount)

        popOutCount.text = "${current}x"
        popOutCount.alpha = 0.6f
        popOutCount.scale = 1.56f

        popOutCount.scaleTo(1f, BIG_POP_OUT_DURATION)
        popOutCount.fadeOut(BIG_POP_OUT_DURATION).after(updateDisplayedCount)

        displayedCountTextSprite.scale = 1f

        displayedCountTextSprite.transformSequence {
            scaleTo(1.1f, SMALL_POP_OUT_DURATION / 2f, Easing.InQuad)
            then()
            scaleTo(1f, SMALL_POP_OUT_DURATION / 2f, Easing.OutQuad)
        }
    }


    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        setCombo(game.stat.combo)
    }

    override fun onUpdate(clock: FrameClock) {
        popOutCount?.scaleOrigin = anchor
        displayedCountTextSprite.scaleOrigin = anchor

        super.onUpdate(clock)
    }


    companion object {
        const val BIG_POP_OUT_DURATION = 0.3f
        const val SMALL_POP_OUT_DURATION = 0.1f
    }

}