package com.osudroid.game.objects

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.math.MathUtils
import com.osudroid.mods.ModHidden
import com.osudroid.ui.v2.game.GameplayPlayfield
import com.osudroid.ui.v2.skin.MainCirclePiece
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.skins.OsuSkin

class GameplayHitCircle : GameObject() {

    private val normalApproachCircle = SkinPieces.current.create(SkinComponent.ApproachCircle)
    private val traceableApproachCircle by lazy { SkinPieces.current.create(SkinComponent.TraceableApproachCircle) }
    private var approachCircle: Component = normalApproachCircle
    private var comboColor = Color4()
    private var listener: GameObjectListener? = null
    private var playfield: GameplayPlayfield? = null
    private var beatmapCircle: HitCircle? = null
    private var passedTime = 0f
    private var timePreempt = 0f
    private var hitOffset = 0f
    private var kiai = false
    private var successfulHit = false
    private val hitSamples = ArrayList<GameplayHitSampleInfo>(5)

    // The texture of the judgement the game gave the circle, shown once it's removed. Null for none.
    private var judgement: String? = null

    /**
     * The circle piece that represents the circle body and overlay.
     */
    private val circlePiece: MainCirclePiece = SkinPieces.current.create(SkinComponent.HitCircle)

    fun init(listener: GameObjectListener, playfield: GameplayPlayfield, beatmapCircle: HitCircle,
             comboColor: Color4) {
        // Storing parameters into fields
        this.beatmapCircle = beatmapCircle
        replayObjectData = null

        val stackedPosition = beatmapCircle.gameplayStackedPosition
        position.set(stackedPosition.x, stackedPosition.y)

        endsCombo = beatmapCircle.isLastInCombo
        this.listener = listener
        context = listener.context
        this.playfield = playfield
        timePreempt = beatmapCircle.timePreempt.toFloat() / 1000

        val mehWindow = beatmapCircle.hitWindow!!.mehWindow.toFloat() / 1000
        hitOffset = mehWindow

        hitTime = beatmapCircle.startTime.toFloat() / 1000
        passedTime = -timePreempt
        startHit = false
        successfulHit = false
        judgement = null
        kiai = (context.timing.isKiai && !OsuSkin.get().isDisableKiai)
        this.comboColor = comboColor

        val initialModifierTime = hitTime - timePreempt
        val scale = beatmapCircle.gameplayScale
        val fadeInDuration = beatmapCircle.timeFadeIn.toFloat() / 1000f

        // Initializing sprites
        circlePiece.setCircleColor(comboColor)
        circlePiece.scale = scale
        circlePiece.alpha = 0f
        circlePiece.setPosition(this.position.x, this.position.y)

        var comboNum = beatmapCircle.indexInCurrentCombo + 1
        if (OsuSkin.get().isLimitComboTextLength) {
            comboNum %= 10
        }

        circlePiece.setComboNumber(comboNum)
        circlePiece.showNumber()
        circlePiece.isVisible = !context.isTraceable ||
            (Settings[OsuSettings.ShowFirstApproachCircle] && context.traceable!!.firstObject === beatmapCircle)

        approachCircle = if (context.isTraceable) traceableApproachCircle else normalApproachCircle
        approachCircle.color = comboColor
        approachCircle.scale = scale * 3 * (beatmapCircle.timePreempt / context.originalTimePreempt).toFloat()
        approachCircle.alpha = 0f
        approachCircle.setPosition(this.position.x, this.position.y)
        approachCircle.isVisible = !context.isHidden ||
            (Settings[OsuSettings.ShowFirstApproachCircle] && context.hidden!!.firstObject === beatmapCircle)

        // Newer objects are below older ones.
        playfield.objects.add(0, this)
        this += circlePiece
        this += approachCircle
        drawIn(playfield.approachCircles, approachCircle)

        val fadeOutCircle = context.fadesOutObjects

        circlePiece.choreographAt(initialModifierTime) { sequence ->
            sequence.fadeIn(fadeInDuration)

            if (fadeOutCircle) {
                val fadeOutDuration = timePreempt * ModHidden.FADE_OUT_DURATION_MULTIPLIER.toFloat()
                sequence.then().fadeOut(fadeOutDuration)
            }
        }

        if (!fadeOutCircle && circlePiece.isVisible) {
            val okWindow = beatmapCircle.hitWindow!!.okWindow.toFloat() / 1000

            circlePiece.choreographAt(hitTime + okWindow) { sequence -> sequence.fadeOut(mehWindow - okWindow) }
        }

        if (approachCircle.isVisible) {
            val easing: Easing
            val approachDifferentMod = context.approachDifferent

            if (approachDifferentMod != null) {
                approachCircle.scale = scale * approachDifferentMod.scale
                easing = approachDifferentMod.easing
            } else {
                easing = Easing.None
            }

            approachCircle.choreographAt(initialModifierTime) { sequence -> sequence
                .fadeTo(0.9f, min(fadeInDuration * 2, timePreempt))
                .scaleTo(scale, timePreempt, easing)
                .after { e -> e.alpha = 0f } }
        }

        if (Settings[OsuSettings.DimHitObjects] && circlePiece.isVisible) {
            // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
            val colorDim = 195f / 255f

            circlePiece.setColor(colorDim, colorDim, colorDim)
            circlePiece.choreographAt(hitTime - HitWindow.MISS_WINDOW.toFloat() / 1000) { sequence -> sequence.colorTo(1f, 1f, 1f, 0.1f) }
        }

        // Initialize samples
        val parsedSamples = beatmapCircle.samples
        hitSamples.ensureCapacity(parsedSamples.size)

        for (i in 0 until parsedSamples.size) {
            val gameplaySample = GameplayHitSampleInfo.obtain()
            gameplaySample.init(parsedSamples[i])

            if (context.samplesMatchPlaybackRate) {
                gameplaySample.frequency = context.timing.speedMultiplier
            }

            hitSamples.add(gameplaySample)
        }

        lifetimeEnd = Float.MAX_VALUE
    }

