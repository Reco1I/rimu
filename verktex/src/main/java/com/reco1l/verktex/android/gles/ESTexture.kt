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


    override fun onBind() {
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, id)
    }

    override fun onLoad() {
        if (id == 0) {
            id = GLES32Helper.glGenTexture()
        }

    }

    override fun onUnload() {
        if (id != 0) {
            GLES32Helper.glDeleteTexture(id)
            id = 0
        }
    }

}