package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.PaintStyle.Fill
import com.reco1l.andengine.shape.PaintStyle.Outline
import com.reco1l.andengine.shape.UICircle.Companion.calculateArcResolution
import com.reco1l.framework.Color4
import com.reco1l.toolkt.toRadians
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object QuadRenderer : BufferRenderer() {

    fun renderQuad(
        gl: GL10,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paintStyle: PaintStyle = Fill,
        color: Color4? = null,
        lineWidth: Float = 1f
    ) {
        when (paintStyle) {
            Fill -> {
                //addVertex(x, y)
                //addVertex(x, y + height)
                //addVertex(x + width, y)
                //addVertex(x + width, y + height)
            }

            Outline -> {
                //addVertex(x, y)
                //addVertex(x, y + height)
                //addVertex(x + width, y + height)
                //addVertex(x + width, y)
            }
        }
    }

    fun renderQuad(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float = 0f,
        paintStyle: PaintStyle = Fill,
        lineWidth: Float = 1f
    ) {
        val r = radius
            .coerceAtMost(min(width, height) / 2f)
            .coerceAtLeast(0f)

        val centerX = x + width / 2f
        val centerY = y + height / 2f

        val segments = if (r <= 0f) 1 else calculateArcResolution(r, r, 90f).coerceAtLeast(1)

        addArc(x + r, y + r, -90f, 0f, r, r, segments, centerX, centerY)

        addTriangle(
            centerX, centerY,
            x + r, y,
            x + width - r, y
        )

        addArc(x + width - r, y + r, 0f, 90f, r, r, segments, centerX, centerY)

        addTriangle(
            centerX, centerY,
            x + width, y + r,
            x + width, y + height - r
        )

        addArc(x + width - r, y + height - r, 90f, 180f, r, r, segments, centerX, centerY)

        addTriangle(
            centerX, centerY,
            x + width - r, y + height,
            x + r, y + height
        )

        addArc(x + r, y + height - r, -90f, -180f, r, r, segments, centerX, centerY)

        addTriangle(
            centerX, centerY,
            x, y + height - r,
            x, y + r
        )
    }


    private fun addArc(
        arcCenterX: Float,
        arcCenterY: Float,
        startAngle: Float,
        endAngle: Float,
        radiusX: Float,
        radiusY: Float,
        segments: Int,
        fanCenterX: Float = arcCenterX,
        fanCenterY: Float = arcCenterY
    ) {
        val start = (startAngle - 90f).toRadians()
        val end = (endAngle - 90f).toRadians()

        val delta = (end - start) / (segments - 1).coerceAtLeast(1)

        var previousX = arcCenterX + radiusX * cos(start)
        var previousY = arcCenterY + radiusY * sin(start)

        for (j in 0 until segments) {
            if (j == 0) continue

            val angle = start + j * delta
            val x = arcCenterX + radiusX * cos(angle)
            val y = arcCenterY + radiusY * sin(angle)

            addTriangle(
                fanCenterX, fanCenterY,
                previousX, previousY,
                x, y
            )

            previousX = x
            previousY = y
        }
    }

}