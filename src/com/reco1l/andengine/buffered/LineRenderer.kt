package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.framework.Color4
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

object LineRenderer : BufferRenderer() {

    fun renderLine(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        strokeWidth: Float = 1f
    ) {
        val fromTopX = fromX + strokeWidth / 2
        val fromTopY = fromY + strokeWidth / 2
        val fromBottomX = fromX - strokeWidth / 2
        val fromBottomY = fromY - strokeWidth / 2
        val toTopX = toX + strokeWidth / 2
        val toTopY = toY + strokeWidth / 2
        val toBottomX = toX - strokeWidth / 2
        val toBottomY = toY - strokeWidth / 2

        addTriangle(fromTopX, fromTopY, fromBottomX, fromBottomY, toTopX, toTopY)
        addTriangle(toTopX, toTopY, fromBottomX, fromBottomY, toBottomX, toBottomY)
    }
}