package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.OsuGameHost
import com.osudroid.game.GameplayVibrations
import kotlinx.coroutines.runBlocking
import com.osudroid.settings.GameFolders
import com.osudroid.settings.GameResolution
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.Platform
import com.edlplan.osu.support.slider.SliderBody
import com.osudroid.GameMode
import com.osudroid.audio.MusicClock
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.constants.BeatmapCountdown
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderRepeat
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.beatmaps.timings.BreakPeriod
import com.osudroid.beatmaps.timings.EffectControlPoint
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.data.toScoreInfo
import com.osudroid.difficulty.BeatmapDifficultyCalculator
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes
import com.osudroid.difficulty.attributes.DroidPerformanceAttributes
import com.osudroid.difficulty.attributes.PerformanceAttributes
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes
import com.osudroid.difficulty.attributes.StandardPerformanceAttributes
import com.osudroid.difficulty.attributes.TimedDifficultyAttributes
import com.osudroid.difficulty.calculator.DroidPerformanceCalculationParameters
import com.osudroid.difficulty.calculator.PerformanceCalculationParameters
import com.osudroid.difficulty.calculator.StandardPerformanceCalculationParameters
import com.osudroid.game.Cursor
import com.osudroid.game.CursorEvent
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.replay.ReplaySettingsPanel
import com.osudroid.math.Interpolation
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import com.osudroid.mods.*
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.toBoardItem
import com.osudroid.resources.Res
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.UIVideo
import com.osudroid.ui.v2.game.AutoCursor
import com.osudroid.ui.v2.game.BreakAnimator
import com.osudroid.ui.v2.game.ComboBurst
import com.osudroid.ui.v2.game.Countdown
import com.osudroid.ui.v2.game.CursorEntity
import com.osudroid.ui.v2.game.FlashLightEntity
import com.osudroid.ui.v2.game.FollowPointConnection
import com.osudroid.ui.v2.game.PauseMenu
import com.osudroid.ui.v2.game.SliderTickSprite
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard
import com.osudroid.ui.v2.modmenu.ModIcon
import com.osudroid.ui.v2.multi.LobbyScene
import com.osudroid.utils.BitSet
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.calculateRate
import com.osudroid.utils.async
import com.osudroid.utils.runSafe
import com.osudroid.utils.stopAsync
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.time.FrameStableClock
import com.reco1l.verktex.time.SongClock
import com.reco1l.verktex.time.Stopwatch
import com.reco1l.verktex.ui.UIAnimatedSprite
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.audio.effect.Metronome
import com.osudroid.audio.MusicService
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.GlobalManager
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.scoring.TouchType
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

open class GameScene : GameObjectListener {
    private val maximumActiveCursorCount = 3
    private var cursors = arrayOfNulls<Cursor>(CursorCount)
    @JvmField
    var audioFilePath: String? = null
    private var gameUIScene: GameUIScene
    private lateinit var root: UIContainer
    private lateinit var backgroundLayer: UIContainer
    private lateinit var playfield: UIContainer
    private lateinit var bgScene: UIContainer
    private lateinit var mgScene: UIContainer
    private lateinit var fgScene: UIContainer
    private var oldScene: UIScene? = null
    private var failAnimation: FailAnimation? = null
    private var pauseMenu: PauseMenu? = null
    private var replayText: UIText? = null
    private var sceneBorder: UIBox? = null
    private var beatmapBackground: UIComponent? = null
    private var beatmapBackgroundWidth = 0f
    private var beatmapBackgroundHeight = 0f
    private var parsedBeatmap: Beatmap? = null
    var playableBeatmap: DroidPlayableBeatmap? = null
        private set
    private var lastBeatmapInfo: BeatmapInfo? = null
    private var scoringScene: ScoringScene? = null
    private var timingControlPoints: Array<TimingControlPoint>? = null
    private var timingControlPointIndex = 0
    private var effectControlPoints: Array<EffectControlPoint>? = null
    private var effectControlPointIndex = 0
    private var activeTimingPoint: TimingControlPoint? = null
    private var activeEffectPoint: EffectControlPoint? = null
    private var lastObjectId = -1
    private var leadOut = 0f
    private var objects: Array<HitObject?>? = null
    private var objectIndex = 0
    private var postSeekFrameCount = 0
    private var comboColors: ArrayList<Color4>? = null
    private var comboWasMissed = false
    private var comboWas100 = false
    private var activeObjects: ArrayList<GameObject>? = null
    private var expiredObjects: ArrayList<GameObject>? = null
    private val processedExpiredObjects: MutableSet<GameObject> = Collections.newSetFromMap(IdentityHashMap())
    private var judgeableObject: GameObject? = null
    private var breakPeriods: Array<BreakPeriod>? = null
    private var breakPeriodIndex = 0
    private var metronome: Metronome? = null
    private var scale = 0f
    lateinit var stat: StatisticV2
    private var gameStarted = false
    private val gameplayClock: FrameStableClock
    private var initialStartTimeValue = 0f
    private var totalLength = Int.MAX_VALUE
    private var paused = false
    private var skipBtn: UISprite? = null
    private var skipTime = 0f
    private var distToNextObject = 0.0
    private var cursorSprites: Array<CursorEntity>? = null
    private var autoCursor: AutoCursor? = null
    private var flashlightSprite: FlashLightEntity? = null
    private var mainCursorId = -1
    private var replay: Replay? = null
    var replaying = false
        private set
    private var replayFilePath: String? = null
    private var dimRectangle: UIBox? = null
    private var comboBurst: ComboBurst? = null
    private var failcount = 0
    private var sliderBorderColor: Color4? = null
    private var sliderPaths: Array<SliderPath?>? = null
    private var sliderRenderPaths: Array<SliderBody.RenderPathCache?>? = null
    private var sliderIndex = 0
    private var unrankedSprite: UISprite? = null
    private val modIcons = ArrayList<ModIcon>()
    private val rateAdjustingMods = ArrayList<IModApplicableToTrackRate>()

    @JvmField
    var hitWindow: HitWindow? = null

    private val effects = GameplayEffects()
    private var lastMods: ModHashMap? = null

    private val loadingRequestId = AtomicInteger(0)
    private var loadingPipeline: CompletableFuture<*>? = null
    private var gameLoadingJob: Job? = null

    private var performanceAttributes: PerformanceAttributes? = null
    private var performanceCalculationParameters: PerformanceCalculationParameters? = null
    @Volatile
    private var droidTimedDifficultyAttributes: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>? = null
    @Volatile
    private var standardTimedDifficultyAttributes: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>? = null

    private var ppCalculationJob: Job? = null

    var replaySettingsPanel: ReplaySettingsPanel? = null
        private set

    // Game

    /**
     * Whether the game is over.
     */
    private var isGameOver = false

    /**
     * The break time animator.
     */
    private var breakAnimator: BreakAnimator? = null

    /**
     * The countdown animator.
     */
    @JvmField
    var countdownAnimator: Countdown? = null

    /**
     * Whether the game is ready to start.
     */
    @JvmField
    var isReadyToStart = false


    // UI

    /**
     * The gameplay HUD
     */
    @JvmField
    var hud: GameplayHUD? = null

    /**
     * Whether the HUD editor mode is enabled.
     */
    @JvmField
    var isHUDEditorMode = false

    /**
     * Whether the game started in HUD editor mode.
     */
    @JvmField
    var startedFromHUDEditor = false


    // Timing

    /**
     * The time at which the last frame was rendered with respect to [Platform.uptimeMillis].
     *
     * If 0, a frame has not been rendered yet.
     */
    private var previousFrameTime = 0L

    /**
     * The start time of the first object in seconds.
     */
    @JvmField
    var firstObjectStartTime = 0f

    /**
     * The end time of the last object in seconds.
     */
    @JvmField
    var lastObjectEndTime = 0f

    // Video support

    /**
     * The current video loading [Job].
     */
    private var videoLoadingJob: Job? = null

    /**
     * Whether video is enabled.
     */
    private var videoEnabled = false

    /**The video sprite*/
    private var video: UIVideo? = null

    /**Video offset aka video start time in seconds*/
    private var videoOffset = 0f

    /**Whether the video has started*/
    private var videoStarted = false


    // Multiplayer

    /**Indicates the last time that the user pressed the back button, used to reset `backPressCount`*/
    private var lastBackPressTime = -1f

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    @JvmField
    var hasFailed = false

    /**Indicates that the player has requested skip*/
    private var isSkipRequested = false

    /**Real time elapsed in milliseconds since the game has started*/
    private var realTimeElapsed = 0L

    /**Real time elapsed in milliseconds since the latest statistic data was sent*/
    private var statisticDataTimeElapsed = 0L

    /**Last score data chunk sent to server, used to determine if the data was changed.*/
    private var lastScoreSent: ScoreBoardItem? = null


    init {
        gameplayClock = FrameStableClock(SongClock(true, true, null))

        gameUIScene = createScene()
    }

    private fun createScene(): GameUIScene {
        root = UIContainer()
        root.width = Dimension.FillAvailable
        root.height = Dimension.FillAvailable
        root.backgroundColor = Color4.Black

        backgroundLayer = UIContainer()
        backgroundLayer.width = Dimension.FillAvailable
        backgroundLayer.height = Dimension.FillAvailable

        playfield = UIContainer()
        playfield.setSize(GameResolution.width.toFloat(), GameResolution.height.toFloat())
        playfield.scaleOrigin = Anchor.TopLeft

        bgScene = createLayer()
        mgScene = createLayer()
        mgScene.clipping = ClipMode.Bounds
        fgScene = createLayer()

        effects.setLayers(bgScene, mgScene)

        playfield += bgScene
        playfield += mgScene
        playfield += fgScene

        root += backgroundLayer
        root += playfield

        // The background of a scene isn't one of its children, so it doesn't inherit the scene's clock. The
        // timelines of the hit objects and the HUD are on the gameplay clock.
        root.clock = gameplayClock

        val scene = GameUIScene()
        scene.background = root
        return scene
    }

    private fun createLayer(): UIContainer {
        val layer = UIContainer()
        layer.setSize(GameResolution.width.toFloat(), GameResolution.height.toFloat())
        return layer
    }

    fun setScoringScene(sc: ScoringScene?) {
        scoringScene = sc
    }

    fun setOldScene(oscene: UIScene?) {
        oldScene = oscene
    }

    private fun loadBackground() {
        if (dimRectangle != null) {
            dimRectangle!!.removeSelf()
            dimRectangle = null
        }

        if (sceneBorder != null) {
            sceneBorder!!.removeSelf()
            sceneBorder = null
        }

        releaseVideo()

        val playableBeatmap = this.playableBeatmap ?: return

        val textureRegion = if (Settings[OsuSettings.SafeBeatmapBackground] || playableBeatmap.events.backgroundFilename == null)
            SkinTextures.get("menu-background")
        else
            Textures.get("::background")

        if (textureRegion == null) {
            val backgroundColor = playableBeatmap.events.backgroundColor

            val box = UIBox()
            box.width = Dimension.FillAvailable
            box.height = Dimension.FillAvailable
            box.backgroundColor = backgroundColor ?: Color4.Black

            beatmapBackground = box
            beatmapBackgroundWidth = 0f
            beatmapBackgroundHeight = 0f
        } else {
            beatmapBackground = UISprite(textureRegion)
            beatmapBackgroundWidth = textureRegion.width.toFloat()
            beatmapBackgroundHeight = textureRegion.height.toFloat()
        }
    }

    fun loadVideo(beatmapInfo: BeatmapInfo) {
        val playableBeatmap = this.playableBeatmap ?: return

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        val brightness = OsuSettings.backgroundBrightness
        val videoFilename = playableBeatmap.events.videoFilename
        // Some graphics backends can't show video frames on some devices (e.g. Vulkan before Android 10).
        val videosSupported = Verktex.graphics.device.capabilities.supportsExternalTextures
        videoEnabled = brightness > 0.02f && Settings[OsuSettings.EnableVideo] && videoFilename != null && videosSupported

        if (!videoEnabled) {
            cancelVideoLoading()
            releaseVideo()
            return
        }

        if (video != null) {
            return
        }

        if (videoLoadingJob != null && !videoLoadingJob!!.isCompleted) {
            return
        }

        // Check if video file exists before attempting to load.
        val videoFile = File(beatmapInfo.absoluteSetDirectory + "/" + videoFilename)

        if (!videoFile.exists()) {
            return
        }

        videoLoadingJob = async {
            try {
                val video = OsuGameHost.current.createVideo(beatmapInfo.absoluteSetDirectory + "/" + videoFilename)
                    ?: return@async
                video.alpha = 0f

                ensureActive()

                this@GameScene.video = video

                // applyBackground is called from the onReady callback once ExoPlayer reports the video dimensions via
                // onVideoSizeChanged, rather than blocking here.
                // If dimensions are already known (fast local file), the callback fires immediately.
                video.setOnReady { updateThread { applyBackground() } }
            } catch (e: Exception) {
                video = null
                Logger.e("GameScene", "Error while loading video background.", e)
            }

            videoLoadingJob = null
        }
    }

    private fun cancelVideoLoading() {
        if (videoLoadingJob != null) {
            videoLoadingJob!!.cancel(CancellationException("Video loading job cancelled"))
            videoLoadingJob = null
        }
    }

    private fun releaseVideo() {
        if (video != null) {
            video!!.release()
            video = null
            videoStarted = false
        }
    }

    private fun applyBackground() {
        // Guard: video sprite exists but dimensions not yet reported by ExoPlayer.
        // onVideoSizeChanged will trigger another applyBackground call when they arrive.
        if (video != null && (video!!.videoWidth == 0 || video!!.videoHeight == 0)) {
            return
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        val brightness = OsuSettings.backgroundBrightness

        val playfieldSize = OsuSettings.playfieldSize

        val video = this.video
        var sceneBorder = this.sceneBorder

        if (sceneBorder == null && Settings[OsuSettings.DisplayPlayfieldBorder] && playfieldSize < 1f) {
            sceneBorder = UIBox().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                border = Border(Dimension.Fixed.Pixels(5f), Color4.White, Color4.White)
                alpha = Interpolation.linear(0.2f, 0.8f, brightness)
                setSize(GameResolution.width.toFloat(), GameResolution.height.toFloat())
            }

            playfield.add(0, sceneBorder)
            this.sceneBorder = sceneBorder
        }

        val showVideo = videoEnabled && video != null
        val background: UIComponent? = if (showVideo) video else beatmapBackground

        val backgroundWidth = if (showVideo) video!!.videoWidth.toFloat() else beatmapBackgroundWidth
        val backgroundHeight = if (showVideo) video!!.videoHeight.toFloat() else beatmapBackgroundHeight

        backgroundLayer.clearChildren()

        if (background != null) {
            if (backgroundWidth > 0f && backgroundHeight > 0f) {
                // Fit in the window through the size instead of the scale, the size is relative to its parent.
                val windowWidth = root.bounds.width
                val windowHeight = root.bounds.height

                val factor = if (Settings[OsuSettings.KeepBackgroundAspectRatio])
                    windowHeight / backgroundHeight
                else
                    windowWidth / backgroundWidth

                background.anchor = Anchor.Center
                background.origin = Anchor.Center
                background.setSize(backgroundWidth * factor, backgroundHeight * factor)
            }

            backgroundLayer += background
        }

        if (dimRectangle == null) {
            dimRectangle = UIBox()
            dimRectangle!!.width = Dimension.FillAvailable
            dimRectangle!!.height = Dimension.FillAvailable
            dimRectangle!!.backgroundColor = Color4.Black
        }

        dimRectangle!!.alpha = 1f - brightness
        backgroundLayer += dimRectangle!!

        if (breakAnimator != null) {
            breakAnimator!!.setDimBrightness(brightness)
            breakAnimator!!.setDimRectangle(dimRectangle)
        }

        // Storyboards are drawn here once they're available again, on top of the background and below the playfield.
    }

