package com.osudroid.ui.v2.songselect

import com.osudroid.game.Gameplay
import com.osudroid.game.loading.PlayRequest
import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.settings.OsuSettings
import com.osudroid.beatmaps.parser.*
import com.osudroid.data.*
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.osudroid.mods.*
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.modmenu.*
import com.osudroid.utils.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.transformSequence
import com.reco1l.verktex.ui.animate
import com.reco1l.verktex.ui.animatableFloat
import com.reco1l.verktex.ui.transitionSlot
import com.reco1l.verktex.audio.AudioStream
import com.reco1l.verktex.ui.shape.Box
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.control.Dropdown
import com.reco1l.verktex.ui.dialog.ConfirmDialog
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.textButton
import okio.IOException
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import kotlin.coroutines.cancellation.CancellationException
import com.osudroid.data.BeatmapSetInfo
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.verticalPadding
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.ui.Icon
import com.reco1l.verktex.ui.button
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed

/**
 * Song select, created each time it's opened. Its sort order and grouping are settings, and it
 * opens on the selected beatmap; the rest of the filter and the solo mods start over.
 */
class SongSelect : Scene() {

    // Back leaves song select, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            back()
            return true
        }
        return false
    }

    /**
     * Whether the leaderboard is showing online scores.
     */
    private val isLeaderboardOnline
        get() = leaderboard.leaderboardType == LeaderboardType.Global

    /**
     * The mods the player selected for solo play.
     */
    private val soloMods = ModHashMap()

    /**
     * The mods song select rates the beatmaps with and plays: the room's in multiplayer.
     */
    val mods: ModHashMap
        get() = Multiplayer.roomScene?.mods ?: soloMods

    private var modMenu: ModMenu? = null

    // The version of the mods the mods indicator shows.
    private var shownModsVersion = -1

    private val beatmapLoadingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val filterBar = SongSelectFilterBar()
    private val leaderboard = SongSelectLeaderboard()
    private val beatmapCarrousel = BeatmapCarrousel()
    private val beatmapInfoWedge = BeatmapInfoWedge { mods }

    private lateinit var modsIndicator: ModsIndicator

    private lateinit var leftSide: LinearContainer
    private lateinit var rightSide: Container
    private lateinit var bottomBar: LinearContainer

    private var beatmapSets = listOf<BeatmapSetInfo>()
    private var filterJob: Job? = null

    private var currentSongPath: String? = null

    // The preview's fade in, and the filters a cancelled play left on the song opening.
    private val musicFade = transitionSlot()
    private val musicFilters = transitionSlot()
    private val musicVolume = animatableFloat({ OsuGame.music.volume }) { OsuGame.music.volume = it }
    private val musicLowPass = animatableFloat({ OsuGame.music.lowPassCutoff }) { OsuGame.music.lowPassCutoff = it }
    private val musicHighPass = animatableFloat({ OsuGame.music.highPassCutoff }) { OsuGame.music.highPassCutoff = it }
    private var currentBeatmapLoadingJob: Job? = null
    private var currentCalculationJob: Job? = null

    // Dims the selection's background behind the lists. The background itself stays at full
    // brightness: a crossfade under a lowered alpha shows the old texture through the new one,
    // brightening while it runs.
    private val backgroundDim = Box().apply {
        width = fill()
        height = fill()
        color = themed { palette.inset }
        alpha = 0f
    }


    init {
        background = Background.Component(backgroundDim)

        filterBar.onFilterChanged = { applyFilter() }

        beatmapCarrousel.apply {
            onBeatmapSelected = { onBeatmapSelected(it) }
            onBeatmapConfirmed = { onBeatmapConfirmed(it) }
            onBeatmapSetLongPress = { showBeatmapSetOptions(it) }
        }

        linearContainer {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            // Left side
            leftSide = linearContainer {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                orientation = Orientation.Vertical
                spacing = 8f.dip

                +beatmapInfoWedge
                +leaderboard
            }

            // Right side
            rightSide = container {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable

                +beatmapCarrousel
                +filterBar
            }
        }

        bottomBar = linearContainer {
            orientation = Orientation.Horizontal
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            padding = Padding(8f.dip, 0f.px)
            spacing = 8f.dip

            textButton {
                text = "Back"
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                leadingIcon = faSolidIcon(FAIcon.ArrowLeft)

                onActionUp = { back() }
            }


            fun bottomBarButton(icon: Int, label: String, onClick: () -> Unit) = button {
                height = wrap()
                minWidth = 92f.dip
                padding = themed { Padding(gap.sm) }
                radius = themed {
                    buttonStyle[density].radius.let { r -> CornerRadius(r, r, 0f.px, 0f.px) }
                }

                linearContainer {
                    orientation = Orientation.Vertical
                    spacing = themed { gap.xs }
                    anchor = Anchor.Center
                    origin = Anchor.Center

                    +Icon(icon).apply {
                        iconSize = themed { typography.h2.size }
                        anchor = Anchor.TopCenter
                        origin = Anchor.TopCenter
                    }

                    text {
                        text = label
                        anchor = Anchor.TopCenter
                        origin = Anchor.TopCenter
                    }
                }
                onActionUp = onClick
            }

            bottomBarButton(FAIcon.Sliders, "Mods") {
                showModMenu()
            }

            bottomBarButton(FAIcon.Shuffle, "Random") {
                if (beatmapCarrousel.data.isNotEmpty()) {
                    beatmapCarrousel.applyComponent(beatmapCarrousel.data.random()) { selectRandom() }
                }
            }

            modsIndicator = ModsIndicator().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                iconSize = 28f.px
                spacing = (-5f).px
            }
            +modsIndicator
        }
    }


    /**
     * Makes this scene the current one, unless it already is.
     */
    fun show() {
        Verktex.withTreeLock {
            if (Verktex.scene !== this) {
                Verktex.pushScene(this)
            }
        }
    }

    /**
     * Leaves this scene without changing the beatmap of the multiplayer room.
     */
    fun back() {
        val modMenu = modMenu

        if (modMenu != null && modMenu.isShown) {
            modMenu.onBack()
        } else {
            back(true)
        }
    }

    private fun showModMenu() {
        val mods = mods

        // A room's mods go to its server when the menu closes.
        val menu = modMenu?.takeIf { it.mods === mods } ?: ModMenu(mods).apply {
            onDifficultyCalculated = { beatmapInfo, starRating ->
                setDifficultyStatistics(beatmapInfo)
                setStarRatingDisplay(starRating)
            }
            onBack = {
                hide()
                Multiplayer.roomScene?.sendMods()
            }
        }

        modMenu = menu
        menu.show()
    }

    private fun back(resetMultiplayerBeatmap: Boolean) {
        cancelJobs()

        Verktex.withTreeLock { Verktex.popScene() }
        beatmapLoadingScope.cancel()

        // The menu plays the next song at the end of this one.
        OsuGame.music.previewTime = null

        if (Multiplayer.isMultiplayer) {
            if (resetMultiplayerBeatmap) {
                resetMultiplayerRoomBeatmap()
            }

            Multiplayer.roomScene?.show()
            return
        }

        OsuGame.music.apply {
            speed = 1f
            setAdjustPitch(false)
        }
    }

    private fun cancelJobs() {
        currentCalculationJob?.cancel(CancellationException("Calculation cancelled, leaving the scene."))
        currentBeatmapLoadingJob?.cancel(CancellationException("Beatmap loading cancelled, leaving the scene."))
    }


    /**
     * Starts playing the given beatmap, or sets it as the beatmap of the room in multiplayer.
     */
    fun onBeatmapConfirmed(beatmapInfo: BeatmapInfo) {
        SkinSounds["menuhit"].play()
        cancelJobs()

        if (Multiplayer.isMultiplayer) {
            setMultiplayerRoomBeatmap(beatmapInfo)
            back(false)
            return
        }

        Gameplay().start(PlayRequest(
            beatmap = beatmapInfo,
            mods = mods,
            leaderboardItems = { leaderboard.scoreBoardItems },
            isLeaderboardOnline = isLeaderboardOnline
        ))
    }

    private fun resetMultiplayerRoomBeatmap() {
        // Locking host from change beatmap before the server responses to beatmapChange
        Multiplayer.roomScene?.isWaitingForBeatmapChange?.store(true)

        if (!Multiplayer.isConnected) {
            return
        }

        val beatmap = Multiplayer.room?.previousBeatmap

        if (beatmap != null) {
            RoomAPI.changeBeatmap(beatmap.md5, beatmap.title, beatmap.artist, beatmap.version, beatmap.creator)
        } else {
            RoomAPI.changeBeatmap()
        }
    }

    private fun setMultiplayerRoomBeatmap(beatmapInfo: BeatmapInfo) {
        // Locking host from change beatmap before the server responses to beatmapChange
        Multiplayer.roomScene?.isWaitingForBeatmapChange?.store(true)

        if (Multiplayer.isConnected) {
            RoomAPI.changeBeatmap(beatmapInfo.md5, beatmapInfo.title, beatmapInfo.artist, beatmapInfo.version, beatmapInfo.creator)
        }
    }


    /**
     * Loads the beatmap sets from the database, showing the ones passing the filter. Called before
     * it's shown, and again when the library changes.
     */
    fun loadBeatmaps() {
        beatmapLoadingScope.launch {
            beatmapSets = DatabaseManager.beatmapInfoTable.getBeatmapSetList()
            applyFilter()
        }
    }

    /**
     * Shows the beatmap sets passing the filter of the filter bar, keeping the selected beatmap if
     * it's still shown.
     */
    fun applyFilter() {
        val filter = filterBar.filter
        val beatmapSets = beatmapSets

        filterJob?.cancel()
        filterJob = beatmapLoadingScope.launch {

            val favorites = if (filter.favoritesOnly) {
                DatabaseManager.beatmapOptionsTable.getFavoriteDirectories().toSet()
            } else emptySet()

            val collection = filter.collection?.let { DatabaseManager.beatmapCollectionsTable.getBeatmaps(it).toSet() }

            ensureActive()
            val models = filter.apply(beatmapSets, OsuSettings.difficultyAlgorithm, favorites, collection).map {
                BeatmapSetModel(it, isDifficulty = filter.grouping == BeatmapGrouping.Difficulty)
            }

            ensureActive()
            updateThread {
                beatmapCarrousel.selectedPanel = null
                beatmapCarrousel.data = models
                beatmapCarrousel.scrollY = 0f
                BeatmapSelection.beatmap?.let(::showBeatmap)
            }
        }
    }

    /**
     * Expands the panel of the given beatmap and scrolls to it, if it's shown.
     */
    private fun showBeatmap(beatmapInfo: BeatmapInfo) {
        val model = beatmapCarrousel.data.find { model -> model.beatmapSetInfo.beatmaps.any { it.path == beatmapInfo.path } } ?: return
        beatmapCarrousel.applyComponent(model) { select(beatmapInfo) }
    }

    /**
     * Shows the options of a beatmap set below its panel.
     */
    private fun showBeatmapSetOptions(panel: BeatmapSetPanel) {
        val beatmapSetInfo = panel.boundData?.beatmapSetInfo ?: return
        val directory = beatmapSetInfo.directory

        // The options of the set decide what the dropdown offers.
        beatmapLoadingScope.launch {
            val options = DatabaseManager.beatmapOptionsTable.getOptions(directory) ?: BeatmapOptions(directory)
            updateThread { showBeatmapSetOptions(panel, beatmapSetInfo, options) }
        }
    }

    private fun showBeatmapSetOptions(panel: BeatmapSetPanel, beatmapSetInfo: BeatmapSetInfo, options: BeatmapOptions) {
        val directory = beatmapSetInfo.directory
        val title = beatmapSetInfo.beatmaps.firstOrNull()?.titleText ?: directory

        Dropdown(panel.button).apply dropdown@{
            maxWidth = 320f.dip

            addButton {
                text = if (options.isFavorite) "Remove from favorites" else "Add to favorites"
                leadingIcon = faSolidIcon(FAIcon.Star)
                onActionUp = {
                    options.isFavorite = !options.isFavorite
                    this@dropdown.hide()

                    beatmapLoadingScope.launch {
                        DatabaseManager.beatmapOptionsTable.upsert(options)

                        if (filterBar.filter.favoritesOnly) {
                            applyFilter()
                        }
                    }
                }
            }

            addButton {
                text = "Manage collections"
                leadingIcon = faSolidIcon(FAIcon.FolderOpen)
                onActionUp = {
                    BeatmapCollectionsDialog(directory).apply {
                        onCollectionsChanged = {
                            filterBar.updateCollections()

                            if (filterBar.filter.collection != null) {
                                applyFilter()
                            }
                        }
                    }.show()
                    this@dropdown.hide()
                }
            }

            addButton {
                text = "Delete"
                leadingIcon = faSolidIcon(FAIcon.Trash)
                color = Color4("#FFBFBF")
                onActionUp = {
                    ConfirmDialog().apply {
                        this.title = "Delete beatmap set"
                        text = "Are you sure you want to delete \"$title\"? Its files will be removed from the device."
                        onConfirm = {
                            async {
                                LibraryManager.deleteBeatmapSet(beatmapSetInfo)
                                updateThread { onBeatmapSetDeleted(directory) }
                            }
                        }
                    }.show()
                    this@dropdown.hide()
                }
            }
        }.show()
    }

    /**
     * Removes a beatmap set from the list after it was deleted.
     */
    fun onBeatmapSetDeleted(directory: String) {
        beatmapSets = beatmapSets.filter { it.directory != directory }

        if (BeatmapSelection.beatmap?.setDirectory == directory) {
            onBeatmapSelected(null)
        }
        applyFilter()
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        BeatmapSelection.beatmap = beatmapInfo

        currentCalculationJob?.cancel(CancellationException("Calculation cancelled due to new selection."))
        currentBeatmapLoadingJob?.cancel(CancellationException("Beatmap loading cancelled due to new selection."))

        beatmapInfoWedge.onBeatmapSelected(beatmapInfo)
        leaderboard.onBeatmapSelected(beatmapInfo)

        currentBeatmapLoadingJob = beatmapLoadingScope.launch {

            ensureActive()
            if (beatmapInfo == null) {
                currentSongPath = null

                ensureActive()
                OsuGame.music.stop()

                return@launch
            }

            if (currentSongPath != beatmapInfo.audioPath) {
                currentSongPath = beatmapInfo.audioPath

                val music = OsuGame.music

                ensureActive()
                music.preLoad(beatmapInfo.audioPath)

                ensureActive()
                OsuGame.applyModsToMusic(mods)

                val previewTime = music.previewPoint(beatmapInfo)

                ensureActive()
                startPreview(previewTime)
            }

            ensureActive()
            currentCalculationJob = beatmapLoadingScope.launch calculationJob@{

                ensureActive()
                // A beatmap whose file was removed or can't be read only shows what the database has.
                val data = try {
                    BeatmapParser(beatmapInfo.path, this@calculationJob).parse(true)
                } catch (e: IOException) {
                    null
                } catch (e: IllegalArgumentException) {
                    null
                }

                if (data == null) {
                    ensureActive()
                    updateThread { beatmapInfoWedge.setDifficultyStatistics(beatmapInfo) }
                    return@calculationJob
                }

                ensureActive()
                val newBeatmapInfo = BeatmapInfo(data, beatmapInfo.dateImported, true, this@calculationJob)
                beatmapInfo.apply(newBeatmapInfo)

                ensureActive()
                updateThread { beatmapInfoWedge.setDifficultyStatistics(beatmapInfo) }

                val mods = mods.deepCopy().values

                ensureActive()
                val attributes = when (OsuSettings.difficultyAlgorithm) {
                    DifficultyAlgorithm.droid -> calculateDroidDifficulty(data, mods, this@calculationJob)
                    DifficultyAlgorithm.standard -> calculateStandardDifficulty(data, mods, this@calculationJob)
                }

                ensureActive()
                updateThread { beatmapInfoWedge.setStarRatingDisplay(attributes.starRating) }
            }
        }
    }

    /**
     * Plays the song loaded from [time], silent, and fades it in. It starts over there once it ends,
     * see [onUpdate]. Called from any thread.
     */
    private fun startPreview(time: Int) {
        OsuGame.music.playPreview(time)

        updateThread {
            musicFade.run { animate(musicVolume, OsuSettings.musicVolume, PREVIEW_FADE_IN, Easing.OutQuad) }
        }
    }

    override fun onUpdate(clock: FrameClock) {
        val mods = mods

        val music = OsuGame.music
        if (music.consumePreviewEnd()) {
            music.previewTime?.let(::startPreview)
        }

        if (mods.version != shownModsVersion) {
            shownModsVersion = mods.version
            modsIndicator.mods = mods.values.toList()
        }

        super.onUpdate(clock)
    }

    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) = updateThread {
        beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)
    }

    fun setStarRatingDisplay(starRating: Double) = updateThread {
        beatmapInfoWedge.setStarRatingDisplay(starRating)
    }

    // Scenes are never attached to a parent, onAttached doesn't run for them.
    override fun onEnter() {
        super.onEnter()

        // The selection's background, dimmed behind the lists.
        backgroundDim.alpha = 0f
        backgroundDim.fadeTo(1f - BACKGROUND_BRIGHTNESS, BeatmapBackground.BRIGHTNESS_DURATION)
        BeatmapBackground.fadeBrightness(1f)

        // Entrance: the wedge slides in from the left and the panels from the right, from a
        // whole window's width away while fading in, and the bottom bar slides up.
        val windowWidth = Verktex.graphics.window.windowWidth.toFloat()

        leftSide.apply {
            clearTransforms()
            alpha = 0f
            translationX = (-windowWidth).px
            transformSequence(0.05f) {
                fadeIn(0.4f, Easing.OutQuart)
                moveToX(0f, 0.4f, Easing.OutQuart)
            }
        }
        rightSide.apply {
            clearTransforms()
            alpha = 0f
            translationX = windowWidth.px
            transformSequence(0.1f) {
                fadeIn(0.4f, Easing.OutQuart)
                moveToX(0f, 0.4f, Easing.OutQuart)
            }
        }
        bottomBar.apply {
            clearTransforms()
            alpha = 0f
            translationY = 20f.dip
            transformSequence(0.1f) {
                fadeIn(0.3f, Easing.OutQuart)
                moveToY(0f, 0.4f, Easing.OutQuart)
            }
        }

        updateThread {
            val currentBeatmapInfo = BeatmapSelection.beatmap

            currentSongPath = currentBeatmapInfo?.audioPath

            // The song playing is the selection's (the menu's, or one a play gave back): it starts
            // over at its preview point from now on.
            val music = OsuGame.music
            if (currentBeatmapInfo != null && music.isPlaying && music.previewTime == null) {
                music.previewTime = music.previewPoint(currentBeatmapInfo)
            }

            // Opens the filters a cancelled play left on the song.
            musicFilters.run {
                animate(musicLowPass, AudioStream.MAX_LOW_PASS_CUTOFF, MUSIC_FILTER_DURATION, Easing.OutQuad)
                animate(musicHighPass, 0f, MUSIC_FILTER_DURATION, Easing.OutQuad)
            }

            filterBar.updateCollections()
            currentBeatmapInfo?.let(::showBeatmap)
        }
    }

    override fun onLeave() {
        super.onLeave()

        // The dim goes with the scene: the next one fades the background from how bright it looked.
        BeatmapBackground.setBrightness(BeatmapBackground.alpha * (1f - backgroundDim.alpha))
    }

    private companion object {
        const val BACKGROUND_BRIGHTNESS = 0.4f

        // How long a preview fades in, and the filters of a cancelled play open, in seconds.
        const val PREVIEW_FADE_IN = 0.3f
        const val MUSIC_FILTER_DURATION = 0.3f
    }
}
