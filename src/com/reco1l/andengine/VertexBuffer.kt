package com.reco1l.andengine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

fun createNativeOrderFloatBuffer(capacity: Int): FloatBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(capacity * Float.SIZE_BYTES)
    byteBuffer.order(ByteOrder.nativeOrder())
    return byteBuffer.asFloatBuffer()
}

class VertexBuffer(initialCapacity: Int) {

    private var internalBuffer = createNativeOrderFloatBuffer(initialCapacity * VERTEX_STRIDE)


    /**
     * The number of vertices currently stored in the buffer.
     */
    var stored = 0
        private set


    /**
     * The capacity of the buffer in terms of the number of vertices it can hold.
     */
    val capacity
        get() = internalBuffer.capacity() / VERTEX_STRIDE


    fun ensureCapacity() {
        if (stored + 1 >= capacity) {
            val newCapacity = capacity * 2 * VERTEX_STRIDE

            internalBuffer.position(0)
            internalBuffer = createNativeOrderFloatBuffer(newCapacity).also { newBuffer ->
                newBuffer.put(internalBuffer)
                newBuffer.position(stored * VERTEX_STRIDE)
                newBuffer.limit(newCapacity)
            }
        }
    }


    fun clear() {
        internalBuffer.clear()
        stored = 0
    }

    fun addVertex(
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        u: Float,
        v: Float
    ) {
        ensureCapacity()
        internalBuffer.put(x)
            .put(y)
            .put(r)
            .put(g)
            .put(b)
            .put(a)
            .put(u)
            .put(v)

        stored++
    }

    fun offsetToPosition() {
        internalBuffer.position(POSITION_OFFSET)
    }

    fun offsetToColor() {
        internalBuffer.position(COLOR_OFFSET)
    }

    fun offsetToTexture() {
        internalBuffer.position(TEXTURE_OFFSET)
    }


    fun getInternalBuffer(): FloatBuffer {
        return internalBuffer
    }
}