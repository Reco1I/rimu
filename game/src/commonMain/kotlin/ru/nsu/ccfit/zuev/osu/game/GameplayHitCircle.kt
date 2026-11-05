package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.math.MathUtils
import com.osudroid.mods.ModHidden
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.game.NumberedCirclePiece
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.skins.OsuSkin

class GameplayHitCircle : GameObject() {

    private val approachCircle: UISprite
    private var comboColor = Color4()
    private var listener: GameObjectListener? = null
    private var scene: UIContainer? = null
    private var beatmapCircle: HitCircle? = null
    private var passedTime = 0f
    private var timePreempt = 0f
    private var hitOffset = 0f
    private var kiai = false
    private var successfulHit = false
    private val hitSamples = ArrayList<GameplayHitSampleInfo>(5)

    /**
     * The circle piece that represents the circle body and overlay.
     */
    private val circlePiece: NumberedCirclePiece


    init {
        circlePiece = NumberedCirclePiece("hitcircle", "hitcircleoverlay")
        approachCircle = UISprite()
        approachCircle.origin = Anchor.Center
    }

    fun init(listener: GameObjectListener, pScene: UIContainer, beatmapCircle: HitCircle,
             comboColor: Color4) {
        // Storing parameters into fields
        this.beatmapCircle = beatmapCircle
        replayObjectData = null

        val stackedPosition = beatmapCircle.screenSpaceGameplayStackedPosition
        position.set(stackedPosition.x, stackedPosition.y)

        endsCombo = beatmapCircle.isLastInCombo
        this.listener = listener
        scene = pScene
        timePreempt = beatmapCircle.timePreempt.toFloat() / 1000

        val mehWindow = beatmapCircle.hitWindow!!.mehWindow.toFloat() / 1000
        hitOffset = mehWindow

        hitTime = beatmapCircle.startTime.toFloat() / 1000
        passedTime = -timePreempt
        startHit = false
        successfulHit = false
        kiai = (GameHelper.isKiai() && !OsuSkin.get().isDisableKiai)
        this.comboColor = comboColor

        val initialModifierTime = hitTime - timePreempt
        val scale = beatmapCircle.screenSpaceGameplayScale
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

        circlePiece.setNumberText(comboNum)
        circlePiece.setNumberScale(OsuSkin.get().comboTextScale)
        circlePiece.isVisible = !GameHelper.isTraceable() ||
            (Config.isShowFirstApproachCircle() && GameHelper.getTraceable()!!.firstObject === beatmapCircle)

        approachCircle.color = comboColor
        approachCircle.scale = scale * 3 * (beatmapCircle.timePreempt / GameHelper.getOriginalTimePreempt()).toFloat()
        approachCircle.alpha = 0f
        approachCircle.setPosition(this.position.x, this.position.y)
        approachCircle.isVisible = !GameHelper.isHidden() ||
            (Config.isShowFirstApproachCircle() && GameHelper.getHidden()!!.firstObject === beatmapCircle)

        approachCircle.textureRegion = SkinTextures.get(
            if (GameHelper.isTraceable()) "defaultapproachcircle" else "approachcircle")

        val scene = scene!!
        scene.add(0, circlePiece)
        scene += approachCircle

        val fadeOutCircle = GameHelper.isHidden() && !GameHelper.getHidden()!!.onlyFadeApproachCircles

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
            val approachDifferentMod = GameHelper.getApproachDifferent()

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

        if (Config.isDimHitObjects() && circlePiece.isVisible) {
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

            if (GameHelper.isSamplesMatchPlaybackRate()) {
                gameplaySample.frequency = GameHelper.getSpeedMultiplier()
            }

            hitSamples.add(gameplaySample)
        }

        lifetimeEnd = Float.MAX_VALUE
    }

    private fun removeFromScene() {
        if (scene == null) {
            return
        }

        lifetimeEnd = hitTime + hitOffset

        for (i in hitSamples.size - 1 downTo 0) {
            hitSamples[i].release()
        }

        hitSamples.clear()
        circlePiece.clearAnimations()
        approachCircle.clearAnimations()
        approachCircle.removeSelf()

        if (successfulHit || !circlePiece.isVisible || circlePiece.alpha == 0f) {
            circlePiece.removeSelf()
        } else {
            circlePiece.choreographNow { sequence ->
                sequence.fadeOut(0.1f).after { e -> updateThread { e.removeSelf() } }
                extendLifetime(sequence)
            }
        }

        scene = null
    }

    private fun playHitSamples() {
        listener!!.playHitSamples(hitSamples)
    }

    override fun update(dt: Float) {
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
                listener.onCircleHit(id, hitOffset, position, endsCombo, replayObjectData.result, comboColor)
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
                listener.onCircleHit(id, hitOffset, position, endsCombo, 0.toByte(), comboColor)
                if (successfulHit) {
                    playHitSamples()
                }
                removeFromScene()
                return
            }
        }

        if (circlePiece.isVisible) {
            if ((GameHelper.isKiai() && !OsuSkin.get().isDisableKiai)) {
                val kiaiModifier = max(0.0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()).toFloat() * 0.5f
                val r = min(1f, comboColor.red + (1 - comboColor.red) * kiaiModifier)
                val g = min(1f, comboColor.green + (1 - comboColor.green) * kiaiModifier)
                val b = min(1f, comboColor.blue + (1 - comboColor.blue) * kiaiModifier)
                kiai = true
                circlePiece.setCircleColor(r, g, b)
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
            listener.onCircleHit(id, 0f, position, endsCombo, ResultType.HIT300.id, comboColor)
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
                val forcedScore = replayObjectData?.result ?: 0

                removeFromScene()
                listener.registerAccuracy(HitObjectType.Normal, mehWindow + 1)
                listener.onCircleHit(id, 10f, position, false, forcedScore, comboColor)
            }
        }
    }

    override fun onExpire() {
        circlePiece.clearAnimations()
        approachCircle.clearAnimations()

        circlePiece.removeSelf()
        approachCircle.removeSelf()

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
