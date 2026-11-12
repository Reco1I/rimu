package com.osudroid.game.replay

import com.osudroid.math.Precision
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.moveToX
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px

/**
 * A panel that contains settings for replay playback. Can be expanded and collapsed by tapping the button on the left.
 */
class ReplaySettingsPanel : UIContainer() {
    lateinit var playbackControl: ReplayPlaybackControl
        private set

    lateinit var visualSettingsControl: ReplayVisualSettingsControl
        private set

    private val isExpanded
        get() = Precision.almostEquals(translationX.resolve(0f), 0f)

    private val elementContainer = UIScrollableContainer().apply {
        x = BUTTON_WIDTH.px

        scrollAxes = Axis.Y
        width = PANEL_WIDTH.px
        height = Dimension.FillAvailable
        showVerticalIndicator = false

        linearContainer {
            width = Dimension.FillAvailable
            spacing = 20f.dip
            padding = Padding(0f.dip, 20f.dip)
            orientation = Orientation.Vertical

            addControls()
        }
    }

    init {
        translationX = PANEL_WIDTH.px
        height = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        alpha = 0f
        elementContainer.isVisible = false

        +ReplaySettingsPanelButton()
        +elementContainer
    }

    override fun onAttached() {
        super.onAttached()
        fadeTo(0.5f, 0.2f)
    }

    fun expand() {
        if (isExpanded) {
            return
        }

        elementContainer.isVisible = true

        clearTransforms()
        moveToX(0f, 0.2f)
        fadeIn(0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }

        clearTransforms()
        moveToX(PANEL_WIDTH, 0.2f)
        fadeTo(0.5f, 0.2f).after { elementContainer.isVisible = false }
    }

    private fun UILinearContainer.addControls() {
        playbackControl = ReplayPlaybackControl()
        +playbackControl

        visualSettingsControl = ReplayVisualSettingsControl()
        +visualSettingsControl
    }

    private inner class ReplaySettingsPanelButton : UIContainer() {
        init {
            background = Background.Color(Color4(0xFF181825))
            radius = CornerRadius(BUTTON_RADIUS.px)

            setSize(BUTTON_WIDTH, 150f)
            y = (-125f).px

            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft

            text {
                rotationZ = -90f
                anchor = Anchor.Center
                origin = Anchor.Center
                text = "Settings"
            }
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            if (event !is PointerEvent || !containsScenePoint(event.x, event.y)) {
                return false
            }

            if (event.action == PointerEvent.Action.Up) {
                if (isExpanded) {
                    collapse()
                } else {
                    expand()
                }
            }

            return event.action != PointerEvent.Action.Cancel
        }
    }

    companion object {
        private const val PANEL_WIDTH = 440f
        private const val BUTTON_WIDTH = 48f
        private const val BUTTON_RADIUS = 12f
    }
}
