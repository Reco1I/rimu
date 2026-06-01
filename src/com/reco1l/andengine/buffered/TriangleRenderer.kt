package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.framework.Color4
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

object TriangleRenderer : BufferRenderer() {

    fun renderTriangle(
        gl: GL10,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        color: Color4? = null,
        paintStyle: PaintStyle = PaintStyle.Fill,
        strokeWidth: Float = 0f
    ) {
        if (color != null) ColorStack.pushColor(gl, color)

        GLHelper.lineWidth(gl, strokeWidth)

        render(gl, when (paintStyle) {
            PaintStyle.Fill -> GL10.GL_TRIANGLES
            PaintStyle.Outline -> GL10.GL_LINE_LOOP
        }) {
            addVertex(centerX, centerY - height / 2f)
            addVertex(centerX - width / 2f, centerY + height / 2f)
            addVertex(centerX + width / 2f, centerY + height / 2f)
        }

        if (color != null) ColorStack.popColor(gl)
    }
}