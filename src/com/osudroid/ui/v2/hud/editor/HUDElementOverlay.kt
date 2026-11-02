package com.osudroid.ui.v2.hud.editor

import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.PaintStyle
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.transformSequence

class HUDElementOverlay(private val element: HUDElement) : UIContainer() {


    /**
     * The position of the outline box that represents the element.
     */
    var outlinePosition = Vec2.Zero
        private set


    private val outline = UIBox().apply {
        paintStyle = PaintStyle.Outline
        color = Color4(0xFFF27272)
        lineWidth = 8f.px
    }

    private val toolbar = UILinearContainer().apply {
        origin = Anchor.BottomCenter
        orientation = UILinearContainer.Orientation.Horizontal
        spacing = 4f.px

        // Toolbar buttons:

        +Button("delete", Color4(0xFF260000)) {
            updateThread {
                element.remove()
            }
        }

        +Button("restore") {
            element.restore()
        }

    }

    private val nameText = UIText().apply {
        origin = Anchor.TopCenter
        fontSize = 14f.dip
        color = Color4(0xFFF27272)
        text = element.name
    }

    private val tips = listOf(
        Tip(Anchor.BottomLeft),
        Tip(Anchor.BottomRight),
        Tip(Anchor.TopLeft),
        Tip(Anchor.TopRight),
    )


    init {
        alpha = 0f
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        +outline
        +toolbar
        +nameText

        tips.forEach { +it }
    }


    /**
     * Updates the outline box to match the element's position and size.
     */
    fun updateOutline() {
        val size = element.transformedSize

        // We need to cancel scale center
        outlinePosition = element.anchoredPosition + element.position - size * element.origin

        outline.x = outlinePosition.x.px
        outline.y = outlinePosition.y.px
        outline.width = size.x.px
        outline.height = size.y.px
    }


    override fun onInputEvent(event: InputEvent): Boolean {
        if (alpha == 1f) {
            return super.onInputEvent(event)
        }
        return false
    }

    override fun onUpdate(clock: FrameClock) {
        updateOutline()

        val size = element.transformedSize
        val centerX = outlinePosition.x + size.x / 2f

        nameText.x = centerX.px
        nameText.y = (outlinePosition.y + size.y).px

        toolbar.x = centerX.px

        // Ensure that the toolbar does not go out of the screen.
        if (outlinePosition.y - toolbar.measuredSize.y - TOOLBAR_MARGIN < 0f) {
            toolbar.origin = Anchor.TopCenter
            toolbar.y = (outlinePosition.y + size.y + TOOLBAR_MARGIN).px
        } else {
            toolbar.origin = Anchor.BottomCenter
            toolbar.y = (outlinePosition.y - TOOLBAR_MARGIN).px
        }

        tips.forEach { tip ->
            tip.x = (outlinePosition.x + size.x * tip.corner.x).px
            tip.y = (outlinePosition.y + size.y * tip.corner.y).px

            // Show only the tips that are not at the origin.
            tip.isVisible = tip.corner.x != element.origin.x && tip.corner.y != element.origin.y
        }

        super.onUpdate(clock)
    }


    /**
     * Represents a tip in the overlay toolbar.
     */
    private inner class Tip(val corner: Vec2) : UIContainer() {

        private var initialX = 0f
        private var initialY = 0f
        private var isDragging = false


        init {
            origin = Anchor.Center
            width = TIP_SIZE.px
            height = TIP_SIZE.px

            // The tip container is bigger than the actual tip in order to make it easier to touch.
            +UIBox().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                backgroundColor = Color4(0xFFF27272)
                radius = TIP_SIZE.px
                width = 0.5f.pct
                height = 0.5f.pct
            }
        }


        override fun onInputEvent(event: InputEvent): Boolean {

            if (!isVisible || event !is PointerEvent) {
                return false
            }

            when (event.action) {
                PointerEvent.Action.Down -> {
                    if (!containsScenePoint(event.x, event.y)) {
                        return false
                    }

                    initialX = event.x
                    initialY = event.y
                    isDragging = true
                    return true
                }

                PointerEvent.Action.Move -> {
                    if (!isDragging) {
                        return false
                    }

                    var deltaX = event.x - initialX
                    var deltaY = event.y - initialY

                    initialX = event.x
                    initialY = event.y

                    if (corner.x == 0f) {
                        deltaX = -deltaX
                    }

                    if (corner.y == 0f) {
                        deltaY = -deltaY
                    }

                    val size = element.transformedSize

                    val scaleX = (element.scaleX + deltaX / size.x).coerceIn(0.5f, 5f)
                    val scaleY = (element.scaleY + deltaY / size.y).coerceIn(0.5f, 5f)

                    element.scale = (scaleX + scaleY) / 2f
                    element.updateConnectionLine()
                    return true
                }

                else -> {
                    val wasDragging = isDragging
                    isDragging = false
                    return wasDragging
                }
            }
        }
    }


    /**
     * Represents a button in the overlay toolbar.
     */
    private inner class Button(texture: String, back: Color4 = Color4(0xFF002626), val action: () -> Unit) : UIContainer() {

        private var isPressed = false


        init {
            width = BUTTON_SIZE.px
            height = BUTTON_SIZE.px
            scaleOrigin = Anchor.Center

            +UIBox().apply {
                radius = 12f.px
                backgroundColor = back
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
            }

            +UISprite().apply {
                this.textureRegion = SkinTextures[texture]
                anchor = Anchor.Center
                origin = Anchor.Center
                width = 0.8f.pct
                height = 0.8f.pct
            }
        }

        override fun onInputEvent(event: InputEvent): Boolean {

            if (event !is PointerEvent) {
                return false
            }

            when (event.action) {
                PointerEvent.Action.Down -> {
                    isPressed = containsScenePoint(event.x, event.y)
                    return isPressed
                }

                PointerEvent.Action.Up -> {
                    if (!isPressed) {
                        return false
                    }
                    isPressed = false

                    action()
                    clearTransforms()
                    transformSequence {
                        scaleTo(0.9f, 0.1f)
                        then()
                        scaleTo(1f, 0.1f)
                    }
                    return true
                }

                else -> return false
            }
        }
    }


    companion object {
        private const val TIP_SIZE = 36f
        private const val BUTTON_SIZE = 46f
        private const val TOOLBAR_MARGIN = 10f
    }

}
