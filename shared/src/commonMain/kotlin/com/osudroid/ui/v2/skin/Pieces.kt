package com.osudroid.ui.v2.skin

import com.osudroid.game.HitResult
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.ui.text.TextAlign
import com.reco1l.verktex.ui.Component
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
     * Sets the combo colour, which a skin may tint the circle with.
     */
    open fun setComboColor(color: Color4) = Unit

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
 * How a spinner looks, filling the layer it's drawn in (the space gameplay is laid out in, 1280 wide). The spinner
 * judges the spins and gives it their rotation and progress; it shows what it gives back on top of the layer.
 */
abstract class SpinnerPiece : Container() {

    init {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
    }

    /**
     * Whether the spinner is added below what its layer has, as osu!stable's old style spinner, rather than above.
     */
    open val isBelowLayer: Boolean
        get() = false

    /**
     * Places the pieces around ([centerX], [centerY]) in a layer [spaceWidth] wide.
     */
    abstract fun layout(centerX: Float, centerY: Float, spaceWidth: Float)

    /**
     * Shows a spinner from [hitTime] for [duration] seconds, fading in over the [timePreempt] before it. Its
     * background isn't shown with Traceable ([isBackgroundVisible]), nor its approach circle with Hidden
     * ([isApproachCircleVisible]).
     */
    abstract fun start(hitTime: Float, duration: Float, timePreempt: Float, isBackgroundVisible: Boolean, isApproachCircleVisible: Boolean)

    /**
     * Turns the spinner to [rotation], in degrees, at [progress] towards clearing it, from 0 to 1.
     */
    abstract fun update(rotation: Float, progress: Float)

    /**
     * The spinner is cleared now, animated unless it's found cleared after a seek ([animate]).
     *
     * @return What to show on top of the layer, null for nothing.
     */
    open fun playCleared(animate: Boolean): Component? = null

    /**
     * A bonus spin gives [score] in total, animated unless it's found after a seek ([animate]).
     *
     * @return What to show on top of the layer, null for nothing.
     */
    abstract fun playBonus(score: Int, animate: Boolean): Component?

    /**
     * The spinner is judged: takes every piece out, what was shown on top too.
     */
    abstract fun end()
}

/**
 * A follow point, which the connection between two objects places, turns and fades, whatever the skin.
 */
abstract class FollowPointPiece : Container() {

    /**
     * The time, on its clock, the follow point starts to show at, for skins animating it from then.
     */
    open var animationStartTime = 0f
}

/**
 * The look of a cursor, which the cursor grows while pressed and turns for the skins asking for it.
 */
abstract class CursorPiece : Container() {

    /**
     * Whether the cursor's position is its centre rather than its top left corner.
     */
    abstract val isCentred: Boolean
}

/**
 * How a cursor's trail is drawn by its particle system.
 */
interface CursorTrailStyle {

    /**
     * The image of each part of the trail, null for none.
     */
    val texture: Texture.Region?

    /**
     * Whether the trail is continuous along the path of the cursor, additive, rather than separate images left
     * every frame.
     */
    val isContinuous: Boolean

    /**
     * Whether the parts are centred on the cursor's position rather than placed from their top left corner.
     */
    val isCentred: Boolean

    /**
     * Whether the parts are turned as the cursor is.
     */
    val rotates: Boolean
}

/**
 * A number of the HUD in a skin's font (the score, the combo, the accuracy…): the font of a texture skin, or text.
 */
abstract class NumberText : Container() {

    /**
     * The text shown, digits and the characters a skin's font has (`.`, `%`, `x`, `d`, `p`).
     */
    abstract var text: String

    /**
     * The space added between characters, negative to overlap them.
     */
    abstract var spacing: Float

    /**
     * The text whose width the number is measured with instead of its own, so it doesn't move as it changes.
     */
    abstract var measureText: String?

    /**
     * Where the text is in the measured width, see [measureText].
     */
    abstract var textAlign: TextAlign

    /**
     * Whether it's drawn additively, as a combo's pop out.
     */
    abstract var isAdditive: Boolean

    /**
     * Gives every digit the same width, so numbers don't shift as they change.
     */
    abstract fun useFixedDigitWidth()

    /**
     * The height of the digits.
     */
    abstract val digitHeight: Float
}

/**
 * The health bar of the HUD.
 */
abstract class HealthBarPiece : Container() {

    /**
     * Shows the health [hp], from 0 to 1, [secondsElapsed] after the previous update.
     */
    abstract fun update(hp: Float, secondsElapsed: Float)

    /**
     * Flashes for a hit, at the health [hp].
     */
    abstract fun flash(hp: Float)
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
