package com.reco1l.verktex.ui

import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader

/**
 * A canvas is a component that exposes low-level rendering settings, such as the blend mode or the
 * depth state, and allows for usage of custom shaders and uniforms.
 */
abstract class UICanvas : UIComponent() {

    /**
     * The shader to use for rendering the canvas. If null, the default shader will be used.
     */
    val shader: GraphicsShader? = null

    /**
     * The blend mode to use for rendering the canvas.
     */
    val blendMode: BlendMode = BlendMode.Additive

    /**
     * The state of the depth buffer for rendering the canvas.
     */
    val depthState: DepthState = DepthState.Default


    override fun onDraw(renderer: GraphicsRenderer) {
        renderer.setState(
            blendMode = blendMode,
            depthState = depthState,
        )
    }
}