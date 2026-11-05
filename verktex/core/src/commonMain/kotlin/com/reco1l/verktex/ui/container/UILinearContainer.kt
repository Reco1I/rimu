package com.reco1l.verktex.ui.container

import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.ui.UIComponent
import kotlin.math.*

inline fun UIContainer.linearContainer(builder: UILinearContainer.() -> Unit): UILinearContainer {
    return UILinearContainer().apply(builder).also(::plusAssign)
}

open class UILinearContainer : UIContainer() {

    /**
     * The orientation of the container.
     */
    var orientation = Orientation.Horizontal
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The spacing between children.
     */
    var spacing: Dimension.Fixed by themeable(Dimension.Zero) { invalidateMeasure() }


    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {
        var totalWidth = 0f
        var totalHeight = 0f

        val childWithFillSize = mutableMapOf<UIComponent, Float>()

        var visibleChildCount = 0

        for (child in this) {

            // Hidden children don't take any space.
            if (!child.isVisible) {
                continue
            }
            visibleChildCount++

            val widthSpec = child.width
            val heightSpec = child.height

            if (orientation == Orientation.Horizontal && widthSpec is Dimension.AvailableSizeFraction) {
                childWithFillSize[child] = widthSpec.factor.coerceIn(0f, 1f)
                continue
            }

            if (orientation == Orientation.Vertical && heightSpec is Dimension.AvailableSizeFraction) {
                childWithFillSize[child] = heightSpec.factor.coerceIn(0f, 1f)
                continue
            }

            val childMeasuredSize = child.measure(contentConstraints.loosen().withoutFillLimits(child))

            when (orientation) {
                Orientation.Horizontal -> {
                    totalWidth += childMeasuredSize.x
                    totalHeight = max(totalHeight, childMeasuredSize.y)
                }
                Orientation.Vertical -> {
                    totalWidth = max(totalWidth, childMeasuredSize.x)
                    totalHeight += childMeasuredSize.y
                }
            }
        }

        val totalSpacing = if (visibleChildCount > 1) spacing.toPixels() * (visibleChildCount - 1) else 0f

        when (orientation) {
            Orientation.Horizontal -> totalWidth += totalSpacing
            Orientation.Vertical -> totalHeight += totalSpacing
        }

        if (childWithFillSize.isNotEmpty()) {
            val freeWidth = (contentConstraints.availableWidth - totalWidth).coerceAtLeast(0f) / childWithFillSize.size
            val freeHeight = (contentConstraints.availableHeight - totalHeight).coerceAtLeast(0f) / childWithFillSize.size

            for ((child, factor) in childWithFillSize) {

                val constraints = when (orientation) {

                    Orientation.Horizontal -> {
                        val assignedPiece = freeWidth * factor

                        contentConstraints.loosen().copy(
                            minWidth = assignedPiece,
                            maxWidth = assignedPiece,
                        )
                    }

                    Orientation.Vertical -> {
                        val assignedPiece = freeHeight * factor

                        contentConstraints.loosen().copy(
                            minHeight = assignedPiece,
                            maxHeight = assignedPiece,
                        )
                    }
                }

                val childMeasuredSize = child.measure(constraints.withoutFillLimits(child))

                when (orientation) {
                    Orientation.Horizontal -> {
                        totalWidth += childMeasuredSize.x
                        totalHeight = max(totalHeight, childMeasuredSize.y)
                    }
                    Orientation.Vertical -> {
                        totalWidth = max(totalWidth, childMeasuredSize.x)
                        totalHeight += childMeasuredSize.y
                    }
                }
            }
        }

        // The own constraints, so the limits of this container (e.g. minWidth) are what filling
        // children are stretched to.
        val measuredSize = contentConstraints.constrain(Vec2(totalWidth, totalHeight)).expand(padding.toVec4())

        // The main axis keeps the size the child got in the first pass.
        stretchFillingChildren(measuredSize) { child ->
            when (orientation) {
                Orientation.Horizontal -> contentConstraints.loosen().copy(minWidth = child.measuredSize.x, maxWidth = child.measuredSize.x)
                Orientation.Vertical -> contentConstraints.loosen().copy(minHeight = child.measuredSize.y, maxHeight = child.measuredSize.y)
            }
        }

        return measuredSize
    }

    // Fills on the main axis share the free space instead, only the cross axis is stretched.
    override fun fillsWrappedWidth(child: UIComponent) =
        orientation == Orientation.Vertical && super.fillsWrappedWidth(child)

    override fun fillsWrappedHeight(child: UIComponent) =
        orientation == Orientation.Horizontal && super.fillsWrappedHeight(child)


    override fun onLayout(bounds: Vec4) {

        val contentLeft = paddingLeft.toPixels()
        val contentTop = paddingTop.toPixels()
        val contentRight = bounds.width - paddingRight.toPixels()
        val contentBottom = bounds.height - paddingBottom.toPixels()

        var currentX = contentLeft
        var currentY = contentTop

        forEach { child ->

            if (!child.isVisible) {
                return@forEach
            }

            // The cell of a child spans the whole cross axis, so its anchor places it along it,
            // e.g. centered vertically in a horizontal container.
            val childBounds = when (orientation) {
                Orientation.Horizontal -> Vec4(
                    x = currentX,
                    y = contentTop,
                    z = currentX + child.measuredSize.x,
                    w = max(contentBottom, contentTop + child.measuredSize.y)
                )
                Orientation.Vertical -> Vec4(
                    x = contentLeft,
                    y = currentY,
                    z = max(contentRight, contentLeft + child.measuredSize.x),
                    w = currentY + child.measuredSize.y
                )
            }

            child.layout(childBounds)

            when (orientation) {
                Orientation.Horizontal -> currentX += child.measuredSize.x + spacing.toPixels()
                Orientation.Vertical -> currentY += child.measuredSize.y + spacing.toPixels()
            }
        }
    }


    /**
     * Defines the orientation of the container's children layout, either horizontally or vertically.
     */
    enum class Orientation {
        Horizontal,
        Vertical
    }
}

