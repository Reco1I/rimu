package com.reco1l.verktex.data

/**
 * Represents layout constraints for a UI component, defining the minimum and maximum width and height.
 * All values are represented in absolute pixels.
 */
data class LayoutConstraints(
    val minWidth: Float,
    val maxWidth: Float,
    val minHeight: Float,
    val maxHeight: Float
) {

    /**
     * Calculates the width of the layout constraints by subtracting the minimum width from the maximum width.
     * This value represents the range of allowed widths for the component.
     */
    val availableWidth
        get() = maxWidth - minWidth

    /**
     * Calculates the height of the layout constraints by subtracting the minimum height from the maximum height.
     * This value represents the range of allowed heights for the component.
     */
    val availableHeight
        get() = maxHeight - minHeight


    /**
     * Constrains the given [size] to be within the layout constraints defined by this instance.
     * The resulting size will have its width and height clamped between the minimum and maximum values
     * specified by [minWidth], [maxWidth], [minHeight], and [maxHeight].
     */
    fun constrain(size: Vec2): Vec2 {
        return Vec2(
            x = size.x.coerceIn(minWidth, maxWidth),
            y = size.y.coerceIn(minHeight, maxHeight)
        )
    }


    /**
     * Resolves the given [width] and [height] dimensions into a new [LayoutConstraints] instance.
     * The resolution process takes into account the minimum and maximum constraints defined by this instance,
     * as well as the available size for each dimension.
     */
    fun resolve(width: Dimension, height: Dimension): LayoutConstraints {

        fun resolveDimension(dimension: Dimension, min: Float, max: Float, availableSize: Float): Float {
            return when (dimension) {
                // Fixed values are converted to pixels and clamped to the min and max constraints
                // from the parent LayoutConstraints.
                is Dimension.Fixed -> dimension.toPixels()

                // Values based on the available size are calculated as a percentage of the available size
                // in parent LayoutConstraints and clamped to the min and max constraints.
                is Dimension.AvailableSizeFraction -> (availableSize * dimension.factor)

                // Content size values do not modify contraints, as they are based on the content
                // size of the component itself. The available size is used as limit.
                is Dimension.ContentSizeFraction -> availableSize
            }.coerceIn(min, max)
        }

        val resolvedWidth = resolveDimension(width, minWidth, maxWidth, availableWidth)
        val resolvedHeight = resolveDimension(height, minHeight, maxHeight, availableHeight)

        val overrideLimitsHorizontally = width is Dimension.ConstraintDriven
        val overrideLimitsVertically = height is Dimension.ConstraintDriven

        return LayoutConstraints(
            minWidth = if (overrideLimitsHorizontally) resolvedWidth else minWidth,
            maxWidth = if (overrideLimitsHorizontally) resolvedWidth else maxWidth,
            minHeight = if (overrideLimitsVertically) resolvedHeight else minHeight,
            maxHeight = if (overrideLimitsVertically) resolvedHeight else maxHeight
        )
    }

    /**
     * Returns a new [LayoutConstraints] instance with the specified padding applied.
     */
    fun shrink(padding: Padding): LayoutConstraints {
        val horizontal = padding.left.toPixels() + padding.right.toPixels()
        val vertical = padding.top.toPixels() + padding.bottom.toPixels()

        return LayoutConstraints(
            minWidth = minWidth - horizontal,
            maxWidth = maxWidth - horizontal,
            minHeight = minHeight - vertical,
            maxHeight = maxHeight - vertical
        )
    }
}


fun Vec4.asLayoutConstraints() = LayoutConstraints(
    minWidth = x,
    maxWidth = x + width,
    minHeight = y,
    maxHeight = y + height
)