package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.GraphicsShader

class ESShader(
    override val source: Source
) : GraphicsShader() {


    private var programId: Int = 0
    private var uniformLocations = HashMap<String, Int>()


    override fun onBind() {
        if (programId == 0) {
            throw IllegalStateException("Shader program is not loaded.")
        }

        GLES32.glUseProgram(programId)
    }

    override fun onLoad() {
        if (programId != 0) {
            unload()
        }

        val vertexId = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER)
        GLES32.glShaderSource(vertexId, source.vertex)
        GLES32.glCompileShader(vertexId)

        val vertexStatus = GLES32Helper.glGetShaderiv(vertexId, GLES32.GL_COMPILE_STATUS)
        if (vertexStatus == 0) {
            val log = GLES32.glGetShaderInfoLog(vertexId)
            throw IllegalStateException("Vertex shader compilation failed:\n$log")
        }

        val fragmentId = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER)
        GLES32.glShaderSource(fragmentId, source.fragment)
        GLES32.glCompileShader(fragmentId)

        val fragmentStatus = GLES32Helper.glGetShaderiv(fragmentId, GLES32.GL_COMPILE_STATUS)
        if (fragmentStatus == 0) {
            val log = GLES32.glGetShaderInfoLog(fragmentId)
            throw IllegalStateException("Fragment shader compilation failed:\n$log")
        }

        programId = GLES32.glCreateProgram()
        GLES32.glAttachShader(programId, vertexId)
        GLES32.glAttachShader(programId, fragmentId)

        GLES32.glLinkProgram(programId)

        val linkStatus = GLES32Helper.glGetProgramiv(programId, GLES32.GL_LINK_STATUS)
        if (linkStatus == 0) {
            val log = GLES32.glGetProgramInfoLog(programId)
            throw IllegalStateException("Shader program linking failed:\n$log")
        }

        GLES32.glDeleteShader(vertexId)
        GLES32.glDeleteShader(fragmentId)

        uniformLocations.clear()

        val uniformCount = GLES32Helper.glGetProgramiv(programId, GLES32.GL_ACTIVE_UNIFORMS)

        for (i in 0 until uniformCount) {
            val uniformInfo = GLES32Helper.glGetActiveUniform(programId, i)
            uniformLocations[uniformInfo.name] = GLES32.glGetUniformLocation(programId, uniformInfo.name)
        }
    }

    override fun onUnload() {
        if (programId != 0) {
            GLES32.glDeleteProgram(programId)
            programId = 0
        }

        uniformLocations.clear()
    }


    override fun applyUniforms(uniformSet: UniformSet) {
        for ((name, value) in uniformSet.entries) {
            val location = uniformLocations[name] ?: continue

            when (value) {
                is Float -> GLES32.glUniform1f(location, value)
                is Int -> GLES32.glUniform1i(location, value)
                is Vec2 -> GLES32.glUniform2f(location, value.x, value.y)
                is Vec4 -> GLES32.glUniform4f(location, value.x, value.y, value.z, value.w)
                is Color4 -> GLES32.glUniform4f(location, value.red, value.green, value.blue, value.alpha)
                is FloatArray -> {
                    when (value.size) {
                        2 -> GLES32.glUniform2fv(location, 1, value, 0)
                        3 -> GLES32.glUniform3fv(location, 1, value, 0)
                        4 -> GLES32.glUniform4fv(location, 1, value, 0)
                        else -> throw IllegalArgumentException("Unsupported float array size: ${value.size}")
                    }
                }
                is IntArray -> {
                    when (value.size) {
                        2 -> GLES32.glUniform2iv(location, 1, value, 0)
                        3 -> GLES32.glUniform3iv(location, 1, value, 0)
                        4 -> GLES32.glUniform4iv(location, 1, value, 0)
                        else -> throw IllegalArgumentException("Unsupported int array size: ${value.size}")
                    }
                }
                else -> throw IllegalArgumentException("Unsupported uniform type: ${value::class.java}")
            }
        }
    }
}