    private fun loadGame(beatmapInfo: BeatmapInfo, rFile: String?, mods: ModHashMap, scope: CoroutineScope?): Boolean {
        if (!OsuGameHost.current.verifyFileIntegrity()) {
            Toasts.showLong(Res.string.file_integrity_tampered)
            return false
        }

        scope?.ensureActive()

        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFilePath = GameFolders.cachePath + "/" +
                MD5Calculator.getStringMD5(rFile) + ".odr"
            Logger.i("GameScene", "ReplayFile = $replayFilePath")
            // Loading runs on a background thread, which waits for the download.
            if (!runBlocking { OnlineFileOperator.downloadFile(rFile, replayFilePath!!) }) {
                Toasts.showLong(Res.string.replay_cantdownload)
                return false
            }
        } else
            this.replayFilePath = rFile

        scope?.ensureActive()

        val shouldParseBeatmap = parsedBeatmap == null || parsedBeatmap!!.md5 != beatmapInfo.md5

        if (shouldParseBeatmap) {
            try {
                parsedBeatmap = BeatmapCache.getBeatmap(beatmapInfo, true, GameMode.Droid, scope)
            } catch (e: IOException) {
                Logger.e("GameScene", "startGame: " + e.message)
                Toasts.showLong(e.message.toString())
                return false
            } catch (e: IllegalArgumentException) {
                Logger.e("GameScene", "startGame: " + e.message)
                Toasts.showLong(e.message.toString())
                return false
            }
        }

        val parsedBeatmap = parsedBeatmap ?: return false

        if (parsedBeatmap.md5 != beatmapInfo.md5) {
            Toasts.showLong(Res.string.file_integrity_tampered)
            return false
        }

        if (parsedBeatmap.hitObjects.objects.isEmpty()) {
            Toasts.showLong("Empty Beatmap")
            return false
        }

        // Ensure that only relevant mods are applied.
        mods.values.removeAll { m -> !m.isRelevant }

        val differentPlayableBeatmap = shouldParseBeatmap || lastMods == null || lastMods != mods

        val playableBeatmap = if (differentPlayableBeatmap)
            parsedBeatmap.createDroidPlayableBeatmap(mods.values)
        else
            this.playableBeatmap!!

        this.playableBeatmap = playableBeatmap

