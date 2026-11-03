package com.reco1l.verktex.graphics

/**
 * Describes how the data of a vertex buffer maps to the attributes of a shader.
 */
class VertexLayout(val attributes: List<Attribute>) {

    /**
     * The size in bytes of the data for one vertex (or one instance).
     */
    val stride = attributes.sumOf { it.sizeInBytes }

    /**
     * The attribute locations this layout uses, one bit per location.
     */
    val locationMask = attributes.fold(0) { mask, attribute -> mask or (1 shl attribute.location) }

    /**
     * The offset in bytes of each attribute, in the same order as [attributes].
     */
    val offsets = IntArray(attributes.size).also { offsets ->
        var offset = 0

        attributes.forEachIndexed { index, attribute ->
            offsets[index] = offset
            offset += attribute.sizeInBytes
        }
    }


    /**
     * A shader attribute.
     */
    data class Attribute(

        /**
         * The location of the attribute in the shader.
         */
        val location: Int,

        /**
         * The number of components of the attribute.
         */
        val size: Int,

        /**
         * The type of each component.
         */
        val type: Type = Type.Float,

        /**
         * Whether the attribute advances once per instance instead of once per vertex.
         */
        val isPerInstance: Boolean = false
    ) {
        val sizeInBytes
            get() = size * type.sizeInBytes
    }

    /**
     * The type of the components of an attribute.
     */
    enum class Type(val sizeInBytes: Int) {
        Float(4),

        /**
         * An unsigned byte that the shader reads as a value between 0 and 1.
         */
        NormalizedByte(1)
    }


    companion object {

        /**
         * The layout used by the built-in shaders: position, UV and color.
         */
        val Default = VertexLayout(listOf(
            Attribute(0, 2),
            Attribute(1, 2),
            Attribute(2, 4, Type.NormalizedByte)
        ))

        /**
         * The layout of the UI shader: [Default] followed by the rounded box parameters and the mode.
         */
        val UI = VertexLayout(listOf(
            Attribute(0, 2),
            Attribute(1, 2),
            Attribute(2, 4, Type.NormalizedByte),
            Attribute(3, 2),
            Attribute(4, 4),
            Attribute(5, 4, Type.NormalizedByte),
            Attribute(6, 4, Type.NormalizedByte),
            Attribute(7, 1)
        ))
    }
}
