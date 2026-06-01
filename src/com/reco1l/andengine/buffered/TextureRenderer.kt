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
        gl: GL10,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color4? = null,
        textureRegion: TextureRegion
    ) {
        if (color != null) ColorStack.pushColor(gl, color, false)

        render(gl, GL10.GL_TRIANGLE_STRIP) {

            GLHelper.enableTextures(gl)
            GLHelper.enableTexCoordArray(gl)
            textureRegion.onApply(gl)

            addVertex(x, y)
            addVertex(x, y + height)
            addVertex(x + width, y)
            addVertex(x + width, y + height)
        }

        if (color != null) ColorStack.popColor(gl)
    }

}