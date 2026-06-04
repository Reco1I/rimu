package com.reco1l.andengine

import com.reco1l.framework.Color4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class VertexBuffer(initialCapacity: Int) {

    private var byteBuffer = ByteBuffer
        .allocateDirect(initialCapacity * MAX_VERTEX_STRIDE)
        .order(ByteOrder.nativeOrder())

    private var indexBuffer = ByteBuffer
        .allocateDirect(initialCapacity * Short.SIZE_BYTES) // 2 bytes per index (short)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()


    /**
     * The number of vertices currently stored in the buffer.
     */
    var vertexCount = 0
        private set

    /**
     * The current vertex stride in bytes.
     */
    var stride = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES
        private set

    /**
     * Indicates whether texture coordinates are included in the vertex data. Setting this to true will
     */
    var useTextures
        get() = stride == MAX_VERTEX_STRIDE
        set(value) {
            stride = if (value) MAX_VERTEX_STRIDE else POSITION_SIZE_BYTES + COLOR_SIZE_BYTES
        }

    /**
     * The capacity of the buffer in terms of the number of vertices it can hold.
     */
    val vertexCapacity
        get() = byteBuffer.capacity() / MAX_VERTEX_STRIDE


    fun ensureCapacity() {
        if (vertexCount + 1 >= vertexCapacity) {
            byteBuffer = byteBuffer.grow(vertexCapacity * MAX_VERTEX_STRIDE * 2)
        }

        if (indexBuffer.capacity() < vertexCapacity * Short.SIZE_BYTES) {
            indexBuffer = indexBuffer.grow(vertexCapacity * Short.SIZE_BYTES * 2)
        }
    }

    private  fun ByteBuffer.grow(newCapacity: Int): ByteBuffer {
        position(0)

        val buffer = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder())
        buffer.put(this)
        buffer.position(position())
        buffer.limit(newCapacity)

        return buffer
    }

    private fun ShortBuffer.grow(newCapacity: Int): ShortBuffer {
        position(0)

        val buffer = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder()).asShortBuffer()
        buffer.put(this)
        buffer.position(position())
        buffer.limit(newCapacity / Short.SIZE_BYTES)

        return buffer
    }


    fun clear() {
        byteBuffer.clear()
        vertexCount = 0
    }

    fun addVertex(
        x: Float,
        y: Float,
        color: Color4,
        u: Float,
        v: Float
    ) {
        ensureCapacity()

        byteBuffer
            .putFloat(x)
            .putFloat(y)
            .put(color.redInt.toByte())
            .put(color.greenInt.toByte())
            .put(color.blueInt.toByte())
            .put(color.alphaInt.toByte())

        if (useTextures) {
            byteBuffer
                .putFloat(u)
                .putFloat(v)
        }

        vertexCount++
    }


    fun fromZero(): ByteBuffer {
        byteBuffer.position(0)
        return byteBuffer
    }

    fun forPosition(): ByteBuffer {
        byteBuffer.position(POSITION_OFFSET_BYTES)
        return byteBuffer
    }

    fun forColor(): ByteBuffer {
        byteBuffer.position(COLOR_OFFSET_BYTES)
        return byteBuffer
    }

    fun forTexture(): ByteBuffer {
        byteBuffer.position(TEXTURE_OFFSET_BYTES)
        return byteBuffer
    }


    companion object {

        const val POSITION_COMPONENTS = 2
        const val POSITION_SIZE_BYTES = POSITION_COMPONENTS * Float.SIZE_BYTES
        const val POSITION_OFFSET_BYTES = 0

        const val COLOR_COMPONENTS = 4
        const val COLOR_SIZE_BYTES = COLOR_COMPONENTS * Byte.SIZE_BYTES
        const val COLOR_OFFSET_BYTES = POSITION_SIZE_BYTES

        const val TEXTURE_COMPONENTS = 2
        const val TEXTURE_SIZE_BYTES = TEXTURE_COMPONENTS * Float.SIZE_BYTES
        const val TEXTURE_OFFSET_BYTES = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES

        const val MAX_VERTEX_STRIDE = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES + TEXTURE_SIZE_BYTES
    }
}