        if (isHUDEditorMode || OsuSkin.get().hudSkinData.hasElement(HUDElements.pp_counter.name)) {
            val finalParsedBeatmap = parsedBeatmap

            // Calculate timed difficulty attributes
            when (OsuSettings.difficultyAlgorithm) {
                DifficultyAlgorithm.droid -> {
                    performanceAttributes = DroidPerformanceAttributes()
                    performanceCalculationParameters = DroidPerformanceCalculationParameters()

                    if (droidTimedDifficultyAttributes == null || differentPlayableBeatmap) {
                        val finalPlayableBeatmap = playableBeatmap

                        ppCalculationJob = async {
                            droidTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(
                                finalParsedBeatmap, finalPlayableBeatmap, this
                            )

                            updateThread {
                                if (objectIndex > 0) {
                                    updatePPValue(objectIndex - 1)
                                }
                            }
                        }
                    }
                }

                DifficultyAlgorithm.standard -> {
                    performanceAttributes = StandardPerformanceAttributes()
                    performanceCalculationParameters = StandardPerformanceCalculationParameters()

                    if (standardTimedDifficultyAttributes == null || differentPlayableBeatmap) {
                        val modValues = mods.values

                        ppCalculationJob = async {
                            standardTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                                finalParsedBeatmap, modValues, this
                            )

                            updateThread {
                                if (objectIndex > 0) {
                                    updatePPValue(objectIndex - 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Load backgrounds early to minimize waiting time.
        loadBackground()
        loadVideo(beatmapInfo)

        rateAdjustingMods.clear()

        for (mod in mods.values) {
            if (mod is IModApplicableToTrackRate) {
                rateAdjustingMods.add(mod)
            }
        }

        // TODO skin manager
        if (shouldParseBeatmap) {
            BeatmapSkinManager.getInstance().loadBeatmapSkin(playableBeatmap.beatmapsetPath)
        }

        val breaks = playableBeatmap.events.breaks

        if (shouldParseBeatmap || breakPeriods == null || breakPeriods!!.size != breaks.size) {
            breakPeriods = breaks.toTypedArray()
        }

        try {
            val musicFile = File(beatmapInfo.audioPath)

            if (!musicFile.exists()) {
                throw FileNotFoundException(musicFile.path)
            }

            audioFilePath = musicFile.path

        } catch (e: Exception) {
            Logger.e("GameScene", "Load Music: " + e.message)
            Toasts.showLong(e.message.toString())
            return false
        }

        val hitObjects = playableBeatmap.hitObjects.objects
        val firstObject = hitObjects[0]
        scale = firstObject.screenSpaceGameplayScale
        effects.setScale(scale)

        GameHelper.setOverallDifficulty(playableBeatmap.difficulty.od)
        GameHelper.setHealthDrain(playableBeatmap.difficulty.hp)
        GameHelper.setSpeedMultiplier(mods.values.calculateRate(Double.NEGATIVE_INFINITY))

        GameHelper.setOriginalTimePreempt(BeatmapDifficulty.difficultyRange(
            playableBeatmap.difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ).toFloat())

        scope?.ensureActive()

        GlobalManager.getInstance().music.preLoad(audioFilePath, GameHelper.getSpeedMultiplier(),
            GameHelper.getSpeedMultiplier() != 1f &&
                (Settings[OsuSettings.ShiftPitchInRateChange] || ModNightCore::class in mods || ModOldNightCore::class in mods))

        gameplayClock.rate = GameHelper.getSpeedMultiplier()

        scope?.ensureActive()

        totalLength = GlobalManager.getInstance().music.length
        judgeableObject = null
        breakPeriodIndex = 0
        objectIndex = 0
        lastObjectId = -1
        hitWindow = playableBeatmap.hitWindow
        videoStarted = false
        videoOffset = playableBeatmap.events.videoStartTime / 1000f

        if (shouldParseBeatmap || objects == null || objects!!.size != hitObjects.size) {
            objects = arrayOfNulls<HitObject>(hitObjects.size)
        }

        val objects = objects!!

        if (differentPlayableBeatmap || objects[0] == null) {
            hitObjects.toTypedArray().copyInto(objects, 0, 0, objects.size)
        }

        firstObjectStartTime = firstObject.startTime.toFloat() / 1000
        lastObjectEndTime = objects[objects.size - 1]!!.endTime.toFloat() / 1000

        val estimatedMaxActiveObjects = max(10, estimateMaximumActiveObjects())

        if (activeObjects != null) {
            activeObjects!!.clear()
            activeObjects!!.ensureCapacity(estimatedMaxActiveObjects)
        } else {
            activeObjects = ArrayList(estimatedMaxActiveObjects)
        }

        if (expiredObjects != null) {
            expiredObjects!!.clear()
            expiredObjects!!.ensureCapacity(estimatedMaxActiveObjects)
        } else {
            expiredObjects = ArrayList(estimatedMaxActiveObjects)
        }

        val firstObjectTimePreempt = firstObject.timePreempt.toFloat() / 1000
        val skipTargetTime = firstObjectStartTime - max(2f, firstObjectTimePreempt)

        gameplayClock.gameplayStartTime = skipTargetTime
        gameplayClock.seek(min(0f, skipTargetTime))
        skipTime = skipTargetTime - 1

        // Some beatmaps specify a current lead-in time, which overrides the default lead-in time above.
        val leadIn = playableBeatmap.general.audioLeadIn / 1000f
        if (leadIn > 0) {
            gameplayClock.seek(min(gameplayClock.time, firstObjectStartTime - leadIn))
        }

        // Ensure the video has time to start.
        // Even when video is not activated, apply offset anyway to ensure that everyone in multiplayer starts at the
        // same time regardless of the setting.
        gameplayClock.seek(min(gameplayClock.time, videoOffset))

        sliderBorderColor = null
        if (playableBeatmap.colors.sliderBorderColor != null) {
            sliderBorderColor = playableBeatmap.colors.sliderBorderColor
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor) {
            sliderBorderColor = OsuSkin.get().sliderBorderColor
        }

        if (sliderBorderColor == null) {
            sliderBorderColor = BeatmapSkinManager.getInstance().sliderColor
        }

        val comboColors = ArrayList<Color4>()
        this.comboColors = comboColors
        for (comboColor in playableBeatmap.colors.comboColors) {
            scope?.ensureActive()

            comboColors.add(comboColor.color)
        }

        if (comboColors.isEmpty() || OsuSkin.get().isForceOverrideComboColor) {
            comboColors.clear()
            comboColors.addAll(OsuSkin.get().comboColor)
        }

        scope?.ensureActive()

        val timingControlPointManager = playableBeatmap.controlPoints.timing
        val effectControlPointManager = playableBeatmap.controlPoints.effect

        if (shouldParseBeatmap || timingControlPoints == null) {
            timingControlPoints = timingControlPointManager.controlPoints.toTypedArray()
        }

        if (shouldParseBeatmap || effectControlPoints == null) {
            effectControlPoints = effectControlPointManager.controlPoints.toTypedArray()
        }

        activeTimingPoint = if (timingControlPoints!!.isNotEmpty()) timingControlPoints!![0] else timingControlPointManager.defaultControlPoint
        activeEffectPoint = if (effectControlPoints!!.isNotEmpty()) effectControlPoints!![0] else effectControlPointManager.defaultControlPoint
        timingControlPointIndex = 0
        effectControlPointIndex = 0

        GameHelper.setBeatLength(activeTimingPoint!!.msPerBeat / 1000)
        GameHelper.setKiai(activeEffectPoint!!.isKiai)
        GameHelper.setCurrentBeatTime(0.0)
        GameHelper.setSamplesMatchPlaybackRate(playableBeatmap.general.samplesMatchPlaybackRate)

        GameObjectPool.getInstance().purge()

        scope?.ensureActive()

        FollowPointConnection.renew(16)
        SliderTickSprite.renew(16)

        // TODO replay
        replaying = false
        var replay: Replay? = Replay(true)
        this.replay = replay
        replay!!.setObjectCount(hitObjects.size)
        replay.setBeatmap(beatmapInfo.fullBeatmapsetName, beatmapInfo.fullBeatmapName, playableBeatmap.md5)

        if (replayFilePath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            val replayStat = StatisticV2()
            replayStat.mod = mods
            replay.setStat(replayStat)

            scope?.ensureActive()

            replaying = replay.load(replayFilePath!!, true)

            if (!replaying) {
                Toasts.showLong(Res.string.replay_invalid)
                return false
            }

            // In older versions, replay uploads are separated from scores, which means that they may not be uploaded
            // for reasons independent of score uploads (e.g., network failure). When this happens, replays may be very
            // off such that it causes gameplay to appear very wrong (e.g., a score has Hard Rock/Mirror mod while its
            // replay does not). While this can theoretically happen to any score data (not just mods), checking for
            // mods for the time being is enough to dislodge major inconsistencies in gameplay.
            // Checking for mod existence (not equality) is enough for this case since affected mods do not have
            // customizations.
            for (mod in mods.values) {
                if (!replay.getStat()!!.mod.containsKey(mod::class)) {
                    Toasts.showLong(Res.string.replay_invalid)
                    return false
                }
            }

            GameHelper.setReplayVersion(replay.replayVersion)
        } else if (ModAutoplay::class in mods) {
            replay = null
            this.replay = null
        }

        scope?.ensureActive()

        GameObjectPool.getInstance().preload()

        sliderIndex = 0

        if (sliderPaths == null || sliderRenderPaths == null || differentPlayableBeatmap) {
            calculateAllSliderPaths(scope)
        }

        lastMods = mods
        lastBeatmapInfo = beatmapInfo

        // Resetting variables before starting the game.
        Multiplayer.finalData = null
        hasFailed = false
        lastBackPressTime = -1f
        isSkipRequested = false
        realTimeElapsed = 0
        statisticDataTimeElapsed = 0
        leadOut = 0f
        lastScoreSent = null
        isGameOver = false

        paused = false
        gameStarted = false
        return true
    }

    val scene: UIScene
        get() = gameUIScene

    fun restartGame() {
        startGame(null, null, null)
    }


    fun startGame(beatmapInfo: BeatmapInfo?, replayFile: String?, mods: ModHashMap?) {
        startGame(beatmapInfo, replayFile, mods, false)
    }

    open fun startGame(beatmapInfo: BeatmapInfo?, replayFile: String?, mods: ModHashMap?, isHUDEditor: Boolean) {
        isReadyToStart = false
        isHUDEditorMode = isHUDEditor
        startedFromHUDEditor = isHUDEditor
        resetPlayfieldSizeScale()

        gameUIScene = createScene()
        failcount = 0
        mainCursorId = -1
        gameplayClock.changeSource(null)
        modIcons.clear()

        val rfile = if (beatmapInfo != null) replayFile else this.replayFilePath
        val requestId = loadingRequestId.incrementAndGet()

        val beatmapToUse = beatmapInfo ?: lastBeatmapInfo
        val isRestart = beatmapInfo == null && replayFile == null && mods == null
        val modsToUse: ModHashMap?

        if (isHUDEditor) {
            modsToUse = ModHashMap()
            modsToUse.put(ModAutoplay::class)
        } else {
            modsToUse = mods?.deepCopy() ?: lastMods
        }

        val scene = GameLoaderScene(this, beatmapToUse!!, modsToUse!!, isRestart)
        Verktex.withTreeLock {
            Verktex.pushScene(scene)
        }

        SkinSounds.get("failsound")!!.stop()

        val pipeline = cancelLoading(false)
            .thenCompose { DifficultyCalculationManager.stopCalculation() }
            .thenRun {
                if (requestId != loadingRequestId.get()) {
                    return@thenRun
                }

                gameLoadingJob = async {
                    var succeeded = false
                    var cancelled = false

                    try {
                        if (requestId != loadingRequestId.get()) {
                            return@async
                        }

                        succeeded = loadGame(beatmapToUse!!, rfile, modsToUse!!, this)

                        if (succeeded && requestId == loadingRequestId.get()) {
                            prepareScene()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            cancelled = true
                            throw e
                        }

                        Logger.e("GameScene", "Failed to load gameplay", e)
                    } finally {
                        if (requestId == loadingRequestId.get()) {
                            if (!succeeded && !cancelled) {
                                updateThread { quit() }
                            }

                            gameLoadingJob = null
                        }
                    }
                }
            }

        loadingPipeline = pipeline

        pipeline.whenComplete { _, _ ->
            if (loadingPipeline === pipeline) {
                loadingPipeline = null
            }
        }
    }

    fun cancelLoading(): CompletableFuture<Unit> {
        return cancelLoading(true)
    }

    private fun cancelLoading(invalidatePendingStart: Boolean): CompletableFuture<Unit> {
        if (invalidatePendingStart) {
            loadingRequestId.incrementAndGet()
        }

        val gameLoadingJob = this.gameLoadingJob
        val videoLoadingJob = this.videoLoadingJob
        val ppCalculationJob = this.ppCalculationJob
        val loadingPipeline = this.loadingPipeline

        this.gameLoadingJob = null
        this.videoLoadingJob = null
        this.ppCalculationJob = null

        val jobCancellation = CompletableFuture.allOf(
            gameLoadingJob.stopAsync(),
            videoLoadingJob.stopAsync(),
            ppCalculationJob.stopAsync()
        )

        @Suppress("UNCHECKED_CAST")
        val pipelineDrain: CompletableFuture<*> = if (loadingPipeline != null)
            (loadingPipeline as CompletableFuture<Any?>).exceptionally { null }
        else
            CompletableFuture.completedFuture(Unit)

        return CompletableFuture.allOf(jobCancellation, pipelineDrain).thenApply { Unit }
    }

    private fun replaceCurrentScene(newScene: UIScene) {
        Verktex.withTreeLock {
            Verktex.popScene()
            Verktex.pushScene(newScene)
        }
    }

    private fun returnToPreviousScene() {
        Verktex.withTreeLock {
            Verktex.popScene()
        }
    }

    private fun prepareScene() {
        val playableBeatmap = this.playableBeatmap ?: return
        val parsedBeatmap = parsedBeatmap!!
        val lastMods = lastMods!!
        val objects = objects!!

        stat = StatisticV2()
        stat.mod = lastMods
        stat.migrateLegacyMods(parsedBeatmap.difficulty)
        stat.calculateModScoreMultiplier(parsedBeatmap.difficulty)
        stat.canFail = ModNoFail::class !in stat.mod
            && ModRelax::class !in stat.mod
            && ModAutopilot::class !in stat.mod
            && ModAutoplay::class !in stat.mod

        var difficultyScoreMultiplier = 1 + min(parsedBeatmap.difficulty.od, 10f) / 10f +
            min(parsedBeatmap.difficulty.hp, 10f) / 10f

        // The maximum CS of osu!droid mapped to osu!standard is ~17.62.
        difficultyScoreMultiplier += (min(parsedBeatmap.difficulty.gameplayCS, 17.62f) - 3) / 4f

        stat.diffModifier = difficultyScoreMultiplier
        stat.beatmapNoteCount = objects.size
        stat.beatmapMaxCombo = parsedBeatmap.maxCombo

        GameHelper.setHardRock(lastMods.ofType(ModHardRock::class))
        GameHelper.setDoubleTime(lastMods.ofType(ModDoubleTime::class))
        GameHelper.setNightCore(if (ModNightCore::class in lastMods) lastMods.ofType(ModNightCore::class) else lastMods.ofType(ModOldNightCore::class))
        GameHelper.setHalfTime(lastMods.ofType(ModHalfTime::class))
        GameHelper.setHidden(lastMods.ofType(ModHidden::class))
        GameHelper.setTraceable(lastMods.ofType(ModTraceable::class))
        GameHelper.setFlashlight(lastMods.ofType(ModFlashlight::class))
        GameHelper.setRelax(lastMods.ofType(ModRelax::class))
        GameHelper.setAutopilot(lastMods.ofType(ModAutopilot::class))
        GameHelper.setAutoplay(lastMods.ofType(ModAutoplay::class))
        GameHelper.setSuddenDeath(lastMods.ofType(ModSuddenDeath::class))
        GameHelper.setPerfect(lastMods.ofType(ModPerfect::class))
        GameHelper.setSynesthesia(lastMods.ofType(ModSynesthesia::class))
        GameHelper.setScoreV2(lastMods.ofType(ModScoreV2::class))
        GameHelper.setEasy(lastMods.ofType(ModEasy::class))
        GameHelper.setMuted(lastMods.ofType(ModMuted::class))
        GameHelper.setFreezeFrame(lastMods.ofType(ModFreezeFrame::class))
        GameHelper.setApproachDifferent(lastMods.ofType(ModApproachDifferent::class))

        val cursorCount = if (replaying && replay != null) replay!!.cursorMoves.size else CursorCount

        cursors = arrayOfNulls(cursorCount)

        for (i in 0 until cursorCount) {
            cursors[i] = Cursor()
        }

        comboWas100 = false
        comboWasMissed = false
        previousFrameTime = 0

        metronome = null
        if ((OsuSettings.metronomeSwitch == 1 && GameHelper.isNightCore())
            || (GameHelper.isMuted() && GameHelper.getMuted()!!.enableMetronome)
            || OsuSettings.metronomeSwitch == 2) {
            metronome = Metronome()
        }

        distToNextObject = 0.0

        // TODO passive objects
        if ((replaying || Settings[OsuSettings.ShowCursor]) && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            val cursorSprites = Array(cursorCount) { CursorEntity() }
            this.cursorSprites = cursorSprites
            for (i in 0 until cursorCount) {
                cursorSprites[i].attachToScene(fgScene)
            }
        } else {
            cursorSprites = null
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor = AutoCursor()
            autoCursor!!.attachToScene(fgScene)
        }

        val countdown = playableBeatmap.general.countdown
        if (Settings[OsuSettings.Corovans] && countdown != BeatmapCountdown.NoCountdown) {
            val cdSpeed = countdown.speed
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH
            if (cdSpeed != 0f && firstObjectStartTime - gameplayClock.time >= cdSpeed * Countdown.COUNTDOWN_LENGTH) {
                countdownAnimator = Countdown(bgScene, cdSpeed, 0f, firstObjectStartTime - gameplayClock.time).also {
                    it.onFinish = { countdownAnimator = null }
                }
            }
        }

        if (Settings[OsuSettings.ComboBurst]) {
            comboBurst = ComboBurst(GameResolution.width.toFloat(), GameResolution.height.toFloat())
            comboBurst!!.attachAll(bgScene)
        }

        val position = Vector2(GameResolution.width - 130, 130)

        for (mod in lastMods.values) {
            if (!mod.isUserPlayable) {
                continue
            }

            val icon = ModIcon(mod)
            icon.setPosition(position.x, position.y)
            icon.origin = Anchor.Center
            icon.setSize(68f, 66f)
            icon.scale = 1.2f

            fgScene += icon
            modIcons.add(icon)

            position.x -= 25f
        }

        val hasUnrankedMod = lastMods.values.any { m -> !m.isRanked }
        if (hasUnrankedMod || Multiplayer.isRemoveSliderLock()) {
            val unrankedSprite = UISprite(SkinTextures.get("play-unranked"))
            this.unrankedSprite = unrankedSprite
            unrankedSprite.anchor = Anchor.TopCenter
            unrankedSprite.origin = Anchor.Center
            unrankedSprite.setPosition(0f, 80f)
            fgScene += unrankedSprite
        }

        if (GameHelper.isFlashlight()) {
            val flashlightSprite = FlashLightEntity(GameHelper.getFlashlight()!!)
            this.flashlightSprite = flashlightSprite
            fgScene.add(0, flashlightSprite)
        }

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        if (hud != null) {
            hud!!.removeSelf()
        }

        val hud = GameplayHUD()
        this.hud = hud

        if (!replaying && !GameHelper.isAutoplay()) {
            // Since block areas are saved in device pixels, we need to map them to scaled pixels.
            val ratio = GameResolution.width.toFloat() / Platform.currentDisplay.width

            for (area in DatabaseManager.blockAreaTable.getAll()) {
                // Attach the block area to the HUD so that it does not get scaled with the playfield.
                val areaBox = object : UIBox() {
                    init {
                        radius = Dimension.Fixed.Pixels(2f)
                        backgroundColor = Color4(30f / 255f, 30f / 255f, 41f / 255f)
                        alpha = 0.15f

                        setPosition(
                            Interpolation.linear(0f, area.x, ratio),
                            Interpolation.linear(0f, area.y, ratio)
                        )

                        setSize(
                            Interpolation.linear(0f, area.width, ratio),
                            Interpolation.linear(0f, area.height, ratio)
                        )
                    }

                    override fun onInputEvent(event: InputEvent): Boolean {
                        if (event is PointerEvent && event.action == PointerEvent.Action.Down
                            && containsScenePoint(event.x, event.y)) {
                            val id = event.pointerId

                            if (id >= 0 && id < getCursorsCount()) {
                                cursors[id]!!.mouseBlocked = true
                            }
                        }

                        return false
                    }
                }

                hud += areaBox
            }
        }

        hud.setEditMode(isHUDEditorMode)
        hud.setSkinData(OsuSkin.get().hudSkinData)

        skipBtn = null
        if (skipTime > 1) {
            val paddingBottom = if (Multiplayer.isConnected) Multiplayer.roomScene!!.chat.buttonHeight else 0f

            val skipBtn = UIAnimatedSprite(SkinTextures.frames("play-skip"), OsuSkin.get().animationFramerate)
            this.skipBtn = skipBtn
            skipBtn.origin = Anchor.BottomRight
            skipBtn.setPosition(GameResolution.width.toFloat(), GameResolution.height - paddingBottom)
            skipBtn.alpha = 0.7f
            hud += skipBtn
        }

        var playname = OsuSettings.onlineUsername

        if (GameHelper.isAutoplay() || replaying) {
            val metadata = playableBeatmap.metadata
            playname = if (replaying) GlobalManager.getInstance().scoring.replayStat!!.playerName else "osu!"

            if (!Settings[OsuSettings.HideReplayMarquee]) {
                val replayText = UIText()
                this.replayText = replayText
                replayText.text = "Watching " + playname + " play " + metadata.artist + " - " + metadata.title + " [" + metadata.version + "]"
                replayText.setPosition(GameResolution.width + 5f, 140f)
                replayText.alpha = 0.7f
                hud.add(0, replayText)
            }
        } else if (Multiplayer.room != null && Multiplayer.room!!.isTeamVersus) {
            //noinspection DataFlowIssue
            playname = Multiplayer.player!!.team.toString()
        }
        stat.playerName = playname

        val breakAnimator = BreakAnimator(fgScene, stat, hud)
        breakAnimator.onEnding = {
            if (Multiplayer.isMultiplayer) {
                Multiplayer.roomScene?.chat?.hide()
            }
        }
        this.breakAnimator = breakAnimator

        if (!startedFromHUDEditor && (GameHelper.isAutoplay() || replaying)) {
            val replaySettingsPanel = ReplaySettingsPanel()
            this.replaySettingsPanel = replaySettingsPanel
            hud += replaySettingsPanel

            val seekControl = replaySettingsPanel.playbackControl.seekControl

            seekControl.onPauseToggle = { isPaused ->
                if (isPaused) {
                    gameplayClock.pause()
                    stopLoopingSamples()

                    if (video != null && videoStarted) {
                        video!!.pause()
                    }
                } else {
                    if (!gameplayClock.isRunning) {
                        gameplayClock.start()
                    }

                    playLoopingSamples()

                    if (video != null && videoStarted) {
                        video!!.play()
                    }
                }
            }

            replaySettingsPanel.visualSettingsControl.onBackgroundBrightnessChanged = { brightness ->
                breakAnimator.setDimBrightness(brightness)

                // We do not want
                if (!breakAnimator.isBreak() && dimRectangle != null) {
                    dimRectangle!!.alpha = 1f - brightness
                }
            }

            seekControl.updateSeekPosition(
                gameplayClock.time,
                (objects[0]!!.startTime / 1000).toFloat(),
                (objects[objects.size - 1]!!.endTime / 1000).toFloat()
            )

            seekControl.onSeek = { targetTime ->
                updateThread { seekTo(targetTime) }
            }
        }

        if (Multiplayer.isMultiplayer) {
            RoomAPI.notifyBeatmapLoaded()
        } else {
            isReadyToStart = true
        }
    }

    /**
     * Starts gameplay. This is used by the game loader once all necessary preprocessing is done.
     */
    fun start() {
        val playableBeatmap = this.playableBeatmap ?: return
        val hud = hud!!

        val music = GlobalManager.getInstance().music

        // Hook up the framed beatmap clock as the scene's clock. Use MusicService as the audio source.
        if (music != null) {
            gameplayClock.changeSource(MusicClock(music))
        }

        // Ensure user-defined offset has time to be applied.
        val props = DatabaseManager.beatmapOptionsTable.getOptions(lastBeatmapInfo!!.setDirectory)
        gameplayClock.userGlobalOffset = OsuSettings.offset / 1000
        gameplayClock.userBeatmapOffset = if (props != null) props.offset / 1000f else 0f

        val firstObjectTimePreempt = playableBeatmap.hitObjects.objects[0].timePreempt.toFloat() / 1000
        gameplayClock.seek(min(gameplayClock.time, firstObjectStartTime - firstObjectTimePreempt))
        initialStartTimeValue = gameplayClock.time

        music?.volume = OsuSettings.musicVolume

        gameplayClock.start()

        if (skipTime <= 1 && Multiplayer.isConnected) {
            Multiplayer.roomScene!!.chat.hide()
        }

        applyPlayfieldSizeScale()
        applyBackground()

        replaySettingsPanel?.visualSettingsControl?.defaultBackgroundBrightness = OsuSettings.backgroundBrightness

        if (!isHUDEditorMode && !Settings[OsuSettings.ShowScoreboard]) {
            for (i in hud.childCount - 1 downTo 0) {
                if (hud[i] is HUDLeaderboard) {
                    hud[i].removeSelf()
                }
            }
        }

        if (!isHUDEditorMode && !replaying && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            Input.dispatchHistoricalMoves = true
        }

        root += hud
        replaceCurrentScene(gameUIScene)

        if (isHUDEditorMode) {
            Toasts.show(Res.string.hudEditor_back_for_menu)
        }
    }

    fun getComboColor(hitObject: HitObject): Color4 {
        val playableBeatmap = this.playableBeatmap

        if (playableBeatmap != null && GameHelper.isSynesthesia()) {
            return ModSynesthesia.getColorFor(playableBeatmap.controlPoints.getClosestBeatDivisor(hitObject.startTime))
        }

        return comboColors!![hitObject.comboIndexWithOffsets % comboColors!!.size]
    }

    private fun update(dt: Float) {
        if (!isReadyToStart) {
            return
        }

        previousFrameTime = Platform.uptimeMillis

        val playableBeatmap = this.playableBeatmap ?: return
        val hud = hud!!

        if (Multiplayer.isMultiplayer) {
            val mSecElapsed = (dt / GameHelper.getSpeedMultiplier() * 1000).toLong()
            realTimeElapsed += mSecElapsed
            statisticDataTimeElapsed += mSecElapsed

            // Sending statistics data every 3000ms if data was changed
            if (statisticDataTimeElapsed > 3000) {
                statisticDataTimeElapsed %= 3000

                if (Multiplayer.isConnected) {
                    val liveScore = stat.toBoardItem()

                    if (liveScore != lastScoreSent) {
                        lastScoreSent = liveScore
                        async { runSafe { RoomAPI.submitLiveScore(lastScoreSent!!.toJson()) } }
                    }
                }
            }
        }

        val mSecPassed = gameplayClock.time * 1000

        if (!isGameOver) {
            val modRate = rateAdjustingMods.calculateRate(mSecPassed.toDouble())

            val replaySettingsRate = replaySettingsPanel?.playbackControl?.rateControl?.rate ?: 1f

            val currentSpeedMultiplier = modRate * replaySettingsRate

            if (currentSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(currentSpeedMultiplier)
                gameplayClock.rate = currentSpeedMultiplier

                if (videoEnabled && video != null) {
                    video!!.setPlaybackSpeed(currentSpeedMultiplier)
                }

                val music = GlobalManager.getInstance().music
                music.speed = modRate
                music.setPitchRate(replaySettingsRate)
            }
        }

        val objects = objects!!

        replaySettingsPanel?.playbackControl?.seekControl?.updateSeekPosition(
            gameplayClock.time,
            (objects[0]!!.startTime / 1000).toFloat(),
            (objects[objects.size - 1]!!.endTime / 1000).toFloat()
        )

        if (replaying) {
            val replay = replay!!
            val replayTimeMs = gameplayClock.time * 1000
            val replayCatchUpTimeMs = (gameplayClock.time + dt / 4) * 1000
            var cIndex: Int

            for (i in 0 until replay.cursorIndex.size) {
                if (replay.cursorMoves.size <= i) {
                    break
                }

                val moveArray = replay.cursorMoves[i]

                cIndex = replay.cursorIndex[i]
                var movement: Replay.ReplayMovement? = null

                // Emulating moves
                while (cIndex < moveArray.size) {
                    // As in the condition it was: the movement is kept even when it's not reached yet.
                    movement = moveArray.movements[cIndex]

                    if (movement.time > replayCatchUpTimeMs) {
                        break
                    }

                    val event = CursorEvent.obtain()

                    event.systemTime = movement.time.toLong()
                    event.trackTime = movement.time.toFloat()
                    event.offset = 0.0
                    event.position.set(movement.x, movement.y)

                    if (movement.touchType == TouchType.DOWN) {
                        event.action = PointerEvent.Action.Down
                        replay.lastMoveIndex[i] = -1
                        hud.onGameplayTouchDown(movement.time / 1000f)
                    } else if (movement.touchType == TouchType.MOVE) {
                        event.action = PointerEvent.Action.Move
                        replay.lastMoveIndex[i] = cIndex
                    } else {
                        event.action = PointerEvent.Action.Up
                    }
                    cursors[i]!!.addEvent(event)
                    replay.cursorIndex[i]++
                    cIndex++
                }

                // Interpolating cursor movements
                if (movement != null && movement.touchType == TouchType.MOVE && replay.lastMoveIndex[i] >= 0) {
                    val lIndex = replay.lastMoveIndex[i]
                    val lastMovement = moveArray.movements[lIndex]
                    val movementTime = movement.time
                    val lastMovementTime = lastMovement.time
                    val duration = lastMovementTime - movementTime

                    if (duration == 0) {
                        continue
                    }

                    val t = (replayTimeMs - movementTime) / duration

                    val event = CursorEvent.obtain()

                    // We don't exactly need systemTime to be accurate here since it's not used for anything important
                    // in replays.
                    event.systemTime = mSecPassed.toLong()
                    event.trackTime = mSecPassed.toLong().toFloat()
                    event.offset = 0.0
                    event.action = PointerEvent.Action.Move
                    event.position.set(
                        lastMovement.x * t + movement.x * (1 - t),
                        lastMovement.y * t + movement.y * (1 - t)
                    )

                    cursors[i]!!.addEvent(event)
                }
            }
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor!!.update(dt)
        } else if (cursorSprites != null) {
            val cursorSprites = cursorSprites!!
            for (i in cursorSprites.indices) {
                val sprite = cursorSprites[i]
                sprite.update(dt)

                val cursor = cursors[i]!!
                val latestEvent = cursor.latestEvent

                if (replaying && latestEvent != null) {
                    sprite.setPosition(latestEvent.position.x, latestEvent.position.y)
                    sprite.setShowing(!latestEvent.isActionUp)
                }

                if (cursor.getLatestDownEvent() != null) {
                    sprite.click()
                }
            }
        }

        if (GameHelper.isFlashlight()) {
            val flashlightSprite = flashlightSprite!!

            if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
                // Check if the main cursor is still valid.
                if (mainCursorId >= 0 && !cursors[mainCursorId]!!.mouseDown) {
                    mainCursorId = -1
                }

                // If no cursor is valid, check for the latest pressed cursor.
                if (mainCursorId < 0) {
                    var index = -1
                    var latestDownEvent: CursorEvent? = null

                    for (i in cursors.indices) {
                        val c = cursors[i]!!
                        val latestCursorDownEvent = c.getLatestDownEvent() ?: continue

                        if (latestDownEvent == null || latestDownEvent.systemTime < latestCursorDownEvent.systemTime) {
                            latestDownEvent = latestCursorDownEvent
                            index = i
                        }
                    }

                    if (latestDownEvent != null) {
                        mainCursorId = index
                    }
                }

                if (mainCursorId != -1) {
                    val cursor = cursors[mainCursorId]!!
                    val latestNonUpEvent = cursor.getLatestNonUpEvent()

                    if (latestNonUpEvent != null) {
                        flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y)
                    }
                }
            }

            flashlightSprite.updateCombo(stat.combo)
        }

        val timingControlPoints = timingControlPoints!!
        while (timingControlPointIndex + 1 < timingControlPoints.size) {
            val nextTimingPoint = timingControlPoints[timingControlPointIndex + 1]

            if (nextTimingPoint.time > mSecPassed) {
                break
            }

            activeTimingPoint = nextTimingPoint
            ++timingControlPointIndex
        }

        val effectControlPoints = effectControlPoints!!
        while (effectControlPointIndex + 1 < effectControlPoints.size) {
            val nextEffectPoint = effectControlPoints[effectControlPointIndex + 1]

            if (nextEffectPoint.time > mSecPassed) {
                break
            }

            activeEffectPoint = nextEffectPoint
            ++effectControlPointIndex
        }

        val activeTimingPoint = activeTimingPoint!!

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000)
        GameHelper.setKiai(activeEffectPoint!!.isKiai)
        GameHelper.setCurrentBeatTime(max(0.0, gameplayClock.time - activeTimingPoint.time / 1000) % GameHelper.getBeatLength())

        if (!isGameOver) {
            val breakPeriods = breakPeriods!!
            val breakAnimator = breakAnimator!!

            if (breakPeriodIndex < breakPeriods.size) {
                if (!breakAnimator.isBreak() && breakPeriods[breakPeriodIndex].startTime / 1000 <= gameplayClock.time) {
                    val period = breakPeriods[breakPeriodIndex++]

                    gameStarted = false
                    breakAnimator.init(period.duration / 1000)
                    if (GameHelper.isFlashlight()) {
                        flashlightSprite!!.onBreak(true)
                    }

                    if (Multiplayer.isConnected)
                        Multiplayer.roomScene!!.chat.show()

                    hud.onBreakStateChange(true)
                }
            }

            if (breakAnimator.isOver()) {

                // Ensure the chat is dismissed if it's still shown
                if (Multiplayer.isConnected) {
                    Multiplayer.roomScene!!.chat.hide()
                }

                gameStarted = true
                hud.onBreakStateChange(false)

                if (GameHelper.isFlashlight()) {
                    flashlightSprite!!.onBreak(false)
                }
            }
        }

        val activeObjects = activeObjects!!
        val expiredObjects = expiredObjects!!

        if (objectIndex >= objects.size && activeObjects.isEmpty() && GameHelper.isFlashlight()) {
            flashlightSprite!!.onBreak(true)
        }

        if (gameStarted) {
            var rate = 0.375
            if (playableBeatmap.difficulty.hp > 0 && distToNextObject > 0) {
                rate = 1 + playableBeatmap.difficulty.hp / (2 * distToNextObject)
            }
            stat.changeHp((-rate).toFloat() * 0.01f * dt)

            if (stat.hp <= 0 && stat.canFail) {
                if (GameHelper.isEasy() && failcount < 3) {
                    failcount++
                    stat.changeHp(1f)
                } else {
                    if (Multiplayer.isMultiplayer) {
                        if (!hasFailed) {
                            Toasts.show("You failed but you can continue playing.")
                        }
                        hasFailed = true
                    } else {
                        gameover()
                        return
                    }
                }
            }
        }

        if (comboBurst != null) {
            if (stat.combo == 0) {
                comboBurst!!.breakCombo()
            } else {
                comboBurst!!.checkAndShow(stat.combo)
            }
        }

        // Clearing expired objects.
        processedExpiredObjects.clear()

        if (!expiredObjects.isEmpty()) {
            for (i in 0 until expiredObjects.size) {
                val obj = expiredObjects[i]

                if (processedExpiredObjects.add(obj)) {
                    obj.onExpire()
                }
            }

            activeObjects.removeAll(expiredObjects)
            expiredObjects.clear()
        }

        updatePassiveObjects(dt)
        updateActiveObjects(dt)

        if (judgeableObject != null && (GameHelper.isAutoplay() || GameHelper.isAutopilot())) {
            autoCursor!!.moveToObject(judgeableObject, gameplayClock.time, this)
        }

        if (videoEnabled && video != null && gameplayClock.time >= videoOffset) {
            val video = video!!

            if (!videoStarted) {
                video.play()
                video.setPlaybackSpeed(GameHelper.getSpeedMultiplier())
                videoStarted = true
            }

            if (video.alpha < 1.0f)
                video.alpha = min(video.alpha + 0.03f, 1.0f)
        }

        var shouldBePunished = false

        while (objectIndex < objects.size) {
            val obj = objects[objectIndex]!!

            // The casts can be simplified, but it is necessary to prevent floating point errors (see how
            // GameplayHitCircle and GameplaySlider track their passed time, where startTime and timePreempt
            // are cast and converted to seconds individually).
            val lifetimeStart = obj.startTime.toFloat() / 1000 - obj.timePreempt.toFloat() / 1000
            val lifetimeDt = gameplayClock.time - lifetimeStart

            if (lifetimeDt < 0) {
                break
            }

            gameStarted = true
            ++objectIndex

            if (unrankedSprite != null) {
                unrankedSprite!!.choreographAt(1.5f) { sequence -> sequence
                    .scaleTo(1.5f, 0.5f)
                    .fadeOut(0.5f) }

                // Make it null to avoid multiple entity modifier registration
                unrankedSprite = null
            }

            if (obj.startTime > totalLength) {
                shouldBePunished = true
                break
            }

            val nextObj = if (objectIndex < objects.size) objects[objectIndex] else null

            distToNextObject = if (nextObj != null)
                max(nextObj.startTime - obj.startTime, activeTimingPoint.msPerBeat / 2) / 1000
            else
                0.0

            hud.onHitObjectLifetimeStart(obj)

            val comboColor = getComboColor(obj)

            var gameObject: GameObject? = null

            if (obj is HitCircle) {
                val gameplayCircle = GameObjectPool.getInstance().circle

                gameplayCircle.init(this, mgScene, obj, comboColor)
                addObject(gameplayCircle)

                if (GameHelper.isAutoplay()) {
                    gameplayCircle.setAutoPlay()
                }

                gameplayCircle.setId(++lastObjectId)

                if (replaying) {
                    gameplayCircle.setReplayData(replay!!.objectData!![gameplayCircle.getId()])
                }

                gameObject = gameplayCircle
            } else if (obj is Spinner) {
                val rps = 2 + 2 * playableBeatmap.difficulty.od / 10f
                val gameplaySpinner = GameObjectPool.getInstance().spinner

                gameplaySpinner.init(this, bgScene, obj, rps, stat)
                addObject(gameplaySpinner)

                if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                    gameplaySpinner.setAutoPlay()
                }

                gameplaySpinner.setId(++lastObjectId)
                if (replaying) {
                    gameplaySpinner.setReplayData(replay!!.objectData!![gameplaySpinner.getId()])
                }

                gameObject = gameplaySpinner
            } else if (obj is Slider) {
                val gameplaySlider = GameObjectPool.getInstance().slider

                gameplaySlider.init(this, mgScene, stat, obj, playableBeatmap.controlPoints,
                    comboColor, sliderBorderColor!!, getSliderPath(sliderIndex)!!, getSliderRenderPath(sliderIndex))

                ++sliderIndex
                addObject(gameplaySlider)

                if (GameHelper.isAutoplay()) {
                    gameplaySlider.setAutoPlay()
                }

                gameplaySlider.setId(++lastObjectId)
                if (replaying) {
                    gameplaySlider.setReplayData(replay!!.objectData!![gameplaySlider.getId()])
                    if (gameplaySlider.getReplayData()!!.tickSet == null)
                        gameplaySlider.getReplayData()!!.tickSet = BitSet()
                }

                gameObject = gameplaySlider
            }

            gameObject?.update(lifetimeDt)

            if (obj !is Spinner && nextObj != null && nextObj !is Spinner && !obj.isLastInCombo) {
                FollowPointConnection.addConnection(bgScene, obj, nextObj)
            }
        }

        if (GameHelper.isPerfect() && stat.hit100 + stat.hit50 + stat.misses > 0) {
            gameover()

            if (!Multiplayer.isMultiplayer) {
                restartGame()
            }
        }

        val mutedMod = GameHelper.getMuted()

        // 节拍器
        if (metronome != null) {
            metronome!!.update(gameplayClock.time, activeTimingPoint)

            if (mutedMod != null) {
                metronome!!.setVolume(1 - mutedMod.volumeAt(stat.combo))
            }
        }

        if (gameplayClock.time >= 0 && mutedMod != null) {
            GlobalManager.getInstance().music.volume =
                OsuSettings.musicVolume * mutedMod.volumeAt(stat.combo)
        }

        if (shouldBePunished || (!isGameOver && objectIndex >= objects.size && activeObjects.isEmpty() && leadOut > 2)) {

            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                gameplayClock.seek(initialStartTimeValue)
                loadGame(lastBeatmapInfo!!, null, lastMods!!, null)
                applyBackground()
                stat.reset()
                skip(true)
                return
            }

            gameUIScene = createScene()
            BeatmapSkinManager.setSkinEnabled(false)
            GameObjectPool.getInstance().purge()
            this.timingControlPoints = null
            this.effectControlPoints = null
            this.objects = null
            activeObjects.clear()
            expiredObjects.clear()
            replaySettingsPanel = null
            this.breakPeriods = null
            cursorSprites = null
            this.playableBeatmap = null
            performanceAttributes = null
            performanceCalculationParameters = null
            droidTimedDifficultyAttributes = null
            standardTimedDifficultyAttributes = null
            sliderPaths = null
            sliderRenderPaths = null
            var replayPath: String? = null
            stat.time = System.currentTimeMillis()
            if (replay != null && !replaying) {
                val ctime = System.currentTimeMillis().toString()
                replayPath = GameFolders.corePath + "Scores/" +
                    MD5Calculator.getStringMD5(lastBeatmapInfo!!.filename + ctime) +
                    ctime.substring(0, min(3, ctime.length)) + ".odr"
                replay!!.setStat(stat)
                replay!!.save(replayPath)
            }
            resetPlayfieldSizeScale()
            cancelVideoLoading()

            val scoringScene = scoringScene
            if (scoringScene != null && !startedFromHUDEditor) {
                if (replaying)
                    scoringScene.load(scoringScene.replayStat!!, null, GlobalManager.getInstance().music, replayPath, null, lastBeatmapInfo)
                else {
                    if (ModAutoplay::class in stat.mod) {
                        stat.playerName = "osu!"
                    }

                    if (Multiplayer.isConnected) {
                        Multiplayer.log("Match ended, moving to results scene.")
                        Multiplayer.roomScene!!.chat.show()

                        async { runSafe { RoomAPI.submitFinalScore(stat.toJson()) } }

                        Toasts.show("Loading room statistics...")
                    }
                    scoringScene.load(stat, lastBeatmapInfo, GlobalManager.getInstance().music, replayPath, parsedBeatmap!!.md5, null)
                }
                GlobalManager.getInstance().music.volume = 0.2f
                replaceCurrentScene(scoringScene.scene!!)
            } else {
                returnToPreviousScene()
            }

            // Resume difficulty calculation.
            DifficultyCalculationManager.calculateDifficulties()
            Input.dispatchHistoricalMoves = false

            if (video != null) {
                video!!.release()
                video = null
                videoStarted = false
            }

            parsedBeatmap = null
        } else if (objectIndex >= objects.size && activeObjects.isEmpty()) {
            gameStarted = false
            leadOut += dt
        }

        if (gameplayClock.time > skipTime - 1f && skipBtn != null) {
            if (Multiplayer.isConnected) {
                Multiplayer.roomScene!!.chat.hide()
            }
            skipBtn!!.removeSelf()
            skipBtn = null
        } else if (skipBtn != null) {
            for (i in cursors.indices) {
                val latestDownEvent = cursors[i]!!.getLatestNonUpEvent()

                if (latestDownEvent != null && Utils.squaredDistance(latestDownEvent.position.x, latestDownEvent.position.y,
                        GameResolution.width.toFloat(), GameResolution.height.toFloat()) < 250 * 250) {
                    if (Multiplayer.isConnected) {
                        if (!isSkipRequested) {
                            isSkipRequested = true
                            SkinSounds.get("menuhit")!!.play()
                            skipBtn!!.isVisible = false

                            async { RoomAPI.requestSkip() }
                            Toasts.show("Skip requested")
                        }
                        return
                    }
                    if (skipBtn != null) {
                        skipBtn!!.removeSelf()
                        skipBtn = null
                    }
                    skip()
                    return
                }
            }
        }

        if (postSeekFrameCount > 0) {
            postSeekFrameCount--
        }
    }

    private fun updateActiveObjects(deltaTime: Float) {
        val activeObjects = activeObjects!!
        judgeableObject = searchJudgeableObject(0)

        for (i in 0 until activeObjects.size) {
            val obj = activeObjects[i]
            obj.update(deltaTime)

            // Advance to the next judgeable object if the current judgeable object is judged.
            // In remove slider lock mode, do this as soon as the current judgeable object is hit instead.
            if (isObjectHittable(obj) && (obj.isJudged() || (Multiplayer.isRemoveSliderLock() && obj.isStartHit()))) {
                judgeableObject = searchJudgeableObject(i + 1)
            }

            if (gameplayClock.time >= obj.lifetimeEnd) {
                expiredObjects!!.add(obj)
            }
        }
    }

    private fun updatePassiveObjects(deltaTime: Float) {

        hud!!.onGameplayUpdate(this, deltaTime)

        breakAnimator!!.update(deltaTime)

        countdownAnimator?.update(deltaTime)
    }

    private fun searchJudgeableObject(startIndex: Int): GameObject? {
        val activeObjects = activeObjects!!

        for (i in startIndex until activeObjects.size) {
            val obj = activeObjects[i]

            if (!obj.isJudged()) {
                return obj
            }
        }

        return null
    }

    fun skip() {
        skip(false)
    }

    fun skip(force: Boolean) {
        if (Multiplayer.isConnected) {
            Multiplayer.roomScene!!.chat.hide()
        }

        if (gameplayClock.time > skipTime - 1f && !force) {
            return
        }

        SkinSounds.get("menuhit")!!.play()

        val difference = skipTime - gameplayClock.time
        gameplayClock.seek(skipTime)

        updateThread {
            updatePassiveObjects(difference)

            if (videoEnabled && video != null) {
                video!!.seekTo(max(0, ((gameplayClock.time - videoOffset) * 1000).toInt()))
            }

            if (skipBtn != null) {
                skipBtn!!.removeSelf()
                skipBtn = null
            }
        }
    }

    private fun onExit() {
        BeatmapSkinManager.setSkinEnabled(false)
        GameObjectPool.getInstance().purge()
        stopLoopingSamples()
        activeObjects?.clear()
        expiredObjects?.clear()
        hud?.removeSelf()
        breakPeriods = null
        replaySettingsPanel = null
        objects = null
        timingControlPoints = null
        effectControlPoints = null
        parsedBeatmap = null
        playableBeatmap = null
        cursorSprites = null
        lastMods = null
        performanceAttributes = null
        performanceCalculationParameters = null
        droidTimedDifficultyAttributes = null
        standardTimedDifficultyAttributes = null
        sliderPaths = null
        sliderRenderPaths = null

        BeatmapSkinManager.getInstance().clearSkin()

        cancelVideoLoading()

        val mSecPassed = gameplayClock.time * 1000
        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap
        val music = GlobalManager.getInstance().music

        if (music != null && selectedBeatmap != null && !Multiplayer.isMultiplayer) {
            // osu!stable restarts the song back to preview time when the player is in the last 10 seconds *or* 2% of the beatmap.
            val continuePreview = mSecPassed < totalLength - 10000 && mSecPassed / totalLength < 0.98f
            val previewTime = if (continuePreview) music.position else selectedBeatmap.previewTime

            if (Settings[OsuSettings.PlayMusicPreview]) {
                async {
                    music.stop()
                    music.preLoad(selectedBeatmap.audioPath)
                    music.play()
                    music.seekTo(previewTime)
                }
            }
        }

        if (replaying) {
            replayFilePath = null
        }
    }

    fun quit() {
        // Disable historical event processing for more efficient ACTION_MOVE reports, since frequent reports are
        Input.dispatchHistoricalMoves = false


        if (sceneBorder != null) {
            sceneBorder!!.removeSelf()
            sceneBorder = null
        }

        onExit()
        resetPlayfieldSizeScale()
        gameUIScene = GameUIScene()

        if (Multiplayer.isMultiplayer) {
            releaseVideo()

            GlobalManager.getInstance().music?.stop()

            val roomScene = Multiplayer.roomScene

            if (Multiplayer.isConnected && roomScene != null) {
                returnToPreviousScene()

                if (Verktex.scene !== roomScene) {
                    roomScene.show()
                }
            } else {
                replaceCurrentScene(LobbyScene())
            }

            return
        }
        SkinSounds.get("failsound")!!.stop()
        returnToPreviousScene()
        releaseVideo()

        // Resume difficulty calculation.
        DifficultyCalculationManager.calculateDifficulties()
    }


    fun reset() {
    }

    //CB打击处理
    private fun registerHit(objectId: Int, score: Int, endCombo: Boolean): String {
        return registerHit(objectId, score, endCombo, true)
    }

    private fun registerHit(objectId: Int, score: Int, endCombo: Boolean, incrementCombo: Boolean): String {

        if (isGameOver) {
            return "hit0"
        }

        val writeReplay = objectId != -1 && replay != null && !replaying
        if (score == 0) {
            if (stat.combo > 30) {
                val sound = SkinSounds.getBeatmap("combobreak", 1)
                sound?.play()
            }
            comboWasMissed = true
            stat.registerHit(0, false, false, incrementCombo)
            if (writeReplay) replay!!.addObjectScore(objectId, ResultType.MISS)
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f)
                gameover()
            }
            if (objectId != -1) {
                updatePPValue(objectId)
            }
            return "hit0"
        }

