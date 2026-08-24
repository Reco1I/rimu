package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture

class ESDevice : GraphicsDevice {

    override val capabilities: GraphicsDevice.Capabilities


    init {
        val maxTextureSize = GLES32Helper.glGetIntegerv(GLES32.GL_MAX_TEXTURE_SIZE).toFloat()

        capabilities = GraphicsDevice.Capabilities(
            maxTextureSize = maxTextureSize
        )
    }


    override fun createTexture(width: Float, height: Float): GraphicsTexture {
        return ESTexture(width, height)
    }

    override fun createBuffer(): ESBuffer {
        return ESBuffer()
    }

    override fun createShader(source: GraphicsShader.Source): ESShader {
        return ESShader(source)
    }
}