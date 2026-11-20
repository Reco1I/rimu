package com.osudroid.ui.v2.skin

import com.osudroid.game.HitResult
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.choreographyTransitions
import kotlin.random.Random

/**
 * The judgement of the default skin, modelled on osu!lazer's `ArgonJudgementPiece`: the result in Fredoka, in its
 * colour, growing slowly as it fades. A miss drops and turns as osu!stable's does. Slider points and ticks show nothing, as in Argon.
 *
 * Its size is that of the default `hit300`… textures, so it lines up with the judgements of a texture skin.
 */
class DefaultJudgementPiece(textureName: String) : JudgementPiece(textureName) {

    private val result = HitResult.fromTexture(textureName)

    private val text = fredokaText(TEXT_SIZE).apply {
        text = when (result) {
            HitResult.Great -> "GREAT"
            HitResult.Ok -> "OK"
            HitResult.Meh -> "MEH"
            HitResult.Miss -> "MISS"
            HitResult.SliderPoint, HitResult.Tick -> ""
        }
        color = colorOf(result)
    }

    // What the animation of the current judgement is played with.
    private var startTime = 0f
    private var baseScale = 1f
    private var missRotation = 0f

    private val judgementTransitions = choreographyTransitions<HitResult> {
        this[HitResult.Miss] = {
            fadeInFromZero(FADE_IN).then(MISS_DURATION - FADE_IN - FADE_OUT).fadeOut(FADE_OUT)

            at(startTime)
                .scaleTo(baseScale, 0.1f, Easing.InQuad)
                .moveToY(0f)
                .moveToY(MISS_DROP, MISS_DURATION, Easing.InQuad)

            at(startTime)
                .rotateTo(0f)
                .rotateTo(missRotation, MISS_DURATION, Easing.InQuad)
        }

        this[HitResult.SliderPoint] = {}
        this[HitResult.Tick] = {}

        otherwise {
            fadeInFromZero(HIT_FADE_IN, Easing.OutQuint).then(HIT_HOLD).fadeOut(FADE_OUT)
            at(startTime).scaleTo(baseScale * 1.15f, HIT_DURATION, Easing.OutQuint)
        }
    }

    init {
        // As big as a hit circle, centred on it. The text is the size of the default `hit300`, centred as it is.
        val size = SkinDimensions.HIT_CIRCLE

        width = size.width.px
        height = size.height.px
        origin = Anchor.Center

        +text

        placeText(text, size.width / 2, size.height / 2)
    }


    override fun play(x: Float, y: Float, result: HitResult, scale: Float, time: Float, spinnerMiss: Boolean): Float {
        clearAnimations()

        startTime = time
        baseScale = scale
        missRotation = (Random.nextFloat() * 2f - 1f) * MISS_ROTATION

        setPosition(x, y)
        translationY = 0f.px
        rotationZ = 0f
        alpha = 1f
        this.scale = if (result == HitResult.Miss && !spinnerMiss) scale * 1.6f else scale

        return judgementTransitions.play(this, result, time)
    }

    private companion object {
        const val TEXT_SIZE = 28f

        const val FADE_IN = 0.12f
        const val HIT_FADE_IN = 0.3f
        const val HIT_HOLD = 0.2f
        const val HIT_DURATION = 1f
        const val FADE_OUT = 0.5f

        const val MISS_DURATION = 1.1f
        const val MISS_DROP = 30f
        const val MISS_ROTATION = 20f

        // The colours osu!lazer gives the results (OsuColour.ForHitResult).
        val GREAT = Color4("#66CCFF")
        val OK = Color4("#B3D944")
        val MEH = Color4("#FFCC22")
        val MISS = Color4("#ED1121")

        fun colorOf(result: HitResult) = when (result) {
            HitResult.Great, HitResult.SliderPoint, HitResult.Tick -> GREAT
            HitResult.Ok -> OK
            HitResult.Meh -> MEH
            HitResult.Miss -> MISS
        }
    }
}
