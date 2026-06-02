package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UICircle.Companion.calculateArcResolution
import com.reco1l.framework.Color4
import com.reco1l.toolkt.toRadians
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
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
        paintStyle: PaintStyle = PaintStyle.Fill,
        color: Color4? = null,
        lineWidth: Float = 1f
    ) {
        if (color != null) ColorStack.push(color, false)

        if (paintStyle == PaintStyle.Outline) {
            GLHelper.lineWidth(gl, lineWidth)
        }

        val segments = calculateArcResolution(width, height, abs(endAngle - startAngle))
        val start = (startAngle - 90f).toRadians()
        val end = (endAngle - 90f).toRadians()

        val delta = (end - start) / max(1, segments - 1)

        for (j in 0 until segments) {
            val angle = start + j * delta
            val x = centerX + cos(angle) * width / 2f
            val y = centerY + sin(angle) * height / 2f
            //addVertex(x, y)
        }

        if (color != null) ColorStack.pop()
    }

}