package com.reco1l.verktex.graphics

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4

/**
 * Represents a shader program that can be used for rendering graphics.
 */
abstract class GraphicsShader : GraphicsResource() {

    /**
     * The source code of the shader program, including the vertex and fragment shader code.
     */
    abstract val source: Source


    /**
     * Applies the given [uniformSet] to the shader program, setting the values of the uniform variables.
     */
    abstract fun applyUniforms(uniformSet: UniformSet)


    /**
     * Represents the source code of a shader program, including the vertex and fragment shader code.
     */
    data class Source(
        /**
         * The source code of the vertex shader.
         */
        val vertex: String,

        /**
         * The source code of the fragment shader.
         */
        val fragment: String,
    )

    /**
     * Represents a set of uniform variables that can be applied to a shader program.
     */
    data class UniformSet(
        /**
         * A map of uniform variable names to their corresponding values. The values can be of any data
         * type that is supported by the using shader program.
         */
        val entries: MutableMap<String, Any> = mutableMapOf()
    ) {

        operator fun set(name: String, value: Float): UniformSet {
            entries[name] = value
            return this
        }

        operator fun set(name: String, value: Vec4): UniformSet {
            entries[name] = value
            return this
        }

        operator fun set(name: String, value: Vec2): UniformSet {
            entries[name] = value
            return this
        }

        operator fun set(name: String, value: Color4): UniformSet {
            entries[name] = value
            return this
        }

    }
}

