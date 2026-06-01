package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.framework.Color4
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

object LineRenderer : BufferRenderer() {

    fun renderLine(
        gl: GL10,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        color: Color4? = null,
        strokeWidth: Float = 1f
    ) {
        if (color != null) ColorStack.pushColor(gl, color, false)

        GLHelper.lineWidth(gl, strokeWidth)

        render(gl, GL10.GL_LINES) {
            addVertex(fromX, fromY)
            addVertex(toX, toY)
        }

        if (color != null) ColorStack.popColor(gl)
    }
}