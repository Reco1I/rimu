package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * OpenGL ES implementation of the [GraphicsTexture] class.
 */
class ESTexture(
    override val width: Float,
    override val height: Float,
) : GraphicsTexture() {

    private var id = 0


    override fun onBindTexture() {
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, id)
    }

    override fun onCreateTexture() {
        if (id == 0) {
            id = GLES32Helper.glGenTexture()
        }

        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, id)
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGBA, width.toInt(), height.toInt(), 0, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, null)

        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE)
    }

    override fun onContextLost() {
        // The GL name died with the context, a fresh one is generated on the next load.
        id = 0
        super.onContextLost()
    }

    override fun onUnload() {
        if (id != 0) {
            GLES32Helper.glDeleteTexture(id)
            id = 0
        }
    }

}