package com.osudroid.ui.v2.hud.editor

import com.osudroid.ui.v2.hud.GameplayHUD
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.osudroid.ui.v2.hud.HUDElements
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.transformSequence
import kotlin.math.abs
import kotlin.math.min

class HUDElementPreview(private val element: HUDElement, val hud: GameplayHUD): UIContainer() {


    private val label = UIText().apply {
        fontSize = 14f.dip
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        text = element.name
        color = Color4.White
    }

    init {
        width = Dimension.FillAvailable
        height = 120f.px
        padding = Padding(12f.px)
        scaleOrigin = Anchor.Center
        background = Background.Color(Color4(0xFF363653))
        radius = CornerRadius(12f.px)

        +element
        +label

        element.setSkinData(
            HUDElementSkinData(
                type = HUDElements[element::class].name,
                anchor = Anchor.TopLeft,
                origin = Anchor.TopLeft,
            )
        )
    }

    override fun onUpdate(clock: FrameClock) {

        // Scaling the element inside the box
        val elementSize = element.measuredSize

        if (elementSize.x > 0f && elementSize.y > 0f) {
            val inner = padding.toVec4()

            element.scale = if (elementSize.x > elementSize.y) {
                min(1f, (bounds.width - inner.x - inner.z) / elementSize.x)
            } else {
                min(1f, (bounds.height - inner.y - inner.w - label.measuredSize.y) / elementSize.y)
            }
        }

        super.onUpdate(clock)
    }

    //region Input handling
    private var initialX = 0f
    private var initialY = 0f
    private var isPressed = false

    // The element inside is only a preview, so it never receives input.
    override fun onInputEvent(event: InputEvent): Boolean {

        if (event !is PointerEvent) {
            return false
        }

        if (event.action == PointerEvent.Action.Down) {
            isPressed = containsScenePoint(event.x, event.y)
            initialX = event.x
            initialY = event.y
        }

        if (event.action == PointerEvent.Action.Up && isPressed) {
            isPressed = false

            if (abs(event.x - initialX) < 1f && abs(event.y - initialY) < 1f) {
                clearTransforms()
                transformSequence {
                    scaleTo(0.9f, 0.1f)
                    then()
                    scaleTo(1f, 0.1f)
                }
                hud.addElement(HUDElementSkinData(HUDElements[element::class].name))
                hud.elementSelector?.collapse()
            }
        }

        return false
    }
    //endregion

}
