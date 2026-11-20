package com.osudroid.game.session

import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderRepeat
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.beatmaps.timings.BreakPeriod
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.game.rules.HealthProcessor
import com.osudroid.game.rules.JudgementResult
import com.osudroid.game.rules.ScoreProcessor
import com.osudroid.game.rules.SliderPart
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Rebuilds the statistic of a play at a time, as a seek does: from the results a replay recorded for each object,
 * or autoplay's, through the same [ScoreProcessor] and [HealthProcessor] the play uses.
 *
 * @param overallDifficulty The OD of the beatmap, which sets the rotations a spinner needs.
 * @param replayVersion The version of the replay, which changed how slider heads are judged.
 */
class ScoreReconstruction(
    private val objects: Array<HitObject>,
    private val timingPoints: Array<TimingControlPoint>,
    private val breaks: Array<BreakPeriod>,
    private val hitWindow: HitWindow,
    private val overallDifficulty: Float,
    private val replayVersion: Int,
    private val stat: StatisticV2,
    private val scoreProcessor: ScoreProcessor,
    private val healthProcessor: HealthProcessor
) {

    /**
     * What a reconstruction reached.
     *
     * @param objectCount The objects judged, the first ones of the beatmap.
     * @param isStarted Whether the play had started, an object reached.
     */
    class Result(val objectCount: Int, val isStarted: Boolean)

    /**
     * Applies the judgements of the objects judged by [targetMs], in milliseconds, and the health drained until then,
     * to the statistic, which is reset first.
     *
     * @param objectData The results a replay recorded for each object, null for autoplay's, which hits every object.
     */
    fun reconstruct(targetMs: Float, objectData: Array<Replay.ReplayObjectData?>?): Result {
        stat.reset()

        var objectCount = 0
        var isStarted = false

        var localTimingIdx = 0
        var localBreakIdx = 0

        // How far continuous HP drain has been applied, and which object-to-object rate segment
        // it's currently within ([objects[rateSegIdx].startTime - timePreempt,
        // objects[rateSegIdx + 1].startTime - timePreempt)). Both advance with elapsed
        // time and is independent of which object's own judgement has been applied yet.
        var drainCursorMs = objects[0].startTime - objects[0].timePreempt
        var rateSegIdx = 0

        for (i in objects.indices) {
            val obj = objects[i]

            val judgementTimeMs = getJudgementTimeMs(i, obj, objectData)

            if (judgementTimeMs > targetMs) {
                break
            }

            val drainTargetMs = min(judgementTimeMs, targetMs.toDouble())

            while (drainCursorMs < drainTargetMs && rateSegIdx < objects.size - 1) {
                val segFromObj = objects[rateSegIdx]
                val segToObj = objects[rateSegIdx + 1]

                // Advance local timing point index to this segment's own "to" object.
                while (localTimingIdx + 1 < timingPoints.size && timingPoints[localTimingIdx + 1].time <= segToObj.startTime) {
                    localTimingIdx++
                }

                val msPerBeat = timingPoints[localTimingIdx].msPerBeat
                val distToNextObject = max(segToObj.startTime - segFromObj.startTime, msPerBeat / 2) / 1000

                val segEndMs = segToObj.startTime - segToObj.timePreempt
                val stopAtMs = min(segEndMs, drainTargetMs)

                if (stopAtMs > drainCursorMs) {
                    // Advance past breaks that fully precede the current segment.
                    while (localBreakIdx < breaks.size && breaks[localBreakIdx].endTime <= drainCursorMs) {
                        localBreakIdx++
                    }

                    val effectiveSecs = HealthProcessor.drainingSeconds(drainCursorMs, stopAtMs, breaks, localBreakIdx)

                    if (healthProcessor.drainSegment(effectiveSecs, distToNextObject)) {
                        return Result(objectCount, isStarted)
                    }

                    drainCursorMs = stopAtMs
                }

                if (drainCursorMs >= segEndMs) {
                    rateSegIdx++
                } else {
                    // Capped short of this segment's own end by drainTargetMs - stop crossing
                    // further for now. The remainder of this same segment continues once a
                    // later object's own judgement time lets drain proceed past it.
                    break
                }
            }

            isStarted = true

            val data = if (objectData != null && i < objectData.size) objectData[i] else null

            val endCombo = obj.isLastInCombo

            if (obj is HitCircle) {
                applyCircleResult(data, endCombo)
            } else if (obj is Slider) {
                applySliderResult(obj, data, endCombo)
            } else if (obj is Spinner) {
                applySpinnerResult(obj, data, endCombo)
            }

            objectCount = i + 1
        }

        return Result(objectCount, isStarted)
    }

    private fun getJudgementTimeMs(idx: Int, obj: HitObject, objectData: Array<Replay.ReplayObjectData?>?): Double {
        if (obj is Slider || obj is Spinner) {
            return obj.endTime
        }

        // Circle: judged when hit (or miss window expires).
        if (objectData != null && idx < objectData.size) {
            val data = objectData[idx]

            if (data != null && data.result != ResultType.MISS.id) {
                return obj.startTime + abs(data.accuracy.toInt())
            }
        }

        val mehWindow = obj.hitWindow?.mehWindow ?: HitWindow.MISS_WINDOW

        return obj.startTime + mehWindow
    }

    private fun reconstructHitOffset(accSeconds: Double) {
        if (abs(accSeconds) <= hitWindow.mehWindow / 1000) {
            stat.addHitOffset(accSeconds)
        }
    }

    private fun applyCircleResult(data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val result = JudgementResult.fromResultType(data?.result ?: ResultType.HIT300.id)

        if (result != JudgementResult.Miss) {
            reconstructHitOffset(if (data != null) data.accuracy / 1000.0 else 0.0)
        }

        scoreProcessor.applyObject(result, endCombo)
    }

    private fun applySliderResult(slider: Slider, data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val result = JudgementResult.fromResultType(data?.result ?: ResultType.HIT300.id)

        if (result == JudgementResult.Miss) {
            scoreProcessor.applyObject(result, endCombo)
            return
        }

        // Slider head: HIT300 implies all ticks were hit; autoplay (data==null) always hits.
        // For HIT50/HIT100, reconstruct whether the head was actually within the hit window,
        // mirroring GameplaySlider.onSliderHeadHit.
        val accSeconds = if (data != null) data.accuracy / 1000.0 else 0.0
        val headHit: Boolean

        if (data == null || result == JudgementResult.Great) {
            headHit = true
        } else {
            val mehWindowSecs = hitWindow.mehWindow / 1000.0
            val sliderDurationSecs = slider.duration / 1000.0
            val lateHitThreshold = if (replayVersion <= 7) min(mehWindowSecs, sliderDurationSecs) else mehWindowSecs

            if (replayVersion >= 6 || mehWindowSecs <= sliderDurationSecs) {
                headHit = -mehWindowSecs <= accSeconds && accSeconds <= lateHitThreshold
            } else {
                headHit = accSeconds <= sliderDurationSecs
            }
        }

        if (headHit) {
            reconstructHitOffset(accSeconds)
            scoreProcessor.applySliderPart(SliderPart.Head)
        }

        // Ticks and repeats from tickSet.
        val nested = slider.nestedHitObjects

        // Skip head (index 0) and tail (last index).
        for (i in 1 until nested.size - 1) {
            val wasHit = data == null || (data.tickSet != null && data.tickSet!![i - 1])

            if (!wasHit) {
                scoreProcessor.applySliderPartMiss()
                continue
            }

            val nestedObj = nested[i]

            if (nestedObj is SliderTick) {
                scoreProcessor.applySliderPart(SliderPart.Tick)
            } else if (nestedObj is SliderRepeat) {
                scoreProcessor.applySliderPart(SliderPart.Repeat)
            }
        }

        // Slider tail: combo is only awarded when the player was tracking at the endpoint.
        // Unlike circles, the result score reflects ticks hit, not timing accuracy.
        val tailTracked = data == null || (data.tickSet != null && data.tickSet!![nested.size - 2])

        scoreProcessor.applySliderTail(result, endCombo, tailTracked)
    }

    private fun applySpinnerResult(spinner: Spinner, data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val duration = spinner.duration.toFloat() / 1000
        var needRotations = (2 + 2 * overallDifficulty / 10f) * duration

        if (duration < 0.05f) {
            needRotations = 0.1f
        }

        val preClear: Int
        val bonus: Int

        if (data != null) {
            // data.accuracy = totalSpins * 4 + resultCode, where totalSpins = fullRotations (pre-clear, 100 pts each)
            // + (bonusScoreCounter - 1) (bonus, 1000 pts each). Split by needRotations to award the correct amounts.
            val totalSpins = (data.accuracy.toInt() and 0xFFFF) shr 2
            preClear = min(totalSpins, ceil(needRotations.toDouble()).toInt() - 1)
            bonus = totalSpins - preClear
        } else {
            // Autoplay always clears the spinner. Reconstruct the pre-clear (100 pts each) and bonus (1000 pts each)
            // rotation split from the spinner's parameters.
            // ceil(needRotations) - 1 rotations are pre-clear; bonus rotations begin at ceil(needRotations) total.
            val totalRotations = 5f * duration

            preClear = ceil(needRotations.toDouble()).toInt() - 1
            bonus = max(0, totalRotations.toInt() - ceil(needRotations.toDouble()).toInt() + 1)
        }

        for (s in 0 until preClear) {
            scoreProcessor.applySpinnerRotation()
        }

        for (s in 0 until bonus) {
            scoreProcessor.applySpinnerBonus()
        }

        applyCircleResult(data, endCombo)
    }
}
