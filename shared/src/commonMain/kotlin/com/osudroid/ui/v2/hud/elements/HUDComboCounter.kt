package com.osudroid.ui.v2.hud.elements

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.transformSequence

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * The combo counter of legacy skins, animated as osu!lazer's.
 *
 * An increment pops a bigger, additive copy of the new combo out at once, and the displayed combo follows
 * [POP_OUT_SMALL_DELAY] later with a small pop. A combo break rolls the displayed combo down to zero, at
 * [ROLLING_DURATION] per combo, and hides it. The combo is hidden while it's zero.
 *
 * Reference: https://github.com/ppy/osu/blob/master/osu.Game/Skinning/LegacyDefaultComboCounter.cs
 */
class HUDComboCounter : HUDElement() {


    private val popOutCount = if (Settings[OsuSettings.AnimateComboText]) SpriteFont(OsuSkin.get().comboPrefix).also {

        it.alpha = 0f
        it.text = "0x"
        it.spacing = -OsuSkin.get().comboOverlap
        it.blendMode = BlendMode.Additive
        it.useFixedDigitWidth()

        +it

    } else null

    private val displayedCountTextSprite = SpriteFont(OsuSkin.get().comboPrefix).also {

        it.alpha = 0f
        it.text = "0x"
        it.spacing = -OsuSkin.get().comboOverlap
        it.useFixedDigitWidth()

        add(0, it)
    }


    // The combo of the play.
    private var current = 0

    // The combo shown by the displayed text.
    private var displayedCount = 0

    // Whether the displayed combo is rolling down after a break.
    private var isRolling = false
    private var rollingFrom = 0
    private var rollingTo = 0
    private var rollingDuration = 0f
    private var rollingElapsed = 0f

    // The time left before the displayed combo follows an increment, negative when none is pending.
    private var popOutSmallDelay = -1f

    // The time left of the displayed text's fade out, negative when it isn't fading out.
    private var fadeOutTimeLeft = -1f


    fun setCombo(value: Int) {

        if (current == value) {
            return
        }

        val previous = current
        current = value

        // Means the animation is disabled by the user.
        if (popOutCount == null) {
            displayedCount = value
            displayedCountTextSprite.text = "${value}x"
            setDisplayedAlpha(if (value == 0) 0f else 1f)
            return
        }

        if (value == 0) {
            onCountRolling(displayedCount, value)
            isRolling = true
            return
        }

        // A roll that's still going ends where it was going.
        if (isRolling) {
            isRolling = false
            setDisplayedCount(rollingTo)
        }
        setDisplayedCount(previous)

        if (previous + 1 == value) {
            onCountIncrement(previous, value)
        } else {
            onCountChange(value)
        }
    }


    private fun setDisplayedCount(value: Int) {
        if (displayedCount == value) {
            return
        }

        when {
            isRolling -> onDisplayedCountRolling(value)
            displayedCount + 1 == value -> onDisplayedCountIncrement(value)
            else -> onDisplayedCountChange(value)
        }

        displayedCount = value
    }

    private fun onCountIncrement(currentValue: Int, newValue: Int) {
        popOutSmallDelay = -1f

        if (displayedCount < currentValue) {
            setDisplayedCount(displayedCount + 1)
        }

        setDisplayedAlpha(1f)
        transformPopOut(newValue)

        popOutSmallDelay = POP_OUT_SMALL_DELAY
    }

    private fun onCountRolling(currentValue: Int, newValue: Int) {
        popOutSmallDelay = -1f

        // Hides the displayed combo if it was increasing from 0 to 1 but didn't finish.
        if (currentValue == 0 && newValue == 0) {
            fadeOutDisplayed()
        }

        rollingFrom = currentValue
        rollingTo = newValue
        rollingDuration = abs(currentValue - newValue) * ROLLING_DURATION
        rollingElapsed = 0f
    }

    private fun onCountChange(newValue: Int) {
        popOutSmallDelay = -1f

        if (newValue == 0) {
            setDisplayedAlpha(0f)
        }

        setDisplayedCount(newValue)
    }

    private fun onDisplayedCountRolling(newValue: Int) {
        if (newValue == 0) {
            fadeOutDisplayed()
        } else {
            setDisplayedAlpha(1f)
        }

        transformNoPopOut(newValue)
    }

    private fun onDisplayedCountChange(newValue: Int) {
        setDisplayedAlpha(if (newValue == 0) 0f else 1f)
        transformNoPopOut(newValue)
    }

    private fun onDisplayedCountIncrement(newValue: Int) {
        setDisplayedAlpha(1f)
        transformPopOutSmall(newValue)
    }


