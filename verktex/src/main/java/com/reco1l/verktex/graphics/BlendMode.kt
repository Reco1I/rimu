package com.reco1l.verktex.graphics

/**
 * Represents the blending mode used for rendering graphics.
 */
enum class BlendMode {

    /**
     * Represents an opaque blending mode where no transparency is applied.
     */
    Opaque,

    /**
     * Represents a blending mode that uses the alpha channel for transparency.
     */
    Alpha,

    /**
     * Represents an additive blending mode where colors are added together, resulting in a brighter effect.
     */
    Additive,

    /**
     * Represents a multiplicative blending mode where colors are multiplied together, resulting in a darker effect.
     */
    Multiply
}