        var scoreName = "hit300"
        if (score == 50) {
            stat.registerHit(50, false, false, incrementCombo)
            if (writeReplay) replay!!.addObjectScore(objectId, ResultType.HIT50)
            scoreName = "hit50"
            comboWas100 = true
        } else if (score == 100) {
            comboWas100 = true
            if (writeReplay) replay!!.addObjectScore(objectId, ResultType.HIT100)
            if (endCombo && !comboWasMissed) {
                stat.registerHit(100, true, false, incrementCombo)
                scoreName = "hit100k"
            } else {
                stat.registerHit(100, false, false, incrementCombo)
                scoreName = "hit100"
            }
        } else if (score == 300) {
            if (writeReplay) replay!!.addObjectScore(objectId, ResultType.HIT300)
            if (endCombo && !comboWasMissed) {
                if (!comboWas100) {
                    stat.registerHit(300, true, true, incrementCombo)
                    scoreName = "hit300g"
                } else {
                    stat.registerHit(300, true, false, incrementCombo)
                    scoreName = "hit300k"
                }
            } else {
                stat.registerHit(300, false, false, incrementCombo)
                scoreName = "hit300"
            }
        }

        if (endCombo) {
            comboWas100 = false
            comboWasMissed = false
        }

        if (objectId != -1) {
            updatePPValue(objectId)
        }

