package com.reco1l.verktex.ui.shape

import kotlin.math.PI
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

inline fun UIContainer.wedge(builder: UIWedge.() -> Unit) = UIWedge().apply(builder).also { +it }

/**
 * A box with its right side slanted, leaving the bottom shorter than the top.
 *
 * It is filled with [color], and [strokeWidth] and [strokeColor] define its outline. The border of a component
 * can't be used for that since it follows the rectangular bounds.
 */
open class UIWedge : UICanvas() {

    /**
     * How much shorter the bottom side is than the top side.
     */
    var shear: Dimension.Fixed = 32f.dip

    /**
     * The width of the outline.
     */
    var strokeWidth: Dimension.Fixed = Dimension.Zero

    /**
     * The color of the outline.
     */
    var strokeColor: Color4 by themeable(Color4.Transparent)


    private var points = FloatArray(0)
    private var triangles = FloatArray(0)

    private fun updateGeometry() {
        val width = bounds.width
        val height = bounds.height
        val shear = shear.toPixels()
        val radius = radius.toPixels().coerceAtMost(height / 2f)

        // The rounded corner goes from the direction the slanted side faces to the bottom of the box.
        val startAngle = atan2(shear, height)
        val endAngle = PI.toFloat() / 2f
        val segments = if (radius > 0f) max(1, CORNER_SEGMENTS) else 0

        val pointCount = 3 + segments + 1

        if (points.size != pointCount * 2) {
            points = FloatArray(pointCount * 2)
            triangles = FloatArray((pointCount - 2) * 6)
        }

        var i = 0

        fun addPoint(x: Float, y: Float) {
            points[i++] = x
            points[i++] = y
        }

        addPoint(0f, 0f)
        addPoint(width, 0f)

        // The center is at the corner's radius from the slanted side and the bottom side.
        val sideLength = sqrt(shear * shear + height * height)
        val centerY = height - radius
        val centerX = width - shear * centerY / height - radius * sideLength / height

        for (segment in 0..segments) {
            val angle = startAngle + (endAngle - startAngle) * segment / max(1, segments)
            addPoint(centerX + cos(angle) * radius, centerY + sin(angle) * radius)
        }

        addPoint(0f, height)

        // The shape is convex, so it can be split into triangles from its first point.
        i = 0
        for (point in 1 until pointCount - 1) {
            triangles[i++] = points[0]
            triangles[i++] = points[1]
            triangles[i++] = points[point * 2]
            triangles[i++] = points[point * 2 + 1]
            triangles[i++] = points[point * 2 + 2]
            triangles[i++] = points[point * 2 + 3]
        }
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        if (bounds.height <= 0f) {
            return
        }

        updateGeometry()

        renderer.drawTriangles(triangles, triangles.size / 2, contentColor)

        val thickness = strokeWidth.toPixels()

        if (thickness > 0f && strokeColor.alpha > 0f) {
            val pointCount = points.size / 2

            for (point in 0 until pointCount) {
                val next = (point + 1) % pointCount

                renderer.drawLine(
                    points[point * 2],
                    points[point * 2 + 1],
                    points[next * 2],
                    points[next * 2 + 1],
                    thickness,
                    strokeColor
                )
            }
        }
    }


    private companion object {
        const val CORNER_SEGMENTS = 8
    }
}
