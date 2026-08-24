package com.reco1l.verktex.ui

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.toPadding
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.TickingClock
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


    override fun onDraw(renderer: GraphicsRenderer) {

        // Padding will be applied to the scene's content, but not to the background since it is not
        // present on the children list.
        padding = Verktex.graphics.window.insets.safeArea.toPadding()

        onPerformLayout()

        background?.draw(renderer)
        super.onDraw(renderer)
    }


    /**
     * Called when the scene should process layout invalidations.
     */
    open fun onPerformLayout() {

        val windowWidth = Verktex.graphics.window.width.toFloat()
        val windowHeight = Verktex.graphics.window.height.toFloat()

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
        override fun onUpdate(clock: TickingClock) {}
    }
}