    private fun removeFromScene() {
        val playfield = playfield ?: return
        val judgement = judgement

        lifetimeEnd = hitTime + hitOffset

        for (i in hitSamples.size - 1 downTo 0) {
            hitSamples[i].release()
        }

        hitSamples.clear()
        circlePiece.clearAnimations()
        approachCircle.clearAnimations()
        approachCircle.removeSelf()

        if (judgement != null && judgement != "hit0" && areBurstsShown()) {
            // The circle bursts itself.
            circlePiece.setCircleColor(comboColor)
            circlePiece.setColor(1f, 1f, 1f)
            burst(playfield, circlePiece, beatmapCircle!!.gameplayScale)
        } else if (successfulHit || !circlePiece.isVisible || circlePiece.alpha == 0f) {
            circlePiece.removeSelf()
        } else {
            circlePiece.choreographNow { sequence ->
                sequence.fadeOut(0.1f).after { e -> updateThread { e.removeSelf() } }
                extendLifetime(sequence)
            }
        }

        if (judgement != null) {
            showJudgement(playfield, judgement, position.x, position.y, beatmapCircle!!.gameplayScale, comboColor)
        }

        this.playfield = null
    }

    private fun playHitSamples() {
        listener!!.playHitSamples(hitSamples)
    }

    override fun updateJudgement(dt: Float) {
        val beatmapCircle = beatmapCircle!!
        val listener = listener!!

        if (beatmapCircle.hitWindow == null) {
            // Circle somehow does not have a judgement window - abandon.
            return
        }

        if (isJudged()) {
            return
        }

        passedTime = listener.elapsedTime - hitTime

        val mehWindow = beatmapCircle.hitWindow!!.mehWindow / 1000

        // If we have clicked circle
        val replayObjectData = replayObjectData
        if (replayObjectData != null) {
            if (passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                hitOffset = replayObjectData.accuracy / 1000f
                listener.registerAccuracy(HitObjectType.Normal, hitOffset.toDouble())
                startHit = true
                successfulHit = abs(hitOffset).toDouble() <= mehWindow
                // Remove circle and register hit in update thread
                judgement = listener.onCircleJudged(id, hitOffset, position, endsCombo, ResultType.fromId(replayObjectData.result))
                if (successfulHit && !listener.isAfterSeek()) {
                    playHitSamples()
                }
                removeFromScene()
                return
            }
        } else if (!autoPlay && listener.isObjectHittable(this)) {
            val hittingCursor = getHittingCursor(listener, beatmapCircle, passedTime.toDouble())

            if (hittingCursor != null) {
                hitOffset = (hittingCursor.hitTime - beatmapCircle.startTime).toFloat() / 1000
                listener.registerAccuracy(HitObjectType.Normal, hitOffset.toDouble())
                startHit = true
                successfulHit = abs(hitOffset).toDouble() <= mehWindow
                // Remove circle and register hit in update thread
                judgement = listener.onCircleJudged(id, hitOffset, position, endsCombo, null)
                if (successfulHit) {
                    playHitSamples()
                }
                removeFromScene()
                return
            }
        }

        if (circlePiece.isVisible) {
            if ((context.timing.isKiai && !OsuSkin.get().isDisableKiai)) {
                kiai = true
                circlePiece.setCircleColor(KiaiColors.at(context, comboColor))
            } else if (kiai) {
                circlePiece.setCircleColor(comboColor)
                kiai = false
            }
        }

        // We are still at approach time. Let entity modifiers finish first.
        if (passedTime < 0) {
            return
        }

        if (autoPlay) {
            // Remove circle and register hit in update thread
            hitOffset = 0f
            listener.registerAccuracy(HitObjectType.Normal, 0.0)
            judgement = listener.onCircleJudged(id, 0f, position, endsCombo, ResultType.HIT300)
            startHit = true
            successfulHit = true
            if (!listener.isAfterSeek()) {
                playHitSamples()
            }
            removeFromScene()
        } else {
            approachCircle.clearAnimations()
            approachCircle.alpha = 1 - MathUtils.clamp(passedTime / 0.05f, 0f, 1f)

            // If passed too much time, counting it as miss
            if (passedTime > mehWindow) {
                startHit = true
                val forcedResult = replayObjectData?.let { ResultType.fromId(it.result) }

                listener.registerAccuracy(HitObjectType.Normal, mehWindow + 1)
                judgement = listener.onCircleJudged(id, 10f, position, false, forcedResult)
                removeFromScene()
            }
        }
    }

    override fun onExpire() {
        circlePiece.clearAnimations()
        approachCircle.clearAnimations()

        circlePiece.removeSelf()
        approachCircle.removeSelf()
        releaseEffects()
        removeSelf()

        for (i in hitSamples.size - 1 downTo 0) {
            hitSamples[i].release()
        }

        hitSamples.clear()

        GameObjectPool.getInstance().putCircle(this)
    }

    override fun isJudged(): Boolean {
        return startHit
    }
}
