package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.borderColor
import android.util.Log
import com.osudroid.data.BeatmapInfo
import com.osudroid.ui.OsuColors
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.interpolateColor
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import kotlin.math.abs
import kotlin.random.*
import com.reco1l.verktex.ui.themed

/**
 * Moves towards [end], jumping to it once it is closer than [snapBy].
 */
internal fun FrameClock.interpolateWithSnap(duration: Float, start: Float, end: Float, snapBy: Float): Float {
    return if (abs(end - start) < snapBy) end else interpolateFloat(duration, start, end)
}


class BeatmapSetPanel(val beatmapCarrousel: BeatmapCarrousel) : RecyclableComponent<BeatmapSetModel>(), IPanelContainer<BeatmapPanel> {

    override val isRecyclable: Boolean
        get() = !button.isPressed && !isExpanded && !isTransitioning


    /**
     * The component the carrousel scrolls to: the selected beatmap panel, or the button while none.
     */
    val scrollTarget: UIComponent
        get() = selectedPanel ?: button


    /**
     * The currently selected beatmap panel.
     */
    override var selectedPanel: BeatmapPanel? = null
        set(value) {
            if (field != value) {
                field = value

                if (beatmapCarrousel.selectedPanel == this) {
                    beatmapCarrousel.onBeatmapSelected?.invoke(value?.beatmapInfo)
                }

                panelsContainer.forEach { panel ->
                    panel as BeatmapPanel
                    if (panel != value) {
                        panel.collapse()
                    } else {
                        panel.expand()
                    }
                }

                if (value != null) {
                    beatmapCarrousel.autoScrollToSelectedPanel = true
                }
            }
        }

    /**
     * Whether the panel is expanded or not.
     */
    var isExpanded = false
        private set


    /**
     * The container for the beatmap panels.
     */
    lateinit var panelsContainer: UILinearContainer

    /**
     * The button itself.
     */
    lateinit var button: UIClickableContainer


    /**
     * The horizontal offset of the button given by the curve of the [beatmapCarrousel].
     */
    var curveOffset = 0f
        set(value) {
            field = value
            updateTranslation()
        }


    private var isTransitioning = false

    private var buttonExtraWidth = 0f
    private var buttonGlow = 0f
    private var panelsExpansion = 0f

    private lateinit var cover: UISprite
    private lateinit var titleText: UIText
    private lateinit var artistText: UIText
    private lateinit var difficultyIndicators: UILinearContainer


    init {
        width = fill()

        linearContainer {
            orientation = Orientation.Vertical
            width = fill()
            spacing = 4f.dip

            button = clickableContainer {
                width = fill()
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                onActionLongPress = { beatmapCarrousel.onBeatmapSetLongPress?.invoke(this@BeatmapSetPanel) }
                onActionUp = { select() }
                borderColor = themed { palette.accent.alpha(0.3f) }
                borderWidth = 1f.dip
                radius = themed { radius.xl }

                cover = sprite {
                    width = fill()
                    height = fill()
                    scaleType = UISprite.ScaleType.Crop
                    backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.5f) }
                    radius = themed { radius.xl }
                }

                box {
                    width = fill()
                    height = fill()
                    backgroundColor = Color4.Black.alpha(0.6f)
                    radius = themed { radius.xl }
                }

                linearContainer {
                    orientation = Orientation.Vertical
                    width = fill()
                    padding = Padding(10f.dip)
                    spacing = 1f.dip

                    titleText = text {
                        text = "Unknown"
                        fontSettings = themed { typography.h3 }
                        fontSize = themed { typography.body.size }
                    }

                    artistText = text {
                        text = "Unknown"
                        fontSettings = themed { typography.caption }
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.8f) }
                    }

