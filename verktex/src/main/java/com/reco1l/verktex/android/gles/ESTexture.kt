package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer

/**
 * OpenGL ES implementation of the [GraphicsTexture] class.
 */
class ESTexture(
    override val width: Float,
    override val height: Float,
) : GraphicsTexture() {

    private var id = 0


    override fun onBindTexture() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
    }

    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: ByteBuffer) {
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, x, y, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
    }

    override fun onCreateTexture() {
        if (id == 0) {
            id = GLESHelper.glGenTexture()
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width.toInt(), height.toInt(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun onContextLost() {
        // The GL name died with the context, a fresh one is generated on the next load.
        id = 0
        super.onContextLost()
    }

    override fun onUnload() {
        if (id != 0) {
            GLESHelper.glDeleteTexture(id)
            id = 0
        }
    }

}