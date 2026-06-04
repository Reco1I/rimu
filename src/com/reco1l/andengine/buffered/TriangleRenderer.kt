package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.shape.PaintStyle
import javax.microedition.khronos.opengles.GL10

object TriangleRenderer : BufferRenderer() {

    fun renderTriangle(
        gl: GL10,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paintStyle: PaintStyle = PaintStyle.Fill,
        lineWidth: Float = 1f
    ) {
        val filled = paintStyle == PaintStyle.Fill

        UIRenderer.setState(gl,
            primitiveType = if (filled) GL10.GL_TRIANGLES else GL10.GL_LINES,
            lineWidth = lineWidth
        )

        if (filled) {
            addTriangle(x, y, x + width, y, x + width / 2f, y + height)
        } else {
            addLine(x, y, x + width, y)
            addLine(x + width, y, x + width / 2f, y + height)
            addLine(x + width / 2f, y + height, x, y)
        }
    }
}