package com.reco1l.andengine.buffered

import org.anddev.andengine.opengl.util.GLHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

const val VERTEX_2D_SIZE = 2

fun createNativeOrderFloatBuffer(capacity: Int): FloatBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(capacity * Float.SIZE_BYTES)
    byteBuffer.order(ByteOrder.nativeOrder())
    return byteBuffer.asFloatBuffer()
}

/**
 * A helper class that manages a resizable FloatBuffer for rendering. It automatically resizes the
 * buffer when the capacity is exceeded, based on the specified reallocation step.
 */
class ResizableFloatBuffer(initialCapacity: Int = 16) {

    var internalBuffer = createNativeOrderFloatBuffer(initialCapacity * VERTEX_2D_SIZE)
        private set

    var verticesStored = 0
        private set

    val capacity
        get() = internalBuffer.capacity() / VERTEX_2D_SIZE


    private fun grow() {
        val usedFloats = verticesStored * VERTEX_2D_SIZE

        val newCapacity = if (capacity == 0) 1 else capacity * 2
        val newBuffer = createNativeOrderFloatBuffer(newCapacity * VERTEX_2D_SIZE)

        internalBuffer.position(0)
        internalBuffer.limit(usedFloats)

        newBuffer.put(internalBuffer)

        newBuffer.position(usedFloats)
        newBuffer.limit(newBuffer.capacity())

        internalBuffer = newBuffer
    }

    fun begin() {
        internalBuffer.position(0)
        internalBuffer.limit(capacity * VERTEX_2D_SIZE)
        verticesStored = 0
    }

    fun end() {
        internalBuffer.position(0)
        internalBuffer.limit(verticesStored * VERTEX_2D_SIZE)
    }

    fun addVertex(x: Float, y: Float) {
        if (verticesStored + 1 > capacity) {
            grow()
        }

        internalBuffer.put(x)
        internalBuffer.put(y)

        verticesStored++
    }

}

open class BufferRenderer {

    protected val buffer = ResizableFloatBuffer()


    protected inline fun render(gl: GL10, primitiveType: Int = GL10.GL_TRIANGLES, block: ResizableFloatBuffer.() -> Unit) {
        GLHelper.disableTextures(gl)
        GLHelper.disableTexCoordArray(gl)

        buffer.begin()
        buffer.block()
        buffer.end()

        GLHelper.enableVertexArray(gl)
        gl.glVertexPointer(VERTEX_2D_SIZE, GL10.GL_FLOAT, 0, buffer.internalBuffer)

        gl.glDrawArrays(primitiveType, 0, buffer.verticesStored)
    }


    protected fun doRender(gl: GL10, primitiveType: Int = GL10.GL_TRIANGLES) {
        GLHelper.disableTextures(gl)
        GLHelper.disableTexCoordArray(gl)
        GLHelper.enableVertexArray(gl)
        gl.glVertexPointer(VERTEX_2D_SIZE, GL10.GL_FLOAT, 0, buffer.internalBuffer)
        gl.glDrawArrays(primitiveType, 0, buffer.verticesStored)
    }
}