                    difficultyIndicators = linearContainer {
                        spacing = 2f.dip
                        padding = Padding(2f.dip)
                    }
                }
            }

            panelsContainer = linearContainer {
                orientation = Orientation.Vertical
                width = fill()
                height = wrap(0f)
                alpha = 0f
                spacing = 4f.dip
            }
        }
    }


    override fun onBind(data: BeatmapSetModel) {
        titleText.text = data.beatmapSetInfo.beatmaps.getOrNull(0)?.titleText ?: "Unknown"
        val firstBeatmap = data.beatmapSetInfo.beatmaps.getOrNull(0)
        val artist = firstBeatmap?.artistText ?: "Unknown"

        artistText.text = if (data.isDifficulty && firstBeatmap != null) "$artist · ${firstBeatmap.version}" else artist
        cover.textureRegion = data.coverTexture

        difficultyIndicators.clearChildren()
        data.beatmapSetInfo.beatmaps.sortedBy { it.getStarRating() }.forEach { beatmap ->
            difficultyIndicators += UIBox().apply {
                width = 12f.dip
                height = 2f.dip
                radius = 8f.dip
                backgroundColor = OsuColors.getStarRatingColor(beatmap.getStarRating().toDouble()).lighten(0.5f)
            }
        }
    }


    fun select(beatmapIndex: Int = 0) {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer[beatmapIndex.coerceIn(0, panelsContainer.childCount - 1)] as BeatmapPanel
    }

    /**
     * Selects the panel of the given beatmap, or the first one if it isn't in this set.
     */
    fun select(beatmapInfo: BeatmapInfo) {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer.firstOrNull { (it as BeatmapPanel).beatmapInfo.path == beatmapInfo.path } as? BeatmapPanel
            ?: panelsContainer.firstOrNull() as? BeatmapPanel
    }

    fun selectRandom() {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer[Random.nextInt(panelsContainer.childCount)] as BeatmapPanel
    }


    override fun onUpdate(clock: FrameClock) {
        val accent = Verktex.theme.palette.accent

        val targetExtraWidth = if (isExpanded) EXPANDED_EXTRA_WIDTH.dip.toPixels() else 0f
        val targetBorderColor = if (isExpanded) accent else accent.alpha(0.3f)

        buttonExtraWidth = clock.interpolateWithSnap(0.1f, buttonExtraWidth, targetExtraWidth, 0.5f)

        buttonGlow = clock.interpolateWithSnap(0.1f, buttonGlow, if (isExpanded) 1f else 0f, 0.01f)

        button.apply {
            borderColor = clock.interpolateColor(0.1f, borderColor, targetBorderColor)
            shadow = if (buttonGlow > 0f) Shadow(accent.alpha(GLOW_ALPHA * buttonGlow), blur = GLOW_BLUR.dip, cutout = true) else null
        }
        updateTranslation()

        // A fraction of the height of the content, measuring it with a fixed height would limit
        // the panels to that height.
        val targetPanelsExpansion = if (isExpanded) 1f else 0f
        val targetPanelsAlpha = if (isExpanded) 1f else 0f

        panelsExpansion = clock.interpolateWithSnap(0.1f, panelsExpansion, targetPanelsExpansion, 0.005f)

        panelsContainer.apply {
            height = wrap(panelsExpansion)
            alpha = clock.interpolateWithSnap(0.1f, alpha, targetPanelsAlpha, 0.1f)
        }

        isTransitioning = buttonExtraWidth != targetExtraWidth
            || buttonGlow != (if (isExpanded) 1f else 0f)
            || button.borderColor != targetBorderColor
            || panelsExpansion != targetPanelsExpansion
            || panelsContainer.alpha != targetPanelsAlpha

        if (!isExpanded && !isTransitioning && panelsContainer.childCount > 0) {
            panelsContainer.clearChildren()
        }

        super.onUpdate(clock)
    }

    fun expand() {
        val data = boundData

        if (data == null) {
            Log.w("BeatmapSetPanel", "Cannot expand panel without bound data!")
            return
        }

        if (!isExpanded) {
            isExpanded = true

            // The difficulties of a collapse that didn't finish yet.
            panelsContainer.clearChildren()

            data.beatmapSetInfo.beatmaps.sortedBy { it.getStarRating() }.forEach {
                panelsContainer += BeatmapPanel(this@BeatmapSetPanel, it)
            }
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            selectedPanel = null
        }
    }


    // Wider than the carrousel, so the panels popping out to the left still go past the screen.
    override fun adjustParentConstraints(constraints: LayoutConstraints): LayoutConstraints {
        val overflow = OVERFLOW.dip.toPixels()

        return constraints.copy(
            minWidth = constraints.minWidth + overflow,
            maxWidth = constraints.maxWidth + overflow
        )
    }

    private fun updateTranslation() {
        button.translationX = (curveOffset + beatmapCarrousel.panelOverhang - buttonExtraWidth).px
    }


    override fun onRecycle() {
        isExpanded = false
        selectedPanel = null

        // The next beatmap set must not show the difficulties of this one, even for a frame.
        panelsContainer.clearChildren()
        panelsContainer.alpha = 0f
        panelsExpansion = 0f
    }


    companion object {

        /**
         * How much wider the panels are than the carrousel, more than the selected ones are moved
         * to the left.
         */
        const val OVERFLOW = 128f

        private const val EXPANDED_EXTRA_WIDTH = 48f
        private const val GLOW_ALPHA = 0.15f
        private const val GLOW_BLUR = 24f
    }
}
