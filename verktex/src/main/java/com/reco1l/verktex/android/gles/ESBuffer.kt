package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.graphics.GraphicsBuffer
import java.nio.ByteBuffer.allocateDirect
import java.nio.ByteOrder
import kotlin.math.max

class ESBuffer : GraphicsBuffer() {

    private var id: Int = 0
    private var data = allocateDirect(0).order(ByteOrder.nativeOrder())


    override val capacity: Int
        get() = data.capacity()

    override val position: Int
        get() = data.position()


    override fun clear() {
        data.clear()
    }

    override fun requireCapacity(requiredBytes: Int) {
        if (data.remaining() >= requiredBytes) {
            return
        }
        data.flip()

        val newCapacity = max(data.capacity() * 2, data.capacity() + requiredBytes)
        val newBuffer = allocateDirect(newCapacity).order(ByteOrder.nativeOrder())
        newBuffer.put(data)

        data = newBuffer
    }


    override fun putByte(byte: Byte) {
        requireCapacity(1)
        data.put(byte)
    }

    override fun putFloat(float: Float) {
        requireCapacity(4)
        data.putFloat(float)
    }

    /**
     * Uploads the written vertex data to the GPU. The buffer is left bound.
     */
    override fun uploadDataToGPU() {
        // glBufferData reads from the buffer's current position, so upload a view starting at 0.
        val written = data.duplicate().order(ByteOrder.nativeOrder())
        written.flip()

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, id)
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, written.limit(), written, GLES32.GL_DYNAMIC_DRAW)
    }

    override fun onBind() {
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, id)
    }

    override fun onLoad() {
        if (id == 0) {
            id = GLES32Helper.glGenBuffer()
        }
    }

    override fun onUnload() {
        if (id != 0) {
            GLES32Helper.glDeleteBuffers(id)
            id = 0
        }
    }

    override fun onContextLost() {
        super.onContextLost()
        // The GL buffer went away with the context, a new one is generated on the next load.
        id = 0
    }

}