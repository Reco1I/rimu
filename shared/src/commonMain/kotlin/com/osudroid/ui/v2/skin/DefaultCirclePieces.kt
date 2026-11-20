package com.osudroid.ui.v2.skin

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.GradientBox
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreography
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.shape.Box
import com.reco1l.verktex.ui.text.Text
import kotlin.math.max
import kotlin.math.min

/**
 * Text drawn in Fredoka at [size] in its parent's units, rasterised at [DefaultSkin.TEXT_RESOLUTION] times that, as the
 * pieces it's in are scaled up on screen.
 */
internal fun fredokaText(size: Float) = Text().apply {
    anchor = Anchor.Center
    origin = Anchor.Center
    fontFamily = BuiltIn.Fonts.Fredoka
    fontWeight = Font.Weight.Bold
    fontSize = (size * DefaultSkin.TEXT_RESOLUTION).px
    scale = 1f / DefaultSkin.TEXT_RESOLUTION
    color = Color4.White
}

/**
 * Places [text], a [fredokaText], centred at ([x], [y]) in its parent: vertically on the ink of its glyphs, not its
 * line, which has room above and below them, and horizontally on their advances, which the font balances by eye: a
 * "1" centred on its ink, its flag on the left, looks pushed to the right.
 *
 * It's placed by hand rather than by its anchor: it's laid out at the size it's rasterised at, which its parent's size
 * would clamp, while it's drawn whole.
 */
internal fun placeText(text: Text, x: Float, y: Float) {
    text.anchor = Anchor.TopLeft
    text.origin = Anchor.TopLeft
    text.scaleOrigin = Anchor.TopLeft

    val content = text.text
    val font = BuiltIn.Fonts.Fredoka[text.fontSize, text.fontWeight]
    val baseline = -font.metrics.ascent

    var width = 0f
    var top = Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    for (char in content) {
        val glyph = font[char.code]
        width += glyph.advance
        // The bearing is negative upwards from the baseline.
        top = min(top, baseline + glyph.bearingY)
        bottom = max(bottom, baseline + glyph.bearingY + glyph.height)
    }

    if (content.isEmpty()) {
        top = 0f
        bottom = 0f
    }

    // The text is rasterised larger and scaled down.
    val scale = text.scaleX
    text.x = (x - width / 2f * scale).px
    text.y = (y - (top + bottom) / 2f * scale).px
    text.translationX = 0f.px
    text.translationY = 0f.px
}

/**
 * A disc of [size] across, centred in its parent, filled with its colour.
 */
internal fun disc(size: Float) = Box().apply {
    width = size.px
    height = size.px
    anchor = Anchor.Center
    origin = Anchor.Center
    radius = CornerRadius((size / 2).px)
}

/**
 * A disc of [size] across, centred in its parent, filled with a vertical gradient.
 */
internal fun gradientDisc(size: Float) = GradientBox().apply {
    width = size.px
    height = size.px
    anchor = Anchor.Center
    origin = Anchor.Center
    radius = CornerRadius((size / 2).px)
    gradientAngle = 90f
}

/**
 * A ring of [size] across and [thickness], centred in its parent, in [color].
 */
internal fun ring(size: Float, thickness: Float, color: Color4 = Color4.White) = Container().apply {
    width = size.px
    height = size.px
    anchor = Anchor.Center
    origin = Anchor.Center
    radius = CornerRadius((size / 2).px)
    border = Border(thickness.px, color)
}

/**
 * The circle of the default skin, modelled on osu!lazer's `ArgonMainCirclePiece`: a white ring around a band of the
 * combo colour, a dark centre with the number in Fredoka, and a white flash when it's hit, as the circle grows and
 * fades.
 */
class DefaultMainCirclePiece(private val hasNumber: Boolean) : MainCirclePiece() {

    private val size = SkinDimensions.HIT_CIRCLE.width

    private val outerFill = disc(size)
    private val outerGradient = gradientDisc(size * OUTER_GRADIENT_SIZE)
    private val innerFill = disc(size * INNER_FILL_SIZE)
    private val outerRing = ring(size, size * BORDER_THICKNESS)

    private val number = fredokaText(size * NUMBER_SIZE).apply { isVisible = hasNumber }

    private val flash = disc(size).apply {
        color = Color4.White
        blendMode = BlendMode.Additive
        alpha = 0f
    }

    private var isNumberShown = true

