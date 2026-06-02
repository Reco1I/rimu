package com.reco1l.andengine

import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.atlas.TextureAtlas
import org.anddev.andengine.opengl.texture.source.ITextureAtlasSource
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10


private fun createNativeOrderFloatBuffer(capacity: Int): FloatBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(capacity * Float.SIZE_BYTES)
    byteBuffer.order(ByteOrder.nativeOrder())
    return byteBuffer.asFloatBuffer()
}

data class RenderState(
    /**
     * The texture atlas to be used for rendering. If null, texturing will be disabled.
     */
    val texture: TextureAtlas<out ITextureAtlasSource>? = null,

    /**
     * The primitive type to be used for rendering. Defaults to GL_TRIANGLES.
     */
    val primitiveType: Int = GL10.GL_TRIANGLES,

    /**
     * The blending information to be used for rendering. Defaults to BlendInfo.Mixture, which
     * corresponds to the standard alpha blending mode.
     */
    val blendInfo: BlendInfo = BlendInfo.Mixture,

    /**
     * The depth testing information to be used for rendering. Defaults to DepthInfo.None, which means
     * that depth testing will be disabled.
     */
    val depthInfo: DepthInfo = DepthInfo.Companion.None,

    /**
     * The scissor rectangle to be used for rendering. If null, scissoring will be disabled. The
     * rectangle is defined as a Vec4 where x and y represent the lower-left corner of the rectangle,
     * and z and w represent the width and height, respectively.
     */
    val scissor: Vec4? = null,
) {

    /**
     * The float buffer holding the vertex data for rendering.
     */
    var buffer = createNativeOrderFloatBuffer(32 * VERTEX_STRIDE)
        private set


    fun ensureBufferCapacity() {
        val position = buffer.position()
        val capacity = buffer.capacity()

        if (position + VERTEX_STRIDE >= capacity) {
            buffer.flip()
            buffer = createNativeOrderFloatBuffer(capacity * 2).also { newBuffer ->
                newBuffer.put(buffer)
                newBuffer.position(position)
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        return other is RenderState &&
                texture == other.texture &&
                primitiveType == other.primitiveType &&
                blendInfo == other.blendInfo &&
                depthInfo == other.depthInfo &&
                scissor == other.scissor
    }

    override fun hashCode(): Int {
        var result = primitiveType
        result = 31 * result + (texture?.hashCode() ?: 0)
        result = 31 * result + blendInfo.hashCode()
        result = 31 * result + depthInfo.hashCode()
        result = 31 * result + (scissor?.hashCode() ?: 0)
        result = 31 * result + buffer.hashCode()
        return result
    }
}