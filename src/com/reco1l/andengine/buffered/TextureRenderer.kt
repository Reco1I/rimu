package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import org.anddev.andengine.opengl.texture.region.TextureRegion
import javax.microedition.khronos.opengles.GL10

object TextureRenderer : BufferRenderer() {

    fun renderTexture(
        gl: GL10,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        textureRegion: TextureRegion
    ) {
        UIRenderer.setState(gl,
            primitiveType = GL10.GL_TRIANGLES,
            texture = textureRegion.texture
        )

        if (pushCacheIfAvailable()) return

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

        UIRenderer.spriteRendered++
    }

}