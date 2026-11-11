package com.reco1l.verktex.android.vulkan

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Linear allocation in a frame's ring: the vertex and uniform data of every draw of the frame is
 * written one after the other, and the ring starts over when the frame comes around again, once the
 * GPU is done with it (the native side waits for the frame's fence when acquiring it). See the
 * lifetime in [com.reco1l.verktex.graphics.GraphicsBuffer].
 *
 * [buffer] is the ring's memory (a mapped buffer, or a plain one in tests).
 */
class VKFrameRing(buffer: ByteBuffer) {

    val buffer: ByteBuffer = buffer.order(ByteOrder.nativeOrder())

    private val ints = this.buffer.asIntBuffer()

    val capacity: Int
        get() = buffer.capacity()

    /**
     * The bytes allocated so far this frame.
     */
    var position = 0
        private set

    fun reset() {
        position = 0
    }

    /**
     * Reserves [size] bytes aligned to [alignment] (a power of two), returning their offset, or -1
     * when the ring is full.
     */
    fun allocate(size: Int, alignment: Int): Int {
        val offset = (position + alignment - 1) and (alignment - 1).inv()
        if (offset + size > capacity) {
            return -1
        }
        position = offset + size
        return offset
    }

    /**
     * Copies the first [count] [words] into the ring, returning their offset, or -1 when it's full.
     */
    fun writeWords(words: IntArray, count: Int): Int = writeWords(words, 0, count)

    /**
     * Copies [count] [words] from [start] into the ring, returning their offset, or -1 when it's full.
     */
    fun writeWords(words: IntArray, start: Int, count: Int): Int {
        val offset = allocate(count * 4, 4)
        if (offset >= 0) {
            ints.position(offset / 4)
            ints.put(words, start, count)
        }
        return offset
    }
}
