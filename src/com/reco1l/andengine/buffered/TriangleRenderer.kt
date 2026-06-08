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

        UIRenderer.activePrimitiveType = if (filled) GL10.GL_TRIANGLES else GL10.GL_LINES
        UIRenderer.activeLineWidth = lineWidth

        if (filled) {
            addTriangle(
                x + width / 2f, y,
                x + width, y + height,
                x, y + height
            )
        } else {
            addLine(x + width / 2f, y, x + width, y + height)
            addLine(x + width, y + height, x, y + height)
            addLine(x, y + height, x + width / 2f, y)
        }
    }
}