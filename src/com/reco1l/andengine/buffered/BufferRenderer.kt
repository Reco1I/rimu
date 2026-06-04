package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.TransformationStack
import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.transform
import com.reco1l.framework.Color4


/**
 * A helper class that manages a resizable FloatBuffer for rendering. It automatically resizes the
 * buffer when the capacity is exceeded, based on the specified reallocation step.
 */
open class BufferRenderer {

    fun addVertex(x: Float, y: Float, u: Float = 0f, v: Float = 0f) {

        val position = TransformationStack.peek().transform(x, y)
        val color = ColorStack.peek() ?: Color4.White

        UIRenderer.buffer.addVertex(
            x = position[0],
            y = position[1],
            color = color,
            u = u,
            v = v
        )
    }


    fun addTriangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ) {
        addVertex(x1, y1)
        addVertex(x2, y2)
        addVertex(x3, y3)
    }

    fun addLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ) {
        addVertex(x1, y1)
        addVertex(x2, y2)
    }
}
