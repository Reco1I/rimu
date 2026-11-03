package com.reco1l.verktex.data

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
)
