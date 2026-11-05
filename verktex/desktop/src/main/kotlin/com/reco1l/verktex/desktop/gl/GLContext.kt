package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.GraphicsContext
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil.NULL

/**
 * The OpenGL 3.3 core context of a GLFW window.
 *
 * The window is created on the main thread, which GLFW requires, and the context is made current on
 * the draw thread by its first [acquireFrame]. LWJGL keeps the functions of a context per thread, so
 * no GL call can be made before that, nor from any other thread.
 */
class GLContext private constructor(val window: GLWindow) : GraphicsContext {

    @Volatile
    private var isCurrent = false

    /**
     * Called on the draw thread the first time the context is current, before anything is drawn.
     */
    var onCurrent: (() -> Unit)? = null

    /**
     * Called on the draw thread after a frame was drawn, right before it's presented. Used to read
     * the frame back, e.g. for screenshots.
     */
    @Volatile
    var onBeforePresent: (() -> Unit)? = null

    override var version = "OpenGL"
        private set

    /**
     * Whether presenting waits for the display's refresh, which caps the frame rate to it. Off, the
     * engine draws as fast as it can. Can be changed at any time, it applies from the next frame.
     */
    @Volatile
    var vsync = false

    // The swap interval set on the context, -1 until it's current.
    private var swapInterval = -1


    override fun acquireFrame(): Boolean {
        if (!isCurrent) {
            glfwMakeContextCurrent(window.handle)
            GL.createCapabilities()

            // The core profile draws nothing without a vertex array bound. One is enough, the
            // attribute pointers are set for every draw anyway, as with OpenGL ES.
            glBindVertexArray(glGenVertexArrays())

            val glVersion: String? = glGetString(GL_VERSION)
            version = if (glVersion != null) "OpenGL $glVersion" else "OpenGL"
            isCurrent = true
            onCurrent?.invoke()
        }

        val interval = if (vsync) 1 else 0
        if (interval != swapInterval) {
            glfwSwapInterval(interval)
            swapInterval = interval
        }

        // Minimised, there's nothing to draw to.
        return window.windowWidth > 0 && window.windowHeight > 0
    }

    override fun present() {
        onBeforePresent?.invoke()
        glfwSwapBuffers(window.handle)
    }

    override fun destroy() {
        // Called once the draw worker stopped: the context is released and the window goes with it.
        // The GL objects die with the context.
        glfwDestroyWindow(window.handle)
    }


    companion object {

        /**
         * Creates a window with an OpenGL 3.3 core context, on the main thread. GLFW must be initialised.
         */
        fun create(title: String, width: Int, height: Int): GLContext {
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
            // Required on macOS for a core profile.
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
            glfwWindowHint(GLFW_DEPTH_BITS, 16)
            glfwWindowHint(GLFW_STENCIL_BITS, 8)
            glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

            val handle = glfwCreateWindow(width, height, title, NULL, NULL)
            if (handle == NULL) {
                throw IllegalStateException("Couldn't create a window with an OpenGL 3.3 core context.")
            }

            // Not current on the main thread, the draw thread makes it current.
            glfwMakeContextCurrent(NULL)

            return GLContext(GLWindow(handle))
        }
    }
}
