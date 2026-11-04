package com.reco1l.verktex.android.gles

import android.opengl.GLES20

/**
 * A helper class for OpenGL ES functions (2.0 and up) that require fetching values into arrays.
 */
object GLESHelper {

    private val intFetcher = IntArray(3)
    private val byteFetcher = ByteArray(256)

    fun glGetProgramiv(program: Int, pname: Int): Int {
        intFetcher[0] = 0
        GLES20.glGetProgramiv(program, pname, intFetcher, 0)
        return intFetcher[0]
    }

    fun glGetActiveUniform(program: Int, index: Int): GlGetActiveUniformResult {
        intFetcher.fill(0)
        byteFetcher.fill(0)

        GLES20.glGetActiveUniform(
            program,
            index,
            byteFetcher.size,
            intFetcher, 0,
            intFetcher, 1,
            intFetcher, 2,
            byteFetcher, 0
        )

        val name = String(byteFetcher, 0, intFetcher[0])
        val size = intFetcher[1]
        val type = intFetcher[2]

        return GlGetActiveUniformResult(name, size, type)
    }

    data class GlGetActiveUniformResult(
        val name: String,
        val size: Int,
        val type: Int
    )

    fun glGetShaderiv(shader: Int, pname: Int): Int {
        intFetcher[0] = 0
        GLES20.glGetShaderiv(shader, pname, intFetcher, 0)
        return intFetcher[0]
    }

    fun glGetIntegerv(pname: Int): Int {
        intFetcher[0] = 0
        GLES20.glGetIntegerv(pname, intFetcher, 0)
        return intFetcher[0]
    }

    fun glGenTexture(): Int {
        intFetcher[0] = 0
        GLES20.glGenTextures(1, intFetcher, 0)
        return intFetcher[0]
    }

    fun glDeleteTexture(textureId: Int) {
        intFetcher[0] = textureId
        GLES20.glDeleteTextures(1, intFetcher, 0)
    }

    fun glGenBuffer(): Int {
        intFetcher[0] = 0
        GLES20.glGenBuffers(1, intFetcher, 0)
        return intFetcher[0]
    }

    fun glDeleteBuffers(bufferId: Int) {
        intFetcher[0] = bufferId
        GLES20.glDeleteBuffers(1, intFetcher, 0)
    }
}