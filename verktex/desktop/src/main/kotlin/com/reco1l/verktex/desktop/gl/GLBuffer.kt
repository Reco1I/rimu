package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.GraphicsBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import org.lwjgl.opengl.GL33.*

/**
 * The OpenGL implementation of [GraphicsBuffer], a vertex buffer.
 */
class GLBuffer : GraphicsBuffer() {

    private var id = 0

    private var uploadBuffer: IntBuffer = allocate(capacity)


    /**
     * Uploads the written vertex data to the GPU. The buffer is left bound.
     *
     * glBufferData gives the buffer new storage, so the driver keeps the data of draws still in
     * flight and the buffer can be rewritten in the next frame.
     */
    override fun uploadDataToGPU() {
        if (uploadBuffer.capacity() < wordCount) {
            uploadBuffer = allocate(capacity)
        }

        uploadBuffer.clear()
        uploadBuffer.put(words, 0, wordCount)
        uploadBuffer.flip()

        glBindBuffer(GL_ARRAY_BUFFER, id)
        glBufferData(GL_ARRAY_BUFFER, uploadBuffer, GL_DYNAMIC_DRAW)
    }

    override fun onBind() {
        glBindBuffer(GL_ARRAY_BUFFER, id)
    }

    override fun onLoad() {
        if (id == 0) {
            id = glGenBuffers()
        }
    }

    override fun onUnload() {
        if (id != 0) {
            glDeleteBuffers(id)
            id = 0
        }
    }

    override fun onContextLost() {
        super.onContextLost()
        // The GL buffer went away with the context, a new one is generated on the next load.
        id = 0
    }


    private companion object {
        fun allocate(bytes: Int): IntBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asIntBuffer()
    }
}
