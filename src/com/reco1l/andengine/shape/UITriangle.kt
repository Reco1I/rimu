package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.*

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
            x = 0f,
            y = 0f,
            width = width,
            height = height,
            paintStyle = paintStyle,
            lineWidth = lineWidth
        )
    }
}


