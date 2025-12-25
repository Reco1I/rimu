package com.reco1l.verktex.ui

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
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

        val region = textureRegion ?: return
        val textureWidth = region.width
        val textureHeight = region.height

        if (textureWidth <= 0f || textureHeight <= 0f) {
            return
        }

        var x = 0f
        var y = 0f
        var width = bounds.width
        var height = bounds.height

        // Fraction of the texture shown on each axis, cropping shows only a part of it.
        var visibleX = 1f
        var visibleY = 1f

        when (scaleType) {

            ScaleType.Crop -> {
                val scale = max(bounds.width / textureWidth, bounds.height / textureHeight)
                visibleX = bounds.width / (textureWidth * scale)
                visibleY = bounds.height / (textureHeight * scale)
            }

            ScaleType.Fit -> {
                val scale = min(bounds.width / textureWidth, bounds.height / textureHeight)
                width = textureWidth * scale
                height = textureHeight * scale
                x = (bounds.width - width) * gravity.x
                y = (bounds.height - height) * gravity.y
            }

            ScaleType.Stretch -> {}
        }

        val uStart = region.u0 + (region.u1 - region.u0) * (1f - visibleX) * gravity.x
        val uEnd = uStart + (region.u1 - region.u0) * visibleX
        val vStart = region.v0 + (region.v1 - region.v0) * (1f - visibleY) * gravity.y
        val vEnd = vStart + (region.v1 - region.v0) * visibleY

        val uv = Vec4(
            x = if (flippedHorizontal) uEnd else uStart,
            y = if (flippedVertical) vEnd else vStart,
            z = if (flippedHorizontal) uStart else uEnd,
            w = if (flippedVertical) vStart else vEnd
        )

        renderer.drawTexture(region.texture, region, x, y, width, height, color = contentColor, uv = uv)
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
         * Stretch the texture to fill the entire sprite, ignoring its aspect ratio.
         */
        Stretch
    }

}

