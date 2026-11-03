package com.reco1l.verktex.graphics

abstract class GraphicsBuffer : GraphicsResource() {

    abstract val capacity: Int
    abstract val position: Int

    fun isEmpty() = position == 0

    /**
     * Discards the written data, so the buffer can be filled again.
     */
    abstract fun clear()

    abstract fun requireCapacity(requiredBytes: Int)
    abstract fun putFloat(float: Float)
    abstract fun putByte(byte: Byte)

    /**
     * Puts 4 bytes stored in memory from the lowest to the highest byte of [value], e.g. a
     * [com.reco1l.verktex.data.Color4.packedRGBA].
     */
    open fun putPackedBytes(value: Int) {
        putByte(value.toByte())
        putByte((value shr 8).toByte())
        putByte((value shr 16).toByte())
        putByte((value shr 24).toByte())
    }

    /**
     * Puts a vertex of the default layout: position, texture coordinates and a packed color.
     */
    open fun putVertex(x: Float, y: Float, u: Float, v: Float, packedRGBA: Int) {
        requireCapacity(VERTEX_STRIDE)
        putFloat(x)
        putFloat(y)
        putFloat(u)
        putFloat(v)
        putPackedBytes(packedRGBA)
    }


    companion object {
        /**
         * The size in bytes of a vertex of [VertexLayout.Default].
         */
        const val VERTEX_STRIDE = 20
    }

    abstract fun uploadDataToGPU()
}