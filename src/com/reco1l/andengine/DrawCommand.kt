package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import javax.microedition.khronos.opengles.GL10

data class DrawCommand(
    val cache: DrawCache? = null,
    val buffer: VertexBuffer,
    val texture: ITexture? = DEFAULT_TEXTURE,
    val primitiveType: Int = DEFAULT_PRIMITIVE_TYPE,
    val blendFunctionSource: Int = DEFAULT_BLEND_FUNCTION_SOURCE,
    val blendFunctionDestination: Int = DEFAULT_BLEND_FUNCTION_DESTINATION,
    val depthTestingEnabled: Boolean = DEFAULT_DEPTH_TESTING_ENABLED,
    val depthMask: Boolean = DEFAULT_DEPTH_MASK,
    val depthFunction: Int = DEFAULT_DEPTH_FUNCTION,
    val lineWidth: Float = DEFAULT_LINE_WIDTH,
    val scissor: Vec4? = DEFAULT_SCISSOR,
) {

    fun equals(
        cache: DrawCache?,
        texture: ITexture?,
        primitiveType: Int,
        blendFunctionSource: Int,
        blendFunctionDestination: Int,
        depthTestingEnabled: Boolean,
        depthMask: Boolean,
        depthFunction: Int,
        lineWidth: Float,
        scissor: Vec4?
    ): Boolean {
        return this.cache == cache &&
                this.texture == texture &&
                this.primitiveType == primitiveType &&
                this.blendFunctionSource == blendFunctionSource &&
                this.blendFunctionDestination == blendFunctionDestination &&
                this.depthTestingEnabled == depthTestingEnabled &&
                this.depthMask == depthMask &&
                this.depthFunction == depthFunction &&
                this.lineWidth == lineWidth &&
                this.scissor == scissor
    }


    companion object {
        val DEFAULT_TEXTURE: ITexture? = null
        val DEFAULT_SCISSOR: Vec4? = null

        const val DEFAULT_PRIMITIVE_TYPE = GL10.GL_TRIANGLES
        const val DEFAULT_BLEND_FUNCTION_SOURCE = GL10.GL_SRC_ALPHA
        const val DEFAULT_BLEND_FUNCTION_DESTINATION = GL10.GL_ONE_MINUS_SRC_ALPHA
        const val DEFAULT_DEPTH_TESTING_ENABLED = false
        const val DEFAULT_DEPTH_MASK = false
        const val DEFAULT_DEPTH_FUNCTION = GL10.GL_LEQUAL
        const val DEFAULT_LINE_WIDTH = 1f
    }
}


data class DrawCache(
    var isDirty: Boolean = true,
    val commands: MutableList<DrawCommand> = mutableListOf(),
)