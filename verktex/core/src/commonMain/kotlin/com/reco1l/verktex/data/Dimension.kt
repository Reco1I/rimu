@file:Suppress("ConstPropertyName")

package com.reco1l.verktex.data

import kotlin.jvm.JvmField
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.ThemeDependent
import com.reco1l.verktex.ui.themed

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
    sealed class Fixed : ConstraintDriven(), ThemeDependent<Fixed> {

        /**
         * The raw value of the dimension.
         */
        abstract val value: Float

        /**
         * Converts this dimension to pixels based on its type and the current display density.
         */
        abstract fun toPixels(): Float


        override val isThemed: Boolean
            get() = false

        override fun resolve(theme: Theme): Fixed = this


        /**
         * Represents a fixed dimension in pixels.
         */
        data class Pixels(override val value: Float) : Fixed() {
            override fun toPixels(): Float = value
        }

        /**
         * Represents a fixed dimension in density-independent pixels (dp).
         */
        data class DensityIndependentPixels(override val value: Float) : Fixed() {
            override fun toPixels(): Float {
                return value * Platform.currentDisplay.density * Platform.resolutionScale
            }
        }

        /**
         * Represents a fixed dimension taken from the theme, see [themed]. Themeable properties
         * resolve it with the theme of their component, elsewhere it's resolved with [Verktex.theme].
         */
        class Themed(val block: Theme.() -> Fixed) : Fixed() {

            override val value: Float
                get() = Verktex.theme.block().value

            override val isThemed: Boolean
                get() = true

            override fun toPixels(): Float = Verktex.theme.block().toPixels()

            override fun resolve(theme: Theme): Fixed = theme.block().resolve(theme)

            override fun toString() = "Themed(${Verktex.theme.block()})"
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
        @JvmField
        val Zero = Fixed.Pixels(0f)

        @JvmField
        val FillAvailable = AvailableSizeFraction(1f)

        @JvmField
        val WrapContent = ContentSizeFraction(1f)
    }
}

/**
 * Indicates that this float value is in pixels (px).
 * It will be used as-is without any conversion.
 */
val Float.px: Dimension.Fixed
    get() {
        if (this == 0f) return Dimension.Zero
        return Dimension.Fixed.Pixels(this)
    }

/**
 * Indicates that this float value is in density-independent pixels (dip).
 * It will be converted to pixels by multiplying it with the device's density factor.
 */
val Float.dip: Dimension.Fixed
    get() {
        if (this == 0f) return Dimension.Zero
        return Dimension.Fixed.DensityIndependentPixels(this)
    }

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
    if (percentage == 1f) return Dimension.FillAvailable
    return Dimension.AvailableSizeFraction(percentage)
}

/**
 * Indicates that this float value represents a percentage of the content size of the corresponding
 * dimension (width or height).
 */
fun wrap(percentage: Float = 1f): Dimension.ContentSizeFraction {
    if (percentage == 1f) return Dimension.WrapContent
    return Dimension.ContentSizeFraction(percentage)
}