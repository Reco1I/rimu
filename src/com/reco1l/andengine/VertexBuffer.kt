package com.reco1l.andengine

import com.reco1l.framework.Color4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class VertexBuffer(initialCapacity: Int) {

    private var byteBuffer = ByteBuffer
        .allocateDirect(initialCapacity * MAX_VERTEX_STRIDE)
        .order(ByteOrder.nativeOrder())


    /**
     * The number of vertices currently stored in the buffer.
     */
    var vertexCount = 0
        private set

    /**
     * The current vertex stride in bytes.
     */
    var stride = NO_TEXTURES_STRIDE
        private set

    /**
     * Indicates whether texture coordinates are included in the vertex data. Setting this to true will
     */
    var useTextures
        get() = stride == MAX_VERTEX_STRIDE
        set(value) {
            stride = if (value) MAX_VERTEX_STRIDE else NO_TEXTURES_STRIDE
        }

    /**
     * The capacity of the buffer in terms of the number of vertices it can hold.
     */
    val vertexCapacity
        get() = byteBuffer.capacity() / MAX_VERTEX_STRIDE


    fun ensureCapacity() {
        if (vertexCount + 1 < vertexCapacity) return
        byteBuffer = byteBuffer.grow(vertexCapacity.coerceAtLeast(2) * MAX_VERTEX_STRIDE * 2)
    }

    private  fun ByteBuffer.grow(newCapacity: Int): ByteBuffer {
        val oldPosition = position()
        position(0)

        val buffer = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder())
        buffer.put(this)
        buffer.position(oldPosition)
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


    fun setTo(other: VertexBuffer) {
        byteBuffer.clear()
        other.byteBuffer.position(0)
        byteBuffer.put(other.byteBuffer)
        byteBuffer.position(0)

        vertexCount = other.vertexCount
        stride = other.stride
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

        const val NO_TEXTURES_STRIDE = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES
        const val MAX_VERTEX_STRIDE = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES + TEXTURE_SIZE_BYTES
    }
}