package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Icon
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.shape.PathShape
import kotlin.math.min

/**
 * The body of a slider in the default skin, as osu!lazer's `ArgonSliderBody`: the dark centre of the circles, with
 * a band of the combo colour along its edge, so that the circles at its ends continue it.
 */
object DefaultSliderBodyStyle : SliderBodyStyle {

    // The body and its band are the circles' band and their centre (see DefaultMainCirclePiece), in the pixels of
    // the 128 wide circle: the band from 0.6 to 0.86 of its size.
    private const val RADIUS = 0.86f * 64f
    private const val BAND = (0.86f - 0.6f) * 64f
    private const val BODY_ALPHA = 0.9f

    override fun apply(body: PathShape, bodyColor: Color4, borderColor: Color4, skinScale: Float) {
        body.halfWidth = RADIUS * skinScale
        body.borderWidth = BAND * skinScale
        body.borderColor = bodyColor
        body.shadowWidth = 0f
        body.bodyColor = bodyColor.darken(3f).alpha(BODY_ALPHA)
        body.bodyCenterColor = bodyColor.darken(4f).alpha(BODY_ALPHA)
    }
}

/**
 * The ball of the default skin: a disc of the combo colour in a white ring, with a white dot ahead of it showing the
 * direction it rolls in, at the size of the default `sliderb`.
 */
class DefaultSliderBallPiece : SliderBallPiece() {

    private val size = SkinDimensions.SLIDER_BALL.width

    private val fill = gradientDisc(size)

    // Turns with the direction, carrying the dot.
    private val direction = Container().apply {
        width = size.px
        height = size.px
        anchor = Anchor.Center
        origin = Anchor.Center

        +disc(size * 0.16f).apply {
            color = Color4.White
            anchor = Anchor.CenterRight
            origin = Anchor.CenterRight
            x = (-size * 0.1f).px
        }
    }

    init {
        width = size.px
        height = size.px
        origin = Anchor.Center

        +fill
        +ring(size, size * 0.07f)
        +direction
    }


    override fun setComboColor(color: Color4) {
        fill.colorStart = color
        fill.colorEnd = color.darken(0.5f)
    }

    override fun setBallRotation(degrees: Float) {
        direction.rotationZ = degrees
    }

    // It's symmetrical along the direction it rolls in.
    override fun setFlipped(value: Boolean) = Unit
}

/**
 * The follow circle of the default skin, as osu!lazer's `ArgonFollowCircle`: a white ring around a faint disc of the
 * combo colour, at the size of the default `sliderfollowcircle`. Its reactions are choreography, so they keep up with
 * the gameplay clock.
 */
class DefaultFollowCirclePiece : FollowCirclePiece() {

    private val size = SkinDimensions.FOLLOW_CIRCLE.width

    private val fill = disc(size * 0.96f).apply { color = Color4.White.alpha(FILL_ALPHA) }

    init {
        width = size.px
        height = size.px
        origin = Anchor.Center

        +fill
        +ring(size * 0.96f, size * 0.025f).apply {
            shadow = Shadow(Color4.White.alpha(0.3f), blur = (size * 0.03f).px, cutout = true)
        }
    }


    override fun setComboColor(color: Color4) {
        // The colour carries the alpha.
        fill.color = color.alpha(FILL_ALPHA)
    }

    override fun playTrackingStart(scale: Float, remainingTime: Float) {
        val startScale = if (alpha == 0f) scale * 0.5f else scaleX

        clearAnimations()
        choreographAt(currentClockTime) { sequence ->
            sequence.scaleTo(startScale)
                .scaleTo(scale, min(remainingTime, 0.3f), Easing.OutQuint)
                .fadeIn(min(remainingTime, 0.1f))
        }
    }

    override fun playTrackingEnd(scale: Float) {
        clearAnimations()
        choreographAt(currentClockTime) { sequence ->
            sequence.scaleTo(scale * 1.3f, 0.2f, Easing.OutQuint).fadeOut(0.2f, Easing.OutQuint)
        }
    }

    override fun playTick(scale: Float) {
        if (alpha == 0f) return

        clearAnimations()
        choreographAt(currentClockTime) { sequence ->
            sequence.scaleTo(scale * 1.08f).scaleTo(scale, 0.2f, Easing.OutQuint)
        }
    }

    override fun playSliderEnd(scale: Float, time: Float): Float {
        clearAnimations()

        var endTime = time
        choreographAt(time) { sequence ->
            sequence.scaleTo(scale * 0.8f, 0.2f, Easing.OutQuad).fadeOut(0.2f, Easing.InQuad)
            endTime = sequence.endTime
        }
        return endTime
    }

    private companion object {
        const val FILL_ALPHA = 0.1f
    }
}

/**
 * The reverse arrow of the default skin: a white chevron, pointing right as the `reversearrow` texture, at its
 * size. The slider turns it and pulses it on the beat.
 */
class DefaultReverseArrow : Container() {

    init {
        val size = SkinDimensions.REVERSE_ARROW.width

        width = size.px
        height = size.px
        origin = Anchor.Center

        +Icon(FAIcon.ChevronRight).apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            iconSize = (size * 0.4f * DefaultSkin.TEXT_RESOLUTION).px
            scale = 1f / DefaultSkin.TEXT_RESOLUTION
            color = Color4.White
            shadow = Shadow(Color4.Black.alpha(0.4f), blur = (size * 0.1f).px)
        }
    }
}

/**
 * The tick of the default skin: a small white ring around a dark centre, at the size of the default
 * `sliderscorepoint`.
 */
class DefaultSliderTick : Container() {

    init {
        val size = SkinDimensions.SLIDER_TICK.width

        width = size.px
        height = size.px
        origin = Anchor.Center

        +disc(size).apply { color = Color4.Black.alpha(0.5f) }
        +ring(size, size * 0.2f)
    }
}
