package com.reco1l.andengine.buffered

import org.anddev.andengine.opengl.texture.region.TextureRegion

object TextureRenderer : BufferRenderer() {

    fun renderTexture(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        textureRegion: TextureRegion
    ) {

        val x1 = textureRegion.textureCoordinateX1
        val y1 = textureRegion.textureCoordinateY1
        val x2 = textureRegion.textureCoordinateX2
        val y2 = textureRegion.textureCoordinateY2

        addVertex(x, y, x1, y1)
        addVertex(x + width, y, x2, y1)
        addVertex(x + width, y + height, x2, y2)

        addVertex(x, y, x1, y1)
        addVertex(x + width, y + height, x2, y2)
        addVertex(x, y + height, x1, y2)
    }

}