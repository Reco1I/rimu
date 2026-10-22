package com.reco1l.verktex.android.gles

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import com.reco1l.verktex.Logger
import com.reco1l.verktex.graphics.GraphicsContext

class ESContext(private val window: ESWindow) : GraphicsContext {

    private val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

    private val config: EGLConfig
    private val context: EGLContext

    // The window surface only exists while the Android surface is valid, so it is created lazily on
    // the draw thread instead of when the context is constructed.
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var surfaceGeneration = -1

    override val version: String
        get() = EGL14.eglQueryString(display, EGL14.EGL_VERSION)


    init {
        EGL14.eglInitialize(display, null, 0, null, 0)

        val configAttributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val configsAvailable = IntArray(1)

        val eglResult = EGL14.eglChooseConfig(
            display,
            configAttributes, 0,
            configs, 0,
            configs.size,
            configsAvailable, 0
        )

        if (!eglResult) {
            throw IllegalStateException("eglChooseConfig failed")
        }

        if (configsAvailable[0] == 0) {
            throw IllegalStateException("No EGLConfig found for the specified attributes! Device may not support OpenGL ES 3.0.")
        }

        config = configs[0]!!

        EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)

        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
            0
        )

        if (context == EGL14.EGL_NO_CONTEXT) {
            throw IllegalStateException("Failed to create EGL context!")
        }
    }


    /**
     * Makes the context current on the calling thread, (re)creating the window surface if needed.
     *
     * @return `false` if there is no valid surface to render to, in which case the frame should be skipped.
     */
    fun prepare(): Boolean {
        if (!window.hasSurface) {
            releaseSurface()
            return false
        }

        if (surface == EGL14.EGL_NO_SURFACE || surfaceGeneration != window.surfaceGeneration) {
            releaseSurface()

            surfaceGeneration = window.surfaceGeneration
            surface = EGL14.eglCreateWindowSurface(
                display,
                config,
                window.holder.surface,
                intArrayOf(EGL14.EGL_NONE),
                0
            )

            if (surface == EGL14.EGL_NO_SURFACE) {
                Logger.w("ESContext", "Failed to create EGL surface, error: ${EGL14.eglGetError()}")
                return false
            }
        }

        makeCurrent()
        return true
    }

    private fun releaseSurface() {
        if (surface == EGL14.EGL_NO_SURFACE) return

        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, surface)
        surface = EGL14.EGL_NO_SURFACE
    }


    override fun makeCurrent() {
        EGL14.eglMakeCurrent(
            display,
            surface,
            surface,
            context
        )
    }

    override fun swapBuffers() {
        EGL14.eglSwapBuffers(display, surface)
    }

    override fun destroy() {
        releaseSurface()
        EGL14.eglDestroyContext(display, context)
    }
}
