package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.shape.Arc
import com.reco1l.verktex.ui.shape.Box

/**
 * The spinner of the default skin, modelled on osu!lazer's `ArgonSpinnerDisc`: a dark disc in a white ring, ticks
 * around it turning with the spins, a progress ring filling clockwise from the top and a centre growing with it. The
 * whole disc grows with the progress, as osu!stable's new style spinner, and is as big as its `spinner-top`. Its
 * texts are in Fredoka, its bonus in Anta as the HUD's numbers.
 */
class DefaultSpinnerPiece : SpinnerPiece() {

    private val size = SkinDimensions.SPINNER_TOP.width

    private val fill = disc(size).apply { color = Color4.Black.alpha(0.5f) }

    private val outerRing = ring(size, size * 0.012f)

    private val glow = ring(size, size * 0.012f, GLOW).apply {
        shadow = Shadow(GLOW, blur = (size * 0.05f).px, cutout = true)
        alpha = 0f
    }

    private val progressRing = Arc().apply {
        width = (size * 0.9f).px
        height = (size * 0.9f).px
        anchor = Anchor.Center
        origin = Anchor.Center
        thickness = size * 0.02f
        roundedCaps = true
        color = Color4.White
        progress = 0f
    }

    // Turn with the spins.
    private val ticks = Container().apply {
        width = size.px
        height = size.px
        anchor = Anchor.Center
        origin = Anchor.Center

        for (i in 0 until TICKS) {
            // Each tick stands in a container turned to its angle, at the top of it.
            +Container().apply {
                width = size.px
                height = size.px
                anchor = Anchor.Center
                origin = Anchor.Center
                rotationZ = i * 360f / TICKS

                +Box().apply {
                    width = (size * 0.012f).px
                    height = (size * 0.05f).px
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    y = (size * 0.1f).px
                    radius = CornerRadius((size * 0.006f).px)
                    color = Color4.White.alpha(0.6f)
                }
            }
        }
    }

    private val centre = disc(size * 0.12f).apply { color = Color4.White }

    // Its size is fixed in the space gameplay is laid out in, which is shorter than the disc on wide screens: limited
    // to the space's height, it would be squeezed into an ellipse.
    private val spinnerDisc = object : Container() {
        override fun adjustParentConstraints(constraints: LayoutConstraints) =
            constraints.copy(maxWidth = Float.POSITIVE_INFINITY, maxHeight = Float.POSITIVE_INFINITY)
    }.apply {
        width = size.px
        height = size.px
        origin = Anchor.Center

        +fill
        +glow
        +outerRing
        +ticks
        +progressRing
        +centre
    }

    private val spinText = fredokaText(36f).apply { text = "SPIN" }
    private val clearText = fredokaText(60f).apply { text = "CLEAR" }
    private val bonusScore = antaText(48f)

    // Where the bonus is centred, in the layer's space.
    private var bonusX = 0f
    private var bonusY = 0f


    override fun layout(centerX: Float, centerY: Float, spaceWidth: Float) {
        spinnerDisc.setPosition(centerX, centerY)
        placeText(spinText, centerX, centerY + size * 0.6f)
        placeText(clearText, centerX, centerY - size * 0.25f)
        bonusX = centerX
        bonusY = centerY + size * 0.25f
        placeText(bonusScore, bonusX, bonusY)
    }

    override fun start(hitTime: Float, duration: Float, timePreempt: Float, isBackgroundVisible: Boolean, isApproachCircleVisible: Boolean) {
        fill.isVisible = isBackgroundVisible
        progressRing.progress = 0f
        ticks.rotationZ = 0f
        centre.scale = 1f
        glow.alpha = 0f

        this += spinnerDisc
        this += spinText

        spinnerDisc.alpha = 0f
        spinnerDisc.scale = MIN_SCALE
        spinnerDisc.choreographAt(hitTime - timePreempt) { sequence -> sequence.fadeIn(timePreempt, Easing.OutQuint) }

        spinText.alpha = 0f
        spinText.choreographAt(hitTime - timePreempt) { sequence -> sequence
            .fadeIn(timePreempt / 2)
            .then(timePreempt / 2 + duration * 0.25f)
            .fadeOut(0.3f) }
    }

    override fun update(rotation: Float, progress: Float) {
        ticks.rotationZ = rotation
        progressRing.progress = progress
        centre.scale = 1f + progress
        spinnerDisc.scale = MIN_SCALE + progress * (1f - MIN_SCALE)
        glow.alpha = progress * 0.8f
    }

    override fun playCleared(animate: Boolean): Component {
        if (animate) {
            clearText.clearAnimations()
            // It fades in only: scaling it would move it, as it's placed from its top left corner.
            clearText.alpha = 0f
            clearText.choreographNow { sequence -> sequence.fadeIn(0.25f, Easing.OutQuint) }
        }
        return clearText
    }

    override fun playBonus(score: Int, animate: Boolean): Component {
        bonusScore.text = score.toString()
        placeText(bonusScore, bonusX, bonusY)

        if (animate) {
            // The centre flashes with each bonus spin.
            centre.clearAnimations()
            centre.choreographNow { sequence -> sequence.colorTo(GLOW).colorTo(Color4.White, 0.2f) }

        }
        return bonusScore
    }

    override fun end() {
        for (piece in arrayOf<Component>(spinnerDisc, spinText, clearText, bonusScore, centre)) {
            piece.clearAnimations()
        }
        for (piece in arrayOf<Component>(spinnerDisc, spinText, clearText, bonusScore)) {
            piece.removeSelf()
        }
        centre.color = Color4.White
    }

    private companion object {
        const val TICKS = 12
        const val MIN_SCALE = 0.9f

        // The blue of osu!lazer's GREAT.
        val GLOW = Color4("#66CCFF")
    }
}
