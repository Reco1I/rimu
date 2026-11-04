package com.reco1l.verktex.android.gles.es2

import android.opengl.GLES20
import com.reco1l.verktex.Logger
import com.reco1l.verktex.android.gles.ESContext
import com.reco1l.verktex.android.gles.ESRenderer
import com.reco1l.verktex.android.AndroidWindow
import com.reco1l.verktex.android.gles.GLESHelper

/**
 * The [ESRenderer] of OpenGL ES 2.0. It has no GPU timer, timer queries are only reachable through
 * extension entry points Android doesn't bind.
 */
class ES2Renderer(
    context: ESContext,
    device: ES2Device,
    window: AndroidWindow,
) : ESRenderer(context, device, window, ES2CommandExecutor()) {

    override fun onContextReady() {
        // OpenGL ES 2.0 only guarantees 8 attributes, the particle shader uses locations up to 8.
        val maxAttributes = GLESHelper.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS)
        if (maxAttributes < REQUIRED_VERTEX_ATTRIBUTES) {
            Logger.w("ES2Renderer", "Only $maxAttributes vertex attributes are supported, shaders using more will fail to draw.")
        }
    }


    private companion object {
        const val REQUIRED_VERTEX_ATTRIBUTES = 9
    }
}
