package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * The Vulkan implementation of [GraphicsDevice].
 */
class VKDevice internal constructor(private val context: VKContext) : GraphicsDevice {

    override val capabilities = GraphicsDevice.Capabilities(
        maxTextureSize = VKNative.maxImageDimension2D(context.handle).toFloat(),
        supportsExternalTextures = VKExternalTexture.isSupported(context),
    )

    override fun createBuffer() = VKBuffer()

    override fun createShader(source: GraphicsShader.Source): GraphicsShader = VKShader(source, context)

    override fun createTexture(width: Float, height: Float): GraphicsTexture = VKTexture(width, height, context)

    override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = VKExternalTexture(width, height, context)
}
