package com.reco1l.andengine.shape

import androidx.annotation.*
import com.reco1l.andengine.buffered.*
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.*
import kotlin.math.*


/**
 * A circle shape.
 *
 * @author Reco1l
 */
open class UICircle : UIBufferedComponent() {

    /**
     * The paint style of the circle.
     */
    var paintStyle = PaintStyle.Fill

    /**
     * The line width if the paint style is [PaintStyle.Outline].
     */
    var lineWidth = 1f

    /**
     * The angle where the circle starts to draw in degrees. By default, it is 0 degrees.
     */
    @FloatRange(-360.0, 360.0)
    var startAngle: Float = 0f

    /**
     * The angle where the circle ends to draw in degrees. By default, it is 360 degrees.
     */
    @FloatRange(-360.0, 360.0)
    var endAngle = 360f


    /**
     * Sets the portion of the circle to be drawn starting from the start angle.
     *
     * Positive values will draw the circle clockwise and negative values will draw it counter-clockwise.
     */
    fun setPortion(value: Float) {
        endAngle = startAngle + 360f * value.coerceIn(-1f, 1f)
    }


    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)

        CircleRenderer.renderCircle(
            gl = gl,
            centerX = width / 2f,
            centerY = height / 2f,
            width = width,
            height = height,
            startAngle = startAngle,
            endAngle = endAngle,
            paintStyle = paintStyle,
            lineWidth = lineWidth
        )
    }

}
