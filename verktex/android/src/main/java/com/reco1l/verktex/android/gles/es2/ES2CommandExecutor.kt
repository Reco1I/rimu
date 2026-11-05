package com.reco1l.verktex.android.gles.es2

import android.opengl.GLES20
import com.reco1l.verktex.android.gles.ESCommandExecutor
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.VertexLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * The [ESCommandExecutor] of OpenGL ES 2.0, which has no instancing and only guarantees 16-bit
 * indices:
 *
 * - Quads are drawn in chunks of at most [MAX_QUADS_PER_DRAW], each one pointing the attributes
 *   at its first vertex, so the indices of one shared buffer always fit in 16 bits.
 * - Instanced draws are expanded on the CPU, every vertex of every instance is written with the
 *   instance's attributes appended, and drawn as plain triangles.
 */
class ES2CommandExecutor : ESCommandExecutor() {

    private val bufferId = IntArray(1)

    // The 16-bit index buffer shared by every quad command, two triangles for each quad of four
    // vertices. Made once for each context.
    private var quadIndexBuffer = 0


    override fun drawQuads(command: DrawCommand, quadCount: Int) {
        val buffer = command.buffer!!
        val layout = command.vertexLayout

        buffer.bind()
        buffer.uploadDataToGPU()
        bindQuadIndices()

        var firstQuad = 0
        while (firstQuad < quadCount) {
            val count = min(MAX_QUADS_PER_DRAW, quadCount - firstQuad)

            applyLayout(layout, firstQuad * 4 * layout.stride)
            disableAttributesExcept(layout.locationMask)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, count * 6, GLES20.GL_UNSIGNED_SHORT, 0)

            firstQuad += count
        }
    }

    private fun bindQuadIndices() {
        if (quadIndexBuffer == 0 || !GLES20.glIsBuffer(quadIndexBuffer)) {
            GLES20.glGenBuffers(1, bufferId, 0)
            quadIndexBuffer = bufferId[0]

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer)

            val indices = ByteBuffer.allocateDirect(MAX_QUADS_PER_DRAW * 6 * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            for (quad in 0 until MAX_QUADS_PER_DRAW) {
                val first = quad * 4
                indices.put(first.toShort()).put((first + 1).toShort()).put((first + 2).toShort())
                indices.put(first.toShort()).put((first + 2).toShort()).put((first + 3).toShort())
            }
            indices.flip()

            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, MAX_QUADS_PER_DRAW * 6 * 2, indices, GLES20.GL_STATIC_DRAW)
            return
        }

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer)
    }

    //region Instancing emulation

    // The last combination of layouts expanded, draws repeat the same one (e.g. particles).
    private var lastVertexLayout: VertexLayout? = null
    private var lastInstanceLayout: VertexLayout? = null
    private var expandedLayout: VertexLayout? = null

    private var expandedWords = IntArray(0)
    private var expandedUpload: IntBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asIntBuffer()
    private var expandedBuffer = 0

    override fun drawInstanced(command: DrawCommand, vertexCount: Int) {
        val vertexBuffer = command.buffer!!
        val instanceBuffer = command.instanceBuffer!!
        val vertexLayout = command.vertexLayout
        val instanceLayout = command.instanceLayout!!

        // Every layout is made of 4-byte floats and groups of 4 bytes, so both are whole words.
        val vertexWords = vertexLayout.stride / 4
        val instanceWords = instanceLayout.stride / 4

        val instanceCount = min(command.instanceCount, instanceBuffer.wordCount / max(instanceWords, 1))
        if (vertexCount == 0 || instanceCount == 0) {
            return
        }

        val layout = combinedLayout(vertexLayout, instanceLayout)
        val totalWords = instanceCount * vertexCount * (vertexWords + instanceWords)

        if (expandedWords.size < totalWords) {
            expandedWords = IntArray(max(totalWords, expandedWords.size * 2))
            expandedUpload = ByteBuffer.allocateDirect(expandedWords.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        }

        val words = expandedWords
        val vertexData = vertexBuffer.words
        val instanceData = instanceBuffer.words
        var position = 0

        for (instance in 0 until instanceCount) {
            val instanceOffset = instance * instanceWords

            for (vertex in 0 until vertexCount) {
                System.arraycopy(vertexData, vertex * vertexWords, words, position, vertexWords)
                position += vertexWords
                System.arraycopy(instanceData, instanceOffset, words, position, instanceWords)
                position += instanceWords
            }
        }

        expandedUpload.clear()
        expandedUpload.put(words, 0, position)
        expandedUpload.flip()

        if (expandedBuffer == 0 || !GLES20.glIsBuffer(expandedBuffer)) {
            GLES20.glGenBuffers(1, bufferId, 0)
            expandedBuffer = bufferId[0]
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, expandedBuffer)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, position * 4, expandedUpload, GLES20.GL_STREAM_DRAW)

        applyLayout(layout)
        disableAttributesExcept(layout.locationMask)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, instanceCount * vertexCount)
    }

    /**
     * The layout of a vertex followed by its instance's attributes, all of them per vertex.
     */
    private fun combinedLayout(vertexLayout: VertexLayout, instanceLayout: VertexLayout): VertexLayout {
        val cached = expandedLayout
        if (cached != null && vertexLayout === lastVertexLayout && instanceLayout === lastInstanceLayout) {
            return cached
        }

        val layout = combineLayouts(vertexLayout, instanceLayout)
        lastVertexLayout = vertexLayout
        lastInstanceLayout = instanceLayout
        expandedLayout = layout
        return layout
    }

    //endregion


    companion object {

        /**
         * The most quads a single draw call indexes, so the indices of its last vertex fit in 16 bits.
         */
        const val MAX_QUADS_PER_DRAW = 65536 / 4

        /**
         * The layout of a vertex of [vertexLayout] followed by the attributes of [instanceLayout],
         * every attribute advancing per vertex.
         */
        fun combineLayouts(vertexLayout: VertexLayout, instanceLayout: VertexLayout): VertexLayout {
            return VertexLayout(
                vertexLayout.attributes.map { it.copy(isPerInstance = false) } +
                instanceLayout.attributes.map { it.copy(isPerInstance = false) }
            )
        }
    }
}
