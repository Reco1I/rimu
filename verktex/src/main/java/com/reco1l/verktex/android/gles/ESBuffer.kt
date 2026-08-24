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
        requireCapacity(data.position() + 1)
        data.put(byte)
    }

    override fun putFloat(float: Float) {
        requireCapacity(data.position() + 4)
        data.putFloat(float)
    }

    override fun uploadDataToGPU() {
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, id)
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, capacity, data, GLES32.GL_DYNAMIC_DRAW)
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)
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

}