package com.osudroid.game

import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.math.Interpolation
import com.osudroid.math.Precision
import com.osudroid.math.Vector2
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.PathApproximation
import com.reco1l.verktex.math.Path
import com.reco1l.verktex.settings.Settings
import kotlin.jvm.JvmField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * The path a [Slider]'s body is drawn along, in osu!pixels relative to the slider's position.
 *
 * Its points are those of the slider's calculated path, thinned out as osu!stable does.
 */
fun createSliderRenderPath(slider: Slider, scope: CoroutineScope?): Path {
    val calculatedPath = slider.path.calculatedPath
    val builder = Path.Builder()

    if (calculatedPath.isEmpty()) {
        return builder.build()
    }

    var distanceThreshold = 0

    if (!Settings[OsuSettings.SnakingOutSliders]) {
        // osu!stable optimizes gameplay path rendering by only including points that are 6 osu!pixels apart.
        // In linear paths, the distance threshold is further extended to 32 osu!pixels.
        // However, we exclude this optimization if snaking out sliders is enabled as slider velocity may not be
        // reliably computed with respect to slider ball travel velocity.
        distanceThreshold = if (slider.path.pathType == SliderPathType.Linear) 32 else 6
    }

    val distanceThresholdSquared = (distanceThreshold * distanceThreshold).toFloat()

    // Additional consideration for Catmull sliders that form "bulbs" around points with identical positions.
    val isCatmull = slider.path.pathType == SliderPathType.Catmull
    val catmullSegmentLength = PathApproximation.CATMULL_DETAIL * 2

    for (i in calculatedPath.indices) {
        if (scope != null) {
            scope.coroutineContext.ensureActive()
        }

        val x = calculatedPath[i].x
        val y = calculatedPath[i].y
        val last = builder.lastOrNull()

        if (last == null) {
            builder.add(x, y)
            continue
        }

        val dx = x - last.x
        val dy = y - last.y

        if (dx * dx + dy * dy > distanceThresholdSquared || i == calculatedPath.size - 1 ||
                (isCatmull && (i + 1) % catmullSegmentLength == 0)) {
            builder.add(x, y)
        }
    }

    // The render path may under-measure the true length of the slider due to the optimization.
    // Normally, the path would need to be extended to account for the true length of the slider.
    // However, in this case we simply let it be as the path is still rendered correctly (its points
    // are still in the correct positions).
    return builder.build()
}

/**
 * Converts the path of a [Slider] to one that can be used in gameplay, in osu!pixels as the playfield is.
 *
 * @return The converted [SliderPath].
 */
fun convertSliderPath(slider: Slider, scope: CoroutineScope?): SliderPath {
    val calculatedPath = slider.path.calculatedPath
    val cumulativeLength = slider.path.cumulativeLength
    val path = SliderPath(slider.path.pathType, calculatedPath.size)

    for (i in calculatedPath.indices) {
        if (scope != null) {
            scope.coroutineContext.ensureActive()
        }

        val p = calculatedPath[i]

        path.set(i, p.x, p.y, cumulativeLength[i].toFloat())
    }

    return path
}

/**
 * The path of a slider as gameplay follows it: its points, in osu!pixels relative to the slider's position, and
 * their distance along it.
 */
class SliderPath(@JvmField val pathType: SliderPathType, anchorPointCount: Int) {
    private val data = FloatArray(anchorPointCount * strip)
    @JvmField
    var anchorCount = 0

    fun set(index: Int, x: Float, y: Float, length: Float) {
        data[index * strip + offsetX] = x
        data[index * strip + offsetY] = y
        data[index * strip + offsetLength] = length
        anchorCount++
    }

    fun getX(index: Int): Float {
        return getData(index, offsetX)
    }

    fun getY(index: Int): Float {
        return getData(index, offsetY)
    }

    fun getLength(index: Int): Float {
        return getData(index, offsetLength)
    }

    fun positionAt(out: Vector2, percentage: Float): Int {
        if (anchorCount == 0) {
            out.set(0f, 0f)
            return 0
        }

        val distance = percentage * getLength(anchorCount - 1)
        val index = getIndexOfDistance(distance)

        interpolate(out, index, distance)

        return index
    }

    private fun interpolate(out: Vector2, index: Int, distance: Float) {
        if (index <= 0) {
            out.set(getX(0), getY(0))
            return
        }

        if (index >= anchorCount) {
            out.set(getX(anchorCount - 1), getY(anchorCount - 1))
            return
        }

        val d0 = getLength(index - 1)
        val d1 = getLength(index)

        if (Precision.almostEquals(d0, d1)) {
            out.set(getX(index - 1), getY(index - 1))
            return
        }

        val t = Interpolation.reverseLinear(distance, d0, d1)

        out.x = Interpolation.linear(getX(index - 1), getX(index), t)
        out.y = Interpolation.linear(getY(index - 1), getY(index), t)
    }

    private fun getIndexOfDistance(d: Float): Int {
        if (anchorCount == 0 || d < getLength(0)) {
            return 0
        }

        if (d >= getLength(anchorCount - 1)) {
            return anchorCount
        }

        var l = 0
        var r = anchorCount - 2

        while (l <= r) {
            val pivot = l + ((r - l) shr 1)
            val length = getLength(pivot)

            if (length < d) {
                l = pivot + 1
            } else if (length > d) {
                r = pivot - 1
            } else {
                return pivot
            }
        }

        return l
    }

    private fun getData(index: Int, offset: Int): Float {
        if (data.size == 0) {
            return 0f
        }

        val idx = index * strip + offset

        if (idx < 0) {
            return 0f
        }

        if (idx >= data.size) {
            return data[data.size - strip + offset]
        }

        return data[idx]
    }

    companion object {
        private const val strip = 3
        private const val offsetX = 0
        private const val offsetY = 1
        private const val offsetLength = 2
    }
}
