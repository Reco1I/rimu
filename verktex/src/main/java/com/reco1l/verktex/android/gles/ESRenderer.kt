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

        BuiltIn.Shaders.Box = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/box.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/box.frag and verktex/main.vert")
        )

        BuiltIn.Shaders.TextureQuad = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/texture.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/texture.frag and verktex/main.vert")
        )

        BuiltIn.Shaders.Video = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/video.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/video.frag and verktex/main.vert")
        )

        BuiltIn.Shaders.Particle = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/particle.vert",
                fragmentShaderPath = "verktex/shaders/particle.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/shaders/particle")
        )

        version = GLES32.glGetString(GLES32.GL_VERSION) ?: run {
            Logger.w("ESRenderer", "Couldn't retrieve OpenGL ES version.")
            "Unknown OpenGL ES version"
        }
    }

    private var capabilitiesRefreshed = false

    override fun prepareFrame(): Boolean {
        if (!context.prepare()) return false

        // The context is current on the draw thread from here on, the device was created without one.
        if (!capabilitiesRefreshed) {
            capabilitiesRefreshed = true
            device.refreshCapabilities()
        }
        return true
    }

    override fun begin() {
        super.begin()

        GLES32.glViewport(0, 0, window.windowWidth, window.windowHeight)
        GLES32.glClearColor(0f, 0f, 0f, 1f)
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT or GLES32.GL_STENCIL_BUFFER_BIT)
    }

    override fun end() {
        super.end()
        context.swapBuffers()
    }

    override fun drawCommand(command: DrawCommand) {
        ESCommandExecutor.execute(command, window.windowWidth, window.windowHeight)
    }
}
