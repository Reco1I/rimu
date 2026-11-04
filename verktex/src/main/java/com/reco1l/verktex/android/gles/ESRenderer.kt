package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsRenderer

/**
 * The OpenGL ES implementation of [GraphicsRenderer], common to every version. Each backend
 * provides the [executor] running the draw commands.
 */
abstract class ESRenderer(
    override val context: ESContext,
    override val device: ESDevice,
    override val window: ESWindow,
    protected val executor: ESCommandExecutor,
) : GraphicsRenderer() {

    final override var version: String


    init {
        BuiltIn.Shaders.SolidQuad = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/quad.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/quad.frag and verktex/main.vert")
        )

        BuiltIn.Shaders.Color = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/main.vert",
                fragmentShaderPath = "verktex/shaders/color.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/color.frag and verktex/main.vert")
        )

        BuiltIn.Shaders.UI = createShader(
            Platform.provider.createShaderSource(
                vertexShaderPath = "verktex/shaders/ui.vert",
                fragmentShaderPath = "verktex/shaders/ui.frag",
                isExternal = false
            ) ?: throw IllegalStateException("Failed to load built-in shader source: verktex/ui.frag and verktex/ui.vert")
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

        version = GLES20.glGetString(GLES20.GL_VERSION) ?: run {
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
            onContextReady()
        }
        return true
    }

    override fun begin() {
        super.begin()

        onFrameBegin()

        GLES20.glViewport(0, 0, window.windowWidth, window.windowHeight)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_STENCIL_BUFFER_BIT)
    }

    override fun end() {
        // Textures and glyphs were uploaded while recording, the cached state is stale.
        executor.resetState()
        super.end()

        onFrameExecuted()

        context.swapBuffers()
    }

    override fun drawCommand(command: DrawCommand) {
        executor.execute(command, window.windowWidth, window.windowHeight)
    }


    /**
     * Called on the draw thread the first time the context is current.
     */
    protected open fun onContextReady() = Unit

    /**
     * Called at the start of every frame, before the window is cleared.
     */
    protected open fun onFrameBegin() = Unit

    /**
     * Called once the frame's commands were executed, before the buffers are swapped.
     */
    protected open fun onFrameExecuted() = Unit
}
