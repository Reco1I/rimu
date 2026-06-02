package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.POSITION_OFFSET
import com.reco1l.andengine.TransformationStack
import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.VERTEX_STRIDE
import com.reco1l.andengine.invoke
import com.reco1l.framework.Color4


/**
 * A helper class that manages a resizable FloatBuffer for rendering. It automatically resizes the
 * buffer when the capacity is exceeded, based on the specified reallocation step.
 */
open class BufferRenderer {

    fun addVertex(x: Float, y: Float, u: Float = 0f, v: Float = 0f) {

        val currentState = UIRenderer.state
        currentState.ensureBufferCapacity()

        val transform = TransformationStack.peek()
        val color = ColorStack.peek()

        currentState.buffer.apply {

            if (transform != null) {
                val output = transform(x, y)
                put(output[0])
                put(output[1])
            } else {
                put(x)
                put(y)
            }

            put(color?.red ?: 1f)
            put(color?.green ?: 1f)
            put(color?.blue ?: 1f)
            put(color?.alpha ?: 1f)
            put(u)
            put(v)
        }
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
}
