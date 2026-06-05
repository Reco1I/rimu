package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import javax.microedition.khronos.opengles.GL10

data class Batch(
    val buffer: VertexBuffer = VertexBuffer(64),

    var texture: ITexture? = DEFAULT_TEXTURE,
    var primitiveType: Int = DEFAULT_PRIMITIVE_TYPE,
    var blendFunctionSource: Int = DEFAULT_BLEND_FUNCTION_SOURCE,
    var blendFunctionDestination: Int = DEFAULT_BLEND_FUNCTION_DESTINATION,
    var depthTestingEnabled: Boolean = DEFAULT_DEPTH_TESTING_ENABLED,
    var depthMask: Boolean = DEFAULT_DEPTH_MASK,
    var depthFunction: Int = DEFAULT_DEPTH_FUNCTION,
    var lineWidth: Float = DEFAULT_LINE_WIDTH,
    var scissor: Vec4? = DEFAULT_SCISSOR,
) {

    init {
        buffer.useTextures = texture != null
    }


    fun setToDefault() {
        set(
            texture = DEFAULT_TEXTURE,
            primitiveType = DEFAULT_PRIMITIVE_TYPE,
            blendFunctionSource = DEFAULT_BLEND_FUNCTION_SOURCE,
            blendFunctionDestination = DEFAULT_BLEND_FUNCTION_DESTINATION,
            depthTestingEnabled = DEFAULT_DEPTH_TESTING_ENABLED,
            depthMask = DEFAULT_DEPTH_MASK,
            depthFunction = DEFAULT_DEPTH_FUNCTION,
            lineWidth = DEFAULT_LINE_WIDTH,
            scissor = DEFAULT_SCISSOR,
        )
    }

    fun set(
        texture: ITexture? = this.texture,
        primitiveType: Int = this.primitiveType,
        blendFunctionSource: Int = this.blendFunctionSource,
        blendFunctionDestination: Int = this.blendFunctionDestination,
        depthTestingEnabled: Boolean = this.depthTestingEnabled,
        depthMask: Boolean = this.depthMask,
        depthFunction: Int = this.depthFunction,
        lineWidth: Float = this.lineWidth,
        scissor: Vec4? = this.scissor,
    ) {
        this.texture = texture
        this.primitiveType = primitiveType
        this.blendFunctionSource = blendFunctionSource
        this.blendFunctionDestination = blendFunctionDestination
        this.depthTestingEnabled = depthTestingEnabled
        this.depthMask = depthMask
        this.depthFunction = depthFunction
        this.lineWidth = lineWidth
        this.scissor = scissor

        buffer.useTextures = texture != null
    }

    fun equals(
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
        return this.texture == texture &&
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