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
    val blendMode: BlendMode = BlendMode.Alpha,

    /**
     * The depth state to be used for rendering the object, controlling how it interacts with
     * the depth buffer.
     */
    val depthState: DepthState = DepthState.Default,

    /**
     * The set of uniform variables to be passed to the shader program during rendering.
     */
    val uniformSet: GraphicsShader.UniformSet = GraphicsShader.UniformSet(),

    /**
     * Whether this command clears the depth buffer instead of drawing. A clear command carries no
     * buffer or shader and is never merged with other commands.
     */
    val clearDepth: Boolean = false,

    /**
     * How the data of [buffer] maps to the attributes of the shader.
     */
    val vertexLayout: VertexLayout = VertexLayout.Default,

    /**
     * The per-instance data of an instanced draw, which is never merged with other commands.
     */
    val instanceBuffer: GraphicsBuffer? = null,

    /**
     * How the data of [instanceBuffer] maps to the attributes of the shader.
     */
    val instanceLayout: VertexLayout? = null,

    /**
     * The number of instances to draw, only used with an [instanceBuffer].
     */
    val instanceCount: Int = 0,
) {

    /**
     * Whether a draw with the given state can be appended to this command instead of starting a new one.
     */
    fun canReuse(
        shader: GraphicsShader?,
        texture: GraphicsTexture?,
        scissor: Vec4?,
        blendMode: BlendMode,
        depthState: DepthState,
        uniformSet: GraphicsShader.UniformSet,
    ): Boolean {
        return !clearDepth &&
                instanceBuffer == null &&
                this.shader == shader &&
                this.uniformSet == uniformSet &&
                this.texture == texture &&
                this.blendMode == blendMode &&
                this.depthState == depthState &&
                this.scissor == scissor
    }
}
