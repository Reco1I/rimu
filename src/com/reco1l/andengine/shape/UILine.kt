package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UILine.*
import com.reco1l.framework.math.Vec2
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

/**
 * A rectangle shape based on [UIComponent].
 */
class UILine : UIBufferedComponent() {

    /**
     * The width of the line.
     */
    var lineWidth = 1f

    /**
     * The starting point of the line.
     */
    var fromPoint = Vec2.Zero

    /**
     * The ending point of the line.
     */
    var toPoint = Vec2.Zero


    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)
        LineRenderer.renderLine(gl, fromPoint.x, fromPoint.y, toPoint.x, toPoint.y, strokeWidth = lineWidth)
    }
}
