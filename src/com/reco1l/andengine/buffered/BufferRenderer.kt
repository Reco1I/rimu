package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.TransformationStack
import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.transform
import com.reco1l.framework.Color4
import com.reco1l.toolkt.toRadians
import kotlin.math.cos
import kotlin.math.sin


/**
 * A helper class that manages a resizable FloatBuffer for rendering. It automatically resizes the
 * buffer when the capacity is exceeded, based on the specified reallocation step.
 */
open class BufferRenderer {

    fun addVertex(x: Float, y: Float, u: Float = 0f, v: Float = 0f) {

        val position = TransformationStack.peek().transform(x, y)
        val color = ColorStack.peek() ?: Color4.White

        UIRenderer.activeBuffer.addVertex(
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

    fun addArc(
        segments: Int,
        arcCenterX: Float,
        arcCenterY: Float,
        startAngle: Float,
        endAngle: Float,
        radiusX: Float,
        radiusY: Float,
        fanCenterX: Float = arcCenterX,
        fanCenterY: Float = arcCenterY,
        filled: Boolean
    ) {
        if (segments <= 0) return

        val start = (startAngle - 90f).toRadians()
        val end = (endAngle - 90f).toRadians()

        val delta = (end - start) / segments

        var previousX = arcCenterX + radiusX * cos(start)
        var previousY = arcCenterY + radiusY * sin(start)

        for (j in 0 .. segments) {

            val angle = start + j * delta
            val x = arcCenterX + radiusX * cos(angle)
            val y = arcCenterY + radiusY * sin(angle)

            if (j > 0) {
                if (filled) {
                    addTriangle(
                        fanCenterX, fanCenterY,
                        previousX, previousY,
                        x, y
                    )
                } else {
                    addLine(
                        previousX, previousY,
                        x, y
                    )
                }
            }

            previousX = x
            previousY = y
        }
    }
}
