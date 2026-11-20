package com.osudroid.game.session

import com.osudroid.beatmaps.timings.EffectControlPoint
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.game.GameplayTiming
import kotlin.math.max

/**
 * Follows the control points of a beatmap as its play goes on, and the beat it's in, which [timing] has.
 *
 * @param defaultTimingPoint The timing point of a beatmap that has none.
 * @param defaultEffectPoint The effect point of a beatmap that has none.
 */
class ControlPointTracker(
    private val timingPoints: Array<TimingControlPoint>,
    private val effectPoints: Array<EffectControlPoint>,
    private val defaultTimingPoint: TimingControlPoint,
    private val defaultEffectPoint: EffectControlPoint,
    private val timing: GameplayTiming
) {
    private var timingIndex = 0
    private var effectIndex = 0

    /**
     * The timing point the play is in.
     */
    var timingPoint = timingPoints.firstOrNull() ?: defaultTimingPoint
        private set

    /**
     * The effect point the play is in.
     */
    var effectPoint = effectPoints.firstOrNull() ?: defaultEffectPoint
        private set


    init {
        timing.beatLength = timingPoint.msPerBeat / 1000
        timing.isKiai = effectPoint.isKiai
        timing.currentBeatTime = 0.0
    }


    /**
     * Moves forward to the control points at [time], in seconds, and to its beat.
     */
    fun update(time: Float) {
        val timeMs = time * 1000

        while (timingIndex + 1 < timingPoints.size) {
            val nextTimingPoint = timingPoints[timingIndex + 1]

            if (nextTimingPoint.time > timeMs) {
                break
            }

            timingPoint = nextTimingPoint
            ++timingIndex
        }

        while (effectIndex + 1 < effectPoints.size) {
            val nextEffectPoint = effectPoints[effectIndex + 1]

            if (nextEffectPoint.time > timeMs) {
                break
            }

            effectPoint = nextEffectPoint
            ++effectIndex
        }

        timing.beatLength = timingPoint.msPerBeat / 1000
        timing.isKiai = effectPoint.isKiai
        timing.currentBeatTime = max(0.0, time - timingPoint.time / 1000) % timing.beatLength
    }

    /**
     * Moves to the control points at [timeMs], in milliseconds, earlier or later, as a seek does.
     */
    fun seekTo(timeMs: Float) {
        timingIndex = 0
        effectIndex = 0

        while (timingIndex + 1 < timingPoints.size && timingPoints[timingIndex + 1].time <= timeMs) {
            timingIndex++
        }

        while (effectIndex + 1 < effectPoints.size && effectPoints[effectIndex + 1].time <= timeMs) {
            effectIndex++
        }

        timingPoint = if (timingPoints.isNotEmpty()) timingPoints[timingIndex] else defaultTimingPoint
        effectPoint = if (effectPoints.isNotEmpty()) effectPoints[effectIndex] else defaultEffectPoint
    }
}
