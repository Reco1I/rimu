package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader

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

    override fun prepareFrame(): Boolean {
        return context.prepare()
    }

    override fun begin() {
        super.begin()

        GLES32.glViewport(0, 0, window.width, window.height)
        GLES32.glClearColor(0f, 0f, 0f, 1f)
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT or GLES32.GL_STENCIL_BUFFER_BIT)
    }

    override fun end() {
        super.end()
        context.swapBuffers()
    }

    override fun drawCommand(command: DrawCommand) {

        if (command.buffer == null || command.shader == null || command.buffer.isEmpty()) return

        // Scissor test, the scissor is in window space (top-left origin) while GL expects bottom-left.
        val scissor = command.scissor
        if (scissor != null) {
            GLES32.glEnable(GLES32.GL_SCISSOR_TEST)
            GLES32.glScissor(
                scissor.x.toInt(),
                (window.height - scissor.y - scissor.w).toInt(),
                scissor.z.toInt().coerceAtLeast(0),
                scissor.w.toInt().coerceAtLeast(0)
            )
        } else {
            GLES32.glDisable(GLES32.GL_SCISSOR_TEST)
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
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0)
            command.texture.bind()
        }

        command.shader.applyUniforms(GraphicsShader.UniformSet().apply {
            // Uniform values persist per program, so reset them to defaults before every draw.
            val viewportSize = Vec2(window.windowWidth.toFloat(), window.windowHeight.toFloat())
            set("uViewportSize", viewportSize)
            set("uColor", Color4.White)
            set("uTexture", 0)
            set("uRadius", 0f)
            set("uBorderWidth", 0f)
            set("uBorderColor", Color4.Transparent)
            set("uSize", viewportSize)
        })
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
