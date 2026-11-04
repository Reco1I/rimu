package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Uniform
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.UniformType
import java.nio.ByteBuffer

/**
 * Packs uniform values by name into a std140 block laid out by `GLSL450VulkanTranslator`. A value
 * comes from the command's set, else from the defaults, as on OpenGL ES, and members without either
 * are zero. Values of a type the member can't take are skipped, as GL would reject them.
 */
object VKUniformPacker {

    /**
     * Writes the block of [uniforms], [blockSize] bytes, at [offset] in [target].
     */
    fun pack(
        target: ByteBuffer,
        offset: Int,
        blockSize: Int,
        uniforms: List<Uniform>,
        values: Map<String, Any>,
        defaults: Map<String, Any>,
    ) {
        // Ring memory holds the previous use of the frame, members left unset must not see it.
        var i = 0
        while (i < blockSize) {
            target.putInt(offset + i, 0)
            i += 4
        }

        for (uniform in uniforms) {
            val value = values[uniform.name] ?: defaults[uniform.name] ?: continue
            write(target, offset + uniform.offset, uniform.type, value)
        }
    }

    private fun write(target: ByteBuffer, at: Int, type: UniformType, value: Any) {
        when (type) {
            UniformType.Float -> when (value) {
                is Float -> target.putFloat(at, value)
                is Int -> target.putFloat(at, value.toFloat())
            }

            UniformType.Int -> if (value is Int) target.putInt(at, value)

            UniformType.Vec2 -> when (value) {
                is Vec2 -> {
                    target.putFloat(at, value.x)
                    target.putFloat(at + 4, value.y)
                }
                is FloatArray -> if (value.size == 2) floats(target, at, value)
            }

            UniformType.Vec3 -> if (value is FloatArray && value.size == 3) floats(target, at, value)

            UniformType.Vec4 -> when (value) {
                is Vec4 -> vec4(target, at, value.x, value.y, value.z, value.w)
                is Color4 -> vec4(target, at, value.red, value.green, value.blue, value.alpha)
                is FloatArray -> if (value.size == 4) floats(target, at, value)
            }

            // Column-major, std140 stores each column as a vec4 so the 16 floats are contiguous.
            UniformType.Mat4 -> if (value is FloatArray && value.size == 16) floats(target, at, value)
        }
    }

    private fun vec4(target: ByteBuffer, at: Int, x: Float, y: Float, z: Float, w: Float) {
        target.putFloat(at, x)
        target.putFloat(at + 4, y)
        target.putFloat(at + 8, z)
        target.putFloat(at + 12, w)
    }

    private fun floats(target: ByteBuffer, at: Int, values: FloatArray) {
        for (index in values.indices) {
            target.putFloat(at + index * 4, values[index])
        }
    }
}
