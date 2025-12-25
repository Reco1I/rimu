package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsRenderer.Companion.VERTEX_2D_STRIDE
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

        // Vertex layout: position (2 floats), UV (2 floats), color (4 normalized unsigned bytes).
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 2, GLES32.GL_FLOAT, false, VERTEX_2D_STRIDE, 0)
        GLES32.glEnableVertexAttribArray(1)
        GLES32.glVertexAttribPointer(1, 2, GLES32.GL_FLOAT, false, VERTEX_2D_STRIDE, 8)
        GLES32.glEnableVertexAttribArray(2)
        GLES32.glVertexAttribPointer(2, 4, GLES32.GL_UNSIGNED_BYTE, true, VERTEX_2D_STRIDE, 16)

        val vertexCount = command.buffer.position / VERTEX_2D_STRIDE

        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertexCount)
    }
}
