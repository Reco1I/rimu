package com.osudroid.ui.v2.skin.legacy

import com.osudroid.ui.v2.game.ScoreNumber
import com.osudroid.ui.v2.skin.SpinnerPiece
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.ClipContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.scaleTo
import kotlin.math.min
import kotlin.random.Random
import com.osudroid.ui.skinning.OsuSkin

private fun centeredSprite(texture: String) = Sprite().apply {
    origin = Anchor.Center
    this.texture = skinTexture(texture)
}

/**
 * osu!stable's old style spinner (`spinner-background`, `-circle`, `-metre`, `-approachcircle`), as osu!lazer's
 * `LegacyOldStyleSpinner`: the background as wide as the screen, the metre filling it by bars from the bottom, the
 * circle turning with the cursor, and "spin" and "clear" texts.
 */
class LegacyClassicSpinnerPiece : SpinnerPiece() {

    private val spinnerBackground = centeredSprite("spinner-background")
    private val circle = centeredSprite("spinner-circle")
    private val approachCircle = centeredSprite("spinner-approachcircle")
    private val spinText = centeredSprite("spinner-spin")
    private val clearText = centeredSprite("spinner-clear")
    private val bonusScore = ScoreNumber(0f, 0f, "", 1.1f, true)

    // The metre fills from the bottom: the sprite keeps its size while the container shows a growing part of it
    // from its bottom, as osu!stable does.
    private val metreSprite = Sprite().apply {
        scaleType = ScaleType.Stretch
        texture = skinTexture("spinner-metre")
        width = fill()
        height = fill()
    }

    private val metre = ClipContainer().apply {
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
        clipAnchor = Anchor.BottomLeft
        clipFactorY = 0f
        +metreSprite
    }

    // The regions the sizes were derived from, see layoutToBackground.
    private var laidOutBackground: Texture.Region? = null
    private var laidOutMetre: Texture.Region? = null
    private var isLaidOut = false
    private var laidOutVisibleWidth = 0f

    override val isBelowLayer: Boolean
        get() = true


    override fun layout(centerX: Float, centerY: Float, visibleWidth: Float) {
        spinnerBackground.setPosition(centerX, centerY)
        circle.setPosition(centerX, centerY)
        metre.setPosition(centerX, centerY)
        approachCircle.setPosition(centerX, centerY)
        spinText.setPosition(centerX, centerY + TEXT_OFFSET)
        clearText.setPosition(centerX, centerY - TEXT_OFFSET)
        bonusScore.setPosition(centerX, centerY + 100)

        layoutToBackground(visibleWidth)
    }

    /**
     * Scales the background to the width of the screen and the metre as it, over the same centre, again whenever
     * their textures changed, as when the skin does, or the window did.
     */
    private fun layoutToBackground(visibleWidth: Float = laidOutVisibleWidth) {
        val backgroundTexture = spinnerBackground.textureRegion
        val metreTexture = metreSprite.textureRegion

        if (isLaidOut && backgroundTexture === laidOutBackground && metreTexture === laidOutMetre && visibleWidth == laidOutVisibleWidth) {
            return
        }
        isLaidOut = true
        laidOutBackground = backgroundTexture
        laidOutMetre = metreTexture
        laidOutVisibleWidth = visibleWidth

        spinnerBackground.scale = if (backgroundTexture != null) visibleWidth / backgroundTexture.displayWidth else 1f

        // The metre matches the background: its own size, so it isn't stretched into the background's when their
        // textures differ, and the background's scale and centre, so both line up. Sized to the screen instead, it
        // would be cut to the height of its layer, the background being taller than the screen.
        metre.setSize(metreTexture?.displayWidth ?: 0f, metreTexture?.displayHeight ?: 0f)
        metre.scale = spinnerBackground.scaleX
    }

    override fun onUpdate(clock: FrameClock) {
        super.onUpdate(clock)

        // The textures may be found after the spinner is laid out.
        if (parent != null) {
            layoutToBackground()
        }
    }

    override fun start(hitTime: Float, duration: Float, timePreempt: Float, isBackgroundVisible: Boolean, isApproachCircleVisible: Boolean) {
        spinnerBackground.isVisible = isBackgroundVisible
        metre.clipFactorY = 0f
        spinnerBackground.color = OsuSkin.current.utils.spinnerBackgroundColor

        add(0, spinText)

        if (isApproachCircleVisible) {
            add(0, approachCircle)
            approachCircle.alpha = 0f

            approachCircle.choreographAt(hitTime) { sequence -> sequence
                .fadeTo(0.75f)
                .fadeTo(1f, duration)
                .scaleTo(2f)
                .scaleTo(0f, duration) }
        }

        add(0, circle)
        add(0, metre)
        add(0, spinnerBackground)

        val fadeDuration = timePreempt * 0.25f
        val fadeInStartTime = hitTime - fadeDuration

        if (spinnerBackground.isVisible) {
            spinnerBackground.alpha = 0f
            spinnerBackground.choreographAt(fadeInStartTime) { sequence -> sequence.fadeIn(fadeDuration) }
        }

        circle.alpha = 0f
        circle.choreographAt(fadeInStartTime) { sequence -> sequence.fadeIn(fadeDuration) }

        metre.alpha = 0f
        metre.choreographAt(fadeInStartTime) { sequence -> sequence.fadeIn(fadeDuration) }

        spinText.alpha = 0f
        spinText.choreographAt(fadeInStartTime) { sequence -> sequence
            .fadeIn(fadeDuration)
            .then(timePreempt / 2)
            .fadeOut(fadeDuration) }
    }

