package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UITriangle.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

/**
 * A rectangle shape based on [UIComponent].
 */
open class UITriangle : UIBufferedComponent() {

    /**
     * The style of painting for the triangle.
     */
    var paintStyle = PaintStyle.Fill

    /**
     * The line width of the triangle if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f

    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)
        TriangleRenderer.renderTriangle(
            gl = gl,
            centerX = x + width / 2f,
            centerY = y + height / 2f,
            width = width,
            height = height,
            color = color,
            paintStyle = paintStyle,
            strokeWidth = lineWidth
        )
    }
}


