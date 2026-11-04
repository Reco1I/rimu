package com.reco1l.verktex.ui.container

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.findScene

inline fun UIContainer.extendIntoSafeAreaContainer(builder: UIExtendIntoSafeAreaContainer.() -> Unit): UIExtendIntoSafeAreaContainer {
    return UIExtendIntoSafeAreaContainer().apply(builder).also(::plusAssign)
}

/**
 * A container laid out as any other, which grows into the safe area of its scene on the sides it is
 * placed against, reaching the edges of the scene (the window, unless the scene is embedded).
 *
 * Unlike [UIIgnoreParentPaddingContainer], it only grows into the safe area, not into any padding
 * of its parent, and only on the sides that touch it.
 *
 * Its children are laid out where they would be without growing, so they stay in the safe area,
 * while the [background] covers the whole container.
 */
open class UIExtendIntoSafeAreaContainer : UIContainer() {

    /**
     * Drawn below the children and laid out over the whole container, including the part grown
     * into the safe area.
     */
    var background: UIComponent? = null
        set(value) {
            if (field === value) return
            field?.parent = null
            field = value
            value?.parent = this
            invalidateLayout()
        }

    /**
     * How much the container grew into the safe area on each side after the last layout pass, as
     * left, top, right and bottom.
     */
    var safeAreaExtent = Vec4.Zero
        private set


    override fun adjustBounds(bounds: Vec4): Vec4 {
        // Against the edges of the scene it's in, the window unless the scene is embedded, e.g. in
        // the stage of the UI test browser.
        val scene = findScene()
        val safeArea = scene?.safeArea ?: Verktex.graphics.window.insets.safeArea
        val sceneWidth = scene?.bounds?.width ?: Verktex.graphics.window.windowWidth.toFloat()
        val sceneHeight = scene?.bounds?.height ?: Verktex.graphics.window.windowHeight.toFloat()

        // The layout positions of the ancestors up to the scene, the transformations are only
        // applied when drawing and aren't up to date yet.
        var sceneX = bounds.x
        var sceneY = bounds.y
        var ancestor = parent
        while (ancestor != null && ancestor !== scene) {
            sceneX += ancestor.bounds.x
            sceneY += ancestor.bounds.y
            ancestor = ancestor.parent
        }

        val sceneRight = sceneX + bounds.width
        val sceneBottom = sceneY + bounds.height

        // A side is placed against the edge of the scene when it reaches the safe area.
        fun extent(inset: Float, distanceToEdge: Float) =
            if (inset > 0f && distanceToEdge <= inset + TOLERANCE) distanceToEdge.coerceAtLeast(0f) else 0f

        safeAreaExtent = Vec4(
            x = extent(safeArea.x, sceneX),
            y = extent(safeArea.y, sceneY),
            z = extent(safeArea.z, sceneWidth - sceneRight),
            w = extent(safeArea.w, sceneHeight - sceneBottom)
        )

        return Vec4(
            x = bounds.x - safeAreaExtent.x,
            y = bounds.y - safeAreaExtent.y,
            z = bounds.z + safeAreaExtent.z,
            w = bounds.w + safeAreaExtent.w
        )
    }

    override fun onLayout(bounds: Vec4) {
        background?.let {
            it.measure(LayoutConstraints(bounds.width, bounds.width, bounds.height, bounds.height))
            it.layout(Vec4(0f, 0f, bounds.width, bounds.height))
        }

        val contentBounds = contentBounds(bounds)
        val extent = safeAreaExtent

        val childBounds = Vec4(
            x = contentBounds.x + extent.x,
            y = contentBounds.y + extent.y,
            z = contentBounds.z - extent.z,
            w = contentBounds.w - extent.w
        )

        forEach { child -> child.layout(childBounds) }
    }

    override fun calculateTransformations() {
        super.calculateTransformations()
        background?.calculateTransformations()
    }

    override fun onUpdate(clock: FrameClock) {
        background?.update(clock)
        super.onUpdate(clock)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        background?.draw(renderer)
        super.onDraw(renderer)
    }


    private companion object {
        const val TOLERANCE = 1f
    }
}
