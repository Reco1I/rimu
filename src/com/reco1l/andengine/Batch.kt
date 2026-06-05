package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import javax.microedition.khronos.opengles.GL10

val DefaultBatchKey = BatchKey()

data class BatchKey(
    val texture: ITexture? = null,
    val primitiveType: Int= GL10.GL_TRIANGLES,
    val blendFunctionSource: Int= GL10.GL_SRC_ALPHA,
    val blendFunctionDestination: Int= GL10.GL_ONE_MINUS_SRC_ALPHA,
    val depthTestingEnabled: Boolean= false,
    val depthMask: Boolean= false,
    val depthFunction: Int= GL10.GL_LEQUAL,
    val lineWidth: Float= 1f,
    val scissor: Vec4?= null
)

data class Batch(
    var buffer: VertexBuffer = VertexBuffer(64),
    var texture: ITexture? = DefaultBatchKey.texture,
    var primitiveType: Int = DefaultBatchKey.primitiveType,
    var blendFunctionSource: Int = DefaultBatchKey.blendFunctionSource,
    var blendFunctionDestination: Int = DefaultBatchKey.blendFunctionDestination,
    var depthTestingEnabled: Boolean = DefaultBatchKey.depthTestingEnabled,
    var depthMask: Boolean = DefaultBatchKey.depthMask,
    var depthFunction: Int = DefaultBatchKey.depthFunction,
    var lineWidth: Float = DefaultBatchKey.lineWidth,
    var scissor: Vec4? = DefaultBatchKey.scissor,
) {

    init {
        buffer.useTextures = texture != null
    }


    fun setToDefault() {
        set(
            texture = DefaultBatchKey.texture,
            primitiveType = DefaultBatchKey.primitiveType,
            blendFunctionSource = DefaultBatchKey.blendFunctionSource,
            blendFunctionDestination = DefaultBatchKey.blendFunctionDestination,
            depthTestingEnabled = DefaultBatchKey.depthTestingEnabled,
            depthMask = DefaultBatchKey.depthMask,
            depthFunction = DefaultBatchKey.depthFunction,
            lineWidth = DefaultBatchKey.lineWidth,
            scissor = DefaultBatchKey.scissor
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


    fun getKey(): BatchKey {
        return BatchKey(
            texture,
            primitiveType,
            blendFunctionSource,
            blendFunctionDestination,
            depthTestingEnabled,
            depthMask,
            depthFunction,
            lineWidth,
            scissor
        )
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

}