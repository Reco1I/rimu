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

    abstract fun uploadDataToGPU()
}