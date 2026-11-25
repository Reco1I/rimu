package com.osudroid.game.objects

import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.ui.v2.game.FollowPointConnection
import com.osudroid.ui.v2.skin.JudgementPiece
import kotlin.math.max

/**
 * The most objects of each kind a play has alive at once, for the pools to hold them before it starts: building one
 * while it plays stalls the frame.
 *
 * @param activeObjects The hit objects of any kind.
 * @param sliderTicks The ticks of the sliders, those of one span each, which a slider shows at once.
 */
data class PoolSizes(
    val activeObjects: Int,
    val circles: Int,
    val sliders: Int,
    val spinners: Int,
    val sliderTicks: Int,
    val followPoints: Int
) {
    companion object {

        /**
         * The sizes for the [objects] of a play, in the order they start.
         */
        fun of(objects: Array<HitObject>): PoolSizes {
            val all = Overlap()
            val circles = Overlap()
            val sliders = Overlap()
            val spinners = Overlap()
            val ticks = Overlap()
            val followPoints = Overlap()

            for (i in objects.indices) {
                val obj = objects[i]
                val start = obj.startTime - obj.timePreempt

                // Objects live until the effects of their judgement end.
                val end = when (obj) {
                    is HitCircle -> obj.startTime + (obj.hitWindow?.mehWindow ?: 0.0)
                    is Slider -> max(obj.startTime + (obj.head.hitWindow?.mehWindow ?: 0.0), obj.endTime)
                    else -> obj.endTime
                } + JudgementPiece.DURATION * 1000

                all.add(start, end)

                when (obj) {
                    is HitCircle -> circles.add(start, end)
                    is Spinner -> spinners.add(start, end)
                    is Slider -> {
                        sliders.add(start, end)
                        ticks.add(start, end, firstSpanTickCount(obj))
                    }
                }

                if (i + 1 < objects.size) {
                    val next = objects[i + 1]

                    if (FollowPointConnection.connects(obj, next)) {
                        // They're all added as the first starts to fade in, and live until the last fades out.
                        followPoints.add(
                            FollowPointConnection.fadeInStartTime(obj) * 1000.0,
                            next.startTime + FollowPointConnection.FADE_DURATION * 1000.0,
                            FollowPointConnection.pointCount(obj, next)
                        )
                    }
                }
            }

            return PoolSizes(
                activeObjects = all.maximum(),
                circles = circles.maximum(),
                sliders = sliders.maximum(),
                spinners = spinners.maximum(),
                sliderTicks = ticks.maximum(),
                followPoints = followPoints.maximum()
            )
        }

        // The ticks a slider shows at once, see SliderTickContainer.init.
        private fun firstSpanTickCount(slider: Slider): Int {
            var count = 0
            for (i in 1 until slider.nestedHitObjects.size) {
                if (slider.nestedHitObjects[i] !is SliderTick) break
                count++
            }
            return count
        }
    }

    // The most of what's added that's alive at once, from the times each starts and ends at.
    private class Overlap {
        private val starts = ArrayList<Double>()
        private val ends = ArrayList<Double>()
        private val counts = ArrayList<Int>()

        fun add(start: Double, end: Double, count: Int = 1) {
            if (count <= 0) return
            starts += start
            ends += end
            counts += count
        }

        fun maximum(): Int {
            val events = ArrayList<Pair<Double, Int>>(starts.size * 2)
            for (i in starts.indices) {
                events += starts[i] to counts[i]
                events += ends[i] to -counts[i]
            }

            // What ends at a time is gone before what starts then.
            events.sortWith(compareBy<Pair<Double, Int>> { it.first }.thenBy { it.second })

            var alive = 0
            var maximum = 0
            for ((_, delta) in events) {
                alive += delta
                maximum = max(maximum, alive)
            }
            return maximum
        }
    }
}
