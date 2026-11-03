package com.reco1l.verktex.data

/**
 * A border drawn along the inner edge of a component, following its shape.
 */
data class Border(

    /**
     * The width of the border.
     */
    val width: Dimension.Fixed,

    /**
     * The color of the border, at the top when [colorBottom] differs.
     */
    val color: Color4,

    /**
     * The color of the border at the bottom, it fades from [color] at the top, e.g. a bezel lighter
     * on top and darker at the bottom.
     */
    val colorBottom: Color4 = color,
) {

    /**
     * Whether the border is visible at all.
     */
    val isVisible: Boolean
        get() = width.value > 0f && (color.alpha > 0f || colorBottom.alpha > 0f)
}
