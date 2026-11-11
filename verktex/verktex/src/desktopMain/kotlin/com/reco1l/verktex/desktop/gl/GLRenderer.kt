package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.FramebufferOrigin
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.graphics.GraphicsRenderer
import org.lwjgl.opengl.GL33.*

/**
 * The OpenGL 3.3 implementation of [GraphicsRenderer], a port of the OpenGL ES 3 one.
 *
 * Unlike on Android, nothing here may call GL before the context is current on the draw thread (see
 * [GLContext]), so the version and the capabilities are read once it is.
 */
class GLRenderer(
    override val context: GLContext,
    override val device: GLDevice,
) : GraphicsRenderer() {

    override val window: GLWindow
        get() = context.window

    override val backend: GraphicsBackend
        get() = GraphicsBackend.OpenGL

    override var version: String = "OpenGL"

    // glCopyTexSubImage2D copies the framebuffer, which starts at the bottom.
    override val framebufferOrigin: FramebufferOrigin
        get() = FramebufferOrigin.BottomLeft

    private val executor = GLCommandExecutor()


    init {
        // Only creates the shader objects, they're compiled when first bound on the draw thread.
        createBuiltInShaders()

        context.onCurrent = {
            device.refreshCapabilities()
            version = context.version
        }
    }


    override fun prepareFrame(): Boolean {
        return context.acquireFrame()
    }

    override fun begin() {
        super.begin()

        glViewport(0, 0, window.windowWidth, window.windowHeight)
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
    }

    override fun end() {
        // Textures and glyphs were uploaded while recording, the cached state is stale.
        executor.resetState()
        super.end()

        context.present()
    }

    override fun drawCommand(command: DrawCommand) {
        executor.execute(command, window.windowWidth, window.windowHeight)
    }
}
