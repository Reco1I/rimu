package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.GraphicsShader

/**
 * The OpenGL ES implementation of [GraphicsShader]. Each backend can adapt the sources and the
 * program before it's linked.
 */
abstract class ESShader(
    override val source: Source
) : GraphicsShader() {


    private var programId: Int = 0
    private var uniformLocations = HashMap<String, Int>()


    override fun onBind() {
        if (programId == 0) {
            throw IllegalStateException("Shader program is not loaded.")
        }

        GLES20.glUseProgram(programId)
    }

    override fun onLoad() {
        if (programId != 0) {
            unload()
        }

        val vertexId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexId, vertexSource())
        GLES20.glCompileShader(vertexId)

        val vertexStatus = GLESHelper.glGetShaderiv(vertexId, GLES20.GL_COMPILE_STATUS)
        if (vertexStatus == 0) {
            val log = GLES20.glGetShaderInfoLog(vertexId)
            throw IllegalStateException("Vertex shader compilation failed:\n$log")
        }

        val fragmentId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentId, fragmentSource())
        GLES20.glCompileShader(fragmentId)

        val fragmentStatus = GLESHelper.glGetShaderiv(fragmentId, GLES20.GL_COMPILE_STATUS)
        if (fragmentStatus == 0) {
            val log = GLES20.glGetShaderInfoLog(fragmentId)
            throw IllegalStateException("Fragment shader compilation failed:\n$log")
        }

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexId)
        GLES20.glAttachShader(programId, fragmentId)

        beforeLink(programId)

        GLES20.glLinkProgram(programId)

        val linkStatus = GLESHelper.glGetProgramiv(programId, GLES20.GL_LINK_STATUS)
        if (linkStatus == 0) {
            val log = GLES20.glGetProgramInfoLog(programId)
            throw IllegalStateException("Shader program linking failed:\n$log")
        }

        GLES20.glDeleteShader(vertexId)
        GLES20.glDeleteShader(fragmentId)

        uniformLocations.clear()
        appliedUniforms.clear()

        val uniformCount = GLESHelper.glGetProgramiv(programId, GLES20.GL_ACTIVE_UNIFORMS)

        for (i in 0 until uniformCount) {
            val uniformInfo = GLESHelper.glGetActiveUniform(programId, i)
            uniformLocations[uniformInfo.name] = GLES20.glGetUniformLocation(programId, uniformInfo.name)
        }
    }

    /**
     * The vertex shader source to compile.
     */
    protected open fun vertexSource(): String = source.vertex

    /**
     * The fragment shader source to compile.
     */
    protected open fun fragmentSource(): String = source.fragment

    /**
     * Called with the program once both shaders are attached, right before it's linked.
     */
    protected open fun beforeLink(program: Int) = Unit

    override fun onUnload() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }

        uniformLocations.clear()
        appliedUniforms.clear()
    }


    // The values the program holds, uniforms keep them between draws. Arrays may be changed in
    // place, so they're always applied.
    private val appliedUniforms = HashMap<String, Any>()

    override fun applyUniforms(uniformSet: UniformSet) {
        for ((name, value) in uniformSet.entries) {
            applyUniform(name, value)
        }
    }

    /**
     * Sets a uniform of the program, unless it already holds the value.
     */
    fun applyUniform(name: String, value: Any) {
        val location = uniformLocations[name] ?: return

        if (value !is FloatArray && value !is IntArray) {
            if (appliedUniforms[name] == value) return
            appliedUniforms[name] = value
        }

        when (value) {
                is Float -> GLES20.glUniform1f(location, value)
                is Int -> GLES20.glUniform1i(location, value)
                is Vec2 -> GLES20.glUniform2f(location, value.x, value.y)
                is Vec4 -> GLES20.glUniform4f(location, value.x, value.y, value.z, value.w)
                is Color4 -> GLES20.glUniform4f(location, value.red, value.green, value.blue, value.alpha)
                is FloatArray -> {
                    when (value.size) {
                        2 -> GLES20.glUniform2fv(location, 1, value, 0)
                        3 -> GLES20.glUniform3fv(location, 1, value, 0)
                        4 -> GLES20.glUniform4fv(location, 1, value, 0)
                        16 -> GLES20.glUniformMatrix4fv(location, 1, false, value, 0)
                        else -> throw IllegalArgumentException("Unsupported float array size: ${value.size}")
                    }
                }
                is IntArray -> {
                    when (value.size) {
                        2 -> GLES20.glUniform2iv(location, 1, value, 0)
                        3 -> GLES20.glUniform3iv(location, 1, value, 0)
                        4 -> GLES20.glUniform4iv(location, 1, value, 0)
                        else -> throw IllegalArgumentException("Unsupported int array size: ${value.size}")
                    }
                }
                else -> throw IllegalArgumentException("Unsupported uniform type: ${value::class.java}")
            }
    }
}