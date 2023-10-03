package com.rian.osu.beatmap.hitobject

import com.rian.osu.beatmap.hitobject.sliderobject.*
import com.rian.osu.beatmap.timings.DifficultyControlPoint
import com.rian.osu.beatmap.timings.TimingControlPoint
import com.rian.osu.math.Vector2
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a slider.
 */
open class Slider(
    /**
     * The time at which this slider starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of the slider relative to the play field.
     */
    position: Vector2,

    /**
     * The timing control point this slider is under effect on.
     */
    timingControlPoint: TimingControlPoint,

    /**
     * The difficulty control point this slider is under effect on.
     */
    difficultyControlPoint: DifficultyControlPoint,

    /**
     * The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     */
    repeatCount: Int,

    /**
     * The path of this slider.
     */
    path: SliderPath,

    /**
     * The slider velocity of the beatmap containing this slider.
     */
    sliderVelocity: Double,

    /**
     * The tick rate of the beatmap containing this slider.
     */
    tickRate: Double,

    /**
     * The multiplier for calculating slider ticks.
     */
    tickDistanceMultiplier: Double
) : HitObjectWithLength(startTime, startTime, position) {
    /**
     * The repetition amount of this slider.
     *
     * Note that 1 repetition means no repeats (1 loop).
     */
    var repeatCount = repeatCount
        private set

    /**
     * The path of this slider.
     */
    var path = path
        private set

    /**
     * The nested hit objects of the slider.
     *
     * Consists of head circle (slider head), slider ticks, repeat points, and tail circle (slider end).
     */
    var nestedHitObjects: ArrayList<SliderHitObject> = ArrayList()
        get() = Collections.unmodifiableList(field) as ArrayList
        private set

    /**
     * The velocity of this slider.
     */
    var velocity: Double
        private set

    /**
     * The head of the slider.
     */
    var head: SliderHead
        private set

    /**
     * The tail of the slider.
     */
    var tail: SliderTail
        private set

    /**
     * The position of the cursor at the point of completion of this slider if it was hit
     * with as few movements as possible. This is set and used by difficulty calculation.
     */
    var lazyEndPosition: Vector2? = null


    /**
     * The distance travelled by the cursor upon completion of this slider if it was hit
     * with as few movements as possible. This is set and used by difficulty calculation.
     */
    var lazyTravelDistance = 0f

    /**
     * The time taken by the cursor upon completion of this slider if it was hit with
     * as few movements as possible. This is set and used by difficulty calculation.
     */
    var lazyTravelTime = 0.0

    /**
     * The duration of one span of this slider.
     */
    var spanDuration: Double
        private set

    init {
        val scoringDistance = 100 * sliderVelocity * difficultyControlPoint.speedMultiplier
        velocity = scoringDistance / timingControlPoint.msPerBeat
        endTime = startTime + repeatCount * path.expectedDistance / velocity
        endPosition = position + path.positionAt((repeatCount % 2).toDouble())
        spanDuration = duration / repeatCount

        head = SliderHead(startTime, position)
        nestedHitObjects.add(head)

        // A very lenient maximum length of a slider for ticks to be generated.
        // This exists for edge cases such as /b/1573664 where the beatmap has been edited by the user, and should never be reached in normal usage.
        val maxLength = 100000.0
        val length = min(maxLength, path.expectedDistance)
        val tickDistance = (scoringDistance / tickRate * tickDistanceMultiplier).coerceIn(0.0, length)

        if (tickDistance != 0.0 && difficultyControlPoint.generateTicks) {
            val minDistanceFromEnd = velocity * 10

            for (span in 0 until repeatCount) {
                val spanStartTime = startTime + span * spanDuration
                val reversed = span % 2 == 1
                val sliderTicks: ArrayList<SliderTick> = ArrayList()

                var d = tickDistance
                while (d <= length) {
                    if (d >= length - minDistanceFromEnd) {
                        break
                    }

                    // Always generate ticks from the start of the path rather than the span to ensure
                    // that ticks in repeat spans are positioned identically to those in non-repeat spans
                    val distanceProgress = d / length
                    val timeProgress = if (reversed) 1 - distanceProgress else distanceProgress
                    val tickPosition = position + path.positionAt(distanceProgress)

                    sliderTicks.add(
                        SliderTick(
                            spanStartTime + timeProgress * spanDuration,
                            tickPosition,
                            span,
                            spanStartTime
                        )
                    )

                    d += tickDistance
                }

                // For repeat spans, ticks are returned in reverse-StartTime order.
                if (reversed) {
                    sliderTicks.reverse()
                }

                nestedHitObjects.addAll(sliderTicks)

                if (span < repeatCount - 1) {
                    val repeatPosition = position + path.positionAt(((span + 1) % 2).toDouble())

                    nestedHitObjects.add(
                        SliderRepeat(
                            spanStartTime + spanDuration,
                            repeatPosition,
                            span,
                            spanStartTime
                        )
                    )
                }
            }
        }

        // Okay, I'll level with you. I made a mistake. It was 2007.
        // Times were simpler. osu! was but in its infancy and sliders were a new concept.
        // A hack was made, which has unfortunately lived through until this day.
        //
        // This legacy tick is used for some calculations and judgements where audio output is not required.
        // Generally we are keeping this around just for difficulty compatibility.
        // Optimistically we do not want to ever use this for anything user-facing going forwards.
        // Temporarily set end time to start time. It will be evaluated later.
        val finalSpanIndex = repeatCount - 1
        val finalSpanStartTime = startTime + finalSpanIndex * spanDuration
        val finalSpanEndTime = max(
            startTime + duration / 2,
            finalSpanStartTime + spanDuration - LEGACY_LAST_TICK_OFFSET
        )

        tail = SliderTail(finalSpanEndTime, endPosition, finalSpanIndex, finalSpanStartTime)

        nestedHitObjects.apply {
            add(tail)
            sortBy { it.startTime }
        }
    }

    override var scale: Float = super.scale
        set(value) {
            field = value

            for (o in nestedHitObjects) {
                o.scale = value
            }
        }

    override fun clone() =
        (super.clone() as Slider).apply {
            path = this@Slider.path.clone()
            head = this@Slider.head.clone()
            tail = this@Slider.tail.clone()
            lazyEndPosition = this@Slider.lazyEndPosition?.copy()

            nestedHitObjects.apply {
                add(head)

                this@Slider.nestedHitObjects.forEachIndexed { index, obj ->
                    if (index == 0 || index == this@Slider.nestedHitObjects.size - 1) {
                        return@forEachIndexed
                    }

                    add(obj.clone())
                }

                add(tail)
            }
        }

    companion object {
        const val LEGACY_LAST_TICK_OFFSET = 36
    }
}
