package com.reco1l.andengine.buffered

import com.reco1l.andengine.shape.PaintStyle
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

object TriangleRenderer : BufferRenderer() {

    fun renderTriangle(
        gl: GL10,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        paintStyle: PaintStyle = PaintStyle.Fill,
        strokeWidth: Float = 0f
    ) {
        GLHelper.lineWidth(gl, strokeWidth)

        addTriangle(
            centerX, centerY - height / 2,
            centerX - width / 2, centerY + height / 2,
            centerX + width / 2, centerY + height / 2
        )

    }
}