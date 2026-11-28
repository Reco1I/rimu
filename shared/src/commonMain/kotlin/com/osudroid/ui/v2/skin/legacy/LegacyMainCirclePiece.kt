package com.osudroid.ui.v2.skin.legacy

import com.osudroid.ui.v2.SpriteFont
import com.osudroid.ui.v2.skin.MainCirclePiece
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.Sprite
import com.osudroid.ui.skinning.OsuSkin
import com.osudroid.ui.skinning.SkinFont

/**
 * The circle of a texture skin, as osu!lazer's `LegacyMainCirclePiece`: the circle texture tinted with the combo
 * colour, its overlay, and the combo number in the skin's hit circle font if [hasNumber]. A slider's head turns into
 * its end, with the end's textures, when the span comes back to it.
 */
class LegacyMainCirclePiece(

    private val circleTexture: String,
    private val overlayTexture: String,
    private val hasNumber: Boolean,
    private val endCircleTexture: String = circleTexture,
    private val endOverlayTexture: String = overlayTexture

) : MainCirclePiece() {

    private val circle = Sprite().also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.texture = skinTexture(circleTexture)
        +it
    }

    private val overlay = Sprite().also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.texture = skinTexture(overlayTexture)
        +it
    }

    private val number = if (hasNumber) SpriteFont(SkinFont.HitCircle).also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.spacing = -OsuSkin.current.fonts.hitCircleOverlap
        +it
    } else null

    init {
        origin = Anchor.Center
    }


    override fun setCircleColor(color: Color4) {
        circle.color = color
    }

    override fun setComboNumber(value: Int) {
        val number = number ?: return
        number.text = value.toString()

        val scale = OsuSkin.current.utils.comboTextScale
        number.textureScaleX = scale
        number.textureScaleY = scale

        applyLayering(number)
    }

    /**
     * Draws the overlay over the number, or under it for skins that ask for it, as the skin may have changed
     * while this piece was pooled.
     */
    private fun applyLayering(number: SpriteFont) {
        val top = if (OsuSkin.current.utils.hitCircleOverlayAboveNumber) overlay else number

        if (this[childCount - 1] !== top) {
            top.removeSelf()
            +top
        }
    }

    override fun showNumber() {
        number?.alpha = 1f
    }

    override fun hideNumber() {
        number?.alpha = 0f
    }

    override fun showAsSliderEnd(value: Boolean) {
        circle.texture = skinTexture(if (value) endCircleTexture else circleTexture)
        overlay.texture = skinTexture(if (value) endOverlayTexture else overlayTexture)

        if (value) hideNumber()
    }

    override fun playHit(scale: Float, time: Float): Float {
        // The circle bursts without its number.
        hideNumber()
        return LegacySkin.burst(this, scale, time)
    }
}
