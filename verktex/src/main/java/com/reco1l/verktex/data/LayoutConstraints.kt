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
     * The maximum width available for the component.
     */
    val availableWidth
        get() = maxWidth

    /**
     * The maximum height available for the component.
     */
    val availableHeight
        get() = maxHeight


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

        // Filling an unbounded axis (e.g. inside a scrollable container) behaves like wrapping the content.
        fun overridesLimits(dimension: Dimension, availableSize: Float) =
            dimension is Dimension.Fixed || (dimension is Dimension.AvailableSizeFraction && availableSize.isFinite())

        val overrideLimitsHorizontally = overridesLimits(width, availableWidth)
        val overrideLimitsVertically = overridesLimits(height, availableHeight)

        return LayoutConstraints(
            minWidth = if (overrideLimitsHorizontally) resolvedWidth else minWidth,
            maxWidth = if (overrideLimitsHorizontally) resolvedWidth else maxWidth,
            minHeight = if (overrideLimitsVertically) resolvedHeight else minHeight,
            maxHeight = if (overrideLimitsVertically) resolvedHeight else maxHeight
        )
    }

    /**
     * Returns constraints with the same maximum size but no minimum size, so that children are not
     * forced to fill the size of their parent.
     */
    fun loosen() = copy(minWidth = 0f, minHeight = 0f)

    /**
     * Returns a new [LayoutConstraints] instance with the specified padding applied.
     */
    fun shrink(padding: Padding): LayoutConstraints {
        val horizontal = padding.left.toPixels() + padding.right.toPixels()
        val vertical = padding.top.toPixels() + padding.bottom.toPixels()

        return LayoutConstraints(
            minWidth = (minWidth - horizontal).coerceAtLeast(0f),
            maxWidth = (maxWidth - horizontal).coerceAtLeast(0f),
            minHeight = (minHeight - vertical).coerceAtLeast(0f),
            maxHeight = (maxHeight - vertical).coerceAtLeast(0f)
        )
    }
}


fun Vec4.asLayoutConstraints() = LayoutConstraints(
    minWidth = x,
    maxWidth = x + width,
    minHeight = y,
    maxHeight = y + height
)