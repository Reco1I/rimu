package ru.nsu.ccfit.zuev.osu.game

import ru.nsu.ccfit.zuev.osu.Constants
import com.osudroid.ui.v2.skinTexture
import com.osudroid.ui.v2.skinFrames
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.game.CursorEvent
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.GameplaySequenceHitSampleInfo
import com.reco1l.verktex.math.Path
import com.reco1l.verktex.ui.UIPath
import com.osudroid.math.MathUtils
import com.osudroid.math.Precision
import com.osudroid.math.Vector2
import com.osudroid.mods.ModHidden
import com.osudroid.mods.ModSynesthesia
import com.osudroid.ui.v2.game.CirclePiece
import com.osudroid.ui.v2.game.NumberedCirclePiece
import com.osudroid.ui.v2.game.SliderTickContainer
import com.osudroid.ui.v2.game.SliderTickSprite
import com.osudroid.utils.BitSet
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.UIAnimatedSprite
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin

class GameplaySlider : GameObject() {

    private val approachCircle: UISprite
    private val startArrow: UISprite
    private val endArrow: UISprite
    private var beatmapSlider: Slider? = null
    private var controlPoints: BeatmapControlPoints? = null
    private var scene: UIContainer? = null
    private var stat: StatisticV2? = null
    private var listener: GameObjectListener? = null
    private var path: SliderPath? = null
    private var hitWindow: HitWindow? = null
    private var elapsedSpanTime = 0.0
    private var timePreempt = 0f
    private var duration = 0.0
    private var spanDuration = 0.0
    private var completedSpanCount = 0
    private var reverse = false
    private val isSliderBallFlip: Boolean
    private var shouldSnakeOut = false

    private val nestedHitSamples = ArrayList<ArrayList<GameplayHitSampleInfo>>(10)
    private val sliderSlideSample: GameplaySequenceHitSampleInfo
    private val sliderWhistleSample: GameplaySequenceHitSampleInfo

    private var headWasHit = false
    private var currentNestedObjectIndex = 0
    private var ticksGot = 0
    private var currentTickSpriteIndex = 0

    private var trackingCursorId = -1
    private var isTracking = false
    private var spanStarted = false

    private val followCircle: UISprite

    // Temporarily used Vector2 to avoid allocations
    private val tmpPoint = Vector2(0f)
    private val tmpTargetPoint = Vector2(0f)
    private var ballAngle = 0f

    private var kiai = false
    private var bodyColor = Color4()
    private var circleColor = Color4()

    //for replay
    private var firstHitAccuracy = 0
    private val tickSet = BitSet()
    private var replayTickIndex = 0

    private var superPath: Path? = null
    private var superPathMaxLength = 0f
    private var preStageFinish = false
    private var lastSnakeInLength = 0f
    private var lastSnakeOutStartLength = 0f
    private var lastSnakeOutEndLength = 0f

    private val sliderBody: UIPath

    /**
     * The absolute slider's path end position.
     * This already takes into account the absolute object's position.
     */
    private val pathEndPosition = Vector2(0f)

    /**
     * The slider ball sprite.
     */
    private val ball: UIAnimatedSprite

    /**
     * The start circle piece of the slider.
     */
    private val headCirclePiece: NumberedCirclePiece

    /**
     * The end circle piece of the slider.
     */
    private val tailCirclePiece: CirclePiece

    /**
     * The slider tick container.
     */
    private val tickContainer: SliderTickContainer

    /**
     * Whether the slider has ended (and all its spans).
     */
    private var isOver = false

    /**
     * Whether the cursor is in the slider's radius.
     */
    private var isInRadius = false


    init {

        headCirclePiece = NumberedCirclePiece(sliderStartCircleTexture, sliderStartCircleOverlayTexture)
        tailCirclePiece = CirclePiece(sliderEndCircleTexture, sliderEndCircleOverlayTexture)

        approachCircle = UISprite()
        approachCircle.origin = Anchor.Center

        startArrow = UISprite()
        startArrow.origin = Anchor.Center
        startArrow.texture = skinTexture("reversearrow")

        endArrow = UISprite()
        endArrow.origin = Anchor.Center
        endArrow.texture = skinTexture("reversearrow")

        ball = UIAnimatedSprite(skinFrames("sliderb"), 60f)
        ball.origin = Anchor.Center

        // Animated or not depending on the skin, which may change while the slider is alive.
        followCircle = UIAnimatedSprite(skinFrames("sliderfollowcircle", orStatic = true), OsuSkin.get().animationFramerate)
        followCircle.origin = Anchor.Center

        sliderBody = UIPath()
        tickContainer = SliderTickContainer()
        isSliderBallFlip = OsuSkin.get().isSliderBallFlip

        sliderSlideSample = GameplaySequenceHitSampleInfo()
        sliderWhistleSample = GameplaySequenceHitSampleInfo()
    }

