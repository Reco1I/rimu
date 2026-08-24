package com.osudroid.game.replay

import com.osudroid.math.Precision
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.linearContainer
import com.reco1l.verktex.ui.text
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import org.anddev.andengine.input.touch.TouchEvent

/**
 * A panel that contains settings for replay playback. Can be expanded and collapsed by tapping the button on the left.
 */
class ReplaySettingsPanel : UIContainer() {
    lateinit var playbackControl: ReplayPlaybackControl
        private set

    lateinit var visualSettingsControl: ReplayVisualSettingsControl
        private set

    private val isExpanded
        get() = Precision.almostEquals(x, 0f)

    private val elementContainer = UIScrollableContainer().apply {
        x = BUTTON_WIDTH

        scrollAxes = Axis.Y
        width = PANEL_WIDTH
        height = Dimension.FillAvailable
        showVerticalIndicator = false

        linearContainer {
            width = Dimension.FillAvailable
            spacing = 20f
            padding = Vec4(0f, 20f)
            orientation = Orientation.Vertical

            addControls()
        }
    }

    init {
        x = PANEL_WIDTH
        height = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        alpha = 0f
        elementContainer.isVisible = false

        +ReplaySettingsPanelButton()
        +elementContainer
    }

    override fun onLoadComplete() {
        fadeTo(0.5f, 0.2f)
    }

    fun expand() {
        if (isExpanded) {
            return
        }

        elementContainer.isVisible = true

        clearEntityModifiers()
        moveToX(0f, 0.2f)
        fadeIn(0.2f)
    }

    fun collapse() {
        if (!isExpanded) {
            return
        }

        clearEntityModifiers()
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
            backgroundColor = Color4(0xFF181825)
            radius = BUTTON_RADIUS

            setSize(BUTTON_WIDTH, 150f)
            y = -125f

            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft

            text {
                rotationZ = -90f
                anchor = Anchor.Center
                origin = Anchor.Center
                fontSize = FontSize.SM
                text = "Settings"
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float) = when {
            event.isActionDown || event.isActionMove -> true

            event.isActionUp -> {
                if (isExpanded) {
                    collapse()
                } else {
                    expand()
                }

                false
            }

            else -> false
        }
    }

    companion object {
        private const val PANEL_WIDTH = 440f
        private const val BUTTON_WIDTH = 48f
        private const val BUTTON_RADIUS = 12f
    }
}
