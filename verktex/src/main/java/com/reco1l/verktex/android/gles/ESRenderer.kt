package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.Logger
import com.reco1l.verktex.android.AndroidWindow
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.FramebufferOrigin
import com.reco1l.verktex.graphics.GraphicsRenderer

/**
 * The OpenGL ES implementation of [GraphicsRenderer], common to every version. Each backend
 * provides the [executor] running the draw commands.
 */
abstract class ESRenderer(
    override val context: ESContext,
    override val device: ESDevice,
    override val window: AndroidWindow,
    protected val executor: ESCommandExecutor,
) : GraphicsRenderer() {

    final override var version: String

    // glCopyTexSubImage2D copies the framebuffer, which starts at the bottom.
    override val framebufferOrigin: FramebufferOrigin
        get() = FramebufferOrigin.BottomLeft


    init {
        createBuiltInShaders()

        version = GLES20.glGetString(GLES20.GL_VERSION) ?: run {
            Logger.w("ESRenderer", "Couldn't retrieve OpenGL ES version.")
            "Unknown OpenGL ES version"
        }
    }

    private var capabilitiesRefreshed = false

    override fun prepareFrame(): Boolean {
        if (!context.acquireFrame()) return false

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

        context.present()
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
