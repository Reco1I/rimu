package com.osudroid.ui.v2.beatmaplisting

import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapSetModel
import com.osudroid.resources.Res
import com.osudroid.ui.OsuColors
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.IconButton
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.borderColor
import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.shape.Box
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.themed
import kotlinx.coroutines.Job

/**
 * The cover of a beatmap set with its status, title and artist, and the progress of its download.
 */
class BeatmapSetHeader(headerHeight: Dimension.Fixed) : Container() {

    /**
     * The set shown.
     */
    var set: BeatmapSetModel? = null
        private set

    private var cover: Sprite
    private var statusBadge: Badge
    private var downloadedBadge: Badge
    private lateinit var titleText: Text
    private lateinit var artistText: Text
    private var progressBar: Box

    private var coverJob: Job? = null


    init {
        width = fill()
        height = headerHeight

        cover = sprite {
            width = fill()
            height = fill()
            scaleType = ScaleType.Crop
            background = Background.Color(themed { palette.accent.shade(0.3f) })
        }

        box {
            width = fill()
            height = fill()
            color = Color4.Black.alpha(0.6f)
        }

        statusBadge = badge {
            x = 8f.dip
            y = 8f.dip
            density = Badge.Density.Small
            variant = Badge.Variant.Solid
        }

        downloadedBadge = badge {
            anchor = Anchor.TopRight
            origin = Anchor.TopRight
            x = (-8f).dip
            y = 8f.dip
            density = Badge.Density.Small
            leadingIcon = faSolidIcon(FAIcon.Check)
            text = Res.string.beatmap_listing_downloaded
            isVisible = false
        }

        linearContainer {
            width = fill()
            anchor = Anchor.Center
            origin = Anchor.Center
            orientation = Orientation.Vertical
            padding = Padding(16f.dip, 0f.dip)
            spacing = 2f.dip

            titleText = text {
                width = fill()
                alignment = Anchor.TopCenter
                maxLines = 1
                fontSettings = themed { typography.h3 }
                fontSize = themed { typography.body.size }
            }

            artistText = text {
                width = fill()
                alignment = Anchor.TopCenter
                maxLines = 1
                fontSettings = themed { typography.caption }
                fontSize = themed { typography.smallCaption.size }
                color = themed { palette.accent.shade(0.8f) }
            }
        }

        progressBar = box {
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            height = 3f.dip
            color = themed { palette.accent }
            isVisible = false
        }
    }


    /**
     * Shows [set], loading its cover.
     */
    fun bind(set: BeatmapSetModel) {
        this.set = set

        titleText.text = set.displayTitle
        artistText.text = set.displayArtist
        statusBadge.text = set.status.stringId

        coverJob?.cancel()
        coverJob = null

        val url = set.thumbnail

        if (url == null) {
            cover.textureRegion = null
            return
        }

        cover.textureRegion = BeatmapCovers.loaded(url)

        if (cover.textureRegion == null) {
            coverJob = BeatmapCovers.load(url) { region ->
                if (this.set?.id == set.id) {
                    cover.textureRegion = region
                }
            }
        }
    }

    /**
     * Stops showing the set, unloading its cover if [unloadCover].
     */
    fun release(unloadCover: Boolean) {
        coverJob?.cancel()
        coverJob = null
        cover.textureRegion = null

        if (unloadCover) {
            set?.thumbnail?.let(BeatmapCovers::unload)
        }
        set = null
    }

    /**
     * Shows whether the set is downloaded, or the progress of its download.
     */
    fun updateState(state: BeatmapListingState) {
        val set = set ?: return
        val download = state.downloadOf(set)

        downloadedBadge.isVisible = download == null && set.id in state.downloadedSetIds

        progressBar.isVisible = download != null
        if (download != null) {
            val progress = download.progress

            if (download.phase == BeatmapDownloader.Phase.Downloading && progress != null) {
                progressBar.width = fill(progress.coerceIn(0f, 1f))
                progressBar.alpha = 1f
            } else {
                // Connecting, importing, or a download of an unknown size.
                progressBar.width = fill()
                progressBar.alpha = 0.4f
            }
        }
    }
}


/**
 * Adds the dots of the difficulties of [set], colored by their star rating, to [container].
 */
