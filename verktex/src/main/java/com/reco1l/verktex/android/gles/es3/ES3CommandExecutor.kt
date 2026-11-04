package com.reco1l.verktex.android.gles.es3

import android.opengl.GLES30
import com.reco1l.verktex.android.gles.ESCommandExecutor
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.VertexLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The [ESCommandExecutor] of OpenGL ES 3.0 and up, which draws instances natively and indexes
 * quads with 32-bit indices.
 */
class ES3CommandExecutor : ESCommandExecutor() {

    private val attributeDivisors = IntArray(MAX_ATTRIBUTES)

    // The index buffer shared by every quad command, see DrawCommand.isQuads: two triangles for
    // each quad of four vertices. It grows to the largest command and is made again for a new
    // context.
    private var quadIndexBuffer = 0
    private var quadIndexCapacity = 0
    private val bufferId = IntArray(1)


    override fun resetAttributes() {
        super.resetAttributes()

        for (location in 0 until MAX_ATTRIBUTES) {
            if (attributeDivisors[location] != 0) {
                GLES30.glVertexAttribDivisor(location, 0)
                attributeDivisors[location] = 0
            }
        }
    }

    override fun applyLayout(layout: VertexLayout, baseOffset: Int) {
        super.applyLayout(layout, baseOffset)

        for (attribute in layout.attributes) {
            val location = attribute.location
            val divisor = if (attribute.isPerInstance) 1 else 0

            if (attributeDivisors[location] != divisor) {
                GLES30.glVertexAttribDivisor(location, divisor)
                attributeDivisors[location] = divisor
            }
        }
    }

    override fun drawQuads(command: DrawCommand, quadCount: Int) {
        val buffer = command.buffer!!
        buffer.bind()
        buffer.uploadDataToGPU()
        applyLayout(command.vertexLayout)
        disableAttributesExcept(command.vertexLayout.locationMask)

        bindQuadIndices(quadCount)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, quadCount * 6, GLES30.GL_UNSIGNED_INT, 0)
    }

    override fun drawInstanced(command: DrawCommand, vertexCount: Int) {
        val buffer = command.buffer!!
        val instanceBuffer = command.instanceBuffer!!
        val instanceLayout = command.instanceLayout!!

        buffer.bind()
        buffer.uploadDataToGPU()
        applyLayout(command.vertexLayout)

        instanceBuffer.bind()
        instanceBuffer.uploadDataToGPU()
        applyLayout(instanceLayout)

        disableAttributesExcept(command.vertexLayout.locationMask or instanceLayout.locationMask)

        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, vertexCount, command.instanceCount)
    }

    private fun bindQuadIndices(quadCount: Int) {
        if (quadIndexBuffer == 0 || !GLES30.glIsBuffer(quadIndexBuffer)) {
            GLES30.glGenBuffers(1, bufferId, 0)
            quadIndexBuffer = bufferId[0]
            quadIndexCapacity = 0
        }

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer)

        if (quadIndexCapacity >= quadCount) {
            return
        }

        val capacity = maxOf(quadCount, quadIndexCapacity * 2, MIN_QUAD_INDEX_CAPACITY)
        val indices = ByteBuffer.allocateDirect(capacity * 6 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()

        for (quad in 0 until capacity) {
            val first = quad * 4
            indices.put(first).put(first + 1).put(first + 2)
            indices.put(first).put(first + 2).put(first + 3)
        }
        indices.flip()

        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, capacity * 6 * 4, indices, GLES30.GL_STATIC_DRAW)
        quadIndexCapacity = capacity
    }


    private companion object {
        const val MIN_QUAD_INDEX_CAPACITY = 1024
    }
}
