package com.reco1l.andengine.component

import android.opengl.GLES10

/**
 * Information about how to behave with the depth buffer.
 */
data class DepthInfo(

    /**
     * Whether to test with the depth buffer.
     */
    val test: Boolean = true,

    /**
     * Whether to write to the depth buffer.
     */
    val mask: Boolean = true,

    /**
     * The function to use during depth testing.
     */
    val function: Int,

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DepthInfo) return false
        if (test != other.test) return false
        if (mask != other.mask) return false
        if (function != other.function) return false
        return true
    }

    override fun hashCode(): Int {
        var result = test.hashCode()
        result = 31 * result + mask.hashCode()
        result = 31 * result + function
        return result
    }

    companion object {

        @JvmField
        val Less = DepthInfo(
            test = true,
            mask = true,
            function = GLES10.GL_LESS,
        )

        @JvmField
        val Default = DepthInfo(
            test = true,
            mask = true,
            function = GLES10.GL_LESS
        )

        val None = DepthInfo(
            test = false,
            mask = false,
            function = GLES10.GL_ALWAYS
        )

    }

}