internal fun addDifficultyDots(container: LinearContainer, set: BeatmapSetModel, max: Int = MAX_DIFFICULTY_DOTS) {
    container.clearChildren()

    set.beatmaps.take(max).forEach { beatmap ->
        container.box {
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            width = 8f.dip
            height = 8f.dip
            radius = CornerRadius(4f.dip)
            color = OsuColors.getStarRatingColor(beatmap.starRating)
            // The hardest ones are black, outlined to be seen on the dark card.
            border = Border(1f.dip, Color4.White.alpha(0.3f))
        }
    }

    if (set.beatmaps.size > max) {
        container.text {
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            text = "+${set.beatmaps.size - max}"
            fontSize = themed { typography.smallCaption.size }
            color = themed { palette.accent.shade(0.8f) }
        }
    }
}

private const val MAX_DIFFICULTY_DOTS = 20


/**
 * A beatmap set found by the listing: its cover, its difficulties and the buttons to preview and download
 * it. Pressing it shows its details.
 */
class BeatmapSetCard(private val state: BeatmapListingState) : RecyclableComponent<BeatmapSetModel>() {

    override val isRecyclable: Boolean
        get() {
            val set = boundData ?: return true
            return state.playingSetId != set.id && state.downloadOf(set) == null
        }

    val header = BeatmapSetHeader(100f.dip)

    private lateinit var difficultyDots: LinearContainer
    private lateinit var previewButton: IconButton
    private lateinit var downloadButton: TextButton
    private lateinit var noVideoButton: TextButton
    private lateinit var mapperText: Text


    companion object {

        /**
         * The width a card needs, columns of the listing are at least as wide. Phones in landscape
         * (640dp and wider, less their cutouts) fit two of them, tablets three or four.
         */
        val MIN_WIDTH = 280f.dip
    }


    init {
        width = fill()

        clickableContainer {
            width = fill()
            clipping = ClipMode.Bounds
            radius = themed { CornerRadius(radius.xl) }
            background = Background.Color(themed { palette.accent.shade(0.12f) })
            borderColor = themed { palette.accent.alpha(0.2f) }
            borderWidth = 1f.dip
            onActionUp = { boundData?.let(state::showDetails) }

            linearContainer {
                width = fill()
                orientation = Orientation.Vertical

                +header

                difficultyDots = linearContainer {
                    width = fill()
                    height = 26f.dip
                    padding = Padding(12f.dip, 0f.dip)
                    spacing = 3f.dip
                }

                linearContainer {
                    width = fill()
                    orientation = Orientation.Vertical
                    padding = Padding(12f.dip, 8f.dip)
                    spacing = 6f.dip
                    background = Background.Color(Color4.Black.alpha(0.2f))

                    linearContainer {
                        spacing = 6f.dip

                        previewButton = iconButton {
                            icon = FAIcon.Play
                            density = Button.Density.Small
                            onActionUp = { boundData?.let(state::togglePreview) }
                        }

                        downloadButton = textButton {
                            leadingIcon = faSolidIcon(FAIcon.Download)
                            text = Res.string.beatmap_listing_download
                            density = Button.Density.Small
                            onActionUp = { boundData?.let { state.download(it, withVideo = true) } }
                        }

                        noVideoButton = textButton {
                            leadingIcon = faSolidIcon(FAIcon.VideoSlash)
                            text = Res.string.beatmap_listing_no_video
                            density = Button.Density.Small
                            onActionUp = { boundData?.let { state.download(it, withVideo = false) } }
                        }
                    }

                    mapperText = text {
                        width = fill()
                        maxLines = 1
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.7f) }
                    }
                }
            }
        }
    }


    override fun onBind(data: BeatmapSetModel) {
        header.bind(data)
        addDifficultyDots(difficultyDots, data)
        mapperText.text = Res.string.beatmap_listing_mapped_by.format(data.creator)
        refresh()
    }

    override fun onRecycle() {
        // The details keep showing the cover.
        header.release(unloadCover = state.detailsSetId != header.set?.id)
    }

    /**
     * Updates what the card shows of the [state].
     */
    fun refresh() {
        val set = boundData ?: return

        previewButton.icon = if (state.playingSetId == set.id) FAIcon.Pause else FAIcon.Play
        downloadButton.isEnabled = state.download == null
        noVideoButton.isEnabled = state.download == null
        noVideoButton.isVisible = state.canDownloadWithoutVideo(set)

        header.updateState(state)
    }
}
