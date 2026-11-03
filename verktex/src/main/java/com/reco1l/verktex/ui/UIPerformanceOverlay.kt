package com.reco1l.verktex.ui

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.PerformanceStats
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.spans
import kotlin.math.min

/**
 * Shows the frame rate and frame time in one line in the bottom right corner, pressing it shows a
 * panel with the draw and update times, their breakdown and a graph of the last frame times.
 * Enabled with the `ShowPerformanceOverlay` setting, drawn above everything else.
 */
class UIPerformanceOverlay : UIClickableContainer() {

    // The one line summary shown while collapsed.
    private val summaryText = UIText().apply {
        anchor = Anchor.BottomRight
        origin = Anchor.BottomRight
        padding = Padding(8f.dip, 4f.dip)
        radius = 8f.dip
        backgroundColor = BACKGROUND_COLOR
        fontSize = 12f.dip
    }

    private val fpsText = UIText().apply {
        fontSize = 18f.dip
        fontWeight = Font.Weight.Bold
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }

    private val frameTimeText = UIText().apply {
        fontSize = 12f.dip
        color = SECONDARY_TEXT_COLOR
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
    }

    private val drawTimeText = statValue()
    private val updateTimeText = statValue()
    private val drawCallsText = statValue()
    private val layoutTimeText = statValue()
    private val recordTimeText = statValue()
    private val submitTimeText = statValue()

    private val panel = UILinearContainer()

    private var timeSinceTextUpdate = TEXT_INTERVAL

    /**
     * Whether the full panel is shown instead of the one line summary.
     */
    var isExpanded = false
        set(value) {
            if (field == value) return
            field = value
            summaryText.isVisible = !value
            panel.isVisible = value
            // Shown with the current values instead of waiting for the next refresh.
            timeSinceTextUpdate = TEXT_INTERVAL
        }


    init {
        anchor = Anchor.BottomRight
        origin = Anchor.BottomRight
        x = (-8f).dip
        y = (-8f).dip

        onActionUp = { isExpanded = !isExpanded }

        +summaryText

        +panel.apply {
            orientation = UILinearContainer.Orientation.Vertical
            anchor = Anchor.BottomRight
            origin = Anchor.BottomRight
            width = PANEL_WIDTH.dip
            padding = Padding(PANEL_PADDING.dip)
            spacing = 8f.dip
            radius = 8f.dip
            backgroundColor = BACKGROUND_COLOR
            isVisible = false

            +UIContainer().apply {
                width = fill()
                +fpsText
                +frameTimeText
            }

            +UIContainer().apply {
                width = fill()
                +Graph()

                // Guide labels, placed just above their lines.
                for ((label, frameTime) in listOf("120" to FRAME_TIME_120, "60" to FRAME_TIME_60)) {
                    +UIText().apply {
                        text = label
                        fontSize = 9f.dip
                        color = GUIDE_COLOR
                        padding = Padding(3f.dip, 0f.dip)
                        radius = 3f.dip
                        backgroundColor = BACKGROUND_COLOR
                        anchor = Anchor.TopRight
                        origin = Anchor.BottomRight
                        x = (-4f).dip
                        y = (GRAPH_HEIGHT * (1f - frameTime / MAX_FRAME_TIME)).dip
                    }
                }
            }

            +UILinearContainer().apply {
                width = fill()
                spacing = STAT_SPACING.dip

                +stat("DRAW", drawTimeText)
                +stat("UPDATE", updateTimeText)
                +stat("DRAW CALLS", drawCallsText)
            }

            // Where the draw time goes.
            +UILinearContainer().apply {
                width = fill()
                spacing = STAT_SPACING.dip

                +stat("LAYOUT", layoutTimeText)
                +stat("RECORD", recordTimeText)
                +stat("SUBMIT", submitTimeText)
            }
        }
    }


    private fun statValue() = UIText().apply {
        fontSize = 13f.dip
    }

    /**
     * A column with a caption above a value, all three share the panel width.
     */
    private fun stat(caption: String, value: UIText) = UILinearContainer().apply {
        orientation = UILinearContainer.Orientation.Vertical
        width = ((PANEL_WIDTH - 2 * PANEL_PADDING - 2 * STAT_SPACING) / 3).dip
        spacing = 1f.dip

        +UIText().apply {
            text = caption
            fontSize = 9f.dip
            color = SECONDARY_TEXT_COLOR
        }
        +value
    }


