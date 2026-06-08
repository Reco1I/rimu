package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import javax.microedition.khronos.opengles.GL10

object LineRenderer : BufferRenderer() {

    fun renderLine(
        gl: GL10,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        lineWidth: Float = 1f
    ) {
        UIRenderer.activePrimitiveType = GL10.GL_LINES
        UIRenderer.activeLineWidth = lineWidth

        addLine(fromX, fromY, toX, toY)
    }
}