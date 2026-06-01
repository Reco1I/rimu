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

    private var lastState = ""


    fun renderQuad(
        gl: GL10,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paintStyle: PaintStyle = Fill,
        color: Color4? = null,
        inheritColors: Boolean = true,
        lineWidth: Float = 1f
    ) {

        val state = "x:$x,y:$y,width:$width,height:$height,paintStyle:$paintStyle,color:$color,lineWidth:$lineWidth"
        if (state == lastState) {
            if (color != null) ColorStack.pushColor(gl, color)
            doRender(gl, when (paintStyle) {
                Fill -> GL10.GL_TRIANGLE_STRIP
                Outline -> GL10.GL_LINE_LOOP
            })
            if (color != null) ColorStack.popColor(gl)
            return
        }
        lastState = state

        if (color != null) ColorStack.pushColor(gl, color)

        when (paintStyle) {
            Fill ->  {
                render(gl, GL10.GL_TRIANGLE_STRIP) {
                    addVertex(x, y)
                    addVertex(x, y + height)
                    addVertex(x + width, y)
                    addVertex(x + width, y + height)
                }
            }
            Outline -> {
                GLHelper.lineWidth(gl, lineWidth)
                render(gl, GL10.GL_LINE_LOOP) {
                    addVertex(x, y)
                    addVertex(x, y + height)
                    addVertex(x + width, y + height)
                    addVertex(x + width, y)
                }
            }
        }

        if (color != null) ColorStack.popColor(gl)
    }

    fun renderQuad(
        gl: GL10,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float = 0f,
        paintStyle: PaintStyle = Fill,
        color: Color4? = null,
        inheritColors: Boolean = true,
        lineWidth: Float = 1f
    ) {
        val state = "x:$x,y:$y,width:$width,height:$height,radius:$radius,paintStyle:$paintStyle,color:$color,lineWidth:$lineWidth"
        if (state == lastState) {
            if (color != null) ColorStack.pushColor(gl, color)
            if (paintStyle == Outline) GLHelper.lineWidth(gl, lineWidth)
            doRender(gl, when (paintStyle) {
                Fill -> GL10.GL_TRIANGLE_FAN
                Outline -> GL10.GL_LINE_STRIP
            })
            if (color != null) ColorStack.popColor(gl)
            return
        }
        lastState = state

        if (color != null) ColorStack.pushColor(gl, color)

        if (paintStyle == Outline) GLHelper.lineWidth(gl, lineWidth)

        render(gl, when (paintStyle) {
            Fill -> GL10.GL_TRIANGLE_FAN
            Outline -> GL10.GL_LINE_STRIP
        }) {

            val r = radius
                .coerceAtMost(min(width, height) / 2f)
                .coerceAtLeast(0f)

            val segments = if (r <= 0f) 1 else calculateArcResolution(r, r, 90f).coerceAtLeast(1)

            if (paintStyle == Fill) {
                addVertex(x + width / 2f, y + height / 2f)
            }

            addArc(x + r, y + r, -90f, 0f, r, r, segments)
            addArc(x + width - r, y + r, 0f, 90f, r, r, segments)
            addArc(x + width - r, y + height - r, 90f, 180f, r, r, segments)
            addArc(x + r, y + height - r, 180f, 270f, r, r, segments)

            addVertex(x, y + r)
        }

        if (color != null) ColorStack.popColor(gl)
    }


    private fun ResizableFloatBuffer.addArc(
        centerX: Float,
        centerY: Float,
        startAngle: Float,
        endAngle: Float,
        radiusX: Float,
        radiusY: Float,
        segments: Int
    ) {
        val start = (startAngle - 90f).toRadians()
        val end = (endAngle - 90f).toRadians()

        val delta = (end - start) / (segments - 1).coerceAtLeast(1)

        for (j in 0 until segments) {
            val angle = start + j * delta

            addVertex(
                x = centerX + radiusX * cos(angle),
                y = centerY + radiusY * sin(angle)
            )
        }
    }

}