    private fun transformPopOut(newValue: Int) {
        val popOutCount = popOutCount ?: return

        popOutCount.clearTransforms()
        popOutCount.text = "${newValue}x"

        popOutCount.scale = 1.56f
        popOutCount.scaleTo(1f, BIG_POP_OUT_DURATION)

        popOutCount.alpha = 0.6f
        popOutCount.fadeOut(BIG_POP_OUT_DURATION)
    }

    private fun transformNoPopOut(newValue: Int) {
        displayedCountTextSprite.clearTransforms()
        displayedCountTextSprite.text = "${newValue}x"
        displayedCountTextSprite.scale = 1f
    }

    private fun transformPopOutSmall(newValue: Int) {
        displayedCountTextSprite.clearTransforms()
        displayedCountTextSprite.text = "${newValue}x"
        displayedCountTextSprite.scale = 1f

        displayedCountTextSprite.transformSequence {
            scaleTo(1.1f, SMALL_POP_OUT_DURATION / 2f, Easing.InQuad)
            then()
            scaleTo(1f, SMALL_POP_OUT_DURATION / 2f, Easing.OutQuad)
        }
    }


    // The displayed text's alpha isn't a transform, so changing it leaves its scale transforms alone.
    private fun setDisplayedAlpha(value: Float) {
        fadeOutTimeLeft = -1f
        displayedCountTextSprite.alpha = value
    }

    private fun fadeOutDisplayed() {
        if (fadeOutTimeLeft < 0f && displayedCountTextSprite.alpha > 0f) {
            fadeOutTimeLeft = FADE_OUT_DURATION
        }
    }


    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        setCombo(game.stat.combo)
    }

    override fun onUpdate(clock: FrameClock) {
        val elapsed = clock.elapsedFrameTime

        if (popOutSmallDelay >= 0f) {
            popOutSmallDelay -= elapsed

            if (popOutSmallDelay < 0f) {
                setDisplayedCount(displayedCount + 1)
            }
        }

        if (isRolling && displayedCount != rollingTo) {
            rollingElapsed += elapsed

            val progress = if (rollingDuration > 0f) min(1f, rollingElapsed / rollingDuration) else 1f
            setDisplayedCount((rollingFrom + (rollingTo - rollingFrom) * progress).roundToInt())
        }

        if (fadeOutTimeLeft >= 0f) {
            val alpha = displayedCountTextSprite.alpha
            // The rest of the fade out from where it is, linearly.
            displayedCountTextSprite.alpha = if (fadeOutTimeLeft > elapsed) alpha * (1f - elapsed / fadeOutTimeLeft) else 0f
            fadeOutTimeLeft = if (fadeOutTimeLeft > elapsed) fadeOutTimeLeft - elapsed else -1f
        }

        updateScaleOrigins()

        super.onUpdate(clock)
    }

    /**
     * Anchored at the bottom left, as it is by default, the texts scale from where osu!lazer scales them: a bit below
     * their centre, the pop out a bit to the left, as osu!stable does. Anchored elsewhere, they scale from the anchor.
     */
    private fun updateScaleOrigins() {
        val height = displayedCountTextSprite.measuredSize.y
        val isBottom = anchor.y >= 1f && height > 0f
        val originY = if (isBottom) (FONT_HEIGHT_RATIO * height + VERTICAL_OFFSET) / height else anchor.y

        displayedCountTextSprite.scaleOrigin = Vec2(anchor.x, originY)

        val popOutCount = popOutCount ?: return
        val popOutWidth = popOutCount.measuredSize.x
        val isLeft = anchor.x <= 0f && popOutWidth > 0f

        popOutCount.scaleOrigin = Vec2(if (isLeft) POP_OUT_OFFSET_X / popOutWidth else anchor.x, originY)
        popOutCount.translationX = (if (isLeft) -POP_OUT_OFFSET_X else 0f).px
    }


    companion object {
        const val BIG_POP_OUT_DURATION = 0.3f
        const val SMALL_POP_OUT_DURATION = 0.1f

        // When the displayed combo follows an increment.
        private const val POP_OUT_SMALL_DELAY = BIG_POP_OUT_DURATION - 0.14f

        private const val FADE_OUT_DURATION = 0.1f

        // Per combo rolled down.
        private const val ROLLING_DURATION = 0.02f

        private const val FONT_HEIGHT_RATIO = 0.625f
        private const val VERTICAL_OFFSET = 9f
        private const val POP_OUT_OFFSET_X = 3f
    }

}
