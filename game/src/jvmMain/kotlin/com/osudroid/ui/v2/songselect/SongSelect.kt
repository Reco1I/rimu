package com.osudroid.ui.v2.songselect

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
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
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.control.UIDropdown
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.textButton
import java.io.IOException
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import kotlin.coroutines.cancellation.CancellationException


object SongSelect : UIScene() {

    /**
     * The scores currently shown in the leaderboard, in the format used by the gameplay leaderboard.
     */
    val scoreBoardItems: List<ScoreBoardItem>
        get() = leaderboard.scoreBoardItems

    /**
     * Whether the leaderboard is showing online scores.
     */
    val isLeaderboardOnline
        get() = leaderboard.leaderboardType == LeaderboardType.Global



    private val beatmapLoadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val filterBar = SongSelectFilterBar()
    private val leaderboard = SongSelectLeaderboard()
    private val beatmapCarrousel = BeatmapCarrousel()
    private val beatmapInfoWedge = BeatmapInfoWedge()
    private val backgroundSprite: UISprite

    private lateinit var modsIndicator: ModsIndicator

    private var beatmapSets = listOf<BeatmapSetInfo>()
    private var filterJob: Job? = null

    private var currentSongPath: String? = null
    private var currentBackgroundPath: String? = null
    private var currentBeatmapLoadingJob: Job? = null
    private var currentCalculationJob: Job? = null


    init {
        backgroundSprite = UISprite().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            textureRegion = SkinTextures["menu-background"]
            scaleType = UISprite.ScaleType.Crop
            color = color.shade(0.4f)
        }
        background = backgroundSprite

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
            linearContainer {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                orientation = UILinearContainer.Orientation.Vertical
                spacing = 8f.dip

                +beatmapInfoWedge
                +leaderboard
            }

            // Right side
            container {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable

                +beatmapCarrousel
                +filterBar
            }
        }

        linearContainer {
            orientation = UILinearContainer.Orientation.Horizontal
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            padding = Padding(8f.dip, 16f.dip)
            spacing = 8f.dip

            textButton {
                text = "Back"
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                leadingIcon = faSolidIcon(FAIcon.ArrowLeft)

                onActionUp = { back() }
            }

            textButton {
                text = "Mods"
                leadingIcon = faSolidIcon(FAIcon.Sliders)
                onActionUp = { ModMenu.show() }
            }

            modsIndicator = ModsIndicator().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                iconSize = 28f.px
                spacing = (-5f).px
            }
            +modsIndicator

            textButton {
                text = "Random"
                leadingIcon = faSolidIcon(FAIcon.Shuffle)
                onActionUp = {
                    if (beatmapCarrousel.data.isNotEmpty()) {
                        beatmapCarrousel.applyComponent(beatmapCarrousel.data.random()) { selectRandom() }
                    }
                }
            }
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
    fun back() = back(true)

    private fun back(resetMultiplayerBeatmap: Boolean) {
        cancelJobs()

        Verktex.withTreeLock { Verktex.popScene() }

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

        val gameScene = OsuGame.gameScene

        gameScene.setOldScene(this)
        gameScene.startGame(beatmapInfo, null, ModMenu.enabledMods)
    }

    private fun resetMultiplayerRoomBeatmap() {
        // Locking host from change beatmap before the server responses to beatmapChange
        Multiplayer.roomScene?.isWaitingForBeatmapChange?.set(true)

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
        Multiplayer.roomScene?.isWaitingForBeatmapChange?.set(true)

        if (Multiplayer.isConnected) {
            RoomAPI.changeBeatmap(beatmapInfo.md5, beatmapInfo.title, beatmapInfo.artist, beatmapInfo.version, beatmapInfo.creator)
        }
    }


    /**
     * Loads the beatmap sets from the database, showing the ones passing the filter.
     */
    fun loadBeatmaps() {
        beatmapSets = DatabaseManager.beatmapInfoTable.getBeatmapSetList()
        applyFilter()
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

            val collection = filter.collection?.let { DatabaseManager.beatmapCollectionsTable.getBeatmaps(it)?.toSet() }

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
        val title = beatmapSetInfo.beatmaps.firstOrNull()?.titleText ?: directory

        UIDropdown(panel.button).apply dropdown@{
            maxWidth = 320f.dip

            val options = DatabaseManager.beatmapOptionsTable.getOptions(directory) ?: BeatmapOptions(directory)

            addButton {
                text = if (options.isFavorite) "Remove from favorites" else "Add to favorites"
                leadingIcon = faSolidIcon(FAIcon.Star)
                onActionUp = {
                    options.isFavorite = !options.isFavorite
                    DatabaseManager.beatmapOptionsTable.upsert(options)
                    this@dropdown.hide()

                    if (filterBar.filter.favoritesOnly) {
                        applyFilter()
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
                    UIConfirmDialog().apply {
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
                currentBackgroundPath = null

                updateThread { backgroundSprite.textureRegion = SkinTextures["menu-background"] }

                ensureActive()
                OsuGame.music.stop()

                return@launch
            }

            val backgroundPath = beatmapInfo.backgroundPath

            if (!Settings[OsuSettings.SafeBeatmapBackground] && backgroundPath != null && currentBackgroundPath != backgroundPath) {
                currentBackgroundPath = backgroundPath

                ensureActive()
                val textureRegion = Textures.load("::background", backgroundPath)

                ensureActive()
                updateThread { backgroundSprite.textureRegion = textureRegion ?: SkinTextures["menu-background"] }
            }

            if (currentSongPath != beatmapInfo.audioPath) {
                currentSongPath = beatmapInfo.audioPath

                val music = OsuGame.music

                ensureActive()
                music.preLoad(beatmapInfo.audioPath)

                ensureActive()
                updateMusicEffects()

                val previewTime = if (beatmapInfo.previewTime >= 0)
                    beatmapInfo.previewTime
                else
                    (music.length * 0.4f).toInt()

                ensureActive()
                music.play()

                ensureActive()
                music.seekTo(previewTime)
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

                val mods = ModMenu.enabledMods.deepCopy().values

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

    fun onModsChanged() {
        modsIndicator.mods = ModMenu.enabledMods.values.toList()
    }

    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) = updateThread {
        beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)
    }

    fun setStarRatingDisplay(starRating: Double) = updateThread {
        beatmapInfoWedge.setStarRatingDisplay(starRating)
    }

    fun updateMusicEffects() {
        val speed = ModMenu.enabledMods.values.calculateRate(Double.POSITIVE_INFINITY)
        val adjustPitch = ModNightCore::class in ModMenu.enabledMods

        OsuGame.music.speed = speed
        OsuGame.music.setAdjustPitch(adjustPitch)
    }

    // Scenes are never attached to a parent, onAttached doesn't run for them.
    override fun onEnter() {
        super.onEnter()

        updateThread {
            val currentBeatmapInfo = BeatmapSelection.beatmap

            currentSongPath = currentBeatmapInfo?.audioPath
            currentBackgroundPath = null

            filterBar.updateCollections()
            currentBeatmapInfo?.let(::showBeatmap)
        }
    }

}
