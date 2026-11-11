package com.reco1l.verktex.data

import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.ThemeDependent

/**
 * A shadow drawn below a component, following its shape.
 */
data class Shadow(

    /**
     * The color of the shadow.
     */
    val color: Color4,

    /**
     * How far the shadow is moved down from the component.
     */
    val offsetY: Dimension.Fixed = Dimension.Zero,

    /**
     * How far the shadow fades past the component's edges.
     */
    val blur: Dimension.Fixed = Dimension.Zero,

    /**
     * Whether the shadow is only drawn outside of the component, starting at full strength from
     * its edges, like a glow. Otherwise it is below the whole component and shows through a
     * translucent background. The cut follows the shadow, so it is meant for one without offset.
     */
    val cutout: Boolean = false,
) : ThemeDependent<Shadow> {

    override val isThemed: Boolean
        get() = color.isThemed || offsetY.isThemed || blur.isThemed


    /**
     * Whether the Shadow is visible at all.
     */
    val isVisible
        get() = color.alpha > 0f && blur.toPixels() > 0f


    override fun resolve(theme: Theme): Shadow {
        if (!isThemed) return this
        return copy(color = color.resolve(theme), offsetY = offsetY.resolve(theme), blur = blur.resolve(theme))
    }

    companion object {
        val None = Shadow(Color4.Transparent)
    }
}
