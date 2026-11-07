package com.reco1l.verktex.graphics

import com.reco1l.verktex.graphics.BlendFactor.DstColor
import com.reco1l.verktex.graphics.BlendFactor.One
import com.reco1l.verktex.graphics.BlendFactor.OneMinusSrcAlpha
import com.reco1l.verktex.graphics.BlendFactor.SrcAlpha
import com.reco1l.verktex.graphics.BlendFactor.Zero

/**
 * Represents the blending mode used for rendering graphics, as separate source and destination
 * factors for the RGB and alpha channels.
 *
 * Shaders output premultiplied colours (their RGB already multiplied by their alpha) and textures hold
 * premultiplied pixels, so the source colour is never multiplied by its alpha again here.
 */
sealed class BlendMode(
    val sourceRGB: BlendFactor,
    val destinationRGB: BlendFactor,
    val sourceAlpha: BlendFactor = sourceRGB,
    val destinationAlpha: BlendFactor = destinationRGB
) {

    /**
     * Represents an opaque blending mode where no transparency is applied.
     */
    object Opaque : BlendMode(One, Zero)

    /**
     * Represents a blending mode that uses the alpha channel for transparency, the source over the
     * destination.
     */
    object Alpha : BlendMode(One, OneMinusSrcAlpha)

    /**
     * Represents an additive blending mode where colors are added together, resulting in a brighter effect.
     * The alpha channel adds the source's alpha weighted by itself, as it did with straight alpha.
     */
    object Additive : BlendMode(One, One, SrcAlpha, One)

    /**
     * Represents a multiplicative blending mode where colors are multiplied together, resulting in a darker effect.
     * A translucent source multiplies the destination partly, a transparent one leaves it as it is.
     */
    object Multiply : BlendMode(DstColor, OneMinusSrcAlpha)

    /**
     * Represents a blending mode with arbitrary factors. Two custom modes are equal when their factors are.
     * The source colour is premultiplied, so `One` stands for what `SrcAlpha` was with straight alpha.
     */
    data class Custom(
        val customSourceRGB: BlendFactor,
        val customDestinationRGB: BlendFactor,
        val customSourceAlpha: BlendFactor = customSourceRGB,
        val customDestinationAlpha: BlendFactor = customDestinationRGB
    ) : BlendMode(customSourceRGB, customDestinationRGB, customSourceAlpha, customDestinationAlpha)
}
