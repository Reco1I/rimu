package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.VertexLayout
import com.reco1l.verktex.graphics.GraphicsShader

/**
 * Executes a [DrawCommand] against the current OpenGL ES context, shared by every renderer drawing through GLES.
 */
object ESCommandExecutor {

    private val IDENTITY_MAT4 = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    // The defaults every draw starts from, rebuilt only when the viewport size changes. Commands are
    // executed on the draw thread only, so one set is shared.
    private var defaultUniformsWidth = -1
    private var defaultUniformsHeight = -1
    private var defaultUniforms = GraphicsShader.UniformSet()

    private fun defaultUniforms(viewportWidth: Int, viewportHeight: Int): GraphicsShader.UniformSet {
        if (viewportWidth != defaultUniformsWidth || viewportHeight != defaultUniformsHeight) {
            defaultUniformsWidth = viewportWidth
            defaultUniformsHeight = viewportHeight

            val viewportSize = Vec2(viewportWidth.toFloat(), viewportHeight.toFloat())

            defaultUniforms = GraphicsShader.UniformSet().apply {
                set("uViewportSize", viewportSize)
                set("uColor", Color4.White)
                set("uTexture", 0)
                set("uRadius", 0f)
                set("uBorderWidth", 0f)
                set("uBorderColor", Color4.Transparent)
                set("uSize", viewportSize)
                set("uTransform", IDENTITY_MAT4)
            }
        }

        return defaultUniforms
    }

    fun execute(command: DrawCommand, viewportWidth: Int, viewportHeight: Int) {

        if (command.clearDepth) {
            // glClear honours the scissor test and the depth mask, either may be left over from the previous command.
            GLES32.glDisable(GLES32.GL_SCISSOR_TEST)
            GLES32.glDepthMask(true)
            GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT)
            return
        }

        if (command.buffer == null || command.shader == null || command.buffer.isEmpty()) return

        // Scissor test, the scissor is in window space (top-left origin) while GL expects bottom-left.
        val scissor = command.scissor
        if (scissor != null) {
            GLES32.glEnable(GLES32.GL_SCISSOR_TEST)
            GLES32.glScissor(
                scissor.x.toInt(),
                (viewportHeight - scissor.y - scissor.w).toInt(),
                scissor.z.toInt().coerceAtLeast(0),
                scissor.w.toInt().coerceAtLeast(0)
            )
        } else {
            GLES32.glDisable(GLES32.GL_SCISSOR_TEST)
        }

        GLES32.glDisable(GLES32.GL_CULL_FACE)

        // Blend
        GLES32.glEnable(GLES32.GL_BLEND)
        val blendMode = command.blendMode
        GLES32.glBlendFuncSeparate(
            blendMode.sourceRGB.toGLES(),
            blendMode.destinationRGB.toGLES(),
            blendMode.sourceAlpha.toGLES(),
            blendMode.destinationAlpha.toGLES()
        )

        // Depth testing
        if (command.depthState.enabled) {
            GLES32.glEnable(GLES32.GL_DEPTH_TEST)
            GLES32.glDepthFunc(command.depthState.function.toGLES())
            GLES32.glDepthMask(command.depthState.writeEnabled)
        } else {
            GLES32.glDisable(GLES32.GL_DEPTH_TEST)
            GLES32.glDepthMask(false)
        }

        command.shader.bind()

        if (command.texture != null) {
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0)
            command.texture.bind()
        }

        // Uniform values persist per program, so reset them to defaults before every draw.
        command.shader.applyUniforms(defaultUniforms(viewportWidth, viewportHeight))
        command.shader.applyUniforms(command.uniformSet)

        command.buffer.bind()
        command.buffer.uploadDataToGPU()
        applyLayout(command.vertexLayout)

        val vertexCount = command.buffer.position / command.vertexLayout.stride

        val instanceBuffer = command.instanceBuffer
        val instanceLayout = command.instanceLayout

        if (instanceBuffer == null || instanceLayout == null) {
            GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertexCount)
        } else {
            instanceBuffer.bind()
            instanceBuffer.uploadDataToGPU()
            applyLayout(instanceLayout)

            GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLES, 0, vertexCount, command.instanceCount)

            resetLayout(instanceLayout)
        }

        resetLayout(command.vertexLayout)
    }

    private fun applyLayout(layout: VertexLayout) {
        layout.attributes.forEachIndexed { index, attribute ->
            GLES32.glEnableVertexAttribArray(attribute.location)
            GLES32.glVertexAttribPointer(
                attribute.location,
                attribute.size,
                when (attribute.type) {
                    VertexLayout.Type.Float -> GLES32.GL_FLOAT
                    VertexLayout.Type.NormalizedByte -> GLES32.GL_UNSIGNED_BYTE
                },
                attribute.type == VertexLayout.Type.NormalizedByte,
                layout.stride,
                layout.offsets[index]
            )
            GLES32.glVertexAttribDivisor(attribute.location, if (attribute.isPerInstance) 1 else 0)
        }
    }

    /**
     * Leaves the attributes as the next draw expects them, as they persist between draws.
     */
    private fun resetLayout(layout: VertexLayout) {
        for (attribute in layout.attributes) {
            GLES32.glVertexAttribDivisor(attribute.location, 0)
            GLES32.glDisableVertexAttribArray(attribute.location)
        }
    }
}
