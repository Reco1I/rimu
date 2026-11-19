package com.osudroid.ui.v2.skin.legacy

import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skin.FollowCirclePiece
import com.osudroid.ui.v2.skin.SliderBallPiece
import com.osudroid.ui.v2.skin.SliderBodyStyle
import com.osudroid.ui.v2.skinFrames
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.AnimatedSprite
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.PathShape
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * The ball of a texture skin: `sliderb`, animated as it rolls, with the default skin's layers under (`sliderb-nd`)
 * and over it (`sliderb-spec`, which neither turns nor flips) when the skin doesn't have its own ball.
 */
class LegacySliderBallPiece : SliderBallPiece() {

    private val under = Sprite().also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.setColor(5 / 255f, 5 / 255f, 5 / 255f)
        +it
    }

    private val ball = AnimatedSprite(skinFrames("sliderb", orStatic = true), 60f).also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        +it
    }

    private val over = Sprite().also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.blendMode = BlendMode.Additive
        +it
    }

    init {
        origin = Anchor.Center
    }


    override fun setComboColor(color: Color4) {
        ball.color = if (OsuSkin.get().isAllowSliderBallTint) color else Color4.White

        // Only shown with the default ball: a skin's own ball ignores them. Looked up again as the skin may have
        // changed while this piece was pooled.
        val isBallSkinned = SkinTextures.isSkinned("sliderb") || SkinTextures.isSkinned("sliderb0")
        under.texture = if (isBallSkinned) null else skinTexture("sliderb-nd")
        over.texture = if (isBallSkinned) null else skinTexture("sliderb-spec")
    }

    override fun setSpeed(pixelsPerSecond: Float) {
        // A frame per pixel rolled.
        ball.frameTime = 1f / pixelsPerSecond
    }

    override fun setBallRotation(degrees: Float) {
        ball.rotationZ = degrees
        under.rotationZ = degrees
    }

    override fun setFlipped(value: Boolean) {
        ball.flippedHorizontal = value
    }
}

/**
 * The follow circle of a texture skin, as osu!lazer's `LegacyFollowCircle`.
 */
class LegacyFollowCirclePiece : FollowCirclePiece() {

    // Animated or not depending on the skin, which may change while the slider is alive.
    private val sprite = AnimatedSprite(skinFrames("sliderfollowcircle", orStatic = true), OsuSkin.get().animationFramerate).also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        +it
    }

    init {
        origin = Anchor.Center
    }


    override fun playTrackingStart(scale: Float, remainingTime: Float) {
        val initialScale = if (alpha == 0f) scale * 0.5f else scaleX

        clearAnimations()
        this.scale = initialScale

        scaleTo(scale, min(remainingTime, 0.18f), Easing.OutQuad)
        fadeIn(min(remainingTime, 0.06f))
    }

    override fun playTrackingEnd(scale: Float) {
        clearAnimations()

        scaleTo(scale * 2, 0.1f)
        fadeOut(0.1f)
    }

    override fun playTick(scale: Float) {
        if (scaleX > 1f) {
            clearAnimations()

            this.scale = scale * 1.1f
            scaleTo(scale, 0.2f)
        }
    }

    override fun playSliderEnd(scale: Float, time: Float): Float {
        clearAnimations()

        var endTime = time
        choreographAt(time) { sequence ->
            sequence.scaleTo(scale * 0.8f, 0.2f, Easing.OutQuad).fadeOut(0.2f, Easing.InQuad)
            endTime = max(endTime, sequence.endTime)
        }
        return endTime
    }
}

/**
 * The body of a slider as osu!stable draws it (see osu!lazer's `LegacySliderBody`), with the widths and alpha of the
 * skin: a gradient from the colour darkened at the border to it lightened at the centre, and a shadow outside the
 * border taking the outer 5/64 of the whole width.
 */
object LegacySliderBodyStyle : SliderBodyStyle {

    private const val SHADOW_PORTION = 5f / 64f

    override fun apply(body: PathShape, bodyColor: Color4, borderColor: Color4, skinScale: Float) {
        val skin = OsuSkin.get()
        val bodyWidth = skin.sliderBodyWidth * skinScale
        val bodyAlpha = skin.sliderBodyBaseAlpha

        body.halfWidth = bodyWidth
        body.borderWidth = skin.sliderBorderWidth * skinScale
        body.borderColor = borderColor
        body.shadowWidth = bodyWidth * SHADOW_PORTION / (1 - SHADOW_PORTION)
        body.bodyColor = darken(bodyColor, 0.1f).alpha(bodyAlpha)
        body.bodyCenterColor = lighten(bodyColor, 0.5f).alpha(bodyAlpha)
    }

    /**
     * Darkens [color] by [amount] as osu!framework does.
     */
    private fun darken(color: Color4, amount: Float): Color4 {
        val scalar = max(1f, 1f + amount)
        return Color4(color.red / scalar, color.green / scalar, color.blue / scalar, color.alpha)
    }

    /**
     * Lightens [color] in a way friendlier to dark or strong colours, as osu!lazer's LegacySliderBody.
     */
    private fun lighten(color: Color4, amount: Float): Color4 {
        val half = amount * 0.5f
        fun channel(value: Float) = min(1f, value * (1 + 0.5f * half) + half)
        return Color4(channel(color.red), channel(color.green), channel(color.blue), color.alpha)
    }
}
