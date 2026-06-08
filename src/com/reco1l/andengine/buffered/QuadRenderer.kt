package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.PaintStyle.Fill
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

object QuadRenderer : BufferRenderer() {

    fun renderQuad(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float = 0f,
        paintStyle: PaintStyle = Fill,
        lineWidth: Float = 1f
    ) {
        val filled = paintStyle == Fill

        UIRenderer.activePrimitiveType = if (filled) GL10.GL_TRIANGLES else GL10.GL_LINES
        UIRenderer.activeLineWidth = lineWidth

        val r = radius
            .coerceAtMost(min(width, height) / 2f)
            .coerceAtLeast(0f)

        val centerX = x + width / 2f
        val centerY = y + height / 2f

        val segments = CircleRenderer.calculateArcResolution(
            width = r,
            height = r,
            angle = 90f
        )

        addArc(
            segments,
            x + r, y + r,
            -90f, 0f,
            r, r,
            centerX, centerY,
            filled
        )

        if (filled) {
            addTriangle(
                centerX, centerY,
                x + r, y,
                x + width - r, y
            )
        } else {
            addLine(x + r, y, x + width - r, y)
        }

        addArc(
            segments,
            x + width - r, y + r,
            0f, 90f,
            r, r,
            centerX, centerY,
            filled
        )

        if (filled) {
            addTriangle(
                centerX, centerY,
                x + width, y + r,
                x + width, y + height - r
            )
        } else {
            addLine(x + width, y + r, x + width, y + height - r)
        }

        addArc(
            segments,
            x + width - r, y + height - r,
            90f, 180f,
            r, r,
            centerX, centerY,
            filled
        )

        if (filled) {
            addTriangle(
                centerX, centerY,
                x + width - r, y + height,
                x + r, y + height
            )
        } else {
            addLine(x + width - r, y + height, x + r, y + height)
        }

        addArc(
            segments,
            x + r, y + height - r,
            180f, 270f,
            r, r,
            centerX, centerY,
            filled
        )

        if (filled) {
            addTriangle(
                centerX, centerY,
                x, y + height - r,
                x, y + r
            )
        } else {
            addLine(x, y + height - r, x, y + r)
        }
    }

}