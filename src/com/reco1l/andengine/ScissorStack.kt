package com.reco1l.andengine

import com.reco1l.framework.math.*
import kotlin.math.*

object ScissorStack {

    private val deque = ArrayDeque<Vec4>()
    private val pool = ArrayDeque<Vec4>(32)


    fun push(x: Float, y: Float, width: Float, height: Float) {

        val intersectedX: Float
        val intersectedY: Float
        val intersectedWidth: Float
        val intersectedHeight: Float

        if (deque.isEmpty()) {
            intersectedX = x
            intersectedY = y
            intersectedWidth = width
            intersectedHeight = height
        } else {
            val current = deque.last()

            val minX = max(current.x, x)
            val minY = max(current.y, y)
            val maxX = min(current.x + current.z, x + width)
            val maxY = min(current.y + current.w, y + height)

            intersectedX = minX
            intersectedY = minY
            intersectedWidth = (maxX - minX)
            intersectedHeight = (maxY - minY)
        }

        val vec4 = pool.removeLastOrNull()
            ?.takeUnless { vec -> vec.x != intersectedX || vec.y != intersectedY || vec.z != intersectedWidth || vec.w != intersectedHeight }
            ?: Vec4(intersectedX, intersectedY, intersectedWidth, intersectedHeight)

        deque.addLast(vec4)
    }

    fun pop(): Vec4? {
        if (deque.isEmpty()) {
            return null
        }

        val vec = deque.removeLast()
        pool.addLast(vec)
        return vec
    }

    fun peek(): Vec4? = deque.lastOrNull()


    private fun readResolve(): Any = ScissorStack

}
