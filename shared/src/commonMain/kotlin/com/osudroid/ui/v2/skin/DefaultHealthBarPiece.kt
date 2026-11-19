package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.ColorInterpolation
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.ui.GradientBox
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.Box

/**
 * The health bar of the default skin, as osu!lazer's `ArgonHealthDisplay`: a rounded track with a glowing fill up to
 * the health, white turning red when it's low, and a marker at its end bulging and flashing with each hit. It's laid
 * out as the default `scorebar-bg`, its fill where `scorebar-colour` is.
 */
class DefaultHealthBarPiece : HealthBarPiece() {

    private val fillWidth = SkinDimensions.HEALTH_BAR_FILL.width
    private val fillHeight = SkinDimensions.HEALTH_BAR_FILL.height

    // Where the fill is in the background, as the new style's.
    private val fillX = 7.5f * 1.6f
    private val fillY = 7.8f * 1.6f

    private val track = Box().apply {
        width = fillWidth.px
        height = fillHeight.px
        x = fillX.px
        y = fillY.px
        radius = CornerRadius((fillHeight / 2).px)
        color = Color4.Black.alpha(0.4f)
    }

    private val fill = GradientBox().apply {
        width = fillWidth.px
        height = fillHeight.px
        x = fillX.px
        y = fillY.px
        radius = CornerRadius((fillHeight / 2).px)
        gradientAngle = 0f
        colorStart = Color4.White.alpha(0.7f)
        colorEnd = Color4.White
        shadow = Shadow(Color4.White.alpha(0.4f), blur = (fillHeight * 0.8f).px, cutout = true)
    }

    private val markerSize = SkinDimensions.HEALTH_BAR_MARKER.width

    private val marker = disc(markerSize).apply {
        anchor = Anchor.TopLeft
        color = Color4.White
        shadow = Shadow(Color4.White.alpha(0.6f), blur = (markerSize * 0.4f).px, cutout = true)
    }

    private val explode = disc(markerSize).apply {
        anchor = Anchor.TopLeft
        color = Color4.White
        alpha = 0f
    }

    private var shownHP = 1f
    private var lastHP = 0f

    init {
        width = SkinDimensions.HEALTH_BAR_BACKGROUND.width.px
        height = SkinDimensions.HEALTH_BAR_BACKGROUND.height.px

        +track
        +fill
        +marker
        +explode
    }


    override fun update(hp: Float, secondsElapsed: Float) {
        shownHP = FloatInterpolation.floatAt(secondsElapsed.coerceIn(0f, 0.2f), shownHP, hp, 0f, 0.2f, Easing.OutQuint)

        // At least as wide as it's tall, so it stays round.
        fill.width = (fillHeight + (fillWidth - fillHeight) * shownHP).px

        val color = colorOf(hp)
        fill.colorStart = color.alpha(0.7f)
        fill.colorEnd = color
        marker.color = color

        marker.x = (fillX + fillHeight / 2 + (fillWidth - fillHeight) * shownHP).px
        marker.y = (fillY + fillHeight / 2).px
        explode.x = marker.x
        explode.y = marker.y

        if (hp > lastHP) {
            bulge()
        }
        lastHP = hp
    }

    override fun flash(hp: Float) {
        bulge()

        explode.clearTransforms()
        explode.color = colorOf(hp)
        explode.alpha = 0.8f
        explode.scale = 1f
        explode.scaleTo(2f, 0.2f, Easing.OutQuint)
        explode.fadeOut(0.2f)
    }

    private fun bulge() {
        marker.clearTransforms()
        marker.scale = 1.4f
        marker.scaleTo(1f, 0.2f, Easing.OutQuad)
    }

    private fun colorOf(hp: Float) =
        if (hp < LOW) ColorInterpolation.colorAt(LOW - hp, Color4.White, DANGER, 0f, LOW) else Color4.White

    private companion object {
        const val LOW = 0.3f
        val DANGER = Color4(0xFFEF4444)
    }
}