    fun init(listener: GameObjectListener, scene: UIContainer, stat: StatisticV2,
             beatmapSlider: Slider, controlPoints: BeatmapControlPoints, comboColor: Color4,
             borderColor: Color4, sliderPath: SliderPath, renderPath: Path) {
        this.listener = listener
        this.scene = scene
        this.stat = stat
        this.beatmapSlider = beatmapSlider
        this.controlPoints = controlPoints

        val stackedPosition = beatmapSlider.screenSpaceGameplayStackedPosition
        position.set(stackedPosition.x, stackedPosition.y)

        hitTime = beatmapSlider.startTime.toFloat() / 1000
        timePreempt = beatmapSlider.timePreempt.toFloat() / 1000
        endsCombo = beatmapSlider.isLastInCombo
        elapsedSpanTime = -timePreempt.toDouble()
        duration = beatmapSlider.duration / 1000
        spanDuration = beatmapSlider.spanDuration / 1000
        path = sliderPath
        hitWindow = beatmapSlider.head.hitWindow
        trackingCursorId = -1
        isTracking = false
        headWasHit = false

        reloadHitSounds()

        val initialModifierTime = hitTime - timePreempt
        val scale = beatmapSlider.screenSpaceGameplayScale

        isOver = false
        isInRadius = false
        spanStarted = false

        reverse = false
        startHit = false
        shouldSnakeOut = false
        ticksGot = 0
        completedSpanCount = 0
        currentTickSpriteIndex = 0
        replayTickIndex = 0
        firstHitAccuracy = 0
        tickSet.clear()
        kiai = (GameHelper.isKiai() && !OsuSkin.get().isDisableKiai)
        preStageFinish = false
        superPathMaxLength = 0f
        lastSnakeInLength = Float.NaN
        lastSnakeOutStartLength = Float.NaN
        lastSnakeOutEndLength = Float.NaN
        bodyColor = comboColor
        if (!OsuSkin.get().isSliderFollowComboColor) {
            val skinBodyColor = OsuSkin.get().sliderBodyColor
            bodyColor = skinBodyColor ?: comboColor
        }
        circleColor = comboColor
        currentNestedObjectIndex = 0

        val applyIncreasedVisibilityToCirclePiece = !GameHelper.isTraceable() ||
            (Settings[OsuSettings.ShowFirstApproachCircle] && GameHelper.getTraceable()!!.firstObject === beatmapSlider)

        // Start circle piece
        headCirclePiece.setCircleTextureRegion(sliderStartCircleTexture)
        headCirclePiece.setOverlayTextureRegion(sliderStartCircleOverlayTexture)
        headCirclePiece.showNumber()
        headCirclePiece.scale = scale
        headCirclePiece.setCircleColor(comboColor)
        headCirclePiece.alpha = 0f
        headCirclePiece.setPosition(this.position.x, this.position.y)
        var comboNum = beatmapSlider.indexInCurrentCombo + 1
        if (OsuSkin.get().isLimitComboTextLength) {
            comboNum %= 10
        }
        headCirclePiece.setNumberText(comboNum)
        headCirclePiece.setNumberScale(OsuSkin.get().comboTextScale)
        headCirclePiece.isVisible = applyIncreasedVisibilityToCirclePiece

        approachCircle.color = comboColor
        approachCircle.scale = scale * 3 * (beatmapSlider.timePreempt / GameHelper.getOriginalTimePreempt()).toFloat()
        approachCircle.alpha = 0f
        approachCircle.setPosition(this.position.x, this.position.y)
        approachCircle.isVisible = !GameHelper.isHidden() ||
            (Settings[OsuSettings.ShowFirstApproachCircle] && GameHelper.getHidden()!!.firstObject === beatmapSlider)

        approachCircle.texture = skinTexture(
            if (GameHelper.isTraceable()) "defaultapproachcircle" else "approachcircle")

        scene.add(0, headCirclePiece)
        scene += approachCircle

        // End circle
        getPositionAt(1f, false, false)
        pathEndPosition.set(tmpPoint.x, tmpPoint.y)

        val initialTailColor = if (GameHelper.isSynesthesia())
            getSynesthesiaComboColor(beatmapSlider.startTime + beatmapSlider.spanDuration)
        else
            comboColor

        tailCirclePiece.scale = scale
        tailCirclePiece.setCircleColor(initialTailColor)
        tailCirclePiece.alpha = 0f
        tailCirclePiece.isVisible = applyIncreasedVisibilityToCirclePiece

        // Repeat arrow at start
        val spanCount = beatmapSlider.spanCount
        if (spanCount > 2) {
            startArrow.alpha = 0f
            startArrow.scale = scale
            startArrow.setPosition(this.position.x, this.position.y)

            getPositionAt(0f, true, false)
            startArrow.rotationZ = ballAngle

            scene.add(0, startArrow)
        }

        // When snaking in is enabled, the first repeat or tail needs to be delayed until the snaking completes.
        val fadeInDelay = if (Settings[OsuSettings.SnakingInSliders]) timePreempt / 3 else 0f
        val headFadeIn = beatmapSlider.head.timeFadeIn.toFloat() / 1000

        // Repeat arrow at end
        if (spanCount > 1) {
            endArrow.alpha = 0f
            endArrow.scale = scale

            if (Settings[OsuSettings.SnakingInSliders]) {
                getPositionAt(0f, false, true)

                tailCirclePiece.setPosition(position.x, position.y)
                endArrow.setPosition(position.x, position.y)
            } else {
                getPositionAt(1f, false, true)

                tailCirclePiece.setPosition(pathEndPosition.x, pathEndPosition.y)
                endArrow.setPosition(pathEndPosition.x, pathEndPosition.y)
            }

            scene.add(0, endArrow)

            // We can use headFadeIn here since the tail's fade in time is the same as the head.
            endArrow.choreographAt(initialModifierTime) { sequence -> sequence
                .delay(fadeInDelay)
                .fadeIn(headFadeIn) }
        }

        scene.add(0, tailCirclePiece)

        if (GameHelper.isHidden() && !GameHelper.getHidden()!!.onlyFadeApproachCircles) {
            val fadeOutDuration = timePreempt * ModHidden.FADE_OUT_DURATION_MULTIPLIER.toFloat()
            val finalTailAlpha = (headFadeIn - fadeInDelay) / headFadeIn

            headCirclePiece.choreographAt(initialModifierTime) { sequence -> sequence
                .fadeIn(headFadeIn)
                .then()
                .fadeOut(fadeOutDuration) }

            tailCirclePiece.choreographAt(initialModifierTime) { sequence -> sequence
                .delay(fadeInDelay)
                .fadeTo(finalTailAlpha, headFadeIn - fadeInDelay)
                .then()
                .fadeOut(fadeOutDuration) }
        } else {
            headCirclePiece.choreographAt(initialModifierTime) { sequence -> sequence.fadeIn(headFadeIn) }

            val mehWindow = hitWindow!!.mehWindow.toFloat() / 1000
            val okWindow = hitWindow!!.okWindow.toFloat() / 1000

            headCirclePiece.choreographAt(hitTime + okWindow) { sequence -> sequence.fadeOut(mehWindow - okWindow) }

            tailCirclePiece.choreographAt(initialModifierTime) { sequence -> sequence
                .delay(fadeInDelay)
                .fadeIn(headFadeIn) }
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
                .fadeTo(0.9f, min(headFadeIn * 2, timePreempt))
                .scaleTo(scale, timePreempt, easing)
                .after { e -> e.alpha = 0f } }
        }

        // Slider track
        // The body is drawn in osu!pixels, scaled to the playfield like the slider's positions are.
        val widthScale = Constants.MAP_ACTUAL_WIDTH.toFloat() / Constants.MAP_WIDTH
        val heightScale = Constants.MAP_ACTUAL_HEIGHT.toFloat() / Constants.MAP_HEIGHT

        val sliderBodyWidth = OsuSkin.get().sliderBodyWidth * scale / widthScale
        val sliderBorderWidth = OsuSkin.get().sliderBorderWidth * scale / widthScale

