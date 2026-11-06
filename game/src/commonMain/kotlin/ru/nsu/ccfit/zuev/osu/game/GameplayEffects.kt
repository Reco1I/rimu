package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.BlendFactor
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.UIAnimatedSprite
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.random.Random

/**
 * The effects shown when hit objects are judged: the judgement sprites (300, 100, miss...), the hit
 * lighting and the bursts of hit circles and slider ends.
 */
class GameplayEffects {

    private var background: UIContainer? = null
    private var foreground: UIContainer? = null
    private var scale = 1f


    /**
     * Sets the containers the effects are added to: hit lighting goes into `background`, below the
     * hit objects, and everything else into `foreground`.
     */
    fun setLayers(background: UIContainer, foreground: UIContainer) {
        this.background = background
        this.foreground = foreground
    }

    /**
     * Sets the scale of the hit objects, effects are scaled the same.
     */
    fun setScale(scale: Float) {
        this.scale = scale
    }


    /**
     * Shows the judgement sprite with the given texture name, e.g. `hit300` or `hit0`.
     */
    fun hit(pos: Vector2, name: String, color: Color4?) {

        val effect = GameObjectPool.getInstance().getEffect(name)
        val hit = effect.hit
        val isAnimated = hit is UIAnimatedSprite && hit.frames.size > 1

        // Reference https://github.com/ppy/osu/blob/ebf637bd3c33f1c886f6bfc81aa9ea2132c9e0d2/osu.Game/Skinning/LegacyJudgementPieceOld.cs

        val fadeInLength = 0.12f
        val fadeOutLength = 0.6f
        val fadeOutDelay = 0.5f

        if (name == "hit0") {
            val rotation = Random.Default.nextDouble(8.6 * 2).toFloat() - 8.6f

            if (isAnimated) {
                // Legacy judgements don't play any transforms if they are an animation.
                effect.init(
                    foreground!!,
                    pos,
                    scale,
                    { sequence -> sequence
                        .fadeIn(fadeInLength)
                        .then(fadeOutDelay)
                        .fadeOut(fadeOutLength) }
                )
            } else {
                effect.init(
                    foreground!!,
                    pos,
                    scale * 1.6f,
                    { sequence -> sequence
                        .fadeIn(fadeInLength)
                        .then(fadeOutDelay)
                        .fadeOut(fadeOutLength) },
                    { sequence -> sequence
                        .scaleTo(scale, 0.1f, Easing.InQuad)
                        .moveToY(-5f)
                        .moveToY(80f, fadeOutDelay + fadeOutLength, Easing.InQuad) },
                    { sequence -> sequence
                        .rotateTo(0f)
                        .rotateTo(rotation, fadeInLength)
                        .then()
                        .rotateTo(rotation * 2, fadeOutDelay + fadeOutLength - fadeInLength, Easing.InQuad) }
                )
            }

            return
        }

        if (Settings[OsuSettings.HitLighting] && name != "sliderpoint10" && name != "sliderpoint30" && SkinTextures.get("lighting") != null) {

            // Reference https://github.com/ppy/osu/blob/a7e110f6693beca6f6e6a20efb69a6913d58550e/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuJudgement.cs#L71-L88

            val light = GameObjectPool.getInstance().getEffect("lighting")
            light.setBlendMode(BlendMode.Custom(BlendFactor.SrcAlpha, BlendFactor.DstAlpha, BlendFactor.SrcAlpha, BlendFactor.DstAlpha))
            light.setColor(color)
            light.init(
                background!!,
                pos,
                scale * 0.8f,
                { sequence -> sequence.scaleTo(scale * 1.2f, 0.6f, Easing.OutQuad) },
                { sequence -> sequence.fadeIn(0.2f).then().fadeOut(1f) }
            )
        }

        // Legacy judgements don't play any transforms if they are an animation.
        if (isAnimated) {
            effect.init(
                foreground!!,
                pos,
                scale,
                { sequence -> sequence.fadeIn(fadeInLength).then(fadeOutDelay).fadeOut(fadeOutLength) }
            )
        } else {
            effect.init(
                foreground!!,
                pos,
                scale * 0.6f,
                { sequence -> sequence.fadeIn(fadeInLength).then(fadeOutDelay).fadeOut(fadeOutLength) },
                { sequence -> sequence
                    .scaleTo(scale * 1.1f, fadeInLength * 0.8f) // t = 0.8
                    .then(fadeInLength * 0.2f) // t = 1.0
                    .scaleTo(scale * 0.9f, fadeInLength * 0.2f) // t = 1.2
                    .then()

                    // stable dictates scale of 0.9->1 over time 1.0 to 1.4, but we are already at 1.2.
                    // so we need to force the current value to be correct at 1.2 (0.95) then complete the
                    // second half of the transform.
                    .scaleTo(scale * 0.95f).scaleTo(scale, fadeInLength * 0.2f) } // t = 1.4
            )
        }
    }

    /**
     * The burst of a hit circle.
     */
    fun burst(pos: Vector2, color: Color4) {
        burst(pos, color, "hitcircle", "hitcircleoverlay")
    }

    /**
     * The burst of the head of a slider.
     */
    fun burstSliderStart(pos: Vector2, color: Color4) {
        burst(pos, color, "sliderstartcircle", "sliderstartcircleoverlay")
    }

    /**
     * The burst of the end of a slider.
     */
    fun burstSliderEnd(pos: Vector2, color: Color4) {
        burst(pos, color, "sliderendcircle", "sliderendcircleoverlay")
    }

    /**
     * The burst of a reverse arrow of a slider.
     */
    fun burstSliderReverse(pos: Vector2, ang: Float, color: Color4) {
        if (!areBurstsShown()) {
            return
        }

        val burst = GameObjectPool.getInstance().getEffect("reversearrow")
        burst.hit.rotationZ = ang
        applyBurst(burst, pos)
    }


    private fun burst(pos: Vector2, color: Color4, circleTexture: String, overlayTexture: String) {
        if (!areBurstsShown()) {
            return
        }

        val circle = GameObjectPool.getInstance().getEffect(circleTexture)
        applyBurst(circle, pos)
        circle.setColor(color)

        val overlay = GameObjectPool.getInstance().getEffect(overlayTexture)
        applyBurst(overlay, pos)
    }

    private fun areBurstsShown(): Boolean {
        return Settings[OsuSettings.BurstEffects] &&
            (GameHelper.getHidden() == null || GameHelper.getHidden()!!.onlyFadeApproachCircles) &&
            !GameHelper.isTraceable()
    }

    private fun applyBurst(effect: GameEffect, pos: Vector2) {

        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174

        val fadeDuration = 0.24f

        effect.init(foreground!!, pos, scale,
            { sequence -> sequence
                .scaleTo(scale * 1.4f, fadeDuration, Easing.OutQuad)
                .fadeOut(fadeDuration) }
        )
    }
}
