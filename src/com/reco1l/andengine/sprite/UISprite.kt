package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.sprite.ScaleType.*
import com.reco1l.andengine.theme.Size
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

/**
 * Sprite that allows to change texture once created.
 */
@Suppress("LeakingThis")
open class UISprite(textureRegion: TextureRegion? = null) : UIBufferedComponent() {

    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false
        set(value) {
            field = value
            textureRegion?.isFlippedHorizontal = value
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value) {
            field = value
            textureRegion?.isFlippedVertical = value
        }

    /**
     * The texture region of the sprite.
     */
    var textureRegion = textureRegion
        set(value) {
            if (field != value) {
                field = value
                onTextureRegionChanged()
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The X position of the texture.
     */
    var textureX = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(value, textureY)
            }
        }

    /**
     * The Y position of the texture.
     */
    var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(textureX, value)
            }
        }

    /**
     * The scale type of the sprite.
     */
    open var scaleType: ScaleType = Fit

    /**
     * The alignment of the texture.
     *
     * If the scale type is [ScaleType.Stretch] it will not take effect.
     */
    var gravity: Vec2 = Anchor.Center


    init {
        width = Size.Auto
        height = Size.Auto

        onTextureRegionChanged()
    }

    override fun onContentChanged() {
        // Content size updated when texture region is changed.
    }

    open fun onTextureRegionChanged() {
        val textureRegion = textureRegion

        textureRegion?.setTexturePosition(textureX, textureY)
        textureRegion?.isFlippedVertical = flippedVertical
        textureRegion?.isFlippedHorizontal = flippedHorizontal
        contentWidth = textureRegion?.width?.toFloat() ?: 0f
        contentHeight = textureRegion?.height?.toFloat() ?: 0f

        blendInfo = if (textureRegion?.texture?.textureOptions?.mPreMultipyAlpha == true) BlendInfo.PreMultiply else BlendInfo.Mixture
    }


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)

        val textureRegion = textureRegion ?: return

        val textureWidth = contentWidth
        val textureHeight = contentHeight

        var quadWidth: Float
        var quadHeight: Float

        when (scaleType) {

            Crop -> {
                val scale = max(width / textureWidth, height / textureHeight)
                quadWidth = textureWidth * scale
                quadHeight = textureHeight * scale
            }

            Fit -> {
                val scale = min(width / textureWidth, height / textureHeight)
                quadWidth = textureWidth * scale
                quadHeight = textureHeight * scale
            }

            Stretch -> {
                quadWidth = width
                quadHeight = height
            }
        }

        val x = (width - quadWidth) * gravity.x
        val y = (height - quadHeight) * gravity.y

        TextureRenderer.renderTexture(gl, x, y, quadWidth, quadHeight, textureRegion = textureRegion)
    }

}

enum class ScaleType {

    /**
     * Scale the texture to fill the entire sprite cropping the excess.
     */
    Crop,

    /**
     * Scale the texture to fit the sprite without cropping.
     */
    Fit,

    /**
     * Scale the texture to fit the sprite without cropping.
     * The texture will be stretched to fill the entire sprite.
     */
    Stretch
}