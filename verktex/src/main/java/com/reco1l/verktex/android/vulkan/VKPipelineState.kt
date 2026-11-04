package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.graphics.BlendFactor
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.VertexLayout

/**
 * The fixed-function state a Vulkan pipeline bakes in, which a draw command sets piecemeal on OpenGL
 * ES. Two draws with equal keys share a pipeline, see [VKRenderer].
 *
 * Layouts are compared by identity, as commands are: the engine shares its layout instances.
 */
data class VKPipelineKey(
    val shader: VKShader,
    val vertexLayout: VertexLayout,
    val instanceLayout: VertexLayout?,
    val blendMode: BlendMode,
) {
    override fun equals(other: Any?): Boolean {
        return other is VKPipelineKey &&
            other.shader === shader &&
            other.vertexLayout === vertexLayout &&
            other.instanceLayout === instanceLayout &&
            other.blendMode == blendMode
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(shader)
        result = 31 * result + System.identityHashCode(vertexLayout)
        result = 31 * result + System.identityHashCode(instanceLayout)
        result = 31 * result + blendMode.hashCode()
        return result
    }
}

object VKPipelineState {

    /**
     * The vertex input of a pipeline, as `VKNative.createPipeline` takes it: binding 0 advances per
     * vertex with [vertexLayout], binding 1 per instance with [instanceLayout] when there's one.
     */
    fun vertexInput(vertexLayout: VertexLayout, instanceLayout: VertexLayout?): IntArray {
        val layouts = listOfNotNull(vertexLayout, instanceLayout)
        val attributeCount = layouts.sumOf { it.attributes.size }
        val input = IntArray(1 + layouts.size * 2 + 1 + attributeCount * 4)

        var i = 0
        input[i++] = layouts.size
        layouts.forEachIndexed { binding, layout ->
            input[i++] = layout.stride
            input[i++] = binding
        }

        input[i++] = attributeCount
        layouts.forEachIndexed { binding, layout ->
            layout.attributes.forEachIndexed { index, attribute ->
                input[i++] = attribute.location
                input[i++] = binding
                input[i++] = format(attribute)
                input[i++] = layout.offsets[index]
            }
        }
        return input
    }

    /**
     * The VkFormat an attribute is read with.
     */
    fun format(attribute: VertexLayout.Attribute): Int = when (attribute.type) {
        VertexLayout.Type.Float -> when (attribute.size) {
            1 -> 100 // VK_FORMAT_R32_SFLOAT
            2 -> 103 // VK_FORMAT_R32G32_SFLOAT
            3 -> 106 // VK_FORMAT_R32G32B32_SFLOAT
            4 -> 109 // VK_FORMAT_R32G32B32A32_SFLOAT
            else -> throw IllegalArgumentException("Unsupported attribute size ${attribute.size}")
        }
        VertexLayout.Type.NormalizedByte -> when (attribute.size) {
            1 -> 9   // VK_FORMAT_R8_UNORM
            2 -> 16  // VK_FORMAT_R8G8_UNORM
            3 -> 23  // VK_FORMAT_R8G8B8_UNORM
            4 -> 37  // VK_FORMAT_R8G8B8A8_UNORM
            else -> throw IllegalArgumentException("Unsupported attribute size ${attribute.size}")
        }
    }

    /**
     * The VkBlendFactors of [blendMode]: source and destination RGB, then source and destination alpha.
     */
    fun blendFactors(blendMode: BlendMode) = intArrayOf(
        factor(blendMode.sourceRGB),
        factor(blendMode.destinationRGB),
        factor(blendMode.sourceAlpha),
        factor(blendMode.destinationAlpha),
    )

    fun factor(factor: BlendFactor): Int = when (factor) {
        BlendFactor.Zero -> 0
        BlendFactor.One -> 1
        BlendFactor.SrcColor -> 2
        BlendFactor.OneMinusSrcColor -> 3
        BlendFactor.DstColor -> 4
        BlendFactor.OneMinusDstColor -> 5
        BlendFactor.SrcAlpha -> 6
        BlendFactor.OneMinusSrcAlpha -> 7
        BlendFactor.DstAlpha -> 8
        BlendFactor.OneMinusDstAlpha -> 9
    }
}
