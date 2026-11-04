package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.borderColor
import android.util.Log
import com.edlplan.ui.fragment.CollectionsManagerFragment
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapOptions
import com.osudroid.data.DatabaseManager
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.control.UIDropdown
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.verktex.ui.text.faSolidIcon
import ru.nsu.ccfit.zuev.osu.LibraryManager
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.interpolateColor
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import kotlin.math.abs
import kotlin.random.*

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
     * The position in scene space of the middle of the currently selected panel.
     */
    val selectedPanelSceneY: Float
        get() {
            val panel = selectedPanel ?: return panelsContainer.toSceneSpace(0f, 0f).y
            return panel.toSceneSpace(0f, panel.bounds.height / 2f).y
        }


    /**
     * The currently selected beatmap panel.
     */
    override var selectedPanel: BeatmapPanel? = null
        set(value) {
            if (field != value) {
                field = value

                if (beatmapCarrousel.selectedPanel == this) {
                    SongSelect.onBeatmapSelected(value?.beatmapInfo)
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


    private var isTransitioning = false

    private var buttonExtraWidth = 0f
    private var buttonBorderWidth = BORDER_WIDTH.dip.toPixels()
    private var panelsExpansion = 0f

    private lateinit var cover: UISprite
    private lateinit var titleText: UIText
    private lateinit var artistText: UIText


    init {
        width = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight

        linearContainer {
            orientation = UILinearContainer.Orientation.Vertical
            width = Dimension.FillAvailable
            spacing = 4f.dip

            button = clickableContainer {
                width = Dimension.FillAvailable
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                onActionLongPress = { showOptions() }
                onActionUp = {
                    // This will prevent race condition, during transition panelsContainer might
                    // get cleared and since the select() method is based on beatmap indices
                    // it might fail to find the corresponding BeatmapPanel causing a IOOBE.
                    if (!isTransitioning) {
                        select()
                    }
                }
                style = Style { theme ->
                    borderColor = theme.palette.accent.copy(alpha = 0.3f)
                    borderWidth = BORDER_WIDTH.dip
                    radius = theme.radius.xl
                }

                cover = sprite {
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                    scaleType = UISprite.ScaleType.Crop
                    style = Style { theme ->
                        backgroundColor = (theme.palette.accent * 0.1f).copy(alpha = 0.5f)
                        radius = theme.radius.xl
                    }
                }

                box {
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                    backgroundColor = Color4.Black.copy(alpha = 0.6f)
                    style = Style { theme ->
                        radius = theme.radius.xl
                    }
                }

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    width = Dimension.FillAvailable
                    padding = Padding(10f.dip)

                    titleText = text {
                        text = "Unknown"
                        style = Style { theme ->
                            fontSettings = theme.typography.h3
                            fontSize = theme.typography.body.size
                            color = theme.palette.accent
                        }
                    }

                    artistText = text {
                        text = "Unknown"
                        style = Style { theme -> color = theme.palette.accent * 0.8f }
                    }
                }
            }

            panelsContainer = linearContainer {
                orientation = UILinearContainer.Orientation.Vertical
                width = Dimension.FillAvailable
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
    }


    fun select(beatmapIndex: Int = 0) {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer[beatmapIndex] as BeatmapPanel
    }

    /**
     * Selects the panel of the given beatmap, or the first one if it isn't in this set.
     */
    fun select(beatmapInfo: BeatmapInfo) {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer.firstOrNull { (it as BeatmapPanel).beatmapInfo.path == beatmapInfo.path } as? BeatmapPanel
            ?: panelsContainer.firstOrNull() as? BeatmapPanel
    }

    /**
     * Shows the options of the beatmap set below the panel.
     */
    fun showOptions() {
        val data = boundData ?: return
        val directory = data.beatmapSetInfo.directory

        UIDropdown(button).apply dropdown@{
            maxWidth = 320f.dip

            val options = DatabaseManager.beatmapOptionsTable.getOptions(directory) ?: BeatmapOptions(directory)

            addButton {
                text = if (options.isFavorite) "Remove from favorites" else "Add to favorites"
                leadingIcon = faSolidIcon(FAIcon.Star)
                onActionUp = {
                    options.isFavorite = !options.isFavorite
                    DatabaseManager.beatmapOptionsTable.upsert(options)
                    this@dropdown.hide()

                    if (SongSelect.isFilteringFavorites) {
                        SongSelect.applyFilter()
                    }
                }
            }

            addButton {
                text = "Manage collections"
                leadingIcon = faSolidIcon(FAIcon.FolderOpen)
                onActionUp = {
                    CollectionsManagerFragment().showToAddToFolder(directory)
                    this@dropdown.hide()
                }
            }

            addButton {
                text = "Delete"
                leadingIcon = faSolidIcon(FAIcon.Trash)
                tintChildren = true
                style += Style { color = Color4("#FFBFBF") }
                onActionUp = {
                    UIConfirmDialog().apply {
                        title = "Delete beatmap set"
                        text = "Are you sure you want to delete \"${titleText.text}\"? Its files will be removed from the device."
                        onConfirm = {
                            async {
                                LibraryManager.deleteBeatmapSet(data.beatmapSetInfo)
                                updateThread { SongSelect.onBeatmapSetDeleted(directory) }
                            }
                        }
                    }.show()
                    this@dropdown.hide()
                }
            }
        }.show()
    }

    fun selectRandom() {
        beatmapCarrousel.selectedPanel = this
        selectedPanel = panelsContainer[Random.nextInt(panelsContainer.childCount)] as BeatmapPanel
    }


    override fun onUpdate(clock: FrameClock) {
        val accent = Verktex.theme.palette.accent

        val targetExtraWidth = if (isExpanded) EXPANDED_EXTRA_WIDTH.dip.toPixels() else 0f
        val targetBorderColor = if (isExpanded) accent else accent.copy(alpha = 0.3f)
        val targetBorderWidth = (if (isExpanded) EXPANDED_BORDER_WIDTH else BORDER_WIDTH).dip.toPixels()

        buttonExtraWidth = clock.interpolateWithSnap(0.1f, buttonExtraWidth, targetExtraWidth, 0.5f)
        buttonBorderWidth = clock.interpolateWithSnap(0.1f, buttonBorderWidth, targetBorderWidth, 0.5f)

        button.apply {
            borderColor = clock.interpolateColor(0.1f, borderColor, targetBorderColor)
            borderWidth = buttonBorderWidth.px
            translationX = (curveOffset + beatmapCarrousel.panelOverhang - buttonExtraWidth).px
        }

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
            || buttonBorderWidth != targetBorderWidth
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


    override fun onRecycle() {
        isExpanded = false
        selectedPanel = null

        // The next beatmap set must not show the difficulties of this one, even for a frame.
        panelsContainer.clearChildren()
        panelsContainer.alpha = 0f
        panelsExpansion = 0f
    }


    private companion object {
        const val BORDER_WIDTH = 1f
        const val EXPANDED_BORDER_WIDTH = 2f
        const val EXPANDED_EXTRA_WIDTH = 32f
    }
}
