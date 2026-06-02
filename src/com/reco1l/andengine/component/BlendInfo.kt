package com.reco1l.andengine.component

import javax.microedition.khronos.opengles.*

/**
 * Determines the blending function for the sprite.
 */
data class BlendInfo(

    /**
     * The source blending factor.
     */
    val sourceFactor: Int,

    /**
     * The destination blending factor.
     */
    val destinationFactor: Int

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlendInfo) return false
        if (sourceFactor != other.sourceFactor) return false
        if (destinationFactor != other.destinationFactor) return false
        return true
    }

    override fun hashCode(): Int {
        var result = sourceFactor
        result = 31 * result + destinationFactor
        return result
    }

    companion object {

        val None = BlendInfo(
            GL10.GL_ONE,
            GL10.GL_ZERO
        )

        val Mixture = BlendInfo(
            GL10.GL_SRC_ALPHA,
            GL10.GL_ONE_MINUS_SRC_ALPHA
        )

        val Additive = BlendInfo(
            GL10.GL_SRC_ALPHA,
            GL10.GL_ONE,
        )

        val PreMultiply = BlendInfo(
            GL10.GL_ONE,
            GL10.GL_ONE_MINUS_SRC_ALPHA
        )

        val Inherit = BlendInfo(-1, -1)

    }
}