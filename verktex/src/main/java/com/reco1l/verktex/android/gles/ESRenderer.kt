package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsRenderer

class ESRenderer(
    override val context: ESContext,
    override val device: ESDevice,
    override val window: ESWindow,
) : GraphicsRenderer() {

    override var version: String


    init {
        BuiltIn.Shaders.SolidQuad = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/quad.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/quad.frag and verktex/main.vert")
        )

        BuiltIn.Shaders.TextureQuad = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/texture.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/texture.frag and verktex/main.vert")
        )

        version = GLES32.glGetString(GLES32.GL_VERSION) ?: run {
            Logger.w("ESRenderer", "Couldn't retrieve OpenGL ES version.")
            "Unknown OpenGL ES version"
        }
    }

    override fun drawCommand(command: DrawCommand) {

        if (command.buffer == null || command.shader == null || command.buffer.isEmpty()) return

        // Scissor test
        val scissor = command.scissor
        if (scissor != null) {
            GLES32.glEnable(GLES32.GL_SCISSOR_TEST)
            GLES32.glScissor(
                scissor.x.toInt(),
                scissor.y.toInt(),
                scissor.z.toInt(),
                scissor.w.toInt()
            )
        }

        GLES32.glDisable(GLES32.GL_CULL_FACE)

        // Blend
        GLES32.glEnable(GLES32.GL_BLEND)
        GLES32.glBlendFunc(
            command.blendMode.toGLESSource(),
            command.blendMode.toGLESDestination()
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
            GLES32.glEnable(GLES32.GL_TEXTURE_2D)
            command.texture.bind()
        } else {
            GLES32.glDisable(GLES32.GL_TEXTURE_2D)
        }

        command.shader.applyUniforms(command.uniformSet)

        command.buffer.bind()
        val vertexCount = command.buffer.position / VERTEX_2D_STRIDE

        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertexCount)
    }
}