        sliderBody.path = renderPath
        sliderBody.setPosition(stackedPosition.x, stackedPosition.y)
        sliderBody.scaleOrigin = Anchor.TopLeft
        sliderBody.scaleX = widthScale
        sliderBody.scaleY = heightScale
        sliderBody.halfWidth = sliderBodyWidth
        sliderBody.borderWidth = sliderBorderWidth
        sliderBody.borderColor = borderColor
        sliderBody.startLength = 0f
        sliderBody.endLength = if (Settings[OsuSettings.SnakingInSliders]) 0f else Float.MAX_VALUE
        sliderBody.alpha = 0f

        superPath = renderPath
        superPathMaxLength = sliderBody.length

        // The body as osu!stable draws it (see osu!lazer's LegacySliderBody): a gradient from the colour
        // darkened at the border to it lightened at the centre, and a shadow outside the border taking
        // the outer 5/64 of the whole width.
        sliderBody.shadowWidth = sliderBodyWidth * SHADOW_PORTION / (1 - SHADOW_PORTION)

        // Head circle not being visible means Traceable is applied to this slider
        if (GameHelper.isTraceable() && !headCirclePiece.isVisible) {
            sliderBody.bodyColor = Color4(0f, 0f, 0f, 0f)
            sliderBody.bodyCenterColor = null
        } else {
            val bodyAlpha = OsuSkin.get().sliderBodyBaseAlpha
            sliderBody.bodyColor = darken(bodyColor, 0.1f).alpha(bodyAlpha)
            sliderBody.bodyCenterColor = lighten(bodyColor, 0.5f).alpha(bodyAlpha)
        }

        scene.add(0, tickContainer)
        scene.add(0, sliderBody)

        tickContainer.init(beatmapSlider)

        if (Settings[OsuSettings.DimHitObjects]) {
            applyDim(headCirclePiece)
            applyDim(tailCirclePiece)
            applyDim(endArrow)
            applyDim(sliderBody)
        }

        sliderBody.choreographAt(initialModifierTime) { sequence ->
            val fadeInDuration = beatmapSlider.timeFadeIn.toFloat() / 1000
            sequence.fadeInFromZero(fadeInDuration)

            if (GameHelper.isHidden() && !GameHelper.getHidden()!!.onlyFadeApproachCircles) {
                // New duration from completed fade in to end (before fading out)
                val fadeOutDuration = duration.toFloat() + timePreempt - fadeInDuration

                sequence.then().fadeOut(fadeOutDuration, Easing.OutQuad)
            }
        }

