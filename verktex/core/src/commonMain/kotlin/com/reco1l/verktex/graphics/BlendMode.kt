package com.reco1l.verktex.graphics

import com.reco1l.verktex.graphics.BlendFactor.DstColor
import com.reco1l.verktex.graphics.BlendFactor.One
import com.reco1l.verktex.graphics.BlendFactor.OneMinusSrcAlpha
import com.reco1l.verktex.graphics.BlendFactor.SrcAlpha
import com.reco1l.verktex.graphics.BlendFactor.Zero

/**
 * Represents the blending mode used for rendering graphics, as separate source and destination
 * factors for the RGB and alpha channels.
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
     * Represents a blending mode that uses the alpha channel for transparency.
     */
    object Alpha : BlendMode(SrcAlpha, OneMinusSrcAlpha)

    /**
     * Represents an additive blending mode where colors are added together, resulting in a brighter effect.
     */
    object Additive : BlendMode(SrcAlpha, One)

    /**
     * Represents a multiplicative blending mode where colors are multiplied together, resulting in a darker effect.
     */
    object Multiply : BlendMode(DstColor, Zero)

    /**
     * Represents alpha blending for colors that are already multiplied by their alpha.
     */
    object PremultipliedAlpha : BlendMode(One, OneMinusSrcAlpha)

    /**
     * Represents a blending mode with arbitrary factors. Two custom modes are equal when their factors are.
     */
    data class Custom(
        val customSourceRGB: BlendFactor,
        val customDestinationRGB: BlendFactor,
        val customSourceAlpha: BlendFactor = customSourceRGB,
        val customDestinationAlpha: BlendFactor = customDestinationRGB
    ) : BlendMode(customSourceRGB, customDestinationRGB, customSourceAlpha, customDestinationAlpha)
}
