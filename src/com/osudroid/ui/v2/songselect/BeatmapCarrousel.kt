package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIRecyclerContainer
import kotlin.math.*

class BeatmapSetModel(
    val beatmapSetInfo: BeatmapSetInfo,

    /**
     * Whether the panel stands for a single difficulty, so it shows its name as well.
     */
    val isDifficulty: Boolean = false,
    var coverTexture: GraphicsTexture.Region? = null,
    var isLoadingCover: Boolean = false,
) {
    override fun toString(): String {
        return beatmapSetInfo.path
    }
}

class BeatmapCarrousel : UIRecyclerContainer<BeatmapSetModel, BeatmapSetPanel>(), IPanelContainer<BeatmapSetPanel> {

    /**
     * The currently selected beatmap set panel.
     */
    override var selectedPanel: BeatmapSetPanel? = null
        set(value) {
            if (field != value) {
                field = value

                boundComponents.values.forEach { panel ->
                    if (panel != value) {
                        panel.collapse()
                    } else {
                        panel.expand()
                    }
                }
            }
        }

    /**
     * Whether to automatically scroll to the selected panel.
     */
    var autoScrollToSelectedPanel = false

    /**
     * How far the panels go past the right edge of the carrousel, so their end is never seen and
     * they seem to continue past the screen. It covers the safe area on the right too.
     */
    var panelOverhang = 0f
        private set


    init {
        scrollAxes = Axis.Y
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        spacing = (-2f).px

        componentWrapper.padding = Padding(0f.px, 200f.px)

        onCreateComponent = { BeatmapSetPanel(this) }
    }


    private fun getPositionPercentage(sceneY: Float, componentHeight: Float): Float {
        val positionInView = sceneY + componentHeight / 2f - toSceneSpace(0f, 0f).y

        return positionInView / bounds.height
    }

    override fun onUpdate(clock: FrameClock) {

        panelOverhang = Verktex.graphics.window.insets.safeArea.z + OVERHANG.dip.toPixels()

        // The panels follow a curve along the height of the container.
        boundComponents.values.forEach { component ->
            val button = component.button

            component.curveOffset = SHEAR * (1f - getPositionPercentage(button.toSceneSpace(0f, 0f).y, button.bounds.height))

            component.panelsContainer.forEach { panel ->
                panel as BeatmapPanel
                panel.curveOffset = SHEAR * (1f - getPositionPercentage(panel.toSceneSpace(0f, 0f).y, panel.bounds.height))
            }
        }

        val panel = selectedPanel
        if (panel != null && autoScrollToSelectedPanel) {

            val panelY = panel.selectedPanelSceneY - toSceneSpace(0f, 0f).y + scrollY - bounds.height / 2f
            val difference = panelY - scrollY

            var targetY = scrollY

            if (abs(difference) > 5f) {
                targetY += difference * 0.2f
            } else {
                targetY = panelY
                autoScrollToSelectedPanel = false
            }

            scrollY = targetY.coerceAtLeast(0f).coerceAtMost(maxScrollY)
        }

        super.onUpdate(clock)
    }

    override fun onInputEvent(event: InputEvent): Boolean {
        if (event is PointerEvent && event.action == PointerEvent.Action.Down && containsScenePoint(event.x, event.y)) {
            autoScrollToSelectedPanel = false
        }
        return super.onInputEvent(event)
    }


    companion object {
        private const val SHEAR = 180f
        private const val OVERHANG = 32f
    }
}