    override fun update(rotation: Float, progress: Float) {
        circle.rotationZ = rotation
        metre.clipFactorY = metreFill(progress)
    }

    override fun playCleared(animate: Boolean): Component {
        if (animate) {
            clearText.alpha = 0f
            clearText.fadeIn(0.25f)
            clearText.scale = 1.5f
            clearText.scaleTo(1f, 0.25f)
        }
        return clearText
    }

    override fun playBonus(score: Int, animate: Boolean): Component {
        bonusScore.setText(score.toString())
        return bonusScore
    }

    override fun end() {
        for (piece in arrayOf<Component>(clearText, spinText, spinnerBackground, approachCircle, circle, metre, bonusScore)) {
            piece.clearAnimations()
            piece.removeSelf()
        }
    }

    /**
     * The fraction of the metre that's shown for the given progress: it fills by whole bars, the one being
     * filled blinking with a chance that grows as it fills, even once the spinner is cleared, unless the skin's
     * SpinnerNoBlink turns the blinking off.
     *
     * Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyOldStyleSpinner.cs
     */
    private fun metreFill(progress: Float): Float {
        val isBlinking = !OsuSkin.current.utils.spinnerNoBlink
        var percent = progress * 100

        if (isBlinking) {
            percent = min(99f, percent)
        }

        var bars = percent.toInt() / 10

        if (isBlinking && Random.nextFloat() < (percent.toInt() % 10) / 10f) {
            bars++
        }

        return bars / METRE_BARS.toFloat()
    }

    private companion object {
        const val METRE_BARS = 10

        // How far the "spin" and "clear" texts are below and above the centre: halfway to the edges of the screen.
        const val TEXT_OFFSET = 180f
    }
}

/**
 * osu!stable's new style spinner (`spinner-top`, `-bottom`, `-middle`, `-middle2`, `-glow`), as osu!lazer's
 * `LegacyNewStyleSpinner`: discs turning with the cursor and growing with the progress, the middle reddening and the
 * glow lighting up, blinking white on each bonus spin.
 */
class LegacyModernSpinnerPiece : SpinnerPiece() {

    private val middle = centeredSprite("spinner-middle")
    private val middle2 = centeredSprite("spinner-middle2")
    private val bottom = centeredSprite("spinner-bottom")
    private val top = centeredSprite("spinner-top")
    private val glow = centeredSprite("spinner-glow").apply { blendMode = BlendMode.Additive }
    private val bonusScore = ScoreNumber(0f, 0f, "", 1.1f, true)


    override fun layout(centerX: Float, centerY: Float, visibleWidth: Float) {
        for (sprite in arrayOf(middle, middle2, bottom, top, glow)) {
            sprite.setPosition(centerX, centerY)
        }
        bonusScore.setPosition(centerX, centerY + 100)
    }

    override fun start(hitTime: Float, duration: Float, timePreempt: Float, isBackgroundVisible: Boolean, isApproachCircleVisible: Boolean) {
        glow.alpha = 0f
        glow.scale = 0.9f
        glow.setColor(0f, 0.8f, 1f)

        if (isBackgroundVisible) {
            middle.alpha = 0f
            middle.scale = 0.9f
            middle2.alpha = 0f
            middle2.scale = 0.9f
        }

        bottom.alpha = 0f
        bottom.scale = 0.9f

        top.alpha = 0f
        top.scale = 0.9f

        this += glow
        this += bottom
        this += top

        if (isBackgroundVisible) {
            this += middle
            this += middle2
        }

        val fadeIn = arrayListOf<Sprite>(top, bottom)
        if (isBackgroundVisible) {
            fadeIn += middle
            fadeIn += middle2
        }
        for (sprite in fadeIn) {
            sprite.choreographAt(hitTime - timePreempt) { sequence -> sequence.fadeIn(timePreempt) }
        }
    }

    override fun update(rotation: Float, progress: Float) {
        top.rotationZ = rotation
        bottom.rotationZ = rotation / 2

        val scale = 0.9f + progress * 0.1f

        middle.setColor(1f, 1 - progress, 1 - progress)
        top.scale = scale
        bottom.scale = scale

        if (middle.parent != null) {
            middle.scale = scale
            middle2.scale = scale
        }

        glow.alpha = progress * 0.8f
        glow.scale = scale
    }

    override fun playBonus(score: Int, animate: Boolean): Component {
        bonusScore.setText(score.toString())

        if (animate) {
            // Blinks white and goes back to its tint.
            glow.choreographNow { sequence -> sequence
                .colorTo(1f, 1f, 1f, 0.1f)
                .then()
                .colorTo(0f, 0.8f, 1f, 0.1f) }
        }
        return bonusScore
    }

    override fun end() {
        for (piece in arrayOf<Component>(middle, middle2, bottom, top, glow, bonusScore)) {
            piece.clearAnimations()
            piece.removeSelf()
        }
    }
}