    init {
        width = size.px
        height = size.px
        origin = Anchor.Center

        +outerFill
        +outerGradient
        +innerFill
        +outerRing
        +number
        +flash

        setCircleColor(Color4.White)
    }


    override fun setCircleColor(color: Color4) {
        outerFill.color = color.darken(4f)
        outerGradient.colorStart = color
        outerGradient.colorEnd = color.darken(0.1f)
        innerFill.color = color.darken(4f)
    }

    override fun setComboNumber(value: Int) {
        number.text = value.toString()
        reset()
        placeText(number, size / 2, size / 2)
    }

    override fun showNumber() {
        isNumberShown = true
        number.alpha = 1f
    }

    override fun hideNumber() {
        isNumberShown = false
        number.alpha = 0f
    }

    // A slider's end has no circle in the default skin, as in osu!lazer's Argon Pro: the head that becomes one hides.
    override fun showAsSliderEnd(value: Boolean) {
        if (value) hideNumber() else reset()
        for (part in parts) part.isVisible = !value
    }

    private val parts: Array<Component>
        get() = arrayOf(outerFill, outerGradient, innerFill, outerRing, number, flash)

    /**
     * Gives back the look before a hit, for the piece to be used again.
     */
    private fun reset() {
        for (part in parts) part.isVisible = true

        for (part in arrayOf<Component>(outerGradient, innerFill, outerRing, number, flash)) {
            part.clearAnimations()
            part.scale = 1f
            part.alpha = 1f
        }
        flash.alpha = 0f
        // The number is rasterised larger and shown smaller.
        number.scale = 1f / DefaultSkin.TEXT_RESOLUTION
        number.alpha = if (isNumberShown) 1f else 0f
    }

    override fun playHit(scale: Float, time: Float): Float {
        reset()

        // Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/Skinning/Argon/ArgonMainCirclePiece.cs
        flash.choreographAt(time) { it.fadeTo(FLASH_ALPHA, FLASH_IN).then().fadeOut(HIT_DURATION, Easing.OutQuint) }
        number.choreographAt(time) { it.fadeOut(FLASH_IN * 2) }
        outerRing.choreographAt(time) { it.scaleTo(1.1f, HIT_DURATION, Easing.OutQuint).fadeOut(HIT_DURATION, Easing.OutQuint) }
        outerGradient.choreographAt(time) { it.scaleTo(1.08f, HIT_DURATION, Easing.OutQuint) }
        innerFill.choreographAt(time) { it.scaleTo(0.8f, HIT_DURATION, Easing.OutQuint) }

        var endTime = time
        choreographAt(time) { sequence ->
            sequence.scaleTo(scale * 1.15f, HIT_DURATION, Easing.OutQuint)
                .delay(FLASH_IN)
                .fadeOut(HIT_DURATION, Easing.OutQuint)
            endTime = sequence.endTime
        }
        return max(endTime, flash.choreography().endTime)
    }

    private companion object {
        // Sizes relative to the circle.
        const val OUTER_GRADIENT_SIZE = 0.86f
        // The slider body's band is the same (DefaultSliderBodyStyle), so a slider's head continues it.
        const val INNER_FILL_SIZE = 0.7f
        const val BORDER_THICKNESS = 0.055f
        const val NUMBER_SIZE = 0.38f

        const val FLASH_ALPHA = 0.9f
        const val FLASH_IN = 0.04f
        const val HIT_DURATION = 0.4f
    }
}

/**
 * The end of a slider in the default skin: nothing, as in osu!lazer's Argon Pro, the body's round end is enough. The
 * reverse arrows are still shown on it.
 */
class DefaultSliderTailPiece : MainCirclePiece() {

    init {
        val size = SkinDimensions.HIT_CIRCLE.width
        width = size.px
        height = size.px
        origin = Anchor.Center
    }

    override fun setCircleColor(color: Color4) = Unit

    override fun playHit(scale: Float, time: Float) = time
}

/**
 * The approach circle of the default skin: a thin white ring with a soft glow, which the object tints with the
 * combo colour.
 */
class DefaultApproachCircle : Container() {

    init {
        val size = SkinDimensions.APPROACH_CIRCLE.width

        width = size.px
        height = size.px
        origin = Anchor.Center

        +ring(size, size * 0.03f).apply {
            shadow = Shadow(Color4.White.alpha(0.4f), blur = (size * 0.04f).px, cutout = true)
        }
    }
}
