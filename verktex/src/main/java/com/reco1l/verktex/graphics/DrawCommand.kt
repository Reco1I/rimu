package com.reco1l.verktex.graphics

import com.reco1l.verktex.data.Vec4

/**
 * Represents a draw command that encapsulates the necessary information for rendering a
 * graphical object.
 */
data class DrawCommand(
    /**
     * The graphics buffer containing the vertex data to be rendered.
     */
    val buffer: GraphicsBuffer? = null,

    /**
     * The shader program to be used for rendering the object.
     */
    val shader: GraphicsShader? = null,

    /**
     * The texture to be applied to the object during rendering.
     */
    val texture: GraphicsTexture? = null,

    /**
     * The scissor rectangle defining the area of the screen where rendering is allowed.
     */
    val scissor: Vec4? = null,

    /**
     * The blend mode to be used for rendering the object, determining how it blends with the background.
     */
    val blendMode: BlendMode = BlendMode.Additive,

    /**
     * The depth state to be used for rendering the object, controlling how it interacts with
     * the depth buffer.
     */
    val depthState: DepthState = DepthState.Default,

    /**
     * The set of uniform variables to be passed to the shader program during rendering.
     */
    val uniformSet: GraphicsShader.UniformSet
)



