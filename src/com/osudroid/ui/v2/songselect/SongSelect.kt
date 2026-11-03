package com.osudroid.ui.v2.songselect

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
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import kotlin.coroutines.cancellation.CancellationException


object SongSelect : UIScene() {

    /**
     * The currently selected beatmap.
     */
    var selectedBeatmap: BeatmapInfo? = null
        private set

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
            color *= 0.4f
        }
        background = backgroundSprite

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
                style = Style {
                    iconSize = 28f.px
                    spacing = (-5f).px
                }
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

        GlobalManager.getInstance().songService.apply {
            setSpeed(1f)
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
        ResourceManager.getInstance().getSound("menuhit")?.play()
        cancelJobs()

        if (Multiplayer.isMultiplayer) {
            setMultiplayerRoomBeatmap(beatmapInfo)
            back(false)
            return
        }

        val gameScene = GlobalManager.getInstance().gameScene

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


    @JvmOverloads
    fun loadBeatmaps(query: String? = null, sort: String = "title") {
        beatmapCarrousel.data = DatabaseManager.beatmapInfoTable.getBeatmapSetList().map { BeatmapSetModel(it) }
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        selectedBeatmap = beatmapInfo

        currentCalculationJob?.cancel(CancellationException("Calculation cancelled due to new selection."))
        currentBeatmapLoadingJob?.cancel(CancellationException("Beatmap loading cancelled due to new selection."))

        beatmapInfoWedge.onBeatmapSelected(beatmapInfo)
        leaderboard.onBeatmapSelected(beatmapInfo)

        currentBeatmapLoadingJob = beatmapLoadingScope.launch {

            GlobalManager.getInstance().selectedBeatmap = beatmapInfo

            ensureActive()
            if (beatmapInfo == null) {
                currentSongPath = null
                currentBackgroundPath = null

                updateThread { backgroundSprite.textureRegion = SkinTextures["menu-background"] }

                ensureActive()
                GlobalManager.getInstance().songService.stop()

                return@launch
            }

            val backgroundPath = beatmapInfo.backgroundPath

            if (!Config.isSafeBeatmapBg() && backgroundPath != null && currentBackgroundPath != backgroundPath) {
                currentBackgroundPath = backgroundPath

                ensureActive()
                val textureRegion = Textures.load("::background", backgroundPath)

                ensureActive()
                updateThread { backgroundSprite.textureRegion = textureRegion ?: SkinTextures["menu-background"] }
            }

            if (currentSongPath != beatmapInfo.audioPath) {
                currentSongPath = beatmapInfo.audioPath

                val songService = GlobalManager.getInstance().songService

                ensureActive()
                songService.preLoad(beatmapInfo.audioPath)

                ensureActive()
                updateMusicEffects()

                val previewTime = if (beatmapInfo.previewTime >= 0)
                    beatmapInfo.previewTime
                else
                    (songService.length * 0.4f).toInt()

                ensureActive()
                songService.play()

                ensureActive()
                songService.seekTo(previewTime)
            }

            ensureActive()
            currentCalculationJob = beatmapLoadingScope.launch calculationJob@{

                ensureActive()
                val data = BeatmapParser(beatmapInfo.path, this@calculationJob).parse(true)

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
                val attributes = when (Config.getDifficultyAlgorithm()) {
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

        GlobalManager.getInstance().songService.setSpeed(speed)
        GlobalManager.getInstance().songService.setAdjustPitch(adjustPitch)
    }

    override fun onAttached() {
        super.onAttached()

        updateThread {
            val currentBeatmapInfo = GlobalManager.getInstance().selectedBeatmap

            currentSongPath = currentBeatmapInfo?.audioPath
            currentBackgroundPath = null

            val model = beatmapCarrousel.data.find { it.beatmapSetInfo.directory == currentBeatmapInfo?.setDirectory } ?: return@updateThread

            beatmapCarrousel.applyComponent(model) {
                val beatmapInfoIndex = model.beatmapSetInfo.beatmaps.indexOfFirst { it.path == currentBeatmapInfo?.path }
                select(beatmapInfoIndex.coerceAtLeast(0))
            }
        }
    }

}
