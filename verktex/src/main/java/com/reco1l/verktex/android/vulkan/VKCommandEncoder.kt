package com.reco1l.verktex.android.vulkan

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * The frame's command stream, ints in a direct buffer read by the native side in one call (see
 * `Opcode` in vk_context.h). It grows as needed and is reused every frame.
 */
class VKCommandEncoder {

    var buffer: ByteBuffer = allocate(INITIAL_INTS)
        private set

    private var ints: IntBuffer = buffer.asIntBuffer()

    /**
     * The number of ints written this frame.
     */
    val size: Int
        get() = ints.position()

    fun reset() {
        ints.clear()
    }

    /**
     * Draws [vertexCount] vertices from [vertexOffset] in the ring with [pipeline], within the scissor,
     * with the uniform blocks at the given ring offsets.
     */
    fun draw(
        pipeline: Long,
        scissorX: Int, scissorY: Int, scissorWidth: Int, scissorHeight: Int,
        vertexOffset: Int, vertexCount: Int,
        vertexUniformsOffset: Int, fragmentUniformsOffset: Int,
    ) {
        ensureRemaining(DRAW_INTS)
        ints.put(OP_DRAW)
        ints.put(pipeline.toInt())
        ints.put((pipeline ushr 32).toInt())
        ints.put(scissorX)
        ints.put(scissorY)
        ints.put(scissorWidth)
        ints.put(scissorHeight)
        ints.put(vertexOffset)
        ints.put(vertexCount)
        ints.put(vertexUniformsOffset)
        ints.put(fragmentUniformsOffset)
    }

    /**
     * The ints written this frame, for tests.
     */
    fun toIntArray(): IntArray {
        val copy = IntArray(size)
        ints.duplicate().apply { flip() }.get(copy)
        return copy
    }

    private fun ensureRemaining(count: Int) {
        if (ints.remaining() >= count) return

        val written = toIntArray()
        buffer = allocate(maxOf(ints.capacity() * 2, written.size + count))
        ints = buffer.asIntBuffer()
        ints.put(written)
    }

    companion object {
        const val OP_DRAW = 1
        const val DRAW_INTS = 11

        private const val INITIAL_INTS = 4096

        private fun allocate(ints: Int) = ByteBuffer.allocateDirect(ints * 4).order(ByteOrder.nativeOrder())
    }
}
