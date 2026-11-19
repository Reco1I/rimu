package com.osudroid.ui.v2.skin

import com.osudroid.game.HitResult
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.shape.PathShape

/*
 * What the hit objects ask of the pieces of a skin. The object places a piece, scales it by the object's scale, and
 * plays on it what doesn't depend on the skin (fading in, Hidden, dimming, the fail animation). The piece draws
 * itself, at the size of the osu!stable texture it stands for (see SkinDimensions), and plays its own reactions to
 * a result at the time it's given, returning when they end so the object lives until then.
 */

/**
 * The circle of a hit circle or a slider's end, with the combo number of the circles that show one.
 */
abstract class MainCirclePiece : Container() {

    /**
     * Sets the colour of the circle: the combo colour, lightened by the object on kiai beats.
     */
    abstract fun setCircleColor(color: Color4)

    /**
     * Sets the number shown, for the circles that show one.
     */
    open fun setComboNumber(value: Int) = Unit

    open fun showNumber() = Unit

    open fun hideNumber() = Unit

    /**
     * Shows a slider's head as the end of the span that comes back to it, after it's hit, or as the head again.
     */
    open fun showAsSliderEnd(value: Boolean) = Unit

    /**
     * Plays the circle being hit from [time] on its clock, with the object's [scale]: osu!stable bursts it.
     *
     * @return The time it ends at.
     */
    abstract fun playHit(scale: Float, time: Float): Float
}

/**
 * The ball following a slider's path.
 */
abstract class SliderBallPiece : Container() {

    /**
     * Sets the combo colour, which a skin may tint the ball with.
     */
    abstract fun setComboColor(color: Color4)

    /**
     * Sets how fast the ball rolls, in pixels on screen per second, for the skins that animate it as it rolls.
     */
    open fun setSpeed(pixelsPerSecond: Float) = Unit

    /**
     * Turns the ball towards the direction it goes, in degrees.
     */
    abstract fun setBallRotation(degrees: Float)

    /**
     * Mirrors the ball, as a skin may ask on the spans going back.
     */
    abstract fun setFlipped(value: Boolean)
}

/**
 * The circle around the ball of a slider while it's tracked. The slider shows and hides it at once without the
 * setting that animates it, and calls these with it.
 */
abstract class FollowCirclePiece : Container() {

    /**
     * Tracking starts, with [remainingTime] seconds left in the slider.
     */
    abstract fun playTrackingStart(scale: Float, remainingTime: Float)

    /**
     * Tracking is lost.
     */
    abstract fun playTrackingEnd(scale: Float)

    /**
     * A tick is hit while tracking.
     */
    abstract fun playTick(scale: Float)

    /**
     * The slider ends at [time], tracked.
     *
     * @return The time it ends at.
     */
    abstract fun playSliderEnd(scale: Float, time: Float): Float
}

/**
 * How a slider's body is drawn: the widths and colours of the [PathShape] it's drawn with.
 */
interface SliderBodyStyle {

    /**
     * Styles [body] for a slider of [bodyColor] with a border of [borderColor]. [skinScale] scales the widths of the
     * skin, which are in the pixels gameplay is laid out in, to the body's space.
     */
    fun apply(body: PathShape, bodyColor: Color4, borderColor: Color4, skinScale: Float)
}

/**
 * The judgement of a hit object (300, 100, miss, a slider point…), shown for the texture [textureName] a hit object
 * was given, and pooled by it.
 */
abstract class JudgementPiece(val textureName: String) : Container() {

    /**
     * Shows the judgement [result] at ([x], [y]) in the space of its parent, for an object of the given [scale],
     * from [time] on its clock. A [spinnerMiss] is a spinner's miss.
     *
     * @return The time its animation ends at.
     */
    abstract fun play(x: Float, y: Float, result: HitResult, scale: Float, time: Float, spinnerMiss: Boolean = false): Float

    companion object {

        /**
         * How long the longest judgement of any skin lasts, in seconds.
         */
        const val DURATION = 1.22f
    }
}
