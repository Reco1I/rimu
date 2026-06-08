package com.reco1l.andengine.shape

import com.reco1l.andengine.component.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.PaintStyle.*
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.*

/**
 * A rectangle shape based on [UIComponent].
 */
open class UIBox : UIBufferedComponent() {

    /**
     * The style of painting for the box.
     */
    var paintStyle = Fill

    /**
     * The line width of the box if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f


    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)
        QuadRenderer.renderQuad(0f, 0f, width, height, radius, paintStyle, lineWidth = lineWidth)
    }

}