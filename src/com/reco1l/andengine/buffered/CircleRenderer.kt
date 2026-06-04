package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.PaintStyle.Fill
import com.reco1l.toolkt.toRadians
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object CircleRenderer : BufferRenderer() {

    fun renderCircle(
        gl: GL10,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        startAngle: Float = 0f,
        endAngle: Float = 360f,
        paintStyle: PaintStyle = Fill,
        lineWidth: Float = 1f
    ) {
        val filled = paintStyle == Fill

        UIRenderer.setState(gl,
            primitiveType = if (filled) GL10.GL_TRIANGLES else GL10.GL_LINES,
            lineWidth = lineWidth
        )

        val segments = calculateArcResolution(
            width = width,
            height = height,
            angle = abs(endAngle - startAngle)
        ).coerceAtLeast(1)

        addArc(
            segments,
            centerX, centerY,
            startAngle, endAngle,
            width / 2f, height / 2f,
            centerX, centerY,
            filled
        )
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

        for (j in 0 until segments) {

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


    fun calculateArcResolution(width: Float, height: Float, angle: Float = 360f): Int {
        if (angle <= 0f || width <= 0f || height <= 0f) return 0

        val averageRadius = (width + height) / 4f
        val angleBetweenSegments = min(5f, 360f / averageRadius)
        val segments = abs(angle) / angleBetweenSegments

        return max(3, segments.toInt())
    }

}