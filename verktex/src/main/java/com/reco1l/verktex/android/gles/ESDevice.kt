package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture

class ESDevice : GraphicsDevice {

    // Written on the draw thread once a context is current, read from the update thread.
    @Volatile
    override var capabilities: GraphicsDevice.Capabilities = GraphicsDevice.Capabilities(maxTextureSize = 2048f)
        private set


    init {
        refreshCapabilities()
    }


    /**
     * Re-reads the capabilities from the current GL context. Does nothing when no context is current.
     */
    fun refreshCapabilities() {
        // No context is current yet when the device is created, in which case the query returns 0.
        // Fall back to 2048, the minimum guaranteed by OpenGL ES 3.0.
        val maxTextureSize = GLES32Helper.glGetIntegerv(GLES32.GL_MAX_TEXTURE_SIZE).toFloat()
        if (maxTextureSize > 0f) {
            capabilities = GraphicsDevice.Capabilities(maxTextureSize = maxTextureSize)
        }
    }


    override fun createTexture(width: Float, height: Float): GraphicsTexture {
        return ESTexture(width, height)
    }

    override fun createExternalTexture(width: Float, height: Float): ESExternalTexture {
        return ESExternalTexture(width, height)
    }

    override fun createBuffer(): ESBuffer {
        return ESBuffer()
    }

    override fun createShader(source: GraphicsShader.Source): ESShader {
        return ESShader(source)
    }
}