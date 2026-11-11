package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.GraphicsShader
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack

/**
 * The OpenGL implementation of [GraphicsShader], a port of the OpenGL ES one. The GLSL ES 3.00
 * sources are compiled as GLSL 3.30, see [GLSL330Translator].
 */
class GLShader(override val source: Source) : GraphicsShader() {

    private var programId = 0
    private val uniformLocations = HashMap<String, Int>()

    // The values the program holds, uniforms keep them between draws. Arrays may be changed in
    // place, so they're always applied.
    private val appliedUniforms = HashMap<String, Any>()


    override fun onBind() {
        if (programId == 0) {
            throw IllegalStateException("Shader program is not loaded.")
        }
        glUseProgram(programId)
    }

    override fun onLoad() {
        if (programId != 0) {
            unload()
        }

        val vertexId = compile(GL_VERTEX_SHADER, GLSL330Translator.translate(source.vertex), "Vertex")
        val fragmentId = compile(GL_FRAGMENT_SHADER, GLSL330Translator.translate(source.fragment), "Fragment")

        programId = glCreateProgram()
        glAttachShader(programId, vertexId)
        glAttachShader(programId, fragmentId)
        glLinkProgram(programId)

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw IllegalStateException("Shader program linking failed:\n${glGetProgramInfoLog(programId)}")
        }

        glDeleteShader(vertexId)
        glDeleteShader(fragmentId)

        uniformLocations.clear()
        appliedUniforms.clear()

        MemoryStack.stackPush().use { stack ->
            val size = stack.mallocInt(1)
            val type = stack.mallocInt(1)

            for (i in 0 until glGetProgrami(programId, GL_ACTIVE_UNIFORMS)) {
                val name = glGetActiveUniform(programId, i, size, type)
                uniformLocations[name] = glGetUniformLocation(programId, name)
            }
        }
    }

    private fun compile(stage: Int, source: String, stageName: String): Int {
        val id = glCreateShader(stage)
        glShaderSource(id, source)
        glCompileShader(id)

        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            throw IllegalStateException("$stageName shader compilation failed:\n${glGetShaderInfoLog(id)}")
        }
        return id
    }

    override fun onUnload() {
        if (programId != 0) {
            glDeleteProgram(programId)
            programId = 0
        }

        uniformLocations.clear()
        appliedUniforms.clear()
    }

    override fun onContextLost() {
        // The program died with the context.
        programId = 0
        super.onContextLost()
    }


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
            is Float -> glUniform1f(location, value)
            is Int -> glUniform1i(location, value)
            is Vec2 -> glUniform2f(location, value.x, value.y)
            is Vec4 -> glUniform4f(location, value.x, value.y, value.z, value.w)
            is Color4 -> glUniform4f(location, value.red, value.green, value.blue, value.alpha)
            is FloatArray -> when (value.size) {
                2 -> glUniform2fv(location, value)
                3 -> glUniform3fv(location, value)
                4 -> glUniform4fv(location, value)
                16 -> glUniformMatrix4fv(location, false, value)
                else -> throw IllegalArgumentException("Unsupported float array size: ${value.size}")
            }
            is IntArray -> when (value.size) {
                2 -> glUniform2iv(location, value)
                3 -> glUniform3iv(location, value)
                4 -> glUniform4iv(location, value)
                else -> throw IllegalArgumentException("Unsupported int array size: ${value.size}")
            }
            else -> throw IllegalArgumentException("Unsupported uniform type: ${value::class.java}")
        }
    }
}
