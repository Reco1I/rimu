package com.reco1l.verktex.ui

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.math.toRadians
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

inline fun UIContainer.gradientBox(builder: UIGradientBox.() -> Unit) = UIGradientBox().apply(builder).also { +it }

/**
 * A box component that displays a linear gradient between two colors.
 */
open class UIGradientBox : UICanvas() {

    /**
     * The angle of the gradient in degrees.
     * - 0° = left to right (horizontal)
     * - 90° = top to bottom (vertical)
     * - 180° = right to left
     * - 270° = bottom to top
     */
    var gradientAngle = 90f

    /**
     * The start color of the gradient.
     */
    var colorStart = Color4.White

    /**
     * The end color of the gradient.
     */
    var colorEnd = Color4.Black


    override fun onDraw(renderer: GraphicsRenderer) {
        val width = bounds.width
        val height = bounds.height

        renderer.setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = GraphicsShader.UniformSet().apply {
                set("uRadius", radius.toPixels())
                set("uBorderWidth", borderWidth.toPixels())
                set("uBorderColor", borderColor)
                set("uSize", Vec2(width, height))
            },
        )

        val directionX = cos(gradientAngle.toRadians())
        val directionY = sin(gradientAngle.toRadians())

        // Half the length of the box projected on the gradient direction, so the corners
        // furthest along it get exactly the start and end colors.
        val extent = abs(width / 2f * directionX) + abs(height / 2f * directionY)

        fun colorAt(x: Float, y: Float): Color4 {
            val projection = (x - width / 2f) * directionX + (y - height / 2f) * directionY
            val t = if (extent > 0f) (projection / extent + 1f) / 2f else 0.5f

            return renderer.transformColor(Color4(
                red = colorStart.red + (colorEnd.red - colorStart.red) * t,
                green = colorStart.green + (colorEnd.green - colorStart.green) * t,
                blue = colorStart.blue + (colorEnd.blue - colorStart.blue) * t,
                alpha = colorStart.alpha + (colorEnd.alpha - colorStart.alpha) * t
            ))
        }

        val topLeft = colorAt(0f, 0f)
        val topRight = colorAt(width, 0f)
        val bottomRight = colorAt(width, height)
        val bottomLeft = colorAt(0f, height)

        renderer.drawGeometry { transform, _ ->
            val (x0, y0) = transform.transform(0f, 0f)
            val (x1, y1) = transform.transform(width, 0f)
            val (x2, y2) = transform.transform(width, height)
            val (x3, y3) = transform.transform(0f, height)

            // A linear gradient is linear across the quad, so per vertex colors are exact.
            with(renderer) {
                addVertex(x0, y0, 0f, 0f, topLeft)
                addVertex(x1, y1, 1f, 0f, topRight)
                addVertex(x2, y2, 1f, 1f, bottomRight)

                addVertex(x0, y0, 0f, 0f, topLeft)
                addVertex(x2, y2, 1f, 1f, bottomRight)
                addVertex(x3, y3, 0f, 1f, bottomLeft)
            }
        }
    }
}
