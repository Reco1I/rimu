package com.reco1l.verktex.data

/**
 * The axes in a bi-dimensional space.
 */
enum class Axis {
    X,
    Y,
    Both,
    None;

    /**
     * Whether this axis is [Y] or [Both].
     */
    val isVertical: Boolean
        get() = this == Both || this == Y

    /**
     * Whether this axis is [X] or [Both].
     */
    val isHorizontal: Boolean
        get() = this == Both || this == X

}