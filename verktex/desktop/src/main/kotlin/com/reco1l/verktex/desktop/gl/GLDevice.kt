package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import org.lwjgl.opengl.GL33.*

/**
 * The OpenGL implementation of [GraphicsDevice].
 */
class GLDevice : GraphicsDevice {

    // Written on the draw thread once the context is current, read from the update thread. Until
    // then the minimum every OpenGL 3.3 implementation supports.
    @Volatile
    override var capabilities = GraphicsDevice.Capabilities(maxTextureSize = 2048f, supportsExternalTextures = false)
        private set


    /**
     * Reads the capabilities from the context, which must be current on the calling thread.
     */
    fun refreshCapabilities() {
        val maxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE).toFloat()
        if (maxTextureSize > 0f) {
            capabilities = capabilities.copy(maxTextureSize = maxTextureSize)
        }
    }


    override fun createBuffer() = GLBuffer()

    override fun createShader(source: GraphicsShader.Source) = GLShader(source)

    override fun createTexture(width: Float, height: Float): GraphicsTexture = GLTexture(width, height)

    // Desktop has no external textures, its video decoder writes frames into a plain texture (see
    // FFmpegVideoDecoder). This one is never fed, which callers know from the capabilities.
    override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = GLTexture(width, height)
}
