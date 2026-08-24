package com.reco1l.verktex.android

import android.graphics.Bitmap
import android.opengl.GLES32
import android.opengl.GLUtils
import com.reco1l.verktex.graphics.GraphicsTexture

class AndroidTextureSource(val bitmap: Bitmap) : GraphicsTexture.Source {

    override val width = bitmap.width.toFloat()
    override val height = bitmap.height.toFloat()

    override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) {
        GLUtils.texSubImage2D(GLES32.GL_TEXTURE_2D, 0, texturePositionX.toInt(), texturePositionY.toInt(), bitmap)
    }
}