package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.framework.math.Vec2
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.*

/**
 * A rectangle shape based on [com.reco1l.andengine.component.UIComponent].
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
        LineRenderer.renderLine(gl, fromPoint.x, fromPoint.y, toPoint.x, toPoint.y, lineWidth)
    }
}
