package com.reco1l.verktex.ui

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader

/**
 * A box component that displays a gradient using a dynamically generated texture.
 * The texture automatically stretches to fill the component size.
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
        super.onDraw(renderer)

        renderer.setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = GraphicsShader.UniformSet().apply {
                set("uRadius", radius.toPixels())
                set("uBorderWidth", borderWidth.toPixels())
                set("uBorderColor", borderColor)
            },
        )

        renderer.drawGeometry { transform, baseColor ->
            TODO("Not implemented yet")
        }

    }
}