        lifetimeEnd = Float.MAX_VALUE
    }

    private fun getPositionAt(percentage: Float, updateBallAngle: Boolean, updateEndArrowRotation: Boolean): Vector2 {
        val path = path!!
        val index = path.positionAt(tmpPoint, percentage)
        tmpPoint.set(tmpPoint.x + position.x, tmpPoint.y + position.y)

        if (path.anchorCount < 2) {
            return tmpPoint
        }

        if (updateBallAngle || updateEndArrowRotation) {
            // Ball rotation depends on the movement direction (reverse or forward).
            // End arrows always point backward (towards the slider body) relative to the tip.
            var direction = if (updateBallAngle) (if (reverse) -1 else 1) else -1

            // Search forward from the current segment index, or backward from the previous one.
            var searchStart = if (direction > 0) index else index - 1

            var found = findRotationTarget(searchStart, direction, tmpPoint, tmpTargetPoint)

            if (!found) {
                // If we reached the end of the path in the intended direction without finding a valid point
                // (e.g., at 0% or 100% progress), search in the opposite direction.
                direction = -direction
                searchStart = if (direction > 0) index else index - 1
                found = findRotationTarget(searchStart, direction, tmpPoint, tmpTargetPoint)

                if (found) {
                    // Reflect the target point across the current position to maintain the original intended direction.
                    tmpTargetPoint.set(tmpPoint.x * 2 - tmpTargetPoint.x, tmpPoint.y * 2 - tmpTargetPoint.y)
                }
            }

            if (found) {
                val aimRotation = toDegrees(Utils.direction(tmpPoint.x, tmpPoint.y, tmpTargetPoint.x, tmpTargetPoint.y))

                if (updateBallAngle) {
                    ballAngle = wrapRotation(aimRotation, ballAngle)
                } else {
                    endArrow.rotationZ = wrapRotation(aimRotation, endArrow.rotationZ)
                }
            }
        }

        return tmpPoint
    }

    /**
     * Searches for the nearest point on the path that is spatially distinct from the current position.
     * This ensures a stable direction vector even when the path contains duplicate points.
     */
    private fun findRotationTarget(startIndex: Int, direction: Int, currentPos: Vector2, outTarget: Vector2): Boolean {
        val path = path!!
        var i = startIndex
        while (i >= 0 && i < path.anchorCount) {
            val xi = path.getX(i) + position.x
            val yi = path.getY(i) + position.y

            if (!Precision.almostEquals(currentPos.x, xi) || !Precision.almostEquals(currentPos.y, yi)) {
                outTarget.set(xi, yi)
                return true
            }
            i += direction
        }
        return false
    }

    /**
     * Wraps the target rotation so that it takes the shortest path (<= 180 degrees) from the current rotation.
     * Prevents visual glitches where sprites might spin 360 degrees when crossing the 180/-180 degree boundary.
     */
    private fun wrapRotation(rotation: Float, currentRotation: Float): Float {
        var rotation = rotation
        while (abs(rotation - currentRotation) > 180) {
            rotation += if (rotation < currentRotation) 360 else -360
        }

        return rotation
    }

    private fun removeFromScene() {
        if (scene == null) {
            return
        }

        val beatmapSlider = beatmapSlider!!

        val modifierStartTime = hitTime +
            max(duration, (if (headWasHit) firstHitAccuracy.toDouble() else hitWindow!!.mehWindow) / 1000.0).toFloat()

        lifetimeEnd = modifierStartTime

        if (Settings[OsuSettings.AnimateFollowCircle]) {
            if (isInRadius) {
                followCircle.clearAnimations()

                followCircle.choreographAt(modifierStartTime) { sequence ->
                    sequence.scaleTo(beatmapSlider.screenSpaceGameplayScale * 0.8f, 0.2f, Easing.OutQuad)
                        .fadeOut(0.2f, Easing.InQuad)

                    extendLifetime(sequence)
                }
            }
        } else {
            followCircle.removeSelf()
        }

        if (GameHelper.getHidden() != null && !GameHelper.getHidden()!!.onlyFadeApproachCircles) {
            sliderBody.removeSelf()
        } else {
            sliderBody.choreographAt(modifierStartTime) { sequence ->
                // Short fade for snaking out sliders to allow for any body color to smoothly disappear.
                sequence.fadeOut(if (headWasHit && shouldSnakeOut) 0.04f else 0.24f)
                    .after { e -> updateThread { e.removeSelf() } }

                extendLifetime(sequence)
            }
        }

        if (!headCirclePiece.isAnimating) {
            // When animating, the head circle will detach after the animation ends.
            headCirclePiece.removeSelf()
        }

        tailCirclePiece.removeSelf()
        approachCircle.removeSelf()
        startArrow.removeSelf()
        endArrow.removeSelf()
        tickContainer.removeSelf()

        stopSlidingSamples()

        for (i in 0 until nestedHitSamples.size) {
            val hitSamples = nestedHitSamples[i]

            for (j in hitSamples.size - 1 downTo 0) {
                hitSamples[j].release()
            }

            hitSamples.clear()
        }

        path = null
        scene = null
    }

    override fun onExpire() {
        headCirclePiece.clearAnimations()
        tailCirclePiece.clearAnimations()
        startArrow.clearAnimations()
        endArrow.clearAnimations()
        approachCircle.clearAnimations()
        followCircle.clearAnimations()
        ball.clearAnimations()
        sliderBody.clearAnimations()
        tickContainer.clearAnimations()

        headCirclePiece.removeSelf()
        tailCirclePiece.removeSelf()
        startArrow.removeSelf()
        endArrow.removeSelf()
        approachCircle.removeSelf()
        followCircle.removeSelf()
        ball.removeSelf()
        sliderBody.removeSelf()
        tickContainer.removeSelf()


        // removeFromScene() stops looping sounds and releases hit samples, but during a seek, onExpire() fires without
        // removeFromScene(). This causes looping sounds to continuously play after a seek, and nested hit samples
        // remain populated, so when the pool reuses this slider, reloadHitSounds() appends to them instead of an empty
        // list, resulting in doubled hitsounds at every tick/repeat.
        stopLoopingSamples()

        for (i in 0 until nestedHitSamples.size) {
            val hitSamples = nestedHitSamples[i]

            for (j in hitSamples.size - 1 downTo 0) {
                hitSamples[j].release()
            }

            hitSamples.clear()
        }

        GameObjectPool.getInstance().putSlider(this)
    }

    private fun onSpanFinish() {
        val hitWindow = hitWindow ?: return
        val beatmapSlider = beatmapSlider!!
        val listener = listener!!

        ++completedSpanCount

        val totalSpanCount = beatmapSlider.spanCount
        val remainingSpans = totalSpanCount - completedSpanCount
        val stillHasSpan = remainingSpans > 0
        val isTracking = isTracking()

        // If slider was in reverse mode, we should swap start and end points
        val spanEndJudgementPosition = if (reverse) position else pathEndPosition

        // Do not judge slider repeats if the slider head has not been hit.
        if (startHit) {
            if (isTracking) {
                playCurrentNestedObjectHitSound()
                ticksGot++
                tickSet.set(replayTickIndex++, true)
            } else {
                tickSet.set(replayTickIndex++, false)
            }

            if (stillHasSpan) {
                listener.onSliderHit(id, if (isTracking) 30 else -1, spanEndJudgementPosition, false,
                    bodyColor, GameObjectListener.SLIDER_REPEAT, isTracking)
            }

            updateSlidingSamplesVolume()
            currentNestedObjectIndex++
        }

        // If slider has more spans
        if (stillHasSpan) {
            reverse = !reverse
            elapsedSpanTime -= spanDuration

            if (isSliderBallFlip) {
                ball.flippedHorizontal = reverse
            }

            // Restore ticks
            tickContainer.onNewSpan(completedSpanCount)
            currentTickSpriteIndex = if (reverse) tickContainer.childCount - 1 else 0

            // Setting visibility of repeat arrows
            if (reverse) {
                if (remainingSpans <= 2) {
                    endArrow.alpha = 0f
                    tailCirclePiece.alpha = 0f
                } else if (GameHelper.isSynesthesia()) {
                    val newColor = getSynesthesiaComboColor(beatmapSlider.nestedHitObjects[currentNestedObjectIndex])

                    tailCirclePiece.color = newColor
                }
            } else if (remainingSpans <= 2 && startHit) {
                startArrow.alpha = 0f
                headCirclePiece.alpha = 0f
            } else if (GameHelper.isSynesthesia()) {
                val newColor =
                    getSynesthesiaComboColor(beatmapSlider.nestedHitObjects[currentNestedObjectIndex])

                headCirclePiece.setCircleColor(newColor)
            }

            listener.onSliderReverse(
                spanEndJudgementPosition,
                if (reverse) endArrow.rotationZ else startArrow.rotationZ,
                bodyColor)

            if (elapsedSpanTime >= spanDuration) {
                // This condition can happen under low frame rate and/or short span duration, which will cause all
                // slider tick judgements in this span to be skipped. Ensure that all slider ticks in the current
                // span has been judged before proceeding to the next span.
                judgeSliderTicks()
                updateSlidingSamplesVolume()

                onSpanFinish()
            }

            return
        }
        isOver = true

        if (!startHit) {
            // Slider head was never hit - miss the entire slider before the end.
            onSliderHeadHit(getLateHitOffset())
        }

        // Calculating score
        var score = 0
        val replayObjectData = replayObjectData

        if (replayObjectData == null) {
            var firstHitScore = 0

            if (GameHelper.isScoreV2()) {
                // If ScoreV2 is active, the accuracy of hitting the slider head is additionally accounted for when judging the entire slider:
                // Getting a 300 for a slider requires getting a 300 judgement for the slider head.
                // Getting a 100 for a slider requires getting a 100 judgement or better for the slider head.
                if (abs(firstHitAccuracy) <= hitWindow.greatWindow) {
                    firstHitScore = 300
                } else if (abs(firstHitAccuracy) <= hitWindow.okWindow) {
                    firstHitScore = 100
                }
            }

            val totalTicks = beatmapSlider.nestedHitObjects.size

            if (ticksGot > 0) {
                score = 50
            }

            if (ticksGot >= totalTicks / 2 && (!GameHelper.isScoreV2() || firstHitScore >= 100)) {
                score = 100
            }

            if (ticksGot >= totalTicks && (!GameHelper.isScoreV2() || firstHitScore == 300)) {
                score = 300
            }
        } else if (replayObjectData.result == ResultType.HIT300.id) {
            score = 300
        } else if (replayObjectData.result == ResultType.HIT100.id) {
            score = 100
        } else if (replayObjectData.result == ResultType.HIT50.id) {
            score = 50
        }

        // In replays older than version 6, slider ends always give combo even when not being tracked.
        val awardCombo = isTracking || (replayObjectData != null && GameHelper.getReplayVersion() < 6)

        listener.onSliderHit(id, score, spanEndJudgementPosition, endsCombo, bodyColor,
            GameObjectListener.SLIDER_END, awardCombo)

        listener.onSliderEnd(id, firstHitAccuracy, tickSet)

        removeFromScene()
    }

    private fun updateTracking(position: Vector2) {
        val listener = listener!!

        if (!startHit) {
            // Do not allow tracking to happen when the slider head is not yet judged.
            isTracking = false
            trackingCursorId = -1
            return
        }

        if (autoPlay || replayObjectData != null) {
            trackingCursorId = 0
            isTracking = true
        } else {
            if (trackingCursorId != -1) {
                // If the slider is being tracked, we only want to check if the tracking cursor is still tracking it.
                val trackingCursor = listener.getCursor(trackingCursorId)
                val latestEvent = trackingCursor.latestEvent

                if (latestEvent != null && !latestEvent.isActionUp) {
                    isTracking = isCursorTracking(position, latestEvent)
                } else {
                    trackingCursorId = -1
                    isTracking = false
                }
            }

            if (trackingCursorId == -1) {
                // Check if any cursor is tracking the slider.
                for (i in 0 until listener.getCursorsCount()) {
                    val cursor = listener.getCursor(i)
                    val latestEvent = cursor.latestEvent

                    if (latestEvent != null && isCursorTracking(position, latestEvent)) {
                        trackingCursorId = i
                        isTracking = true
                        break
                    }
                }
            }
        }

        updateFollowCircleTrackingState()
    }

    private fun isCursorTracking(trackingPosition: Vector2, cursorEvent: CursorEvent): Boolean {
        if (cursorEvent.isActionUp) {
            return false
        }

        if (GameHelper.isAutopilot()) {
            return true
        }

        val trackingDistanceThresholdSquared = getTrackingDistanceThresholdSquared(isTracking())
        return Utils.squaredDistance(trackingPosition, cursorEvent.position) <= trackingDistanceThresholdSquared
    }

    private fun isTracking(): Boolean {
        val replayObjectData = replayObjectData
        return isTracking && replayObjectData == null ||
            replayObjectData != null && replayObjectData.tickSet!![replayTickIndex]
    }

    private fun updateFollowCircleTrackingState() {
        val scale = beatmapSlider!!.screenSpaceGameplayScale
        val isTracking = isTracking()

        if (isTracking && !isInRadius) {
            playSlidingSamples()

            if (Settings[OsuSettings.AnimateFollowCircle]) {
                val remainTime = max(0.0, duration - elapsedSpanTime).toFloat()
                val initialScale = if (followCircle.alpha == 0f) scale * 0.5f else followCircle.scaleX

                followCircle.clearAnimations()
                followCircle.scale = initialScale

                followCircle.scaleTo(scale, min(remainTime, 0.18f), Easing.OutQuad)
                followCircle.fadeIn(min(remainTime, 0.06f))
            } else {
                followCircle.alpha = 1f
            }
        } else if (!isTracking && isInRadius) {
            stopSlidingSamples()

            if (Settings[OsuSettings.AnimateFollowCircle]) {
                followCircle.clearAnimations()

                followCircle.scaleTo(scale * 2, 0.1f)
                followCircle.fadeOut(0.1f)
            } else {
                followCircle.alpha = 0f
            }
        }

        isInRadius = isTracking
    }

    override fun update(dt: Float) {
        val scene = scene ?: return
        val listener = listener!!
        val beatmapSlider = beatmapSlider!!

        elapsedSpanTime = listener.elapsedTime - hitTime - completedSpanCount * spanDuration

        val elapsedTime = completedSpanCount * spanDuration + elapsedSpanTime

        // If the slider head is not judged yet
        if (!startHit) {
            val replayObjectData = replayObjectData
            if (replayObjectData == null) {
                if (!autoPlay) {
                    if (elapsedTime <= getLateHitThreshold()) {
                        if (listener.isObjectHittable(this)) {
                            val hittingCursor = getHittingCursor(listener, beatmapSlider.head, elapsedTime)

                            if (hittingCursor != null) {
                                onSliderHeadHit((hittingCursor.hitTime - beatmapSlider.startTime) / 1000)
                            }
                        }
                    } else {
                        // If it's too late, mark this hit missing.
                        onSliderHeadHit(getLateHitOffset())
                    }
                } else if (elapsedSpanTime >= 0) {
                    onSliderHeadHit(0.0)
                }
            } else {
                val accuracy = replayObjectData.accuracy / 1000.0

                if (elapsedTime + dt / 2 > accuracy) {
                    onSliderHeadHit(accuracy)
                }
            }
        }

        if (headCirclePiece.isVisible) {
            if ((GameHelper.isKiai() && !OsuSkin.get().isDisableKiai)) {
                val kiaiModifier = max(0.0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()).toFloat() * 0.5f
                val r = min(1f, circleColor.red + (1 - circleColor.red) * kiaiModifier)
                val g = min(1f, circleColor.green + (1 - circleColor.green) * kiaiModifier)
                val b = min(1f, circleColor.blue + (1 - circleColor.blue) * kiaiModifier)
                kiai = true
                headCirclePiece.setCircleColor(r, g, b)
            } else if (kiai) {
                headCirclePiece.setCircleColor(circleColor)
                kiai = false
            }
        }

        if (elapsedSpanTime < 0) // we at approach time
        {
            if (startHit) {
                // Hide the approach circle if the slider is already hit.
                approachCircle.clearAnimations()
                approachCircle.alpha = 0f
            }

            if (Settings[OsuSettings.SnakingInSliders]) {
                val percentage = MathUtils.clamp((timePreempt + elapsedSpanTime).toFloat() / (timePreempt / 3), 0f, 1f)

                if (percentage < 1) {
                    if (superPath != null) {
                        val length = superPathMaxLength * percentage
                        applySnakeBodyLength(false, length, false)
                    }

                    val position = getPositionAt(percentage, false, true)

                    tailCirclePiece.setPosition(position.x, position.y)
                    endArrow.setPosition(position.x, position.y)
                } else {
                    if (!preStageFinish && superPath != null) {
                        applySnakeBodyLength(false, superPathMaxLength, true)
                        preStageFinish = true
                    }

                    getPositionAt(1f, false, true)

                    tailCirclePiece.setPosition(pathEndPosition.x, pathEndPosition.y)
                    endArrow.setPosition(pathEndPosition.x, pathEndPosition.y)
                }
            }
            return
        }

        // If we entered this phase without going through the approach phase (e.g., after a seek), the snake-in
        // animation may not have completed yet. Force the body to its full length here.
        if (!preStageFinish && Settings[OsuSettings.SnakingInSliders] && superPath != null) {
            applySnakeBodyLength(false, superPathMaxLength, true)
            preStageFinish = true
        }

        sliderSlideSample.update(dt)
        sliderWhistleSample.update(dt)

        val scale = beatmapSlider.screenSpaceGameplayScale

        // Note that this is not using ball.hasParent() because the ball's auto-detach animation can fire slightly
        // before onSpanFinish() due to float/double precision differences in end-time computation, causing
        // ball.hasParent() == false while followCircle is still attached.
        if (!spanStarted) {
            spanStarted = true

            ball.frameTime = 1f / (beatmapSlider.velocity.toFloat() * Slider.BASE_SCORING_DISTANCE * scale)
            ball.scale = scale
            ball.flippedHorizontal = false
            ball.alpha = 1f

            followCircle.alpha = 0f
            if (!Settings[OsuSettings.AnimateFollowCircle]) {
                followCircle.scale = scale
            }

            // isInRadius may already be true if the slider head was hit during the approach phase,
            // before this block ran. Reset it so updateFollowCircleTrackingState correctly enters
            // the "tracking starts" branch and makes the follow circle visible.
            isInRadius = false

            scene += ball
            scene += followCircle

            ball.choreographAt(beatmapSlider.endTime.toFloat() / 1000) { sequence ->
                sequence.fadeOut().after { e -> updateThread { e.removeSelf() } } }
        }

        approachCircle.clearAnimations()

        if (startHit) {
            approachCircle.alpha = 0f
        } else {
            approachCircle.alpha = 1 - MathUtils.clamp(elapsedTime.toFloat() / 0.05f, 0f, 1f)
        }

        val percentage = MathUtils.clamp((elapsedSpanTime / spanDuration).toFloat(), 0f, 1f)
        val bodyProgress = if (reverse) 1 - percentage else percentage

        if (shouldSnakeOut && Settings[OsuSettings.SnakingOutSliders] && completedSpanCount == beatmapSlider.spanCount - 1) {
            val length = bodyProgress * superPathMaxLength

            // In reverse, the snaking out animation starts from the end node.
            applySnakeBodyLength(!reverse, length, false)
        }

        // Ball position
        val ballPos = getPositionAt(bodyProgress, true, false)
        updateTracking(ballPos)
        listener.onTrackingSliders(isTracking())

        judgeSliderTicks()
        updateSlidingSamplesVolume()

        // Setting position of ball and follow circle
        followCircle.setPosition(ballPos.x, ballPos.y)
        ball.setPosition(ballPos.x, ballPos.y)
        ball.rotationZ = ballAngle

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            listener.updateAutoBasedPos(ballPos.x, ballPos.y)
        }

        // If we got 100% time, finishing slider
        if (percentage >= 1) {
            onSpanFinish()
        }
    }

    override fun isJudged(): Boolean {
        return if (listener?.isRemoveSliderLock() == true) startHit else isOver
    }

    private fun getTrackingDistanceThresholdSquared(isTracking: Boolean): Float {
        val radius = beatmapSlider!!.screenSpaceGameplayRadius.toFloat()
        var distanceThresholdSquared = radius * radius

        if (isTracking) {
            // Multiply by 4 as the follow circle radius is 2 times larger than the object radius.
            distanceThresholdSquared *= 4f
        }

        return distanceThresholdSquared
    }

    private fun onSliderHeadHit(hitOffset: Double) {
        // Reference: https://github.com/ppy/osu/blob/bca42e9d24f1b8e433f63db8dbf5d36d8b811b36/osu.Game.Rulesets.Osu/Objects/Drawables/SliderInputManager.cs#L78
        // The reference does not fully represent the cases below as they are mixed with replay handling.
        val hitWindow = hitWindow
        if (startHit || hitWindow == null) {
            return
        }

        val listener = listener!!
        val beatmapSlider = beatmapSlider!!
        val replayObjectData = replayObjectData

        startHit = true

        // Override hit offset with replay data when available.
        firstHitAccuracy = replayObjectData?.accuracy?.toInt() ?: (hitOffset * 1000).toInt()
        val mehWindow = hitWindow.mehWindow / 1000

        if (replayObjectData == null || GameHelper.getReplayVersion() >= 6 || mehWindow <= duration) {
            listener.registerAccuracy(HitObjectType.Slider, hitOffset)
            headWasHit = -mehWindow <= hitOffset && hitOffset <= getLateHitThreshold()

            if (headWasHit) {
                if (!listener.isAfterSeek()) {
                    playCurrentNestedObjectHitSound()
                }
                ticksGot++
                shouldSnakeOut = true
                listener.onSliderHit(id, 30, position,
                    false, bodyColor, GameObjectListener.SLIDER_START, true)
            } else {
                listener.onSliderHit(id, -1, position,
                    false, bodyColor, GameObjectListener.SLIDER_START, false)
            }
        } else if (hitOffset <= duration) {
            // In replays older than version 6, when the 50 hit window is longer than the duration of the slider,
            // the slider head is considered to *not* exist if it was not hit until the slider is over.
            // It is a very weird behavior, but that's what it actually was...
            listener.registerAccuracy(HitObjectType.Slider, hitOffset)
            headWasHit = true
            if (!listener.isAfterSeek()) {
                playCurrentNestedObjectHitSound()
            }
            ticksGot++
            shouldSnakeOut = true
            listener.onSliderHit(id, 30, position,
                false, bodyColor, GameObjectListener.SLIDER_START, true)
        }

        updateSlidingSamplesVolume()
        currentNestedObjectIndex++

        // When the head is hit late:
        // - If the cursor has at all times been within range of the expanded follow area, hit all nested objects that have been passed through.
        // - If the cursor has at some point left the expanded follow area, miss those nested objects instead.

        // Get current ball position.
        val progress = MathUtils.clamp(elapsedSpanTime / spanDuration, 0.0, 1.0).toFloat()
        val ballPos = getPositionAt(if (reverse) 1 - progress else progress, false, false)

        val distanceTrackingThresholdSquared = getTrackingDistanceThresholdSquared(true)

        // Check whether the cursor is within the follow area.
        updateTracking(ballPos)

        val currentTime = getGameplayPassedTimeMilliseconds()
        var allTicksInRange = false

        val nestedObjects = beatmapSlider.nestedHitObjects

        // Replays force their hit results per nested object, so we do not need to check for tracking here.
        if (isTracking() && replayObjectData == null) {
            allTicksInRange = true

            // In Autoplay, the cursor is always at the ball's exact position, so the distance check below
            // is always satisfied and can be skipped. This also avoids false misses after seeking: when
            // seeking into the middle of a slider, elapsedSpanTime > 0 immediately, so past ticks are
            // evaluated with the ball already ahead of them — their positions would fail the distance
            // check even though Autoplay would always have tracked them.
            if (!autoPlay) {
                // Do not judge the slider end as it will be judged in onSpanFinish.
                for (i in 1 until nestedObjects.size - 1) {
                    val nestedObject = nestedObjects[i]

                    // Stop the process when a nested object that can't be hit before the current time is reached.
                    if (nestedObject.startTime > currentTime) {
                        break
                    }

                    // When the first nested object that is further outside the follow area is reached,
                    // forcefully miss all other nested objects that would otherwise be valid to be hit.
                    // This covers a case of a slider overlapping itself that requires tracking to a tick on an outer edge.
                    val nestedPosition = nestedObject.screenSpaceGameplayStackedPosition
                    val distanceSquared = Utils.squaredDistance(nestedPosition.x, nestedPosition.y, ballPos.x, ballPos.y)

                    if (distanceSquared > distanceTrackingThresholdSquared) {
                        allTicksInRange = false
                        break
                    }
                }
            }
        }

        // Reset span count completion counter to properly account for judged nested objects.
        completedSpanCount = 0

        // Do not judge the slider end as it will be judged in onSpanFinish.
        for (i in 1 until nestedObjects.size - 1) {
            val nestedObject = nestedObjects[i]

            // Stop the process when a nested object that can't be hit before the current time is reached.
            if (nestedObject.startTime > currentTime) {
                break
            }

            val nestedPosition = nestedObject.screenSpaceGameplayStackedPosition
            tmpPoint.set(nestedPosition.x, nestedPosition.y)

            val isSliderTick = nestedObject is SliderTick
            val isHit = allTicksInRange || (replayObjectData != null && replayObjectData.tickSet!![replayTickIndex])
            val type = if (isSliderTick) GameObjectListener.SLIDER_TICK else GameObjectListener.SLIDER_REPEAT

            if (isHit) {
                if (!listener.isAfterSeek()) {
                    playCurrentNestedObjectHitSound()
                }
                ticksGot++
                tickSet.set(replayTickIndex++, true)
                listener.onSliderHit(id, if (isSliderTick) 10 else 30, tmpPoint, false, bodyColor, type, true)
            } else {
                tickSet.set(replayTickIndex++, false)
                listener.onSliderHit(id, -1, tmpPoint, false, bodyColor, type, false)
            }

            if (!isSliderTick) {
                // When a repeat is encountered, one span has passed.
                ++completedSpanCount
            }

            currentNestedObjectIndex++
        }

        // Recalculate elapsedSpanTime using the now-correct completedSpanCount so that
        // getGameplayPassedTimeMilliseconds() and percentage reflect the actual playback position
        // rather than an inflated value that would trigger premature span finishes or tick judgements.
        elapsedSpanTime = listener.elapsedTime - hitTime - completedSpanCount * spanDuration

        if (beatmapSlider.spanCount - completedSpanCount > 1) {
            // The head stays visible as the end of the next span, so it must not fade out for being missed.
            headCirclePiece.clearAnimations()

            // Change the head circle to the end circle piece.
            headCirclePiece.hideNumber()
            headCirclePiece.setCircleTextureRegion(sliderEndCircleTexture)
            headCirclePiece.setOverlayTextureRegion(sliderEndCircleOverlayTexture)

            startArrow.alpha = 1f
        } else if (headCirclePiece.isVisible) {
            headCirclePiece.clearAnimations()

            if (hitOffset >= -mehWindow || headCirclePiece.alpha == 0f) {
                headCirclePiece.removeSelf()
            } else {
                // Slider head is hit too early - slowly fade it.
                headCirclePiece.choreographNow { sequence ->
                    sequence.fadeOut(0.1f).after { e -> updateThread { e.removeSelf() } }
                    extendLifetime(sequence)
                }
            }
        }
    }

    private fun getLateHitThreshold(): Double {
        val mehWindow = hitWindow!!.mehWindow / 1000

        // In replays older than version 7, the slider head's hit window is capped to the slider's duration.
        if (replayObjectData != null && GameHelper.getReplayVersion() <= 7) {
            return min(mehWindow, duration)
        }

        return mehWindow
    }

    private fun getLateHitOffset(): Double {
        // Add 0.013s to maintain pre-version 1.8 behavior where the slider head is judged 13ms after the 50 hit window.
        // This also ensures that hit accuracy truncation in replay does not affect the late hit judgement.
        return (hitWindow!!.mehWindow + 13) / 1000.0
    }

    private fun getGameplayPassedTimeMilliseconds(): Double {
        return beatmapSlider!!.startTime + (completedSpanCount * spanDuration + elapsedSpanTime) * 1000
    }

    private fun judgeSliderTicks() {
        // Do not judge slider ticks until the slider head is hit.
        if (!startHit || tickContainer.childCount == 0) {
            return
        }

        val beatmapSlider = beatmapSlider!!
        val listener = listener!!
        val scale = beatmapSlider.screenSpaceGameplayScale

        val nestedObjects = beatmapSlider.nestedHitObjects
        var nestedObjectToJudge = nestedObjects[currentNestedObjectIndex]
        val currentTime = getGameplayPassedTimeMilliseconds()

        while (nestedObjectToJudge is SliderTick && currentTime >= nestedObjectToJudge.startTime) {
            val isTracking = isTracking()

            if (isTracking) {
                if (Settings[OsuSettings.AnimateFollowCircle] && followCircle.scaleX > 1f) {
                    followCircle.clearAnimations()

                    followCircle.scale = scale * 1.1f
                    followCircle.scaleTo(scale, 0.2f)
                }

                playCurrentNestedObjectHitSound()
                ticksGot++
                tickSet.set(replayTickIndex++, true)
            } else {
                tickSet.set(replayTickIndex++, false)
            }

            val tickPosition = nestedObjectToJudge.screenSpaceGameplayStackedPosition
            tmpPoint.set(tickPosition.x, tickPosition.y)

            listener.onSliderHit(id, if (isTracking) 10 else -1, tmpPoint, false, bodyColor, GameObjectListener.SLIDER_TICK, isTracking)
            currentNestedObjectIndex++

            val tickSprite = tickContainer[currentTickSpriteIndex] as SliderTickSprite
            tickSprite.onHit(isTracking)

            if (reverse && currentTickSpriteIndex > 0) {
                currentTickSpriteIndex--
            } else if (!reverse && currentTickSpriteIndex < tickContainer.childCount - 1) {
                currentTickSpriteIndex++
            }

            nestedObjectToJudge = nestedObjects[currentNestedObjectIndex]
        }
    }

    private fun reloadHitSounds() {
        val beatmapSlider = beatmapSlider!!
        val nestedObjects = beatmapSlider.nestedHitObjects
        nestedHitSamples.ensureCapacity(nestedObjects.size)

        for (i in 0 until nestedObjects.size) {
            val nestedObjectSamples = nestedObjects[i].samples
            val nestedObjectSampleCount = nestedObjectSamples.size

            var nestedHitSample = if (i < nestedHitSamples.size) nestedHitSamples[i] else null

            if (nestedHitSample != null) {
                nestedHitSample.ensureCapacity(nestedObjectSampleCount)
            } else {
                nestedHitSample = ArrayList(max(5, nestedObjectSampleCount))
                nestedHitSamples.add(nestedHitSample)
            }

            for (j in 0 until nestedObjectSampleCount) {
                val gameplaySample = GameplayHitSampleInfo.obtain()
                gameplaySample.init(nestedObjectSamples[j])

                if (GameHelper.isSamplesMatchPlaybackRate()) {
                    gameplaySample.frequency = GameHelper.getSpeedMultiplier()
                }

                nestedHitSample.add(gameplaySample)
            }
        }

        sliderSlideSample.reset()
        sliderWhistleSample.reset()

        val startTime = beatmapSlider.startTime.toFloat()

        for (i in 0 until beatmapSlider.auxiliarySamples.size) {
            if (sliderSlideSample.isInitialized && sliderWhistleSample.isInitialized) {
                break
            }

            val auxiliarySample = beatmapSlider.auxiliarySamples[i]
            val firstSample = auxiliarySample[0].second

            if (firstSample !is BankHitSampleInfo) {
                continue
            }

            if (firstSample.name == "sliderslide") {
                sliderSlideSample.init(startTime, auxiliarySample)
            } else if (firstSample.name == "sliderwhistle") {
                sliderWhistleSample.init(startTime, auxiliarySample)
            }
        }

        if (GameHelper.isSamplesMatchPlaybackRate()) {
            sliderSlideSample.frequency = GameHelper.getSpeedMultiplier()
            sliderWhistleSample.frequency = GameHelper.getSpeedMultiplier()
        }

        sliderSlideSample.isLooping = true
        sliderWhistleSample.isLooping = true

        updateSlidingSamplesVolume()
    }

    private fun playCurrentNestedObjectHitSound() {
        listener!!.playHitSamples(nestedHitSamples[currentNestedObjectIndex])
    }

    private fun applyDim(piece: UIComponent) {
        if (piece.parent == null || !piece.isVisible) {
            return
        }

        // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
        val colorDim = 195f / 255f

        piece.setColor(colorDim, colorDim, colorDim)
        piece.choreographAt(hitTime - HitWindow.MISS_WINDOW.toFloat() / 1000) { sequence -> sequence.colorTo(1f, 1f, 1f, 0.1f) }
    }

    override fun playLoopingSamples() {
        if (isInRadius) {
            playSlidingSamples()
        }
    }

    override fun stopLoopingSamples() {
        sliderSlideSample.stopAll()
        sliderWhistleSample.stopAll()
    }

    private fun playSlidingSamples() {
        sliderSlideSample.play()
        sliderWhistleSample.play()
    }

    private fun stopSlidingSamples() {
        sliderSlideSample.stop()
        sliderWhistleSample.stop()
    }

    private fun updateSlidingSamplesVolume() {
        val muted = GameHelper.getMuted()

        if (muted != null && muted.affectsHitSounds) {
            val volume = muted.volumeAt(stat!!.combo)

            sliderSlideSample.volume = volume
            sliderWhistleSample.volume = volume
        }
    }

    private fun getSynesthesiaComboColor(hitObject: HitObject): Color4 {
        return getSynesthesiaComboColor(hitObject.startTime)
    }

    private fun getSynesthesiaComboColor(time: Double): Color4 {
        return ModSynesthesia.getColorFor(controlPoints!!.getClosestBeatDivisor(time))
    }

    private fun applySnakeBodyLength(updateStartLength: Boolean, targetLength: Float, force: Boolean) {
        if (updateStartLength) {
            if (!force && !lastSnakeOutStartLength.isNaN() &&
                abs(targetLength - lastSnakeOutStartLength) < snakeLengthUpdateThreshold) {
                return
            }

            sliderBody.startLength = targetLength
            lastSnakeOutStartLength = targetLength
            return
        }

        val previous = if (lastSnakeInLength.isNaN()) lastSnakeOutEndLength else
            (if (lastSnakeOutEndLength.isNaN()) lastSnakeInLength else max(lastSnakeInLength, lastSnakeOutEndLength))

        if (!force && !previous.isNaN() && abs(targetLength - previous) < snakeLengthUpdateThreshold) {
            return
        }

        sliderBody.endLength = targetLength
        lastSnakeInLength = targetLength
        lastSnakeOutEndLength = targetLength
    }

    companion object {

        // The part of the whole width of the body that its shadow takes, as osu!lazer's LegacySliderBody.
        private const val SHADOW_PORTION = 5f / 64f

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

        private const val sliderStartCircleTexture = "sliderstartcircle"
        private const val sliderStartCircleOverlayTexture = "sliderstartcircleoverlay"
        private const val sliderEndCircleTexture = "sliderendcircle"
        private const val sliderEndCircleOverlayTexture = "sliderendcircleoverlay"

        // Avoid rebuilding slider body mesh for tiny length deltas every frame.
        private const val snakeLengthUpdateThreshold = 0.75f

        /**
         * Converts [radians] to degrees as `Math.toDegrees` does.
         */
        private fun toDegrees(radians: Float) = (radians.toDouble() * (180.0 / PI)).toFloat()
    }
}
