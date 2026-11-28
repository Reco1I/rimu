package com.osudroid.ui.v2.skin.legacy

import com.osudroid.game.HitResult
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skinFrames
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.texture.FramesReference
import com.reco1l.verktex.texture.Textures
import com.osudroid.ui.v2.skin.JudgementPiece
import com.reco1l.verktex.ui.AnimatedSprite
import com.reco1l.verktex.ui.Choreography
import com.reco1l.verktex.ui.choreographyTransitions
import com.osudroid.ui.skinning.OsuSkin
import kotlin.random.Random

/**
 * The judgement of a hit object (300, 100, miss, a slider point…), shown with the skin's texture [textureName], as
 * osu!stable shows it (osu!lazer's `LegacyJudgementPieceOld`). Its animation is a choreography played at the time of
 * the judgement, so seeking shows it as it was.
 *
 * Owned by the hit object it judges, until the object expires, and pooled by texture.
 */
class LegacyJudgementPiece(textureName: String) : JudgementPiece(textureName) {

    private val sprite = AnimatedSprite(
        if (textureName in ANIMATED) skinFrames(textureName, orStatic = true) else StaticSkinTexture(textureName),
        OsuSkin.current.utils.animationFramerate
    ).also {
        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.isLoop = false
        +it
    }

    // What the animation of the current judgement is played with.
    private var startTime = 0f
    private var baseScale = 1f
    private var missRotation = 0f
    private var isSpinnerMiss = false

    // Legacy judgements don't play any transforms if they are an animation.
    private val isAnimated
        get() = sprite.frames.size > 1

    private val judgementTransitions = choreographyTransitions<HitResult> {
        this[HitResult.Miss] = {
            when {
                isSpinnerMiss -> fadeIn(0.15f).then(0.35f).fadeOut(0.25f)
                isAnimated -> fades()
                else -> {
                    fades()

                    at(startTime)
                        .scaleTo(baseScale, 0.1f, Easing.InQuad)
                        .moveToY(-5f)
                        .moveToY(80f, FADE_OUT_DELAY + FADE_OUT_LENGTH, Easing.InQuad)

                    at(startTime)
                        .rotateTo(0f)
                        .rotateTo(missRotation, FADE_IN_LENGTH)
                        .then()
                        .rotateTo(missRotation * 2, FADE_OUT_DELAY + FADE_OUT_LENGTH - FADE_IN_LENGTH, Easing.InQuad)
                }
            }
        }

        otherwise {
            fades()

            if (!isAnimated) {
                at(startTime)
                    .scaleTo(baseScale * 1.1f, FADE_IN_LENGTH * 0.8f) // t = 0.8
                    .then(FADE_IN_LENGTH * 0.2f) // t = 1.0
                    .scaleTo(baseScale * 0.9f, FADE_IN_LENGTH * 0.2f) // t = 1.2
                    .then()

                    // stable dictates scale of 0.9->1 over time 1.0 to 1.4, but we are already at 1.2.
                    // so we need to force the current value to be correct at 1.2 (0.95) then complete the
                    // second half of the transform.
                    .scaleTo(baseScale * 0.95f).scaleTo(baseScale, FADE_IN_LENGTH * 0.2f) // t = 1.4
            }
        }
    }

    init {
        origin = Anchor.Center
    }

    private fun Choreography.fades() =
        fadeInFromZero(FADE_IN_LENGTH).then(FADE_OUT_DELAY).fadeOut(FADE_OUT_LENGTH)


    // A spinner's miss only fades.
    override fun play(x: Float, y: Float, result: HitResult, scale: Float, time: Float, spinnerMiss: Boolean): Float {
        clearAnimations()
        sprite.stop()
        sprite.play()

        startTime = time
        baseScale = scale
        isSpinnerMiss = spinnerMiss && result == HitResult.Miss
        missRotation = Random.nextDouble(8.6 * 2).toFloat() - 8.6f

        setPosition(x, y)
        alpha = 1f
        this.scale = when {
            isSpinnerMiss || isAnimated -> scale
            result == HitResult.Miss -> scale * 1.6f
            else -> scale * 0.6f
        }

        return judgementTransitions.play(this, result, time)
    }

    private companion object {

        const val FADE_IN_LENGTH = 0.12f
        const val FADE_OUT_LENGTH = 0.6f
        const val FADE_OUT_DELAY = 0.5f

        // The judgements a skin can animate.
        val ANIMATED = setOf("hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g")
    }
}

/**
 * The skin texture named [name] as the only frame, following it as the skin changes.
 */
private data class StaticSkinTexture(val name: String) : FramesReference.Lookup() {

    override val revision: Int
        get() = Textures.revision + SkinTextures.revision

    override fun lookup() = listOfNotNull(SkinTextures[name])
}
