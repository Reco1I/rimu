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
            invalidateLayout()
        }

    /**
     * The spacing between children.
     */
    var spacing: Dimension.Fixed = Dimension.Zero
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }


    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {
        var totalWidth = 0f
        var totalHeight = 0f

        val childWithFillSize = mutableMapOf<UIComponent, Float>()

        for (child in this) {

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

            val childMeasuredSize = child.measure(contentConstraints)

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

        val childCount = children.size

        totalWidth += if (childCount > 1) spacing.toPixels() * (childCount - 1) else 0f
        totalHeight += if (childCount > 1) spacing.toPixels() * (childCount - 1) else 0f

        if (childWithFillSize.isNotEmpty()) {
            val freeWidth = (contentConstraints.availableWidth - totalWidth).coerceAtLeast(0f) / childWithFillSize.size
            val freeHeight = (contentConstraints.availableHeight - totalHeight).coerceAtLeast(0f) / childWithFillSize.size

            for ((child, factor) in childWithFillSize) {

                val constraints = when (orientation) {

                    Orientation.Horizontal -> {
                        val assignedPiece = freeWidth * factor

                        contentConstraints.copy(
                            minWidth = assignedPiece,
                            maxWidth = assignedPiece,
                        )
                    }

                    Orientation.Vertical -> {
                        val assignedPiece = freeHeight * factor

                        contentConstraints.copy(
                            minHeight = assignedPiece,
                            maxHeight = assignedPiece,
                        )
                    }
                }

                val childMeasuredSize = child.measure(constraints)

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

        val measuredSize = Vec2(totalWidth, totalHeight).expand(padding.toVec4())

        return parentConstraints.constrain(measuredSize)
    }

    override fun onLayout(bounds: Vec4) {

        var currentX = 0f
        var currentY = 0f

        forEach { child ->

            val childBounds = Vec4(
                x = currentX,
                y = currentY,
                z = child.measuredSize.x,
                w = child.measuredSize.y
            )

            child.layout(childBounds)

            when (orientation) {
                Orientation.Horizontal -> currentX += childBounds.z + spacing.toPixels()
                Orientation.Vertical -> currentY += childBounds.w + spacing.toPixels()
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

