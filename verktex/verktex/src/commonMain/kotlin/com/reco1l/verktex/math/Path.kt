package com.reco1l.verktex.math

import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * A polyline: straight lines between points, with the length along it up to each point.
 *
 * The points are packed as x, y pairs. A path never changes, [cut] and [simplified] make new ones.
 */
class Path private constructor(private val coordinates: FloatArray, val pointCount: Int) {

    /**
     * The length along the path up to each point.
     */
    private val lengths = FloatArray(pointCount)

    init {
        var length = 0f
        for (i in 1 until pointCount) {
            length += hypot(x(i) - x(i - 1), y(i) - y(i - 1))
            lengths[i] = length
        }
    }


    /**
     * The length of the path.
     */
    val length: Float
        get() = if (pointCount == 0) 0f else lengths[pointCount - 1]

    /**
     * The smallest rectangle holding every point, as left, top, right and bottom. Zero without
     * points.
     */
    val bounds: Vec4 by lazy {
        if (pointCount == 0) {
            return@lazy Vec4(0f, 0f, 0f, 0f)
        }

        var left = x(0)
        var top = y(0)
        var right = left
        var bottom = top

        for (i in 1 until pointCount) {
            left = min(left, x(i))
            top = min(top, y(i))
            right = max(right, x(i))
            bottom = max(bottom, y(i))
        }

        Vec4(left, top, right, bottom)
    }


    fun x(index: Int) = coordinates[index * 2]

    fun y(index: Int) = coordinates[index * 2 + 1]

    operator fun get(index: Int) = Vec2(x(index), y(index))

    /**
     * The length along the path up to the point at [index].
     */
    fun lengthAt(index: Int) = lengths[index]

    /**
     * The index of the last point at or before [distance] along the path, or of the last point if
     * [distance] is past its end.
     */
    fun indexAt(distance: Float): Int {
        if (pointCount == 0) {
            return -1
        }

        if (distance >= length) {
            return pointCount - 1
        }

        var start = 0
        var end = pointCount - 1
        while (end - start > 1) {
            val middle = (start + end) / 2
            if (lengths[middle] <= distance) {
                start = middle
            } else {
                end = middle
            }
        }
        return start
    }

    /**
     * The point at [distance] along the path. Past its end, the path continues in the direction of
     * its last line.
     */
    fun pointAt(distance: Float): Vec2 {
        require(pointCount > 0) { "The path has no points." }

        if (distance >= length) {
            val last = pointCount - 1
            if (pointCount < 2) {
                return get(last)
            }

            val lineLength = lengths[last] - lengths[last - 1]
            if (lineLength <= EPSILON) {
                return get(last)
            }

            val extra = distance - length
            return Vec2(
                x(last) + (x(last) - x(last - 1)) / lineLength * extra,
                y(last) + (y(last) - y(last - 1)) / lineLength * extra
            )
        }

        val index = indexAt(distance)
        val lineLength = lengths[index + 1] - lengths[index]

        if (lineLength <= EPSILON) {
            return get(index)
        }

        val progress = (distance - lengths[index]) / lineLength
        return Vec2(
            x(index) + (x(index + 1) - x(index)) * progress,
            y(index) + (y(index + 1) - y(index)) * progress
        )
    }

    /**
     * The part of this path between [start] and [end] along it, without points closer than 0.01 to
     * the previous one.
     */
    fun cut(start: Float, end: Float): Path {
        val builder = Builder()

        fun add(point: Vec2) = builder.addIfFartherThan(point.x, point.y, 0.01f)

        add(pointAt(start))
        for (i in indexAt(start) + 1..indexAt(end)) {
            builder.addIfFartherThan(x(i), y(i), 0.01f)
        }
        add(pointAt(end))

        return builder.build()
    }

    /**
     * This path without the points closer than [minDistance] to the previous point kept. The first
     * and the last points are always kept.
     */
    fun simplified(minDistance: Float): Path {
        val builder = Builder()

        for (i in 0 until pointCount) {
            if (i == 0 || i == pointCount - 1) {
                builder.add(x(i), y(i))
            } else {
                builder.addIfFartherThan(x(i), y(i), minDistance)
            }
        }

        return builder.build()
    }


    /**
     * Makes a [Path] from points added in order.
     */
    class Builder {

        private var coordinates = FloatArray(32)
        private var count = 0

        /**
         * The amount of points added so far.
         */
        val pointCount
            get() = count

        fun add(x: Float, y: Float): Builder {
            if (count * 2 + 2 > coordinates.size) {
                coordinates = coordinates.copyOf(coordinates.size * 2)
            }

            coordinates[count * 2] = x
            coordinates[count * 2 + 1] = y
            count++
            return this
        }

        fun add(point: Vec2) = add(point.x, point.y)

        /**
         * Adds the point unless it's [distance] or closer to the last one added.
         */
        fun addIfFartherThan(x: Float, y: Float, distance: Float): Builder {
            if (count > 0) {
                val dx = x - coordinates[count * 2 - 2]
                val dy = y - coordinates[count * 2 - 1]

                if (dx * dx + dy * dy <= distance * distance) {
                    return this
                }
            }
            return add(x, y)
        }

        /**
         * The last point added, null without points.
         */
        fun lastOrNull() = if (count == 0) null else Vec2(coordinates[count * 2 - 2], coordinates[count * 2 - 1])

        fun build() = Path(coordinates.copyOf(count * 2), count)
    }


    companion object {
        private const val EPSILON = 0.0000001f

        val Empty = Path(FloatArray(0), 0)

        /**
         * A path through [points], given as x, y pairs.
         */
        fun of(vararg points: Float): Path {
            require(points.size % 2 == 0) { "Points are x, y pairs, got ${points.size} numbers." }
            return Path(points.copyOf(), points.size / 2)
        }
    }
}
