@file:Suppress("ConstPropertyName")

package com.reco1l.verktex.data

import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex

/**
 * Represents a dimension that can be used to define the size of a UI component.
 */
sealed class Dimension {

    /**
     * Represents a dimension that is driven by constraints, such as fixed values, available size
     * percentages.
     * This does not include content size dimensions, which are based on the content of the component
     * itself at does not modify layout constraints.
     */
    sealed class ConstraintDriven : Dimension() {

        /**
         * Resolves this dimension into an absolute pixel value based on the available size.
         */
        fun resolve(availableSize: Float): Float {
            return when (this) {
                is Fixed -> toPixels()
                is AvailableSizeFraction -> availableSize * factor
            }
        }

    }

    /**
     * Represents a fixed dimension that can be defined in pixels or density-independent pixels (dp).
     */
    sealed class Fixed : ConstraintDriven() {

        /**
         * The raw value of the dimension.
         */
        abstract val value: Float

        /**
         * Converts this dimension to pixels based on its type and the current display density.
         */
        abstract fun toPixels(): Float


        /**
         * Represents a fixed dimension in pixels.
         */
        class Pixels(override val value: Float) : Fixed() {
            override fun toPixels(): Float = value
        }

        /**
         * Represents a fixed dimension in density-independent pixels (dp).
         */
        class DensityIndependentPixels(override val value: Float) : Fixed() {
            override fun toPixels(): Float {
                return value * Platform.currentDisplay.density
            }
        }
    }

    /**
     * Represents a dimension that is a percentage of the available size of the corresponding
     * dimension (width or height).
     *
     * @param factor The percentage of the available size (0.0 to 1.0).
     */
    data class AvailableSizeFraction(val factor: Float) : ConstraintDriven()

    /**
     * Represents a dimension that is a percentage of the content size of the corresponding
     * dimension (width or height).
     *
     * @param factor The percentage of the content size (0.0 to 1.0).
     */
    data class ContentSizeFraction(val factor: Float) : Dimension()


    companion object {
        val Zero = Fixed.Pixels(0f)
        val Max = Fixed.Pixels(Float.MAX_VALUE)

        val FillAvailable = AvailableSizeFraction(1f)
        val WrapContent = ContentSizeFraction(1f)
    }
}

/**
 * Indicates that this float value is in pixels (px).
 * It will be used as-is without any conversion.
 */
val Float.px: Dimension.Fixed
    get() = Dimension.Fixed.Pixels(this)

/**
 * Indicates that this float value is in density-independent pixels (dip).
 * It will be converted to pixels by multiplying it with the device's density factor.
 */
val Float.dip: Dimension.Fixed
    get() = Dimension.Fixed.DensityIndependentPixels(this)

/**
 * Indicates that this float value represents a percentage of the available size of the corresponding
 * dimension (width or height).
 */
val Float.pct: Dimension.AvailableSizeFraction
    get() = Dimension.AvailableSizeFraction(this)


/**
 * Indicates that this float value represents a percentage of the content size of the corresponding
 * dimension (width or height).
 */
fun fill(percentage: Float = 1f): Dimension.AvailableSizeFraction {
    return Dimension.AvailableSizeFraction(percentage)
}

/**
 * Indicates that this float value represents a percentage of the content size of the corresponding
 * dimension (width or height).
 */
fun wrap(percentage: Float = 1f): Dimension.ContentSizeFraction {
    return Dimension.ContentSizeFraction(percentage)
}