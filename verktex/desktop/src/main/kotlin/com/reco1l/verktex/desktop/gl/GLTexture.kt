package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.PixelBuffer
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import org.lwjgl.opengl.GL33.*

/**
 * The OpenGL implementation of [GraphicsTexture], an RGBA8 texture.
 */
class GLTexture(
    override val width: Float,
    override val height: Float,
) : GraphicsTexture() {

    private var id = 0


    override fun onBindTexture() {
        glBindTexture(GL_TEXTURE_2D, id)
    }

    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: PixelBuffer) {
        // Already bound when the pixels are written while binding, not by flushUploads.
        glBindTexture(GL_TEXTURE_2D, id)
        // Rows are tightly packed and 4 bytes per pixel, the default alignment of 4 holds.
        glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
    }

    override fun onCreateTexture() {
        if (id == 0) {
            id = glGenTextures()
        }

        glBindTexture(GL_TEXTURE_2D, id)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.toInt(), height.toInt(), 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    override fun onContextLost() {
        // The GL name died with the context, a fresh one is generated on the next load.
        id = 0
        super.onContextLost()
    }

    override fun onUnload() {
        if (id != 0) {
            glDeleteTextures(id)
            id = 0
        }
    }
}