        return scoreName
    }


    override fun onCircleHit(id: Int, accuracy: Float, pos: Vector2, endCombo: Boolean, forcedScore: Byte, color: Color4) {
        val acc = accuracy
        val playableBeatmap = this.playableBeatmap ?: return
        val hud = hud!!

        if (GameHelper.isAutoplay()) {
            autoCursor!!.click()
            hud.onGameplayTouchDown(parsedBeatmap!!.hitObjects.objects[id].startTime.toFloat() / 1000)
        }

        val accuracy = abs(acc)
        val writeReplay = replay != null && !replaying
        if (writeReplay) {
            val sacc = (acc * 1000).toInt().toShort()
            replay!!.addObjectResult(id, sacc, null)
        }

        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        val isCursorTrackingActive = !GameHelper.isAutoplay() && !GameHelper.isAutopilot()
        val nearestCursorId = if (isCursorTrackingActive) getNearestCursorId(pos.x, pos.y) else -1

        if (GameHelper.isFlashlight() && isCursorTrackingActive) {
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId
                val latestNonUpEvent = cursors[mainCursorId]!!.getLatestNonUpEvent()

                if (latestNonUpEvent != null) {
                    flashlightSprite!!.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y)
                }
            }
        }
        GameplayVibrations.circle()

        if (accuracy > playableBeatmap.hitWindow.mehWindow / 1000 || forcedScore == ResultType.MISS.id) {
            effects.hit(pos, "hit0", color)
            registerHit(id, 0, endCombo)
            return
        }

        val scoreName: String
        if (forcedScore == ResultType.HIT300.id ||
            forcedScore.toInt() == 0 && accuracy <= playableBeatmap.hitWindow.greatWindow / 1000) {
            scoreName = registerHit(id, 300, endCombo)
        } else if (forcedScore == ResultType.HIT100.id ||
            forcedScore.toInt() == 0 && accuracy <= playableBeatmap.hitWindow.okWindow / 1000) {
            scoreName = registerHit(id, 100, endCombo)
        } else {
            scoreName = registerHit(id, 50, endCombo)
        }

        effects.burst(pos, color)
        effects.hit(pos, scoreName, color)

        if (nearestCursorId >= 0) {
            val aimErrorCursorEvent = cursors[nearestCursorId]!!.getLatestNonUpEvent()

            if (aimErrorCursorEvent != null) {
                val objectRadius = parsedBeatmap!!.hitObjects.objects[id].screenSpaceGameplayRadius.toFloat()
                hud.onAimJudgement(pos, aimErrorCursorEvent.position, objectRadius)
            }
        }

        hud.onNoteHit(stat)
    }

    override fun onSliderReverse(pos: Vector2, ang: Float, color: Color4) {
        effects.burstSliderReverse(pos, ang, color)
    }

    override fun onSliderHit(id: Int, score: Int, judgementPos: Vector2, endCombo: Boolean,
                             color: Color4, type: Int, incrementCombo: Boolean) {
        val hud = hud!!

        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        val isCursorTrackingActive = !GameHelper.isAutoplay() && !GameHelper.isAutopilot()
        val isFlashlightActive = GameHelper.isFlashlight() && isCursorTrackingActive
        val isSliderHeadHit = type == GameObjectListener.SLIDER_START && score > 0 && isCursorTrackingActive

        var nearestCursorId = -1

        if (isFlashlightActive || isSliderHeadHit) {
            nearestCursorId = getNearestCursorId(judgementPos.x, judgementPos.y)
        }

        if (isFlashlightActive && nearestCursorId >= 0) {
            mainCursorId = nearestCursorId
            val latestNonUpEvent = cursors[mainCursorId]!!.getLatestNonUpEvent()

            if (latestNonUpEvent != null) {
                flashlightSprite!!.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y)
            }
        }

        GameplayVibrations.slider()

        // Whole slider was missed.
        if (score == 0) {
            effects.hit(judgementPos, "hit0", color)
            registerHit(id, 0, endCombo)
            return
        }

        // Nested object was missed.
        if (score == -1) {
            if (stat.combo > 30) {
                val sound = SkinSounds.getBeatmap("combobreak", 1)
                sound?.play()
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f)
                gameover()
            }
            stat.registerHit(0, true, false)
            return
        }

        var scoreName = "hit0"

        when (type) {
            GameObjectListener.SLIDER_START -> {
                if (incrementCombo) {
                    scoreName = "sliderpoint30"
                    stat.registerHit(30, false, false)
                    stat.addSliderHeadHit()
                    effects.burstSliderStart(judgementPos, color)

                    if (nearestCursorId >= 0) {
                        val aimErrorCursorEvent = cursors[nearestCursorId]!!.getLatestNonUpEvent()

                        if (aimErrorCursorEvent != null) {
                            val objectRadius = parsedBeatmap!!.hitObjects.objects[id].screenSpaceGameplayRadius.toFloat()
                            hud.onAimJudgement(judgementPos, aimErrorCursorEvent.position, objectRadius)
                        }
                    }

                    if (GameHelper.isAutoplay()) {
                        hud.onGameplayTouchDown(parsedBeatmap!!.hitObjects.objects[id].startTime.toFloat() / 1000)
                    }
                }
            }

            GameObjectListener.SLIDER_REPEAT -> {
                if (incrementCombo) {
                    scoreName = "sliderpoint30"
                    stat.registerHit(30, false, false)
                    stat.addSliderRepeatHit()
                }
            }

            GameObjectListener.SLIDER_TICK -> {
                if (incrementCombo) {
                    scoreName = "sliderpoint10"
                    stat.registerHit(10, false, false)
                    stat.addSliderTickHit()
                }
            }

            GameObjectListener.SLIDER_END -> {
                // Register the slider end hit to statistics first before calling registerHit so that updatePPValue
                // receives the correct slider end hit count.
                if (incrementCombo) {
                    stat.addSliderEndHit()
                }

                scoreName = registerHit(id, score, endCombo, incrementCombo)

                if (incrementCombo) {
                    effects.burstSliderEnd(judgementPos, color)
                }
            }
        }

        effects.hit(judgementPos, scoreName, color)

        hud.onNoteHit(stat)
    }

    override fun onSpinnerStart(id: Int) {
        if (GameHelper.isAutoplay()) {
            autoCursor!!.click()
            hud!!.onGameplayTouchDown(parsedBeatmap!!.hitObjects.objects[id].startTime.toFloat() / 1000)
        }
    }

    override fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int) {
        if (score == 1000) {
            stat.registerHit(score, false, false)
            return
        }

        if (replay != null && !replaying) {
            var acc = (totalScore * 4).toShort()
            when (score) {
                300 -> acc = (acc + 3).toShort()
                100 -> acc = (acc + 2).toShort()
                50 -> acc = (acc + 1).toShort()
            }
            replay!!.addObjectResult(id, acc, null)
        }

        val pos = Vector2(GameResolution.width.toFloat() / 2,
            GameResolution.height.toFloat() / 2)

        GameplayVibrations.spinner()

        if (score == 0) {
            val effect = GameObjectPool.getInstance().getEffect(
                "hit0")
            effect.init(
                gameUIScene,
                pos,
                scale,
                { sequence -> sequence.fadeIn(0.15f)
                    .then(0.35f)
                    .fadeOut(0.25f) }
            )
            registerHit(id, 0, endCombo)
            return
        }

        val scoreName: String
        val hitWindow = hitWindow

        // Simulate a hit for hit error meter registration.
        val accuracy = when (score) {
            300 -> {
                scoreName = registerHit(id, 300, endCombo)
                0.0
            }

            100 -> {
                scoreName = registerHit(id, 100, endCombo)
                hitWindow!!.greatWindow + 1
            }

            50 -> {
                scoreName = registerHit(id, 50, endCombo)
                hitWindow!!.okWindow + 1
            }

            else -> {
                scoreName = "hit0"
                hitWindow!!.mehWindow + 1
            }
        }.toFloat() / 1000

        effects.hit(pos, scoreName, null)

        hud!!.onAccuracyRegister(HitObjectType.Spinner, accuracy)
        hud!!.onNoteHit(stat)
    }

    override fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?) {
        var volume = 1f
        val muted = GameHelper.getMuted()

        if (muted != null && muted.affectsHitSounds) {
            volume = muted.volumeAt(stat.combo)
        }

        for (i in 0 until samples!!.size) {
            val sample = samples[i]
            sample.volume = volume
            sample.play()
        }
    }

    private fun playLoopingSamples() {
        val activeObjects = activeObjects ?: return

        for (i in 0 until activeObjects.size) {
            activeObjects[i].playLoopingSamples()
        }
    }

    private fun stopLoopingSamples() {
        val activeObjects = activeObjects ?: return

        for (i in 0 until activeObjects.size) {
            activeObjects[i].stopLoopingSamples()
        }
    }

    override fun addObject(`object`: GameObject?) {
        activeObjects!!.add(`object`!!)
    }

    override fun isObjectHittable(`object`: GameObject?): Boolean {
        return `object` === judgeableObject
    }

    override fun getCursor(index: Int): Cursor {
        return cursors[index]!!
    }

    private fun onPointerEvent(event: PointerEvent): Boolean {
        val offset = if (previousFrameTime > 0)
            (event.time - previousFrameTime) * GameHelper.getSpeedMultiplier()
        else
            0f
        val eventTime = (gameplayClock.time * 1000 + offset).toInt()

        if (replaying || isGameOver) {
            return false
        }

        val id = event.pointerId
        if (id < 0 || id >= getCursorsCount()) {
            return false
        }

        val cursor = cursors[id]!!
        val action = event.action

        if (action == PointerEvent.Action.Down) {
            if (cursor.mouseBlocked) {
                cursor.mouseBlocked = false
                return true
            }

            var activeCursorCount = 0

            for (i in cursors.indices) {
                if (activeCursorCount >= maximumActiveCursorCount) {
                    break
                }

                if (cursors[i]!!.mouseDown) {
                    ++activeCursorCount
                }
            }

            if (activeCursorCount >= maximumActiveCursorCount) {
                return false
            }
        }

        val sprite = if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot() && cursorSprites != null)
            cursorSprites!![id]
        else
            null

        val playfieldPosition = playfield.toLocalSpaceFromScene(event.x, event.y)
        val cursorEvent = CursorEvent.obtain(event, playfieldPosition.x, playfieldPosition.y)

        cursorEvent.trackTime = gameplayClock.time * 1000
        cursorEvent.offset = offset.toDouble()

        sprite?.setPosition(cursorEvent.position.x, cursorEvent.position.y)

        if (action == PointerEvent.Action.Down) {

            sprite?.setShowing(true)

            if (!GameHelper.isAutoplay()) {
                hud!!.onGameplayTouchDown(eventTime / 1000f)
            }

            cursor.addEvent(cursorEvent)

            replay?.addPress(eventTime, cursorEvent.trackPosition, id)
        } else if (cursor.mouseDown && action == PointerEvent.Action.Move) {

            sprite?.setShowing(true)

            cursor.addEvent(cursorEvent)

            replay?.addMove(eventTime, cursorEvent.trackPosition, id)

        } else if (cursor.mouseDown && action == PointerEvent.Action.Up) {

            sprite?.setShowing(false)

            cursor.addEvent(cursorEvent)

            replay?.addUp(eventTime, id)

        } else if (action == PointerEvent.Action.Cancel) {
            removeAllCursors()
        } else {
            return false
        }
        return true
    }

    private fun removeAllCursors() {
        val currentTime = Platform.uptimeMillis
        val offset = if (previousFrameTime > 0)
            (currentTime - previousFrameTime) * GameHelper.getSpeedMultiplier()
        else
            0f
        val time = gameplayClock.time * 1000 + offset

        for (i in cursors.indices) {
            val cursor = cursors[i]!!

            if (cursor.mouseDown) {
                val upEvent = CursorEvent.obtain()

                upEvent.systemTime = currentTime
                upEvent.trackTime = time
                upEvent.action = PointerEvent.Action.Up

                cursor.addEvent(upEvent)

                replay?.addUp(time.toInt(), i)
            }

            cursorSprites?.get(i)?.setShowing(false)
        }
    }

    fun pause() {

        if (paused) {
            return
        }

        if (isHUDEditorMode) {
            hud!!.onBackPress()
            return
        }

        if (Multiplayer.isMultiplayer) {
            // Setting a delay of 300ms minimum for the player to tap back button again.
            if (lastBackPressTime > 0 && realTimeElapsed - lastBackPressTime > max(300f, Settings[OsuSettings.BackButtonPressTime] * 1.5f)) {
                // Room being null can happen when the player disconnects from socket while playing
                if (Multiplayer.isConnected)
                    async { runSafe { RoomAPI.submitFinalScore(stat.toJson()) } }

                Multiplayer.log("Player left the match.")
                quit()
                return
            }

            lastBackPressTime = realTimeElapsed.toFloat()
            Toasts.show("Tap twice to exit to room.")
            return
        }

        if (isGameOver) {
            // Finishing the game over animation now.
            GlobalManager.getInstance().music.setFrequencyForcefully(101f)
            return
        }

        if (video != null && videoStarted) {
            video!!.pause()
        }

        stopLoopingSamples()

        if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot() && !replaying) {
            removeAllCursors()
        }

        gameplayClock.pause()
        paused = true

        gameUIScene.ignoreUpdate = true
        hud!!.ignoreUpdate = true

        showPauseMenu(false)
    }

    private fun showPauseMenu(isFail: Boolean) {
        pauseMenu = PauseMenu(this, isFail)
        pauseMenu!!.show()
    }

    fun gameover() {

        if (isGameOver) {
            return
        }
        isGameOver = true

        if (!replaying) {
            removeAllCursors()
        }

        if (Multiplayer.isMultiplayer) {
            if (Multiplayer.isConnected) {
                Multiplayer.log("Player has lost, moving to room scene.")
                async { runSafe { RoomAPI.submitFinalScore(stat.toJson()) } }
            }
            quit()
            return
        }

        stopLoopingSamples()
        val music = GlobalManager.getInstance().music

        if (GameHelper.isPerfect()) {
            video?.pause()

            gameplayClock.pause()
            paused = true
            gameUIScene.ignoreUpdate = true
            hud!!.ignoreUpdate = true
            return
        }

        SkinSounds.get("failsound")!!.play()
        gameStarted = false

        val initialFrequency = music.frequency

        // Wind down animation for failing based on osu!stable behavior.
        failAnimation = FailAnimation(music, initialFrequency)
    }

    /**
     * The wind down animation played when failing, based on osu!stable behavior.
     */
    private inner class FailAnimation(private val music: MusicService, private val initialFrequency: Float) {
        private var elapsedTime = 0f

        private fun applyEffectToLayer(layer: UIContainer) {
            if (layer.alpha > 0) {
                layer.alpha = max(0f, layer.alpha - 0.007f)
            }

            for (i in 0 until layer.childCount) {
                val entity = layer[i]

                val y = entity.y.resolve(0f)
                entity.setPosition(entity.x.resolve(0f), if (y < 0f) y * 0.6f else y * 1.01f)

                if (entity.rotationZ == 0f) {
                    entity.rotationZ = entity.rotationZ + Random.Default.nextDouble(-0.02, 0.02).toFloat() * 180 / MathUtils.PI_F
                } else if (entity.rotationZ > 0) {
                    entity.rotationZ = entity.rotationZ + 0.01f * 180 / MathUtils.PI_F
                } else {
                    entity.rotationZ = entity.rotationZ - 0.01f * 180 / MathUtils.PI_F
                }
            }
        }

        fun update(secondsElapsed: Float) {
            elapsedTime += secondsElapsed

            // In osu!stable, the update is capped to 60 FPS. This means in higher framerates, the animations
            // need to be slowed down to match 60 FPS.
            val sixtyFPS = 1 / 60f

            if (elapsedTime < sixtyFPS) {
                return
            }

            elapsedTime -= sixtyFPS

            if (music.frequency > 101) {
                applyEffectToLayer(mgScene)
                applyEffectToLayer(bgScene)

                val decreasedFrequency = max(101f, music.frequency - 300)
                val decreasedSpeed = GameHelper.getSpeedMultiplier() * (1 - (initialFrequency - decreasedFrequency) / initialFrequency)

                if (videoEnabled && video != null) {
                    video!!.setPlaybackSpeed(decreasedSpeed)
                }

                music.setFrequencyForcefully(decreasedFrequency)
            } else {
                if (videoEnabled && video != null) {
                    video!!.pause()
                }

                // Ensure music frequency is reset back to what it was.
                music.setFrequencyForcefully(initialFrequency)
                gameplayClock.pause()
                paused = true

                gameUIScene.ignoreUpdate = true
                hud!!.ignoreUpdate = true

                failAnimation = null

                showPauseMenu(true)
            }
        }
    }

    fun resume() {
        if (!paused) {
            return
        }

        gameUIScene.ignoreUpdate = false
        hud!!.ignoreUpdate = false

        if (pauseMenu != null) {
            pauseMenu!!.hide()
            pauseMenu = null
        }

        paused = false

        if (isGameOver) {
            quit()
            return
        }

        if (!gameplayClock.isRunning
            && (replaySettingsPanel == null || !replaySettingsPanel!!.playbackControl.seekControl.isPlaybackPaused)) {
            gameplayClock.start()

            if (video != null && videoStarted) {
                video!!.play()
            }

            playLoopingSamples()
        }
    }

    val isPaused: Boolean
        get() = paused

    override fun getCursorsCount(): Int {
        return cursors.size
    }


    override fun registerAccuracy(type: HitObjectType?, acc: Double) {
        val mehWindow = hitWindow!!.mehWindow / 1000

        if (abs(acc) <= mehWindow) {
            if (type != HitObjectType.Spinner) {
                stat.addHitOffset(acc)
            }

            if (replaying) {
                scoringScene!!.replayStat!!.addHitOffset(acc)
            }
        }

        hud!!.onAccuracyRegister(type!!, acc.toFloat())
    }


    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?) {
        onTrackingSliders(false)
        if (GameHelper.isAutoplay()) {
            autoCursor!!.onSliderEnd()
        }
        if (replay != null && !replaying) {
            val acc = accuracy.toShort()
            replay!!.addObjectResult(id, acc, tickSet!!.copy())
        }
    }

    override fun onTrackingSliders(isTrackingSliders: Boolean) {
        if (GameHelper.isAutoplay()) {
            autoCursor!!.onSliderTracking()
        }
        if (GameHelper.isFlashlight()) {
            flashlightSprite!!.onTrackingSliders(isTrackingSliders)
        }
    }

    override fun onUpdatedAutoCursor(pX: Float, pY: Float) {
        if (GameHelper.isFlashlight()) {
            flashlightSprite!!.onMouseMove(pX, pY)
        }
    }

    override fun updateAutoBasedPos(pX: Float, pY: Float) {
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor!!.setPosition(pX, pY, this)
        }
    }

    private fun getNearestCursorId(pX: Float, pY: Float): Int {
        var nearestDistance = Float.POSITIVE_INFINITY
        var id = -1

        for (i in cursors.indices) {
            val latestEvent = cursors[i]!!.getLatestNonUpEvent()

            if (latestEvent != null) {
                val distance = Utils.squaredDistance(pX, pY, latestEvent.position.x, latestEvent.position.y)

                if (distance < nearestDistance) {
                    id = i
                    nearestDistance = distance
                }
            }
        }

        return id
    }

    private fun calculateAllSliderPaths(scope: CoroutineScope?) {
        scope?.ensureActive()

        val playableBeatmap = this.playableBeatmap

        if (playableBeatmap == null || playableBeatmap.hitObjects.sliderCount == 0) {
            return
        }

        val sliderPaths = arrayOfNulls<SliderPath>(playableBeatmap.hitObjects.sliderCount)
        val sliderRenderPaths = arrayOfNulls<SliderBody.RenderPathCache>(playableBeatmap.hitObjects.sliderCount)
        var index = 0

        for (obj in playableBeatmap.hitObjects.objects) {
            scope?.ensureActive()

            if (obj !is Slider) {
                continue
            }
            val slider = obj

            sliderPaths[index] = GameHelper.convertSliderPath(slider, scope)

            scope?.ensureActive()

            val scale = slider.screenSpaceGameplayScale
            val sliderBodyWidth = OsuSkin.get().sliderBodyWidth * scale
            val sliderBorderWidth = OsuSkin.get().sliderBorderWidth * scale
            val sliderHintWidth = OsuSkin.get().sliderHintWidth * scale
            val isHintVisible = OsuSkin.get().isSliderHintEnable &&
                slider.distance > OsuSkin.get().sliderHintShowMinLength

            sliderRenderPaths[index] = SliderBody.createCache(
                GameHelper.convertSliderPath(sliderPaths[index]!!, scope),
                sliderBodyWidth,
                sliderBorderWidth,
                isHintVisible,
                sliderHintWidth,
                Settings[OsuSettings.SnakingInSliders] || Settings[OsuSettings.SnakingOutSliders]
            )

            ++index
        }


        this.sliderPaths = sliderPaths
        this.sliderRenderPaths = sliderRenderPaths
    }

    private fun getSliderPath(index: Int): SliderPath? {
        val sliderPaths = sliderPaths
        return if (sliderPaths != null && index < sliderPaths.size && index >= 0) {
            sliderPaths[index]
        } else {
            null
        }
    }

    private fun getSliderRenderPath(index: Int): SliderBody.RenderPathCache {
        val sliderRenderPaths = sliderRenderPaths ?: throw IllegalStateException(
            "Slider render paths have not been calculated; sliderRenderPaths is null, index=$index"
        )

        if (index < 0 || index >= sliderRenderPaths.size) {
            throw IndexOutOfBoundsException(
                "Invalid slider render path index " + index + " for sliderRenderPaths.length=" + sliderRenderPaths.size
            )
        }

        val renderPath = sliderRenderPaths[index] ?: throw IllegalStateException(
            "Slider render path at index " + index + " is null after calculation; sliderRenderPaths.length=" + sliderRenderPaths.size
        )

        return renderPath
    }

    /**
     * The time that gameplay started relative to the start of the [Beatmap], in seconds.
     */
    open val initialStartTime: Float
        get() = initialStartTimeValue

    /**
     * The current elapsed time relative to the start of the [Beatmap], in seconds.
     */
    override val elapsedTime: Float
        get() = gameplayClock.time

    override fun isRemoveSliderLock(): Boolean {
        return Multiplayer.isRemoveSliderLock()
    }

    override fun isAfterSeek(): Boolean {
        return postSeekFrameCount > 0
    }

    fun saveFailedReplay(): Boolean {
        stat.time = System.currentTimeMillis()
        if (replay != null && !replaying) {
            val replay = replay!!
            val objects = objects!!

            //write misses to replay
            for (obj in activeObjects!!) {
                stat.registerHit(0, false, false)
                replay.addObjectScore(obj.getId(), ResultType.MISS)
            }
            while (objectIndex < objects.size) {
                ++objectIndex
                stat.registerHit(0, false, false)
                replay.addObjectScore(++lastObjectId, ResultType.MISS)
            }

            val currentTime = System.currentTimeMillis().toString()
            val odrFilename = MD5Calculator.getStringMD5(lastBeatmapInfo!!.filename + currentTime) + currentTime.substring(0, min(3, currentTime.length)) + ".odr"

            replayFilePath = GameFolders.scorePath + odrFilename
            replay.setStat(stat)
            replay.save(replayFilePath!!)

            if (stat.totalScoreWithMultiplier > 0 && ModAutoplay::class !in stat.mod) {
                stat.replayFilename = odrFilename
                stat.beatmapMD5 = lastBeatmapInfo!!.md5

                try {
                    DatabaseManager.scoreInfoTable.insertScore(stat.toScoreInfo())
                } catch (e: Exception) {
                    Logger.e("GameScene", "Failed to save score to database", e)
                }
            }

            Toasts.showLong(Res.string.message_save_replay_successful)
            replayFilePath = null
            return true
        } else {
            Toasts.showLong(Res.string.message_save_replay_failed)
            return false
        }
    }

    private fun updatePPValue(objectId: Int) {
        if (Settings[OsuSettings.HideInGameUI] || !isHUDEditorMode && !OsuSkin.get().hudSkinData.hasElement(HUDElements.pp_counter.name)) {
            return
        }

        val pp = when (OsuSettings.difficultyAlgorithm) {
            DifficultyAlgorithm.droid -> getDroidPPAt(objectId)
            DifficultyAlgorithm.standard -> getStandardPPAt(objectId)
        }

        stat.pp = pp
    }

    private fun getDroidPPAt(objectId: Int): Double {
        val droidTimedDifficultyAttributes = droidTimedDifficultyAttributes
        val performanceCalculationParameters = performanceCalculationParameters

        if (droidTimedDifficultyAttributes == null || performanceCalculationParameters == null || objectId < 0 ||
            objectId >= droidTimedDifficultyAttributes.size) {
            return 0.0
        }

        val timedAttributes = droidTimedDifficultyAttributes[objectId]

        performanceCalculationParameters.populate(stat, timedAttributes.sliderCount, timedAttributes.sliderTickCount, timedAttributes.sliderRepeatCount)

        BeatmapDifficultyCalculator.calculateDroidPerformance(timedAttributes.attributes,
            performanceCalculationParameters as DroidPerformanceCalculationParameters,
            performanceAttributes as DroidPerformanceAttributes)

        return performanceAttributes!!.total
    }

    private fun getStandardPPAt(objectId: Int): Double {
        val standardTimedDifficultyAttributes = standardTimedDifficultyAttributes
        val performanceCalculationParameters = performanceCalculationParameters

        if (standardTimedDifficultyAttributes == null || performanceCalculationParameters == null || objectId < 0 ||
            objectId >= standardTimedDifficultyAttributes.size) {
            return 0.0
        }

        val timedAttributes = standardTimedDifficultyAttributes[objectId]

        performanceCalculationParameters.populate(stat, timedAttributes.sliderCount, timedAttributes.sliderTickCount, timedAttributes.sliderRepeatCount)

        BeatmapDifficultyCalculator.calculateStandardPerformance(timedAttributes.attributes,
            performanceCalculationParameters as StandardPerformanceCalculationParameters,
            performanceAttributes as StandardPerformanceAttributes)

        return performanceAttributes!!.total
    }

    private fun applyPlayfieldSizeScale() {
        val windowWidth = root.bounds.width

        if (windowWidth <= 0f) {
            return
        }

        val playfieldSize = OsuSettings.playfieldSize
        val resolutionWidth = GameResolution.width.toFloat()
        val resolutionHeight = GameResolution.height.toFloat()

        // The point of the playfield that ends up at the center of the window.
        val centerX = resolutionWidth * Interpolation.linear(
            Interpolation.linear(0f, 0.5f, playfieldSize),
            Interpolation.linear(1f, 0.5f, playfieldSize),
            1 - OsuSettings.playfieldHorizontalPosition
        )

        val centerY = resolutionHeight * Interpolation.linear(
            Interpolation.linear(0f, 0.5f, playfieldSize),
            Interpolation.linear(1f, 0.5f, playfieldSize),
            1 - OsuSettings.playfieldVerticalPosition
        )

        val resolutionScale = windowWidth / resolutionWidth

        playfield.scale = resolutionScale * playfieldSize
        playfield.translationX = (resolutionScale * (resolutionWidth / 2f - centerX * playfieldSize)).px
        playfield.translationY = (resolutionScale * (resolutionHeight / 2f - centerY * playfieldSize)).px
    }

    private fun resetPlayfieldSizeScale() {
        playfield.scale = root.bounds.width / GameResolution.width
        playfield.translationX = Dimension.Zero
        playfield.translationY = Dimension.Zero
    }

    private fun estimateMaximumActiveObjects(): Int {
        val objects = objects ?: return 0

        // Estimate the maximum number of simultaneously active objects to pre-size the lists and minimize
        // array reallocations.
        val lifetimeEnds = java.util.PriorityQueue<Double>(max(1, objects.size / 4))
        var estimatedMaxActiveObjects = 0

        for (`object` in objects) {
            `object`!!
            val lifetimeStart = `object`.startTime - `object`.timePreempt

            // Remove all objects that have expired by the time this object's lifetime starts.
            while (!lifetimeEnds.isEmpty() && lifetimeEnds.peek() <= lifetimeStart) {
                lifetimeEnds.poll()
            }

            val lifetimeEnd: Double

            if (`object` is HitCircle) {
                val hitWindow = `object`.hitWindow
                lifetimeEnd = `object`.startTime + (hitWindow?.mehWindow ?: 0.0)
            } else if (`object` is Slider) {
                lifetimeEnd = max(`object`.startTime + `object`.head.hitWindow!!.mehWindow, `object`.endTime)
            } else {
                lifetimeEnd = `object`.endTime
            }

            lifetimeEnds.add(lifetimeEnd)
            estimatedMaxActiveObjects = max(estimatedMaxActiveObjects, lifetimeEnds.size)
        }

        return estimatedMaxActiveObjects
    }

    //region Seeking

    private fun seekTo(targetTimeSeconds: Float) {
        val playableBeatmap = this.playableBeatmap
        val objects = objects

        if (playableBeatmap == null || objects == null) {
            return
        }

        val activeObjects = activeObjects!!
        val expiredObjects = expiredObjects!!
        val hud = hud!!
        val breakAnimator = breakAnimator!!

        val clampedTime = MathUtils.clamp(
            targetTimeSeconds,
            (objects[0]!!.startTime / 1000).toFloat(),
            (objects[objects.size - 1]!!.endTime / 1000).toFloat()
        )

        val targetMs = clampedTime * 1000

        // Force-expire all active objects (detach from scene and return to pool).
        for (i in 0 until activeObjects.size) {
            val obj = activeObjects[i]

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire()
            }
        }

        // Also expire objects that were queued for cleanup but not yet processed.
        for (i in 0 until expiredObjects.size) {
            val obj = expiredObjects[i]

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire()
            }
        }

        activeObjects.clear()
        expiredObjects.clear()
        processedExpiredObjects.clear()
        judgeableObject = null

        // Detach any lingering hit effects from the gameplay scene.
        for (i in mgScene.childCount - 1 downTo 0) {
            val child = mgScene[i]

            if (child is UISprite && child.choreography != null) {
                // Hit effects need to be pooled, so we cannot directly detach.
                // finish() fires the effect's fade-out callback, which schedules putEffect() so the GameEffect
                // wrapper is returned to the pool rather than orphaned.
                child.choreography!!.finish()
            }

            // We detach directly so that the next frame is not orphaned with hit effects that were not detached (since
            // GameEffect's callback is scheduled for the next update tick).
            child.removeSelf()
        }

        // Remove follow points spawned for objects before the seek point.
        FollowPointConnection.clearAll(bgScene)

        // Reset spawn counters.
        objectIndex = 0
        sliderIndex = 0
        lastObjectId = -1
        gameStarted = false
        leadOut = 0f
        comboWasMissed = false
        comboWas100 = false
        failcount = 0
        isGameOver = false
        hasFailed = false

        // Reset auto cursor modifiers.
        autoCursor?.stopMoving()

        // Clear pending cursor events.
        for (i in cursors.indices) {
            val cursor = cursors[i]

            if (cursor != null) {
                cursor.reset(Platform.uptimeMillis, 0f)
                cursor.latestProcessedDownEventIndex = 0
                cursor.latestProcessedEventIndex = 0
            }
        }

        // Advance timing and effect control points to the target time.
        timingControlPointIndex = 0
        effectControlPointIndex = 0

        val timingControlPoints = timingControlPoints!!
        val effectControlPoints = effectControlPoints!!

        while (timingControlPointIndex + 1 < timingControlPoints.size && timingControlPoints[timingControlPointIndex + 1].time <= targetMs) {
            timingControlPointIndex++
        }

        while (effectControlPointIndex + 1 < effectControlPoints.size && effectControlPoints[effectControlPointIndex + 1].time <= targetMs) {
            effectControlPointIndex++
        }

        val controlPoints = playableBeatmap.controlPoints

        activeTimingPoint = if (timingControlPoints.isNotEmpty())
            timingControlPoints[timingControlPointIndex] else controlPoints.timing.defaultControlPoint

        activeEffectPoint = if (effectControlPoints.isNotEmpty())
            effectControlPoints[effectControlPointIndex] else controlPoints.effect.defaultControlPoint

        // Advance break period index past fully elapsed breaks.
        breakPeriodIndex = 0

        val breakPeriods = breakPeriods

        if (breakPeriods != null) {
            while (breakPeriodIndex < breakPeriods.size && breakPeriods[breakPeriodIndex].endTime <= targetMs) {
                breakPeriodIndex++
            }
        }

        // Reset replay cursor movement indices.
        if (replaying) {
            replay!!.cursorIndex.fill(0)
            replay!!.lastMoveIndex.fill(-1)
        }

        // Reconstruct scoring state up to the seek target.
        stat.reset()
        reconstructStatAtTime(targetMs)

        // Reset any in-progress break animation, then re-initialize if the seek target is inside a break.
        breakAnimator.reset()

        if (breakPeriods != null && breakPeriodIndex < breakPeriods.size) {
            val bp = breakPeriods[breakPeriodIndex]

            if (bp.startTime <= targetMs && targetMs < bp.endTime) {
                gameStarted = false
                val totalDuration = bp.duration / 1000f
                val elapsedTime = ((targetMs - bp.startTime) / 1000.0).toFloat()
                breakAnimator.init(totalDuration, elapsedTime)
                breakPeriodIndex++
                hud.onBreakStateChange(true)
            } else {
                hud.onBreakStateChange(false)
            }
        } else {
            hud.onBreakStateChange(false)
        }

        hud.onNoteHit(stat)
        hud.onSeek()

        // Replay all touch-down events up to the seek target so HUD elements
        // that depend on touch history can reconstruct their state correctly.
        if (GameHelper.isAutoplay() || replaying) {
            if (GameHelper.isAutoplay()) {
                for (i in objects.indices) {
                    val tapTime = objects[i]!!.startTime.toFloat() / 1000f

                    if (tapTime > clampedTime) {
                        break
                    }

                    hud.onGameplayTouchDown(tapTime)
                }
            } else {
                val replay = replay!!
                val cursorCount = replay.cursorMoves.size

                for (i in 0 until cursorCount) {
                    val moveArray = replay.cursorMoves[i]

                    for (j in 0 until moveArray.size) {
                        val movement = moveArray.movements[j]

                        val tapTime = movement.time / 1000f

                        if (tapTime > clampedTime) {
                            break
                        }

                        if (movement.touchType == TouchType.DOWN) {
                            hud.onGameplayTouchDown(tapTime)
                        }
                    }
                }
            }
        }

        updatePPValue(objectIndex - 1)

        // For variable-rate mods (WindUp/WindDown), the rate at the seek target may differ from
        // the current rate. FramedBeatmapClock.seek() subtracts (userGlobalOffset * rate) from the
        // seek position to compute the audio byte offset, so the rate must be updated first.
        if (!rateAdjustingMods.isEmpty()) {
            val modRate = rateAdjustingMods.calculateRate(targetMs.toDouble())

            val replaySettingsRate = replaySettingsPanel?.playbackControl?.rateControl?.rate ?: 1f

            val targetSpeedMultiplier = modRate * replaySettingsRate

            if (targetSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(targetSpeedMultiplier)
                gameplayClock.rate = targetSpeedMultiplier
            }
        }

        // Seek the beatmap clock (also seeks the audio source).
        gameplayClock.seek(clampedTime)

        // Seek video.
        if (videoEnabled && video != null) {
            video!!.seekTo(max(0, ((clampedTime - videoOffset) * 1000).toInt()))
        }

        // Suppress hitsounds for slider objects (head, ticks, repeats) that have already passed the seek target.
        // Objects are spawned in the same frame as seekTo runs, but updated in the next frame,
        // so the flag must survive 2 update frames.
        postSeekFrameCount = 2
    }

    private fun reconstructStatAtTime(targetMs: Float) {
        val playableBeatmap = this.playableBeatmap
        val objects = objects

        if (playableBeatmap == null || objects == null || objects.isEmpty()) {
            return
        }

        val timingControlPoints = timingControlPoints!!
        val difficulty = playableBeatmap.difficulty
        var localTimingIdx = 0
        var localBreakIdx = 0
        val objectData = if (replaying) replay!!.objectData else null

        // How far continuous HP drain has been applied, and which object-to-object rate segment
        // it's currently within ([objects[rateSegIdx].startTime - timePreempt,
        // objects[rateSegIdx + 1].startTime - timePreempt)). Both advance with elapsed
        // time and is independent of which object's own judgement has been applied yet.
        var drainCursorMs = objects[0]!!.startTime - objects[0]!!.timePreempt
        var rateSegIdx = 0

        for (i in objects.indices) {
            val obj = objects[i]!!

            val judgementTimeMs = getJudgementTimeMs(i, obj, objectData)

            if (judgementTimeMs > targetMs) {
                break
            }

            val drainTargetMs = min(judgementTimeMs, targetMs.toDouble())

            while (drainCursorMs < drainTargetMs && rateSegIdx < objects.size - 1) {
                val segFromObj = objects[rateSegIdx]!!
                val segToObj = objects[rateSegIdx + 1]!!

                // Advance local timing point index to this segment's own "to" object.
                while (localTimingIdx + 1 < timingControlPoints.size && timingControlPoints[localTimingIdx + 1].time <= segToObj.startTime) {
                    localTimingIdx++
                }

                val msPerBeat = timingControlPoints[localTimingIdx].msPerBeat
                val distToNextObject = max(segToObj.startTime - segFromObj.startTime, msPerBeat / 2) / 1000

                val drainRate = if (difficulty.hp > 0 && distToNextObject > 0)
                    1 + difficulty.hp / (2 * distToNextObject)
                else
                    0.375

                val segEndMs = segToObj.startTime - segToObj.timePreempt
                val stopAtMs = min(segEndMs, drainTargetMs)

                if (stopAtMs > drainCursorMs) {
                    // Advance past breaks that fully precede the current segment.
                    val breakPeriods = breakPeriods
                    if (breakPeriods != null) {
                        while (localBreakIdx < breakPeriods.size && breakPeriods[localBreakIdx].endTime <= drainCursorMs) {
                            localBreakIdx++
                        }
                    }

                    val effectiveSecs = calculateEffectiveDrainDuration(drainCursorMs, stopAtMs, localBreakIdx)

                    // Apply drain incrementally so that a large drain section can consume multiple Easy lives.
                    // A one-shot stat.changeHp clamps at 0 and loses the excess, causing at most one Easy revive per drain
                    // section regardless of how deep HP would have gone.
                    var remainingDrain = (drainRate * 0.01 * effectiveSecs).toFloat()

                    while (remainingDrain > 0) {
                        val currentHp = stat.hp

                        if (remainingDrain < currentHp) {
                            stat.changeHp(-remainingDrain)
                            break
                        }

                        remainingDrain -= currentHp
                        stat.changeHp(-currentHp)

                        if (!stat.canFail) {
                            break
                        }

                        if (GameHelper.isEasy() && failcount < 3) {
                            failcount++
                            stat.changeHp(1f)
                        } else {
                            return
                        }
                    }

                    drainCursorMs = stopAtMs
                }

                if (drainCursorMs >= segEndMs) {
                    rateSegIdx++
                } else {
                    // Capped short of this segment's own end by drainTargetMs - stop crossing
                    // further for now. The remainder of this same segment continues once a
                    // later object's own judgement time lets drain proceed past it.
                    break
                }
            }

            if (!gameStarted) {
                gameStarted = true
            }

            val data = if (objectData != null && i < objectData.size) objectData[i] else null

            val endCombo = obj.isLastInCombo

            if (obj is HitCircle) {
                applyCircleResult(data, endCombo)
            } else if (obj is Slider) {
                sliderIndex++
                applySliderResult(obj, data, endCombo)
            } else if (obj is Spinner) {
                applySpinnerResult(obj, data, endCombo)
            }

            lastObjectId = i
            objectIndex = i + 1
        }
    }

    private fun getJudgementTimeMs(idx: Int, obj: HitObject, objectData: Array<Replay.ReplayObjectData?>?): Double {
        if (obj is Slider || obj is Spinner) {
            return obj.endTime
        }

        // Circle: judged when hit (or miss window expires).
        if (objectData != null && idx < objectData.size) {
            val data = objectData[idx]

            if (data != null && data.result != ResultType.MISS.id) {
                return obj.startTime + abs(data.accuracy.toInt())
            }
        }

        val mehWindow = obj.hitWindow?.mehWindow ?: HitWindow.MISS_WINDOW

        return obj.startTime + mehWindow
    }

    private fun reconstructHitOffset(accSeconds: Double) {
        if (abs(accSeconds) <= hitWindow!!.mehWindow / 1000) {
            stat.addHitOffset(accSeconds)
        }
    }

    private fun applyCircleResult(data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val result = data?.result ?: ResultType.HIT300.id

        if (result == ResultType.MISS.id) {
            comboWasMissed = true
            stat.registerHit(0, false, false)
        } else {
            reconstructHitOffset(if (data != null) data.accuracy / 1000.0 else 0.0)

            if (result == ResultType.HIT50.id) {
                stat.registerHit(50, false, false)
                comboWas100 = true
            } else if (result == ResultType.HIT100.id) {
                comboWas100 = true
                stat.registerHit(100, endCombo && !comboWasMissed, false)
            } else {
                if (endCombo && !comboWasMissed) {
                    stat.registerHit(300, true, !comboWas100)
                } else {
                    stat.registerHit(300, false, false)
                }
            }
        }

        if (endCombo) {
            comboWas100 = false
            comboWasMissed = false
        }
    }

    private fun applySliderResult(slider: Slider, data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val result = data?.result ?: ResultType.HIT300.id

        if (result == ResultType.MISS.id) {
            comboWasMissed = true
            stat.registerHit(0, false, false)

            if (endCombo) {
                comboWas100 = false
                comboWasMissed = false
            }

            return
        }

        // Slider head: HIT300 implies all ticks were hit; autoplay (data==null) always hits.
        // For HIT50/HIT100, reconstruct whether the head was actually within the hit window,
        // mirroring GameplaySlider.onSliderHeadHit.
        val accSeconds = if (data != null) data.accuracy / 1000.0 else 0.0
        val headHit: Boolean

        if (data == null || result == ResultType.HIT300.id) {
            headHit = true
        } else {
            val replayVersion = GameHelper.getReplayVersion()
            val mehWindowSecs = hitWindow!!.mehWindow / 1000.0
            val sliderDurationSecs = slider.duration / 1000.0
            val lateHitThreshold = if (replayVersion <= 7) min(mehWindowSecs, sliderDurationSecs) else mehWindowSecs

            if (replayVersion >= 6 || mehWindowSecs <= sliderDurationSecs) {
                headHit = -mehWindowSecs <= accSeconds && accSeconds <= lateHitThreshold
            } else {
                headHit = accSeconds <= sliderDurationSecs
            }
        }

        if (headHit) {
            reconstructHitOffset(accSeconds)
            stat.registerHit(30, false, false)
            stat.addSliderHeadHit()
        }

        // Ticks and repeats from tickSet.
        val nested = slider.nestedHitObjects

        // Skip head (index 0) and tail (last index).
        for (i in 1 until nested.size - 1) {
            val wasHit = data == null || (data.tickSet != null && data.tickSet!![i - 1])

            if (!wasHit) {
                stat.registerHit(0, true, false)
                continue
            }

            val nestedObj = nested[i]

            if (nestedObj is SliderTick) {
                stat.registerHit(10, false, false)
                stat.addSliderTickHit()
            } else if (nestedObj is SliderRepeat) {
                stat.registerHit(30, false, false)
                stat.addSliderRepeatHit()
            }
        }

        // Slider tail: combo is only awarded when the player was tracking at the endpoint.
        // Unlike circles, the result score reflects ticks hit, not timing accuracy.
        val tailTracked = data == null || (data.tickSet != null && data.tickSet!![nested.size - 2])
        val tailResult = data?.result ?: ResultType.HIT300.id

        if (tailResult == ResultType.HIT50.id) {
            comboWas100 = true
            stat.registerHit(50, false, false, tailTracked)
        } else if (tailResult == ResultType.HIT100.id) {
            comboWas100 = true
            stat.registerHit(100, endCombo && !comboWasMissed, false, tailTracked)
        } else {
            if (endCombo && !comboWasMissed) {
                stat.registerHit(300, true, !comboWas100, tailTracked)
            } else {
                stat.registerHit(300, false, false, tailTracked)
            }
        }

        if (endCombo) {
            comboWas100 = false
            comboWasMissed = false
        }

        if (tailTracked) {
            stat.addSliderEndHit()
        }
    }

    private fun applySpinnerResult(spinner: Spinner, data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val duration = spinner.duration.toFloat() / 1000
        var needRotations = (2 + 2 * playableBeatmap!!.difficulty.od / 10f) * duration

        if (duration < 0.05f) {
            needRotations = 0.1f
        }

        val preClear: Int
        val bonus: Int

        if (data != null) {
            // data.accuracy = totalSpins * 4 + resultCode, where totalSpins = fullRotations (pre-clear, 100 pts each)
            // + (bonusScoreCounter - 1) (bonus, 1000 pts each). Split by needRotations to award the correct amounts.
            val totalSpins = (data.accuracy.toInt() and 0xFFFF) shr 2
            preClear = min(totalSpins, ceil(needRotations.toDouble()).toInt() - 1)
            bonus = totalSpins - preClear
        } else {
            // Autoplay always clears the spinner. Reconstruct the pre-clear (100 pts each) and bonus (1000 pts each)
            // rotation split from the spinner's parameters.
            // ceil(needRotations) - 1 rotations are pre-clear; bonus rotations begin at ceil(needRotations) total.
            val totalRotations = 5f * duration

            preClear = ceil(needRotations.toDouble()).toInt() - 1
            bonus = max(0, totalRotations.toInt() - ceil(needRotations.toDouble()).toInt() + 1)
        }

        for (s in 0 until preClear) {
            stat.registerSpinnerHit()
        }

        for (s in 0 until bonus) {
            stat.registerHit(1000, false, false)
        }

        applyCircleResult(data, endCombo)
    }

    /**
     * Returns the effective drain duration in seconds for the segment [startMs, endMs], subtracting any time that falls
     * within a break period. `startBreakIdx` should be the first break period index whose end time is >= startMs
     * (caller advances this monotonically as segments progress forward in time).
     */
    private fun calculateEffectiveDrainDuration(startMs: Double, endMs: Double, startBreakIdx: Int): Double {
        if (startMs >= endMs) {
            return 0.0
        }

        var total = endMs - startMs

        val breakPeriods = breakPeriods
        if (breakPeriods != null) {
            for (i in startBreakIdx until breakPeriods.size) {
                val bp = breakPeriods[i]

                if (bp.startTime >= endMs) {
                    break
                }

                val overlap = min(bp.endTime.toDouble(), endMs) - max(bp.startTime.toDouble(), startMs)

                if (overlap > 0) {
                    total -= overlap
                }
            }
        }

        return max(0.0, total) / 1000
    }

    //endregion

    private inner class GameUIScene : UIScene() {
        private val stopwatch = Stopwatch()
        private var lastRootWidth = 0f
        private var areModIconsAnimated = false

        init {
            clock = gameplayClock
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            if (hud != null && hud!!.onInputEvent(event)) {
                return true
            }

            if (event is PointerEvent) {
                // Pointer events come from the main thread while the gameplay state belongs to the update thread.
                return Verktex.withTreeLock { onPointerEvent(event) }
            }

            return super.onInputEvent(event)
        }

        override fun onUpdate(clock: FrameClock) {
            val deltaTimeSec = clock.elapsedFrameTime

            // The playfield follows the size of the window, which is only known after the first layout.
            if (root.bounds.width != lastRootWidth) {
                lastRootWidth = root.bounds.width
                applyPlayfieldSizeScale()
                applyBackground()
            }

            if (!areModIconsAnimated) {
                areModIconsAnimated = true

                for (i in 0 until modIcons.size) {
                    val finalTimeOffset = i * 0.25f

                    modIcons[i].choreographNow { sequence -> sequence
                        .scaleTo(1f, 0.25f)
                        .delay(2 - finalTimeOffset)
                        .fadeOut(0.5f)
                        .scaleTo(1.5f, 0.5f) }
                }

                modIcons.clear()
            }

            failAnimation?.update(deltaTimeSec)

            if (replayText != null) {
                updateReplayText(deltaTimeSec)
            }

            stopwatch.restart()

            do {
                // The scene's clock only paces the animations, it must be advanced here. Each step moves it
                // at most a frame-stable amount, more steps are taken while it is catching up.
                gameplayClock.processFrame()
                this@GameScene.update(gameplayClock.elapsedFrameTime)

                for (i in cursors.indices) {
                    cursors[i]!!.reset(previousFrameTime, gameplayClock.time * 1000)
                }

                super.onUpdate(clock)
                // 10ms of allowance when catching up.
            } while (gameplayClock.requiresCatchUp && stopwatch.elapsedSeconds < 0.01f)
        }

        private fun updateReplayText(deltaTimeSec: Float) {
            val replayText = replayText!!
            val textWidth = replayText.bounds.width
            val distance = GameResolution.width + 5 + textWidth
            val x = replayText.x.resolve(0f) - distance / 40f * deltaTimeSec

            replayText.setPosition(if (x < -textWidth) GameResolution.width + 5f else x, 140f)
        }
    }

    companion object {
        const val CursorCount = Replay.CursorCount
    }
}
