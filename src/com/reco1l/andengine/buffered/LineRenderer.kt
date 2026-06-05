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
        UIRenderer.setBatchOptions(gl,
            primitiveType = GL10.GL_LINES,
            lineWidth = lineWidth
        )

        addVertex(fromX, fromY)
        addVertex(toX, toY)
    }
}