    override fun onUpdate(clock: FrameClock) {
        // Re-measuring the texts is not free, so they refresh a few times per second only.
        timeSinceTextUpdate += clock.elapsedFrameTime

        if (timeSinceTextUpdate >= TEXT_INTERVAL) {
            timeSinceTextUpdate = 0f

            val fps = PerformanceStats.fps
            val fpsColor = colorFor(if (fps > 0f) 1000f / fps else 0f)

            if (!isExpanded) {
                summaryText.spans {
                    color(fpsColor) { bold { +"%.0f FPS".format(fps) } }
                    color(SECONDARY_TEXT_COLOR) { +"  %.1f ms".format(PerformanceStats.frameTime) }
                }
                super.onUpdate(clock)
                return
            }

            fpsText.text = "%.0f FPS".format(fps)
            fpsText.color = fpsColor
            frameTimeText.text = "%.1f ms · max %.1f ms".format(PerformanceStats.frameTime, PerformanceStats.maxFrameTime)
            drawTimeText.text = "%.1f ms".format(PerformanceStats.drawTime)
            updateTimeText.text = "%.2f ms".format(PerformanceStats.updateTime)
            drawCallsText.text = PerformanceStats.drawCommands.toString()
            layoutTimeText.text = "%.2f ms".format(PerformanceStats.layoutTime)
            recordTimeText.text = "%.2f ms".format(PerformanceStats.recordTime)
            submitTimeText.text = "%.2f ms".format(PerformanceStats.submitTime)
        }

        super.onUpdate(clock)
    }


    /**
     * One bar per frame with guide lines at 120 and 60 fps.
     */
    private class Graph : UICanvas() {

        init {
            width = fill()
            height = GRAPH_HEIGHT.dip
            radius = 4f.dip
            backgroundColor = GRAPH_BACKGROUND_COLOR
        }

        override fun onDraw(renderer: GraphicsRenderer) = with(renderer) {
            val width = bounds.width
            val height = bounds.height
            val barWidth = width / PerformanceStats.HISTORY_SIZE

            // All bars share one draw call, as a plain quad shader needs no per-rectangle uniforms.
            setState(shader = BuiltIn.Shaders.SolidQuad, uniformSet = GraphicsShader.UniformSet())

            drawGeometry { transform, baseColor ->
                for (i in 0 until PerformanceStats.HISTORY_SIZE) {
                    val frameTime = PerformanceStats.frameTimeAt(i)
                    if (frameTime <= 0f) continue

                    val barHeight = min(frameTime / MAX_FRAME_TIME, 1f) * height
                    val color = colorFor(frameTime).copy(alpha = baseColor.alpha)

                    val x = i * barWidth
                    val y = height - barHeight

                    val (x0, y0) = transform.transform(x, y)
                    val (x1, y1) = transform.transform(x + barWidth, y)
                    val (x2, y2) = transform.transform(x + barWidth, height)
                    val (x3, y3) = transform.transform(x, height)

                    addVertex(x0, y0, 0f, 0f, color)
                    addVertex(x1, y1, 1f, 0f, color)
                    addVertex(x2, y2, 1f, 1f, color)

                    addVertex(x0, y0, 0f, 0f, color)
                    addVertex(x2, y2, 1f, 1f, color)
                    addVertex(x3, y3, 0f, 1f, color)
                }
            }

            for (guide in floatArrayOf(FRAME_TIME_120, FRAME_TIME_60)) {
                drawRect(0f, height - guide / MAX_FRAME_TIME * height, width, 1f, GUIDE_COLOR)
            }
        }
    }


    companion object {
        private const val TEXT_INTERVAL = 0.25f
        private const val PANEL_WIDTH = 300f
        private const val PANEL_PADDING = 10f
        private const val STAT_SPACING = 6f
        private const val GRAPH_HEIGHT = 56f

        // Milliseconds, the graph is full at 30 fps.
        private const val MAX_FRAME_TIME = 1000f / 30f
        private const val FRAME_TIME_120 = 1000f / 120f
        private const val FRAME_TIME_60 = 1000f / 60f

        private val BACKGROUND_COLOR = Color4(0xCC11111B)
        private val GRAPH_BACKGROUND_COLOR = Color4(0x40000000)
        private val SECONDARY_TEXT_COLOR = Color4(0xFFA6ADC8)
        private val GUIDE_COLOR = Color4(0x80FFFFFF)
        private val GOOD_COLOR = Color4(0xFFA6E3A1)
        private val OK_COLOR = Color4(0xFFF9E2AF)
        private val BAD_COLOR = Color4(0xFFF38BA8)

        /**
         * Green up to 120 fps, yellow up to 60 fps, red below.
         */
        private fun colorFor(frameTime: Float) = when {
            frameTime <= FRAME_TIME_120 -> GOOD_COLOR
            frameTime <= FRAME_TIME_60 -> OK_COLOR
            else -> BAD_COLOR
        }
    }
}
