package com.reco1l.verktex.data

/**
 * Represents the padding of a UI component, which defines the space between the component's
 * content and its boundaries.
 *
 * All padding values are represented as [Dimension] objects.
 */
data class Padding(
    val left: Dimension.Fixed = Dimension.Zero,
    val top: Dimension.Fixed = Dimension.Zero,
    val right: Dimension.Fixed = Dimension.Zero,
    val bottom: Dimension.Fixed = Dimension.Zero
) {

    constructor(all: Dimension.Fixed) : this(all, all, all, all)
    constructor(horizontal: Dimension.Fixed, vertical: Dimension.Fixed) : this(horizontal, vertical, horizontal, vertical)


    /**
     * Converts this [Padding] instance to a [Vec4] representation, where each side is converted to pixels.
     */
    fun toVec4(): Vec4 {
        return Vec4(
            x = left.toPixels(),
            y = top.toPixels(),
            z = right.toPixels(),
            w = bottom.toPixels()
        )
    }


    companion object {
        /**
         * A [Padding] instance with all sides set to zero.
         */
        val Zero = Padding()
    }
}


fun Vec4.toPadding(): Padding {
    return Padding(
        left = x.px,
        top = y.px,
        right = z.px,
        bottom = w.px
    )
}

fun horizontalPadding(horizontal: Dimension.Fixed): Padding {
    return Padding(horizontal, Dimension.Zero)
}

fun verticalPadding(vertical: Dimension.Fixed): Padding {
    return Padding(Dimension.Zero, vertical)
}