package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.graphics.GraphicsBuffer
import java.nio.ByteBuffer.allocateDirect
import java.nio.ByteOrder

class ESBuffer : GraphicsBuffer() {

    private var id: Int = 0

    private var uploadBuffer = allocateDirect(capacity).order(ByteOrder.nativeOrder()).asIntBuffer()


    /**
     * Uploads the written vertex data to the GPU. The buffer is left bound.
     *
     * glBufferData gives the buffer new storage, so the driver keeps the data of draws still in
     * flight and the buffer can be rewritten in the next frame.
     */
    override fun uploadDataToGPU() {
        if (uploadBuffer.capacity() < wordCount) {
            uploadBuffer = allocateDirect(capacity).order(ByteOrder.nativeOrder()).asIntBuffer()
        }

        uploadBuffer.clear()
        uploadBuffer.put(words, 0, wordCount)
        uploadBuffer.flip()

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, wordCount * 4, uploadBuffer, GLES20.GL_DYNAMIC_DRAW)
    }

    override fun onBind() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
    }

    override fun onLoad() {
        if (id == 0) {
            id = GLESHelper.glGenBuffer()
        }
    }

    override fun onUnload() {
        if (id != 0) {
            GLESHelper.glDeleteBuffers(id)
            id = 0
        }
    }

    override fun onContextLost() {
        super.onContextLost()
        // The GL buffer went away with the context, a new one is generated on the next load.
        id = 0
    }
}
