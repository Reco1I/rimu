package com.reco1l.verktex.data

import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.ThemeDependent

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
) : ThemeDependent<Border> {

    override val isThemed: Boolean
        get() = width.isThemed || color.isThemed || colorBottom.isThemed

    override fun resolve(theme: Theme): Border {
        if (!isThemed) return this
        return copy(width = width.resolve(theme), color = color.resolve(theme), colorBottom = colorBottom.resolve(theme))
    }


    /**
     * Whether the border is visible at all.
     */
    val isVisible: Boolean
        get() = width.value > 0f && (color.alpha > 0f || colorBottom.alpha > 0f)


    companion object {
        val None = Border(0f.px, Color4.Transparent)
    }
}
