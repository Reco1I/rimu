package com.reco1l.verktex.ui

import com.reco1l.verktex.data.fill
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.ui.container.UIContainer

inline fun UIContainer.crossfadeSprite(builder: UICrossfadeSprite.() -> Unit): UICrossfadeSprite {
    return UICrossfadeSprite().apply(builder).also(::plusAssign)
}

/**
 * A sprite that fades between textures instead of swapping them, e.g. for backgrounds.
 *
 * It alternates between two sprites on each [crossfadeTo], the one showing the previous texture
 * keeps it until the fade ends. A region released meanwhile would be drawn released, so textures
 * loaded under a key should alternate between two keys as well.
 */
open class UICrossfadeSprite : UIContainer() {

    private val sprites = Array(2) { index ->
        UISprite().apply {
            width = fill()
            height = fill()
            alpha = if (index == 0) 1f else 0f
        }
    }

    // The sprite showing the current texture.
    private var frontIndex = 0


    /**
     * How long a fade takes, in seconds.
     */
    var crossfadeDuration = 0.5f

    /**
     * How the textures are scaled to the bounds, see [UISprite.scaleType].
     */
    var scaleType: UISprite.ScaleType
        get() = sprites[0].scaleType
        set(value) = sprites.forEach { it.scaleType = value }

    /**
     * The texture shown, or being faded to. Setting it fades to it in [crossfadeDuration].
     */
    var textureRegion: GraphicsTexture.Region?
        get() = sprites[frontIndex].textureRegion
        set(value) = crossfadeTo(value)


    init {
        sprites.forEach { +it }
    }


    /**
     * Fades from the texture shown to [region] in [duration] seconds, zero to swap them.
     */
    fun crossfadeTo(region: GraphicsTexture.Region?, duration: Float = crossfadeDuration) {
        val backIndex = 1 - frontIndex
        val front = sprites[frontIndex]
        val back = sprites[backIndex]

        // A fade still running is finished at once.
        front.clearAnimations()
        back.clearAnimations()

        back.textureRegion = region

        // The second sprite is drawn above the first one, so it fades in over it or out from it.
        if (backIndex == 1) {
            front.alpha = 1f
            back.alpha = 0f
            back.fadeIn(duration)
        } else {
            back.alpha = 1f
            front.alpha = 1f
            front.fadeOut(duration)
        }

        frontIndex = backIndex
    }
}
