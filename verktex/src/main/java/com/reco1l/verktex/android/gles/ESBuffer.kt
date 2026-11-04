package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.graphics.GraphicsBuffer
import java.nio.ByteBuffer.allocateDirect
import java.nio.ByteOrder
import kotlin.math.max

class ESBuffer : GraphicsBuffer() {

    private var id: Int = 0

    // Written as ints into a plain array, which is much cheaper per value than a direct buffer,
    // and copied into the direct buffer in one go when uploaded. Every vertex layout is made of
    // 4-byte floats and groups of 4 bytes, so the data is always whole ints.
    private var words = IntArray(INITIAL_WORDS)
    private var wordCount = 0

    // Bytes put one by one until they make up a whole int.
    private var pendingBytes = 0
    private var pendingByteCount = 0

    private var uploadBuffer = allocateDirect(INITIAL_WORDS * 4).order(ByteOrder.nativeOrder()).asIntBuffer()


    /**
     * The words written so far, valid up to [writtenWordCount]. Read by backends that rearrange
     * the data on the CPU, e.g. to emulate instancing.
     */
    internal val data: IntArray
        get() = words

    /**
     * The number of whole words written, bytes still pending to make up a word aren't counted.
     */
    internal val writtenWordCount: Int
        get() = wordCount


    override val capacity: Int
        get() = words.size * 4

    override val position: Int
        get() = wordCount * 4 + pendingByteCount


    override fun clear() {
        wordCount = 0
        pendingBytes = 0
        pendingByteCount = 0
    }

    override fun requireCapacity(requiredBytes: Int) {
        val requiredWords = wordCount + (pendingByteCount + requiredBytes + 3) / 4
        if (requiredWords > words.size) {
            words = words.copyOf(max(words.size * 2, requiredWords))
        }
    }


    override fun putByte(byte: Byte) {
        val shift = if (IS_LITTLE_ENDIAN) pendingByteCount * 8 else (3 - pendingByteCount) * 8
        pendingBytes = pendingBytes or ((byte.toInt() and 0xFF) shl shift)

        if (++pendingByteCount == 4) {
            requireCapacity(0)
            words[wordCount++] = pendingBytes
            pendingBytes = 0
            pendingByteCount = 0
        }
    }

    override fun putFloat(float: Float) {
        requireCapacity(4)
        words[wordCount++] = java.lang.Float.floatToRawIntBits(float)
    }

    override fun putPackedBytes(value: Int) {
        requireCapacity(4)
        words[wordCount++] = if (IS_LITTLE_ENDIAN) value else Integer.reverseBytes(value)
    }

    override fun putVertex(x: Float, y: Float, u: Float, v: Float, packedRGBA: Int) {
        if (wordCount + 5 > words.size) {
            requireCapacity(VERTEX_STRIDE)
        }

        val words = words
        var i = wordCount
        words[i++] = java.lang.Float.floatToRawIntBits(x)
        words[i++] = java.lang.Float.floatToRawIntBits(y)
        words[i++] = java.lang.Float.floatToRawIntBits(u)
        words[i++] = java.lang.Float.floatToRawIntBits(v)
        words[i++] = if (IS_LITTLE_ENDIAN) packedRGBA else Integer.reverseBytes(packedRGBA)
        wordCount = i
    }

    /**
     * Uploads the written vertex data to the GPU. The buffer is left bound.
     */
    override fun uploadDataToGPU() {
        if (uploadBuffer.capacity() < wordCount) {
            uploadBuffer = allocateDirect(words.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        }

        uploadBuffer.clear()
        uploadBuffer.put(words, 0, wordCount)
        uploadBuffer.flip()

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, wordCount * 4, uploadBuffer, GLES20.GL_DYNAMIC_DRAW)
    }

    override fun onBind() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
    }

    override fun onLoad() {
        if (id == 0) {
            id = GLESHelper.glGenBuffer()
        }
    }

    override fun onUnload() {
        if (id != 0) {
            GLESHelper.glDeleteBuffers(id)
            id = 0
        }
    }

    override fun onContextLost() {
        super.onContextLost()
        // The GL buffer went away with the context, a new one is generated on the next load.
        id = 0
    }


    private companion object {
        const val INITIAL_WORDS = 256

        val IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
    }
}
