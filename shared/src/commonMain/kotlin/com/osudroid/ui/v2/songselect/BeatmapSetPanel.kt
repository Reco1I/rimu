package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.ui.borderColor
import com.reco1l.verktex.Logger
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
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.animatableColor
import com.reco1l.verktex.ui.animatableFloat
import com.reco1l.verktex.ui.animate
import com.reco1l.verktex.ui.animateColor
import com.reco1l.verktex.ui.transitionSlot
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.ClickableContainer
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.shape.Box
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import kotlin.math.abs
import kotlin.random.*
import com.reco1l.verktex.ui.themed


class BeatmapSetPanel(val beatmapCarrousel: BeatmapCarrousel) : RecyclableComponent<BeatmapSetModel>(), IPanelContainer<BeatmapPanel> {

    override val isRecyclable: Boolean
        get() = !button.isPressed && !isExpanded && !isTransitioning


    /**
     * The component the carrousel scrolls to: the selected beatmap panel, or the button while none.
     */
    val scrollTarget: Component
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
        private set(value) {
            if (field != value) {
                field = value
                animateExpansion(value)
            }
        }


    /**
     * The container for the beatmap panels.
     */
    lateinit var panelsContainer: LinearContainer

    /**
     * The button itself.
     */
    lateinit var button: ClickableContainer


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

    private lateinit var cover: Sprite
    private lateinit var titleText: Text
    private lateinit var artistText: Text
    private lateinit var difficultyIndicators: LinearContainer


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
                radius = themed { CornerRadius(radius.xl) }

                cover = sprite {
                    width = fill()
                    height = fill()
                    scaleType = ScaleType.Crop
                    background = Background.Color(themed { (palette.accent.shade(0.1f)).alpha(0.5f) })
                    radius = themed { CornerRadius(radius.xl) }
                }

                box {
                    width = fill()
                    height = fill()
                    color = Color4.Black.alpha(0.6f)
                    radius = themed { CornerRadius(radius.xl) }
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
            difficultyIndicators += Box().apply {
                width = 12f.dip
                height = 2f.dip
                radius = CornerRadius(8f.dip)
                color = OsuColors.getStarRatingColor(beatmap.getStarRating().toDouble()).lighten(0.5f)
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
        // The overhang of the carrousel can change while scrolling.
        updateTranslation()
        super.onUpdate(clock)
    }

    private val expansion = transitionSlot()

    private val buttonExtraWidthProperty = animatableFloat({ buttonExtraWidth }, {
        buttonExtraWidth = it
        updateTranslation()
    })

    private val buttonGlowProperty = animatableFloat({ buttonGlow }, {
        buttonGlow = it
        val accent = Verktex.theme.palette.accent
        button.shadow = if (it > 0f) Shadow(accent.alpha(GLOW_ALPHA * it), blur = GLOW_BLUR.dip, cutout = true) else Shadow.None
    })

    private val buttonBorderColorProperty by lazy { animatableColor({ button.borderColor }, { button.borderColor = it }) }

    // A fraction of the height of the content, measuring it with a fixed height would limit the panels to that height.
    private val panelsExpansionProperty = animatableFloat({ panelsExpansion }, {
        panelsExpansion = it
        panelsContainer.height = wrap(it)
    })

    private val panelsAlphaProperty = animatableFloat({ panelsContainer.alpha }, { panelsContainer.alpha = it })

    private fun animateExpansion(expanded: Boolean) {
        val accent = Verktex.theme.palette.accent
        val target = if (expanded) 1f else 0f

        isTransitioning = true

        expansion.run {
            animate(buttonExtraWidthProperty, if (expanded) EXPANDED_EXTRA_WIDTH.dip.toPixels() else 0f, EXPANSION_DURATION, Easing.OutQuint)
            animate(buttonGlowProperty, target, EXPANSION_DURATION, Easing.OutQuint)
            animateColor(buttonBorderColorProperty, if (expanded) accent else accent.alpha(0.3f), EXPANSION_DURATION, Easing.OutQuint)
            animate(panelsAlphaProperty, target, EXPANSION_DURATION, Easing.OutQuint)
            animate(panelsExpansionProperty, target, EXPANSION_DURATION, Easing.OutQuint).after {
                isTransitioning = false

                if (!isExpanded) {
                    panelsContainer.clearChildren()
                }
            }
        }
    }

    fun expand() {
        val data = boundData

        if (data == null) {
            Logger.w("BeatmapSetPanel", "Cannot expand panel without bound data!")
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

        // Collapsed at once: the next beatmap set must not show the difficulties of this one, even for a frame.
        expansion.cancel()
        isTransitioning = false
        panelsContainer.clearChildren()
        panelsAlphaProperty.value = 0f
        panelsExpansionProperty.value = 0f
        buttonExtraWidthProperty.value = 0f
        buttonGlowProperty.value = 0f
        buttonBorderColorProperty.value = Verktex.theme.palette.accent.alpha(0.3f)
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
        private const val EXPANSION_DURATION = 0.1f
    }
}
