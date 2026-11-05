package com.osudroid.math

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A path made of straight lines between points, as the slider bodies are drawn.
 *
 * Its points are kept as they're added, so changing them changes the path. Call [measure] after adding
 * them to be able to know the [length] of the path and [cut] it.
 */
class LinePath {

    private val points = ArrayList<Vector2>()

    /**
     * The length of the path up to each point, see [measure].
     */
    private var lengths = FloatArray(0)


    /**
     * The amount of points.
     */
    fun size() = points.size

    /**
     * Gets the point at [index].
     */
    operator fun get(index: Int) = points[index]

    /**
     * Gets the last point.
     */
    fun getLast() = points[points.size - 1]

    /**
     * Adds a point at the end of the path.
     */
    fun add(point: Vector2) {
        points.add(point)
    }

    /**
     * Removes all points.
     */
    fun clear() {
        points.clear()
        lengths = FloatArray(0)
    }

    /**
     * Measures the length of the path up to each of its points.
     */
    fun measure() {
        if (lengths.size != points.size) {
            lengths = FloatArray(points.size)
        }

        var length = 0f
        for (i in 1 until points.size) {
            length += distance(points[i - 1], points[i])
            lengths[i] = length
        }
    }

    /**
     * The length of the path, as [measure] found it.
     */
    fun getLength() = if (lengths.isEmpty()) 0f else lengths[lengths.size - 1]

    /**
     * The point at [length] along the path. Past its end, the path continues in the direction of its last
     * line.
     */
    fun pointAt(length: Float): Vector2 {
        val totalLength = getLength()

        if (length >= totalLength) {
            val last = getLast()
            if (points.size < 2) {
                return Vector2(last.x, last.y)
            }

            val previous = points[points.size - 2]
            val lineLength = distance(previous, last)
            val extra = length - totalLength

            return Vector2(
                last.x + (last.x - previous.x) / lineLength * extra,
                last.y + (last.y - previous.y) / lineLength * extra
            )
        }

        val index = indexAt(length)
        val start = points[index]
        val end = points[index + 1]
        val lineLength = distance(start, end)

        if (lineLength <= 0.0000001f) {
            return Vector2(start.x, start.y)
        }

        val progress = (length - lengths[index]) / lineLength
        return Vector2(start.x * (1 - progress) + end.x * progress, start.y * (1 - progress) + end.y * progress)
    }

    /**
     * The index of the last point at or before [length] along the path, or of the last point if [length] is
     * past its end.
     */
    fun indexAt(length: Float): Int {
        if (length >= getLength()) {
            return lengths.size - 1
        }

        var start = 0
        var end = lengths.size - 1
        while (end - start > 1) {
            val middle = (start + end) / 2
            if (lengths[middle] <= length) {
                start = middle
            } else {
                end = middle
            }
        }
        return start
    }

    /**
     * Creates the part of this path between [start] and [end] along it, without points closer than 0.01 to
     * the previous one.
     */
    fun cut(start: Float, end: Float): LinePath {
        val startIndex = indexAt(start) + 1
        val endIndex = indexAt(end)

        val result = LinePath()
        var previous: Vector2? = null

        fun addPoint(point: Vector2) {
            val copy = Vector2(point.x, point.y)
            if (previous == null || distance(previous!!, copy) > 0.01f) {
                result.add(copy)
                previous = copy
            }
        }

        addPoint(pointAt(start))
        for (i in startIndex..endIndex) {
            addPoint(points[i])
        }
        addPoint(pointAt(end))

        return result
    }


    companion object {

        /**
         * The distance between two points.
         */
        @kotlin.jvm.JvmStatic
        fun distance(a: Vector2, b: Vector2) = hypot(a.x - b.x, a.y - b.y)

        /**
         * The angle of the line from [start] to [end], in radians.
         */
        @kotlin.jvm.JvmStatic
        fun angle(start: Vector2, end: Vector2) = atan2(end.y - start.y, end.x - start.x)

        /**
         * Sets [out] to the point at [angle] radians on the unit circle.
         */
        @kotlin.jvm.JvmStatic
        fun pointOnCircle(angle: Float, out: Vector2) = out.set(cos(angle), sin(angle))

        /**
         * Sets [out] to the unit vector perpendicular to the line from [start] to [end], rotated
         * counterclockwise from it.
         */
        @kotlin.jvm.JvmStatic
        fun perpendicular(start: Vector2, end: Vector2, out: Vector2): Vector2 {
            val x = end.x - start.x
            val y = end.y - start.y
            val length = hypot(x, y)
            return out.set(-y / length, x / length)
        }
    }
}
