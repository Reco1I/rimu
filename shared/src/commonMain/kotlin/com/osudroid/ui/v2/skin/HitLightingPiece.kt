package com.osudroid.ui.v2.skin

import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.BlendFactor
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt

/**
 * The light behind a judgement, in the combo color, as osu!lazer's `DrawableOsuJudgement` shows it. Owned by the
 * hit object it lights, and drawn below every object.
 *
 * It shows the skin's `lighting`, or [glow], an image made in code, for the default skin.
 */
class HitLightingPiece(glow: Texture.Region? = null) : Sprite() {

    init {
        origin = Anchor.Center
        if (glow != null) textureRegion = glow else texture = skinTexture("lighting")

        // The colour is premultiplied, so One weighs it by its alpha as SrcAlpha would with straight alpha.
        blendMode = BlendMode.Custom(BlendFactor.One, BlendFactor.DstAlpha, BlendFactor.SrcAlpha, BlendFactor.DstAlpha)
    }

    /**
     * Lights ([x], [y]) in the space of its parent in [color] (white if null), for an object of the given [scale],
     * from [time] on its clock.
     *
     * @return The time it ends at.
     */
    fun play(x: Float, y: Float, color: Color4?, scale: Float, time: Float): Float {
        // Reference https://github.com/ppy/osu/blob/a7e110f6693beca6f6e6a20efb69a6913d58550e/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuJudgement.cs#L71-L88
        clearAnimations()
        setPosition(x, y)
        this.color = color ?: Color4.White
        this.scale = scale * 0.8f
        alpha = 1f

        choreographAt(time) { sequence -> sequence.scaleTo(scale * 1.2f, 0.6f, Easing.OutQuad) }
        choreographAt(time) { sequence -> sequence.fadeInFromZero(0.2f).then().fadeOut(1f) }

        return choreography!!.endTime
    }
}
