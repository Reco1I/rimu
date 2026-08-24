package com.reco1l.verktex.ui

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.*


inline fun UIContainer.sprite(builder: UISprite.() -> Unit): UISprite {
    return UISprite().apply(builder).also(::plusAssign)
}

/**
 * Sprite that allows to change texture once created.
 */
open class UISprite(texture: GraphicsTexture.Region? = null) : UICanvas() {

    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false

    /**
     * The texture region of the sprite.
     */
    var textureRegion: GraphicsTexture.Region? = texture
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The scale type of the sprite.
     */
    var scaleType: ScaleType = ScaleType.Fit

    /**
     * The alignment of the texture.
     *
     * If the scale type is [ScaleType.Stretch] it will not take effect.
     */
    var gravity: Vec2 = Anchor.Center


    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {
        val textureSize = textureRegion?.let { region ->
            Vec2(
                x = region.width,
                y = region.height
            )
        } ?: Vec2.Zero

        return parentConstraints.constrain(textureSize)
    }

    override fun onDraw(renderer: GraphicsRenderer) {

        val textureRegion = textureRegion ?: return
        val textureWidth = measuredSize.x
        val textureHeight = measuredSize.y

        var quadWidth: Float
        var quadHeight: Float

        when (scaleType) {

            ScaleType.Crop -> {
                val scale = max(bounds.width / textureWidth, bounds.height / textureHeight)
                quadWidth = textureWidth * scale
                quadHeight = textureHeight * scale
            }

            ScaleType.Fit -> {
                val scale = min(bounds.width / textureWidth, bounds.height / textureHeight)
                quadWidth = textureWidth * scale
                quadHeight = textureHeight * scale
            }

            ScaleType.Stretch -> {
                quadWidth = bounds.width
                quadHeight = bounds.height
            }
        }

        val x = (bounds.width - quadWidth) * gravity.x
        val y = (bounds.height - quadHeight) * gravity.y

        renderer.drawTexture(textureRegion.texture, textureRegion, x, y, quadWidth, quadHeight)
    }


    /**
     * Defines how the texture should be scaled to fit the sprite.
     */
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

}

