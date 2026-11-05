package com.reco1l.verktex.data

/**
 * Determines how the component should be clipped.
 */
enum class ClipMode {
    /**
     * No clipping is applied.
     */
    Disabled,

    /**
     * Clips the component to its own bounds.
     */
    Bounds,

    /**
     * Clips the component to its padding bounds.
     */
    Padding,
}