package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * The Vulkan implementation of [GraphicsDevice].
 */
class VKDevice internal constructor(private val context: VKContext) : GraphicsDevice {

    override val capabilities = GraphicsDevice.Capabilities(
        maxTextureSize = VKNative.maxImageDimension2D(context.handle).toFloat()
    )

    override fun createBuffer() = VKBuffer()

    override fun createShader(source: GraphicsShader.Source): GraphicsShader = VKShader(source, context)

    override fun createTexture(width: Float, height: Float): GraphicsTexture = VKTexture(width, height)

    override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = VKExternalTexture(width, height)
}
