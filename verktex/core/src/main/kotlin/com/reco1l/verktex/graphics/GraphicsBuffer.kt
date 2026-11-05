package com.reco1l.verktex.graphics

import com.reco1l.verktex.Logger
import kotlin.math.max

/**
 * Vertex data written on the CPU and uploaded by the backend when a draw using it is executed.
 *
 * Lifetime: a draw reads the buffer's data as it is when the frame is executed (in
 * [GraphicsRenderer.end]), so a buffer used by a draw recorded this frame must not be cleared or
 * written again until then. Once the frame was executed it can be rewritten right away for the next
 * one, even while the GPU may still be drawing the previous frame: the backend makes each upload a
 * copy that stays valid until the GPU is done with it (GL orphans the storage, a backend with frames
 * in flight writes it to a per-frame ring).
 */
abstract class GraphicsBuffer : GraphicsResource() {

    // Written as ints into a plain array, which is much cheaper per value than a direct buffer, and
    // copied by the backend in one go when uploaded. Every vertex layout is made of 4-byte floats and
    // groups of 4 bytes, so the data is always whole ints. The words are little-endian, the byte order
    // of every CPU Verktex runs on (ARM and x86 in their usual mode, and WebAssembly).
    private var _words = IntArray(INITIAL_WORDS)
    private var _wordCount = 0

    // Bytes put one by one until they make up a whole int.
    private var pendingBytes = 0
    private var pendingByteCount = 0


    /**
     * The data written so far in native (little-endian) byte order, valid up to [wordCount]. Backends upload it, or
     * read it to rearrange it on the CPU. It must not be modified.
     */
    val words: IntArray
        get() = _words

    /**
     * The number of whole words written, bytes still pending to make up a word aren't counted.
     */
    val wordCount: Int
        get() = _wordCount

    val capacity: Int
        get() = _words.size * 4

    val position: Int
        get() = _wordCount * 4 + pendingByteCount

    fun isEmpty() = position == 0


    /**
     * Whether a draw recorded this frame uses the buffer and wasn't executed yet, set by the renderer.
     */
    internal var isRecorded = false

    /**
     * Discards the written data, so the buffer can be filled again.
     */
    fun clear() {
        if (isRecorded) {
            // The recorded draw would draw what is written next instead, see the lifetime above.
            Logger.w("GraphicsBuffer", "A buffer was cleared while a draw of this frame still uses it.")
        }

        _wordCount = 0
        pendingBytes = 0
        pendingByteCount = 0
    }

    fun requireCapacity(requiredBytes: Int) {
        val requiredWords = _wordCount + (pendingByteCount + requiredBytes + 3) / 4
        if (requiredWords > _words.size) {
            _words = _words.copyOf(max(_words.size * 2, requiredWords))
        }
    }

    fun putByte(byte: Byte) {
        pendingBytes = pendingBytes or ((byte.toInt() and 0xFF) shl (pendingByteCount * 8))

        if (++pendingByteCount == 4) {
            requireCapacity(0)
            _words[_wordCount++] = pendingBytes
            pendingBytes = 0
            pendingByteCount = 0
        }
    }

    fun putFloat(float: Float) {
        requireCapacity(4)
        _words[_wordCount++] = float.toRawBits()
    }

    /**
     * Puts 4 bytes stored in memory from the lowest to the highest byte of [value], e.g. a
     * [com.reco1l.verktex.data.Color4.packedRGBA].
     */
    fun putPackedBytes(value: Int) {
        requireCapacity(4)
        _words[_wordCount++] = value
    }

    /**
     * Puts a vertex of the default layout: position, texture coordinates and a packed color.
     */
    fun putVertex(x: Float, y: Float, u: Float, v: Float, packedRGBA: Int) {
        if (_wordCount + 5 > _words.size) {
            requireCapacity(VERTEX_STRIDE)
        }

        val words = _words
        var i = _wordCount
        words[i++] = x.toRawBits()
        words[i++] = y.toRawBits()
        words[i++] = u.toRawBits()
        words[i++] = v.toRawBits()
        words[i++] = packedRGBA
        _wordCount = i
    }

    /**
     * Uploads the [wordCount] words written to the GPU, for the draw being executed. See the
     * lifetime above for how long the uploaded data must stay valid.
     */
    abstract fun uploadDataToGPU()


    companion object {
        /**
         * The size in bytes of a vertex of [VertexLayout.Default].
         */
        const val VERTEX_STRIDE = 20

        private const val INITIAL_WORDS = 256
    }
}
