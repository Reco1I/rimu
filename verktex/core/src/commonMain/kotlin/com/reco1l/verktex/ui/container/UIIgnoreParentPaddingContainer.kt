package com.reco1l.verktex.ui.container

import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.fill

inline fun UIContainer.ignoreParentPaddingContainer(builder: UIIgnoreParentPaddingContainer.() -> Unit): UIIgnoreParentPaddingContainer {
    return UIIgnoreParentPaddingContainer().apply(builder).also(::plusAssign)
}

/**
 * A container covering its parent including its padding, e.g. the safe area of a [com.reco1l.verktex.ui.UIScene],
 * so its background reaches the edges of the window, like the backdrop of a modal.
 *
 * Its children keep to the parent's padding by default (see [keepsParentPadding]), so they stay in
 * the safe area and follow it, e.g. while the on-screen keyboard shifts it.
 */
open class UIIgnoreParentPaddingContainer : UIContainer() {

    /**
     * Whether the children are laid out inside the parent's padding, which is then this container's
     * padding. Without it they can cover the whole parent too.
     */
    var keepsParentPadding = true


    init {
        width = fill()
        height = fill()
    }


    override fun adjustParentConstraints(constraints: LayoutConstraints): LayoutConstraints {
        val parent = parent ?: return constraints

        val horizontal = parent.paddingLeft.toPixels() + parent.paddingRight.toPixels()
        val vertical = parent.paddingTop.toPixels() + parent.paddingBottom.toPixels()

        return LayoutConstraints(
            minWidth = constraints.minWidth + horizontal,
            maxWidth = constraints.maxWidth + horizontal,
            minHeight = constraints.minHeight + vertical,
            maxHeight = constraints.maxHeight + vertical
        )
    }

    override fun layout(assignedBounds: Vec4) {
        val parent = parent ?: return super.layout(assignedBounds)

        if (keepsParentPadding && padding != parent.padding) {
            padding = parent.padding
        }

        // The parent lays its children out inside its padding, this one covers it as well.
        super.layout(Vec4(
            x = assignedBounds.x - parent.paddingLeft.toPixels(),
            y = assignedBounds.y - parent.paddingTop.toPixels(),
            z = assignedBounds.z + parent.paddingRight.toPixels(),
            w = assignedBounds.w + parent.paddingBottom.toPixels()
        ))
    }
}
