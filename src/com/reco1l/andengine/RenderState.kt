package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import javax.microedition.khronos.opengles.GL10

data class RenderState(

    /**
     * The float buffer holding the vertex data for rendering.
     */
    val buffer: VertexBuffer = VertexBuffer(32),

    /**
     * The texture atlas to be used for rendering. If null, texturing will be disabled.
     */
    val texture: ITexture? = null,

    /**
     * The primitive type to be used for rendering. Defaults to GL_TRIANGLES.
     */
    val primitiveType: Int = GL10.GL_TRIANGLES,

    val blendFunctionSource: Int = GL10.GL_SRC_ALPHA,

    val blendFunctionDestination: Int = GL10.GL_ONE_MINUS_SRC_ALPHA,

    val depthTestingEnabled: Boolean = false,

    val depthMask: Boolean = false,

    val depthFunction: Int = GL10.GL_LEQUAL,

    /**
     * The scissor rectangle to be used for rendering. If null, scissoring will be disabled. The
     * rectangle is defined as a Vec4 where x and y represent the lower-left corner of the rectangle,
     * and z and w represent the width and height, respectively.
     */
    val scissor: Vec4? = null,
) {

    override fun equals(other: Any?): Boolean {
        return other is RenderState &&
                texture == other.texture &&
                primitiveType == other.primitiveType &&
                blendFunctionSource == other.blendFunctionSource &&
                blendFunctionDestination == other.blendFunctionDestination &&
                depthTestingEnabled == other.depthTestingEnabled &&
                depthMask == other.depthMask &&
                depthFunction == other.depthFunction &&
                scissor == other.scissor
    }

    override fun hashCode(): Int {
        var result = primitiveType
        result = 31 * result + (texture?.hashCode() ?: 0)
        result = 31 * result + blendFunctionSource
        result = 31 * result + blendFunctionDestination
        result = 31 * result + depthTestingEnabled.hashCode()
        result = 31 * result + depthMask.hashCode()
        result = 31 * result + depthFunction
        result = 31 * result + (scissor?.hashCode() ?: 0)
        result = 31 * result + buffer.hashCode()
        return result
    }
}