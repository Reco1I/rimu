package com.reco1l.verktex.ui

import com.reco1l.verktex.PerformanceStats
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer

open class UIScene : UIContainer() {

    /**
     * The background component of the scene. This component will be drawn before any other
     * children of the scene and will not be affected by the scene's padding.
     */
    var background: UIComponent? = null


    init {
        width = fill()
        height = fill()
    }


    /**
     * Whether this scene is a child of another component instead of a root of the engine, e.g. a
     * scene shown inside the stage of the UI test browser. Its parent lays it out then.
     */
    val isEmbedded
        get() = parent != null

    /**
     * The safe area the content keeps while the scene is embedded, as left, top, right and bottom,
     * set by its parent. E.g. the UI test browser sets the window's so a scene looks as in the game.
     * A root scene uses the window's instead.
     */
    var embeddedSafeArea = Vec4.Zero

    /**
     * The safe area of the scene, as left, top, right and bottom: the window's for a root scene, or
     * [embeddedSafeArea] while embedded.
     */
    val safeArea
        get() = if (isEmbedded) embeddedSafeArea else Verktex.graphics.window.insets.safeArea


    override fun onDraw(renderer: GraphicsRenderer) {

        if (isEmbedded) {
            padding = Padding(
                left = embeddedSafeArea.x.px,
                top = embeddedSafeArea.y.px,
                right = embeddedSafeArea.z.px,
                bottom = embeddedSafeArea.w.px
            )
            layoutEmbeddedBackground()
            background?.draw(renderer)
            super.onDraw(renderer)
            return
        }

        // A background left from when the scene was embedded would follow its old transformations.
        background?.let { if (it.parent === this) it.parent = null }

        // Padding will be applied to the scene's content, but not to the background since it is not
        // present on the children list.
        // The safe area is shifted up while the on-screen keyboard would cover the focused input.
        val safeArea = Verktex.graphics.window.insets.safeArea
        val imeShift = Verktex.imeShift
        padding = Padding(
            left = safeArea.x.px,
            top = (safeArea.y - imeShift).px,
            right = safeArea.z.px,
            bottom = (safeArea.w + imeShift).px
        )

        val layoutStartNanos = System.nanoTime()
        onPerformLayout()
        PerformanceStats.addLayoutTime(System.nanoTime() - layoutStartNanos)

        background?.draw(renderer)
        super.onDraw(renderer)
    }


    override fun onUpdate(clock: FrameClock) {
        background?.update(clock)
        super.onUpdate(clock)
    }

    /**
     * Lays the background out over the scene, which its parent laid out. The scene is the parent of
     * its background meanwhile, so the background follows the transformations of the scene and its
     * parents (e.g. a scaled down stage). It isn't one of the children, those are laid out with the
     * padding.
     */
    private fun layoutEmbeddedBackground() {
        val background = background ?: return
        val width = bounds.width
        val height = bounds.height

        if (background.parent !== this) {
            background.parent = this
        }

        background.measure(LayoutConstraints(
            minWidth = width,
            maxWidth = width,
            minHeight = height,
            maxHeight = height
        ))
        background.layout(Vec4(x = 0f, y = 0f, z = width, w = height))

        // The scene doesn't tell its background when it moves, as it does with its children.
        background.calculateTransformations()
    }

    /**
     * Called when the scene should process layout invalidations.
     */
    open fun onPerformLayout() {

        val windowWidth = Verktex.graphics.window.windowWidth.toFloat()
        val windowHeight = Verktex.graphics.window.windowHeight.toFloat()

        val constraints = LayoutConstraints(
            minWidth = 0f,
            maxWidth = windowWidth,
            minHeight = 0f,
            maxHeight = windowHeight
        )

        measure(constraints)
        background?.measure(constraints)

        val bounds = Vec4(
            x = 0f,
            y = 0f,
            z = windowWidth,
            w = windowHeight
        )

        layout(bounds)
        background?.layout(bounds)
    }


    /**
     * Called when this scene becomes the current one, either pushed or shown again after the scene
     * above it was popped.
     */
    open fun onEnter() {}

    /**
     * Called when this scene stops being the current one, either popped or covered by a new scene.
     */
    open fun onLeave() {}

    open fun onGameWindowFocusChange(hasFocus: Boolean) {
        // Override this method to handle game window focus changes.
    }


    /**
     * A special scene that represents an empty or default state. This scene is used as a placeholder
     * when there are no other scenes in the stack.
     *
     * It does not perform any drawing or updating operations and cannot get children added to it.
     * It serves as a base case for the scene stack, ensuring that there is always at least one scene
     * present.
     */
    companion object EmptyScene : UIScene() {
        override fun onDraw(renderer: GraphicsRenderer) {}
        override fun onUpdate(clock: FrameClock) {}
    }
}


/**
 * The scene this component is in, the closest one if scenes are nested, e.g. a scene embedded in
 * the stage of the UI test browser.
 */
internal fun UIComponent.findScene(): UIScene? {
    var current = parent
    while (current != null) {
        if (current is UIScene) return current
        current = current.parent
    }
    return null
}
