package com.edlplan.andengine

import com.osudroid.math.Vector2
import com.osudroid.utils.FloatArraySlice
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

class TriangleBuilder @JvmOverloads constructor(size: Int = 1) : FloatArraySlice() {

    @JvmField
    var maxX = 0f

    @JvmField
    var maxY = 0f

    init {
        ary = FloatArray(6 * size)
    }

    fun add(p1: Vector2, p2: Vector2, p3: Vector2) {
        if (length + 6 > ary.size) {
            ary = ary.copyOf(ary.size * 3 / 2 + 6)
        }
        ary[length++] = p1.x
        ary[length++] = p1.y
        ary[length++] = p2.x
        ary[length++] = p2.y
        ary[length++] = p3.x
        ary[length++] = p3.y

        if (p1.x > maxX) maxX = p1.x
        if (p2.x > maxX) maxX = p2.x
        if (p3.x > maxX) maxX = p3.x

        if (p1.y > maxY) maxY = p1.y
        if (p2.y > maxY) maxY = p2.y
        if (p3.y > maxY) maxY = p3.y
    }

    fun applyVertices(slice: FloatArraySlice) {
        slice.offset = 0
        if (slice.ary.size < length) {
            slice.ary = FloatArray(length)
        }
        slice.length = length
        ary.copyInto(slice.ary, 0, 0, length)
    }

    fun reset() {
        length = 0
        maxX = 0f
        maxY = 0f
    }
}
