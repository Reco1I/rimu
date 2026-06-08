package com.reco1l.andengine

import com.reco1l.framework.Color4
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VertexBuffer(initialCapacity: Int) {

    private var byteBuffer = ByteBuffer
        .allocateDirect(initialCapacity * VERTEX_STRIDE)
        .order(ByteOrder.nativeOrder())


    /**
     * The number of vertices currently stored in the buffer.
     */
    var vertexCount = 0
        private set

    /**
     * The capacity of the buffer in terms of the number of vertices it can hold.
     */
    val vertexCapacity
        get() = byteBuffer.capacity() / VERTEX_STRIDE


    fun growIfNeeded() {
        if (vertexCount + 1 < vertexCapacity) return

        val capacity = vertexCapacity * 2
        val position = byteBuffer.position()

        byteBuffer.position(0)

        val newBuffer = ByteBuffer.allocateDirect(capacity * VERTEX_STRIDE).order(ByteOrder.nativeOrder())
        newBuffer.put(byteBuffer)
        newBuffer.position(position)

        byteBuffer = newBuffer
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
        growIfNeeded()

        byteBuffer
            .putFloat(x)
            .putFloat(y)
            .put(color.redInt.toByte())
            .put(color.greenInt.toByte())
            .put(color.blueInt.toByte())
            .put(color.alphaInt.toByte())
            .putFloat(u)
            .putFloat(v)

        vertexCount++
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

        const val VERTEX_STRIDE = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES + TEXTURE_SIZE_BYTES
    }
}