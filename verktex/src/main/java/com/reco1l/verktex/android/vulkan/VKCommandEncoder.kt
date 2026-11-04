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
     * Draws with [pipeline] within the scissor, with the uniform blocks at the given ring offsets.
     *
     * The vertices start at [vertexOffset] in the ring. When [indexed], [count] quads of four vertices
     * are drawn through the quad indices, otherwise [count] vertices, [instanceCount] times when there
     * is per-instance data at [instanceOffset]. A [texture] of 0 binds a white texture.
     */
    fun draw(
        pipeline: Long,
        scissorX: Int, scissorY: Int, scissorWidth: Int, scissorHeight: Int,
        vertexOffset: Int, count: Int,
        vertexUniformsOffset: Int, fragmentUniformsOffset: Int,
        texture: Long = 0L,
        instanceOffset: Int = 0, instanceCount: Int = 0,
        indexed: Boolean = false,
    ) {
        ensureRemaining(1 + DRAW_INTS)
        ints.put(OP_DRAW)
        putLong(pipeline)
        ints.put(scissorX)
        ints.put(scissorY)
        ints.put(scissorWidth)
        ints.put(scissorHeight)
        ints.put(vertexOffset)
        ints.put(count)
        ints.put(vertexUniformsOffset)
        ints.put(fragmentUniformsOffset)
        putLong(texture)
        ints.put(instanceOffset)
        ints.put(instanceCount)
        ints.put(if (indexed) 1 else 0)
    }

    /**
     * Clears the depth buffer.
     */
    fun clearDepth() {
        ensureRemaining(1)
        ints.put(OP_CLEAR_DEPTH)
    }

    /**
     * Copies what was drawn so far into [texture], top-left origin.
     */
    fun capture(texture: Long) {
        ensureRemaining(1 + CAPTURE_INTS)
        ints.put(OP_CAPTURE)
        putLong(texture)
    }

    /**
     * The ints written this frame, for tests.
     */
    fun toIntArray(): IntArray {
        val copy = IntArray(size)
        ints.duplicate().apply { flip() }.get(copy)
        return copy
    }

    private fun putLong(value: Long) {
        ints.put(value.toInt())
        ints.put((value ushr 32).toInt())
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
        const val OP_CLEAR_DEPTH = 2
        const val OP_CAPTURE = 3

        /**
         * The ints following each opcode.
         */
        const val DRAW_INTS = 15
        const val CAPTURE_INTS = 2

        private const val INITIAL_INTS = 4096

        private fun allocate(ints: Int) = ByteBuffer.allocateDirect(ints * 4).order(ByteOrder.nativeOrder())
    }
}
