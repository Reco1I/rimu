package com.osudroid.ui.v2.beatmaplisting

import com.osudroid.beatmaplisting.BeatmapModel
import com.osudroid.beatmaplisting.BeatmapSetModel
import com.osudroid.resources.Res
import com.osudroid.ui.OsuColors
import com.osudroid.ui.v2.StarRatingBadge
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.IconButton
import com.reco1l.verktex.ui.LabeledBadge
import com.reco1l.verktex.ui.Modal
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.button
import com.reco1l.verktex.ui.container.GridContainer
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.gridContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.themed
import kotlin.math.roundToLong

/**
 * The details of a beatmap set of the listing: its difficulties, with the attributes of the selected one,
 * and the buttons to preview and download it.
 */
class BeatmapSetDetails(private val state: BeatmapListingState) : Modal() {

    /**
     * The set shown, null while hidden.
     */
    val set: BeatmapSetModel?
        get() = header.set

    private val header = BeatmapSetHeader(140f.dip)

    private lateinit var difficultyButtons: LinearContainer
    private lateinit var versionText: Text
    private lateinit var starRatingBadge: StarRatingBadge
    private lateinit var attributes: GridContainer
    private lateinit var mapperText: Text
    private lateinit var previewButton: IconButton
    private lateinit var downloadButton: TextButton
    private lateinit var noVideoButton: TextButton


    init {
        detachOnHide = true

        card.apply {
            width = fill()
            maxWidth = 520f.dip

            linearContainer {
                width = fill()
                orientation = Orientation.Vertical

                +header

                linearContainer {
                    width = fill()
                    orientation = Orientation.Vertical
                    padding = Padding(16f.dip)
                    spacing = 12f.dip

                    scrollableContainer {
                        width = fill()
                        scrollAxes = Axis.X
                        showHorizontalIndicator = false

                        difficultyButtons = linearContainer {
                            spacing = 4f.dip
                        }
                    }

                    linearContainer {
                        width = fill()
                        spacing = 8f.dip

                        versionText = text {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            maxLines = 1
                            fontSettings = themed { typography.h3 }
                        }

                        starRatingBadge = StarRatingBadge().apply {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            density = Badge.Density.Small
                        }
                        +starRatingBadge
                    }

                    attributes = gridContainer {
                        width = fill()
                        minColumnWidth = 110f.dip
                        columnSpacing = 6f.dip
                        rowSpacing = 6f.dip
                    }

                    mapperText = text {
                        width = fill()
                        maxLines = 1
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.7f) }
                    }

                    linearContainer {
                        spacing = 6f.dip

                        previewButton = iconButton {
                            icon = FAIcon.Play
                            onActionUp = { set?.let(state::togglePreview) }
                        }

                        downloadButton = textButton {
                            leadingIcon = faSolidIcon(FAIcon.Download)
                            text = Res.string.beatmap_listing_download
                            onActionUp = { set?.let { state.download(it, withVideo = true) } }
                        }

                        noVideoButton = textButton {
                            leadingIcon = faSolidIcon(FAIcon.VideoSlash)
                            text = Res.string.beatmap_listing_download_no_video
                            onActionUp = { set?.let { state.download(it, withVideo = false) } }
                        }
                    }
                }
            }
        }
    }


    /**
     * Shows the details of [set], with its first difficulty selected.
     */
    fun show(set: BeatmapSetModel) {
        state.detailsSetId = set.id
        header.bind(set)

        mapperText.text = Res.string.beatmap_listing_mapped_by.format(set.creator)

        difficultyButtons.clearChildren()
        set.beatmaps.forEachIndexed { index, beatmap ->
            difficultyButtons.button {
                density = Button.Density.Small
                onActionUp = { select(index) }

                box {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    width = 12f.dip
                    height = 12f.dip
                    radius = CornerRadius(6f.dip)
                    color = OsuColors.getStarRatingColor(beatmap.starRating)
                    // The hardest ones are black, outlined to be seen on the dark card.
                    border = Border(1f.dip, Color4.White.alpha(0.3f))
                }
            }
        }

        select(0)
        refresh()
        show()
    }

    private fun select(index: Int) {
        val beatmap = set?.beatmaps?.getOrNull(index) ?: return

        difficultyButtons.forEachIndexed { i, button -> (button as Button).isSelected = i == index }

        versionText.text = beatmap.version
        starRatingBadge.rating = beatmap.starRating
        starRatingBadge.applyColorsInstantly()

        showAttributes(beatmap)
    }

    private fun showAttributes(beatmap: BeatmapModel) {
        attributes.clearChildren()

        fun attribute(label: CharSequence, value: String) {
            attributes += LabeledBadge().apply {
                width = fill()
                density = Badge.Density.Small
                this.label = label
                this.value = value
            }
        }

        attribute("AR", beatmap.ar.formatAttribute())
        attribute("OD", beatmap.od.formatAttribute())
        attribute("CS", beatmap.cs.formatAttribute())
        attribute("HP", beatmap.hp.formatAttribute())
        attribute(Res.string.beatmap_listing_bpm, beatmap.bpm.formatAttribute())
        attribute(Res.string.beatmap_listing_length, formatLength(beatmap.lengthSec))
        attribute(Res.string.beatmap_listing_circles, beatmap.circleCount.toString())
        attribute(Res.string.beatmap_listing_sliders, beatmap.sliderCount.toString())
        attribute(Res.string.beatmap_listing_spinners, beatmap.spinnerCount.toString())
    }

    /**
     * Updates what the details show of the [state].
     */
    fun refresh() {
        val set = set ?: return

        previewButton.icon = if (state.playingSetId == set.id) FAIcon.Pause else FAIcon.Play
        downloadButton.isEnabled = state.download == null
        noVideoButton.isEnabled = state.download == null
        noVideoButton.isVisible = state.canDownloadWithoutVideo(set)

        header.updateState(state)
    }


    override fun onHidden() {
        val shownSet = set
        state.detailsSetId = null

        // A card of the listing may still show the cover.
        header.release(unloadCover = shownSet != null && !state.isShownInListing(shownSet.id))
        super.onHidden()
    }

    override fun onInputEvent(event: InputEvent): Boolean {
        if (isShown && event is KeyboardEvent && event.isBackPress) {
            hide()
            return true
        }
        return super.onInputEvent(event)
    }


    companion object {

        /**
         * The value of an attribute with at most two decimals, without trailing zeros.
         */
        private fun Double.formatAttribute(): String {
            val hundredths = (this * 100).roundToLong()
            return when {
                hundredths % 100 == 0L -> (hundredths / 100).toString()
                hundredths % 10 == 0L -> formatString("%.1f", this)
                else -> formatString("%.2f", this)
            }
        }

        /**
         * The length as m:ss, or h:mm:ss from an hour.
         */
        fun formatLength(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = seconds % 3600 / 60
            val remainder = seconds % 60

            return if (hours > 0) {
                formatString("%d:%02d:%02d", hours, minutes, remainder)
            } else {
                formatString("%d:%02d", minutes, remainder)
            }
        }
    }
}
