package com.reco1l.andengine

import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import java.util.Stack

object ColorStack {

    private val deque = ArrayDeque<Color4>()


    fun push(color: Color4, inheritAncestors: Boolean = true) {

        var red = color.red
        var green = color.green
        var blue = color.blue
        var alpha = color.alpha

        if (!deque.isEmpty()) {
            val previous = deque.last()
            if (inheritAncestors) {
                red *= previous.red
                green *= previous.green
                blue *= previous.blue
            }
            alpha *= previous.alpha
        }

        val color = Color4(
            red.coerceIn(0f, 1f),
            green.coerceIn(0f, 1f),
            blue.coerceIn(0f, 1f),
            alpha.coerceIn(0f, 1f)
        )

        deque.addLast(color)
    }

    fun pop(): Color4? {
        if (deque.isEmpty()) {
            return null
        }
        return deque.removeLast()
    }

    fun peek(): Color4? {
        return deque.lastOrNull()
    }


    private fun readResolve(): Any = ColorStack

}
