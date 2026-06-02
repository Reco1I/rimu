package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.PaintStyle.Fill
import com.reco1l.andengine.shape.PaintStyle.Outline
import com.reco1l.andengine.shape.UICircle.Companion.calculateArcResolution
import com.reco1l.framework.Color4
import com.reco1l.toolkt.toRadians
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object TextureRenderer : BufferRenderer() {

    fun renderTexture(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        textureRegion: TextureRegion
    ) {

        /*GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
        textureRegion.onApply(gl)

        val x1 = textureRegion.textureCoordinateX1
        val y1 = textureRegion.textureCoordinateY1
        val x2 = textureRegion.textureCoordinateX2
        val y2 = textureRegion.textureCoordinateY2
*/
        addTriangle(
            x, y,
            x, y + height,
            x + width, y
        )

        addTriangle(
            x + width, y,
            x, y + height,
            x + width, y + height
        )
    }

}