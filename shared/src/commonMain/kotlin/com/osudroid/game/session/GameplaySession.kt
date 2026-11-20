package com.osudroid.game.session

import com.osudroid.game.Gameplay
import com.osudroid.game.loading.LoadedPlay
import com.osudroid.game.loading.PlayRequest
import com.reco1l.verktex.math.Path
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.OsuGameHost
import com.osudroid.game.GameplayVibrations
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.Platform
import com.osudroid.GameMode
import com.osudroid.audio.MusicClock
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.HitWindow
import com.osudroid.game.rules.HealthProcessor
import com.osudroid.game.rules.HitJudging
import com.osudroid.game.rules.JudgementResult
import com.osudroid.game.rules.ScoreProcessor
import com.osudroid.game.rules.SliderPart
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
import com.osudroid.game.GameplayContext
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.PlayfieldMapping
import com.osudroid.game.SliderPath
import com.osudroid.game.convertSliderPath
import com.osudroid.game.createSliderRenderPath
import com.osudroid.game.replay.ReplaySettingsPanel
import com.osudroid.math.Interpolation
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import com.osudroid.mods.*
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.toBoardItem
import com.osudroid.resources.Res
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skinFrames
import com.reco1l.verktex.ui.Video
import com.osudroid.ui.v2.game.AutoCursor
import com.osudroid.ui.v2.game.BreakAnimator
import com.osudroid.ui.v2.game.ComboBurst
import com.osudroid.ui.v2.game.Countdown
import com.osudroid.ui.v2.game.CursorEntity
import com.osudroid.ui.v2.game.FlashLightEntity
import com.osudroid.ui.v2.game.FollowPointConnection
import com.osudroid.ui.v2.game.GameplayPlayfield
import com.osudroid.ui.v2.skin.JudgementPiece
import com.osudroid.ui.v2.game.PauseMenu
import com.osudroid.ui.v2.game.GameplaySliderTick
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.GameplayState
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard
import com.osudroid.ui.v2.modmenu.ModIcon
import com.osudroid.utils.BitSet
import com.osudroid.utils.DoubleMinHeap
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.calculateRate
import com.osudroid.utils.async
import com.osudroid.utils.runSafe
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.time.FrameStableClock
import com.reco1l.verktex.time.SongClock
import com.reco1l.verktex.time.Stopwatch
import com.reco1l.verktex.ui.AnimatedSprite
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.ScalingContainer
import com.reco1l.verktex.ui.shape.Box
import com.reco1l.verktex.ui.text.Text
import okio.FileNotFoundException
import okio.Path.Companion.toPath
import okio.IOException
import kotlin.coroutines.cancellation.CancellationException
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
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.GameObjectPool
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.scoring.TouchType
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import ru.nsu.ccfit.zuev.skins.OsuSkin
import com.osudroid.utils.currentTimeMillis
import kotlin.concurrent.Volatile
import com.reco1l.verktex.input.KeyboardEvent

/**
 * One attempt at a [LoadedPlay]: its scene, the state of its gameplay and its judgement.
 *
 * [Gameplay] makes one for each attempt, [prepare]s it while loading and [start]s it once the loader is done: its
 * state starts afresh with it, apart from the HUD editor's loop, which plays the attempt again in place.
 */
class GameplaySession(
    /**
     * The play this attempt is of.
     */
    val gameplay: Gameplay,
    private val loaded: LoadedPlay,
    request: PlayRequest
) : GameObjectListener, GameplayState {
    private val maximumActiveCursorCount = 3
    private var cursors = arrayOfNulls<Cursor>(CursorCount)
    private val gameplayClock = FrameStableClock(SongClock(true, true, null))
    private val gameUIScene: GameUIScene
    private lateinit var root: Container
    private lateinit var backgroundLayer: Container
    private lateinit var gameplaySpace: ScalingContainer
    private lateinit var playfield: Container
    private lateinit var bgScene: Container
    private lateinit var objectPlayfield: GameplayPlayfield
    private lateinit var fgScene: Container

    override val leaderboardItems = request.leaderboardItems
    override val isLeaderboardOnline = request.isLeaderboardOnline

    // The replay to watch, a file.
    private val replayPath = request.replay

    private var failAnimation: FailAnimation? = null
    private var pauseMenu: PauseMenu? = null
    private var replayText: Text? = null
    private var sceneBorder: Container? = null
    private var beatmapBackground: Component? = null
    private var beatmapBackgroundWidth = 0f
    private var beatmapBackgroundHeight = 0f

    private val parsedBeatmap
        get() = loaded.parsedBeatmap
    override val playableBeatmap: DroidPlayableBeatmap
        get() = loaded.playableBeatmap
    private val mods
        get() = loaded.mods
    private val objects
        get() = loaded.objects
    private val timingControlPoints
        get() = loaded.timingControlPoints
    private val effectControlPoints
        get() = loaded.effectControlPoints
    private val breakPeriods
        get() = loaded.breakPeriods

    private var timingControlPointIndex = 0
    private var effectControlPointIndex = 0
    private var activeTimingPoint: TimingControlPoint? = null
    private var activeEffectPoint: EffectControlPoint? = null
    private var lastObjectId = -1
    private var leadOut = 0f
    private var objectIndex = 0

    // The object whose follow points are added next, as they start to fade in, see FollowPointConnection.fadeInStartTime.
    private var followPointIndex = 0
    private var postSeekFrameCount = 0
    private var comboColors: ArrayList<Color4>? = null
    private val activeObjects = ArrayList<GameObject>()
    private val expiredObjects = ArrayList<GameObject>()
    // By identity: GameObject doesn't override equals or hashCode.
    private val processedExpiredObjects: MutableSet<GameObject> = HashSet()
    private var judgeableObject: GameObject? = null
    private var breakPeriodIndex = 0
    private var metronome: Metronome? = null
    private var scale = 0f
    override lateinit var stat: StatisticV2

    /**
     * What the gameplay of this attempt depends on, made as it's prepared.
     */
    override var context = GameplayContext()
        private set
    private lateinit var scoreProcessor: ScoreProcessor
    private lateinit var healthProcessor: HealthProcessor
    private var gameStarted = false
    private var initialStartTimeValue = 0f
    private var totalLength = Int.MAX_VALUE
    private var paused = false
    private var skipBtn: Sprite? = null
    private var skipTime = 0f
    private var distToNextObject = 0.0
    private var cursorSprites: Array<CursorEntity>? = null
    private var autoCursor: AutoCursor? = null
    private var flashlightSprite: FlashLightEntity? = null
    private var mainCursorId = -1
    private var replay: Replay? = null
    var replaying = false
        private set
    private var comboBurst: ComboBurst? = null
    private var sliderBorderColor: Color4? = null
    private var sliderIndex = 0
    private var unrankedSprite: Sprite? = null
    private val modIcons = ArrayList<ModIcon>()
    private val rateAdjustingMods = ArrayList<IModApplicableToTrackRate>()

    override val hitWindow: HitWindow
        get() = playableBeatmap.hitWindow

    private var performanceAttributes: PerformanceAttributes? = null
    private var performanceCalculationParameters: PerformanceCalculationParameters? = null

    override var replaySettingsPanel: ReplaySettingsPanel? = null
        private set

    // Game

    /**
     * Whether the game is over.
     */
    private var isGameOver = false

    /**
     * Whether the attempt ended, its results shown, or was left: nothing of it is updated anymore.
     */
    private var isFinished = false

    /**
     * The break time animator.
     */
    private var breakAnimator: BreakAnimator? = null

    /**
     * The countdown animator.
     */
    private var countdownAnimator: Countdown? = null


    // UI

    /**
     * The gameplay HUD, made as the attempt is prepared.
     */
    lateinit var hud: GameplayHUD
        private set

    /**
     * Whether the HUD editor mode is enabled.
     */
    override var isHUDEditorMode = request.isHUDEditor

    /**
     * Whether the game started in HUD editor mode.
     */
    private val startedFromHUDEditor = request.isHUDEditor


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
    override val firstObjectStartTime
        get() = loaded.firstObjectStartTime

    /**
     * The end time of the last object in seconds.
     */
    override val lastObjectEndTime
        get() = loaded.lastObjectEndTime

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
    private var video: Video? = null

    /**Video offset aka video start time in seconds*/
    private var videoOffset = 0f

    /**Whether the video has started*/
    private var videoStarted = false


    // Multiplayer

    /**Indicates the last time that the user pressed the back button, used to reset `backPressCount`*/
    private var lastBackPressTime = -1f

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    var hasFailed = false
        private set

    /**Indicates that the player has requested skip*/
    private var isSkipRequested = false

    /**Real time elapsed in milliseconds since the game has started*/
    private var realTimeElapsed = 0L

    /**Real time elapsed in milliseconds since the latest statistic data was sent*/
    private var statisticDataTimeElapsed = 0L

    /**Last score data chunk sent to server, used to determine if the data was changed.*/
    private var lastScoreSent: ScoreBoardItem? = null


    init {
        gameUIScene = createScene()
    }

    private fun createScene(): GameUIScene {
        root = Container()
        root.width = Dimension.FillAvailable
        root.height = Dimension.FillAvailable
        root.background = Background.Color(Color4.Black)

        backgroundLayer = Container()
        backgroundLayer.width = Dimension.FillAvailable
        backgroundLayer.height = Dimension.FillAvailable

        // Gameplay is laid out in a space 1280 wide, as osu!droid's, scaled to the width of the window.
        gameplaySpace = ScalingContainer()
        gameplaySpace.referenceSize = Vec2(PlayfieldMapping.REFERENCE_WIDTH, 720f)

        // Scaled and moved in it by the playfield size settings.
        playfield = Container()
        playfield.width = Dimension.FillAvailable
        playfield.height = Dimension.FillAvailable
        playfield.scaleOrigin = Anchor.TopLeft

        bgScene = createLayer()
        fgScene = createLayer()

        // Hit objects are in osu!pixels, in the playfield, over the spinners and below the cursors.
        objectPlayfield = GameplayPlayfield()
        // Objects can be loaded before the first layout, the space is as wide as the window in proportion.
        val window = Verktex.graphics.window
        objectPlayfield.setSpaceSize(
            PlayfieldMapping.REFERENCE_WIDTH,
            PlayfieldMapping.REFERENCE_WIDTH * window.windowHeight / window.windowWidth.coerceAtLeast(1)
        )

        playfield += bgScene
        playfield += objectPlayfield
        playfield += fgScene

        gameplaySpace += playfield

        root += backgroundLayer
        root += gameplaySpace

        // The background of a scene isn't one of its children, so it doesn't inherit the scene's clock. The
        // timelines of the hit objects and the HUD are on the gameplay clock.
        root.clock = gameplayClock

        val scene = GameUIScene()
        scene.backgroundComponent = root
        return scene
    }

    private fun createLayer(): Container {
        val layer = Container()
        layer.width = Dimension.FillAvailable
        layer.height = Dimension.FillAvailable
        return layer
    }

    private fun loadBackground() {
        if (sceneBorder != null) {
            sceneBorder!!.removeSelf()
            sceneBorder = null
        }

        releaseVideo()

        val textureRegion = if (Settings[OsuSettings.SafeBeatmapBackground] || playableBeatmap.events.backgroundFilename == null)
            SkinTextures.get("menu-background")
        else
            Textures.get("::background")

        if (textureRegion == null) {
            val backgroundColor = playableBeatmap.events.backgroundColor

            val box = Box()
            box.width = Dimension.FillAvailable
            box.height = Dimension.FillAvailable
            box.color = backgroundColor ?: Color4.Black

            beatmapBackground = box
            beatmapBackgroundWidth = 0f
            beatmapBackgroundHeight = 0f
        } else {
            beatmapBackground = Sprite(textureRegion)
            beatmapBackgroundWidth = textureRegion.width.toFloat()
            beatmapBackgroundHeight = textureRegion.height.toFloat()
        }
    }

    /**
     * Loads the video of the beatmap, or releases it, as the settings changed.
     */
    fun loadVideo() {
        val beatmapInfo = loaded.beatmapInfo

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        val brightness = OsuSettings.backgroundBrightness
        val videoFilename = playableBeatmap.events.videoFilename
        // Some platforms can't play videos, e.g. Vulkan before Android 10 can't show their frames.
        val videosSupported = Platform.canPlayVideos
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
        val videoFile = (beatmapInfo.absoluteSetDirectory + "/" + videoFilename).toPath()

        if (!Platform.fileSystem.exists(videoFile)) {
            return
        }

        videoLoadingJob = async {
            try {
                val video = Video(beatmapInfo.absoluteSetDirectory + "/" + videoFilename)
                if (!video.isAvailable) {
                    return@async
                }
                video.alpha = 0f

                ensureActive()

                this@GameplaySession.video = video

                // applyBackground is called from the onReady callback once the decoder reports the video
                // dimensions, rather than blocking here.
                // If dimensions are already known (fast local file), the callback fires immediately.
                video.setOnReady { updateThread { applyBackground() } }
            } catch (e: Exception) {
                video = null
                Logger.e(TAG, "Error while loading video background.", e)
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

    internal fun releaseVideo() {
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
            sceneBorder = Container().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                border = Border(Dimension.Fixed.Pixels(5f), Color4.White, Color4.White)
                alpha = Interpolation.linear(0.2f, 0.8f, brightness)
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
            }

            playfield.add(0, sceneBorder)
            this.sceneBorder = sceneBorder
        }

        val showVideo = videoEnabled && video != null
        val background: Component? = if (showVideo) video else beatmapBackground

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

        // Dimmed by fading it over the black of the root rather than covering it with black, which
        // costs a fullscreen layer to the GPU, and a fully dimmed background isn't drawn at all.
        backgroundLayer.alpha = brightness

        if (breakAnimator != null) {
            breakAnimator!!.setDimBrightness(brightness)
            breakAnimator!!.setDimmedLayer(backgroundLayer)
        }

        // Storyboards are drawn here once they're available again, on top of the background and below the playfield.
    }

    /**
     * Prepares this attempt while the loader is shown: its background, its music, its clock and its replay.
     *
     * @param isNewBeatmap Whether the beatmap isn't the one of the attempt before, whose skin is loaded then.
     * @return Whether the attempt can be played.
     */
    fun prepare(scope: CoroutineScope, isNewBeatmap: Boolean): Boolean {
        if (isHUDEditorMode || OsuSkin.get().hudSkinData.hasElement(HUDElements.pp_counter.name)) {
            when (OsuSettings.difficultyAlgorithm) {
                DifficultyAlgorithm.droid -> {
                    performanceAttributes = DroidPerformanceAttributes()
                    performanceCalculationParameters = DroidPerformanceCalculationParameters()
                }

                DifficultyAlgorithm.standard -> {
                    performanceAttributes = StandardPerformanceAttributes()
                    performanceCalculationParameters = StandardPerformanceCalculationParameters()
                }
            }
        }

        // The difficulty at each object may be calculated while playing: the pp so far is shown then.
        loaded.timedDifficulty.onCalculated = {
            if (gameplay.session === this && objectIndex > 0) {
                updatePPValue(objectIndex - 1)
            }
        }

        return prepareAttempt(scope, replayPath, isNewBeatmap)
    }

    // Sets up the background, the music, the clock and the replay of the attempt, and the state of its gameplay as it
    // starts: the HUD editor plays the attempt again in place with it.
    private fun prepareAttempt(scope: CoroutineScope?, replayPath: String?, loadBeatmapSkin: Boolean): Boolean {

        // Load backgrounds early to minimize waiting time.
        loadBackground()
        loadVideo()

        rateAdjustingMods.clear()

        for (mod in mods.values) {
            if (mod is IModApplicableToTrackRate) {
                rateAdjustingMods.add(mod)
            }
        }

        // TODO skin manager
        if (loadBeatmapSkin) {
            BeatmapSkinManager.getInstance().loadBeatmapSkin(playableBeatmap.beatmapsetPath)
        }

        val hitObjects = playableBeatmap.hitObjects.objects
        val firstObject = hitObjects[0]
        scale = firstObject.gameplayScale

        val initialSpeedMultiplier = mods.values.calculateRate(Double.NEGATIVE_INFINITY)

        scope?.ensureActive()

        OsuGame.music.preLoad(loaded.audioPath, initialSpeedMultiplier,
            initialSpeedMultiplier != 1f &&
                (Settings[OsuSettings.ShiftPitchInRateChange] || ModNightCore::class in mods || ModOldNightCore::class in mods))

        gameplayClock.rate = initialSpeedMultiplier

        scope?.ensureActive()

        totalLength = OsuGame.music.length
        judgeableObject = null
        breakPeriodIndex = 0
        objectIndex = 0
        followPointIndex = 0
        lastObjectId = -1
        videoStarted = false
        videoOffset = playableBeatmap.events.videoStartTime / 1000f

        val estimatedMaxActiveObjects = max(10, estimateMaximumActiveObjects())

        activeObjects.clear()
        activeObjects.ensureCapacity(estimatedMaxActiveObjects)
        expiredObjects.clear()
        expiredObjects.ensureCapacity(estimatedMaxActiveObjects)

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

        activeTimingPoint = timingControlPoints.firstOrNull() ?: playableBeatmap.controlPoints.timing.defaultControlPoint
        activeEffectPoint = effectControlPoints.firstOrNull() ?: playableBeatmap.controlPoints.effect.defaultControlPoint
        timingControlPointIndex = 0
        effectControlPointIndex = 0

        GameObjectPool.getInstance().purge()

        scope?.ensureActive()

        FollowPointConnection.renew(16)
        GameplaySliderTick.renew(16)

        // TODO replay
        replaying = false
        var replay: Replay? = Replay(true)
        this.replay = replay
        replay!!.setObjectCount(hitObjects.size)
        replay.setBeatmap(loaded.beatmapInfo.fullBeatmapsetName, loaded.beatmapInfo.fullBeatmapName, playableBeatmap.md5)

        if (replayPath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            val replayStat = StatisticV2()
            replayStat.mod = mods
            replay.setStat(replayStat)

            scope?.ensureActive()

            replaying = replay.load(replayPath, true)

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

        } else if (ModAutoplay::class in mods) {
            replay = null
            this.replay = null
        }

        scope?.ensureActive()

        val context = GameplayContext(
            mods = mods,
            healthDrain = playableBeatmap.difficulty.hp,
            originalTimePreempt = BeatmapDifficulty.difficultyRange(
                playableBeatmap.difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
            ).toFloat(),
            samplesMatchPlaybackRate = playableBeatmap.general.samplesMatchPlaybackRate,
            replayVersion = if (replaying) replay!!.replayVersion else 0
        )

        context.timing.speedMultiplier = initialSpeedMultiplier
        context.timing.beatLength = activeTimingPoint!!.msPerBeat / 1000
        context.timing.isKiai = activeEffectPoint!!.isKiai
        context.timing.currentBeatTime = 0.0
        this.context = context

        SkinTextures.preloadGameplay()
        GameObjectPool.getInstance().preload()

        sliderIndex = 0

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

    override val scene: Scene
        get() = gameUIScene

    override val replayId: Int
        get() = gameplay.scoring.replayID

    /**
     * Takes the job loading the video, to stop it with the loading of the play.
     */
    fun takeVideoLoadingJob(): Job? {
        val job = videoLoadingJob
        videoLoadingJob = null
        return job
    }

    /**
     * Makes the statistics, the cursors, the HUD and the rest of the scene of this attempt, once it's [prepare]d. It's
     * ready to start then, or once every player is in multiplayer.
     */
    fun prepareScene() {
        val lastMods = mods

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
        stat.healthDrain = context.healthDrain
        stat.beatmapNoteCount = objects.size
        stat.beatmapMaxCombo = parsedBeatmap.maxCombo

        val cursorCount = if (replaying && replay != null) replay!!.cursorMoves.size else CursorCount

        cursors = arrayOfNulls(cursorCount)

        for (i in 0 until cursorCount) {
            cursors[i] = Cursor()
        }

        scoreProcessor = ScoreProcessor(stat)
        healthProcessor = HealthProcessor(stat, playableBeatmap.difficulty.hp, if (context.isEasy) EasyLives else 0)
        previousFrameTime = 0

        metronome = null
        if ((OsuSettings.metronomeSwitch == 1 && context.isNightCore)
            || (context.isMuted && context.muted!!.enableMetronome)
            || OsuSettings.metronomeSwitch == 2) {
            metronome = Metronome()
        }

        distToNextObject = 0.0

        // TODO passive objects
        if ((replaying || Settings[OsuSettings.ShowCursor]) && !context.isAutoplay && !context.isAutopilot) {
            val cursorSprites = Array(cursorCount) { CursorEntity() }
            this.cursorSprites = cursorSprites
            for (i in 0 until cursorCount) {
                cursorSprites[i].attachToScene(fgScene)
            }
        } else {
            cursorSprites = null
        }

        if (context.isAutoplay || context.isAutopilot) {
            // From the center of the playfield.
            val start = objectPlayfield.mapping.toResolution(PlayfieldMapping.PLAYFIELD_WIDTH / 2, PlayfieldMapping.PLAYFIELD_HEIGHT / 2)
            autoCursor = AutoCursor(start.x, start.y)
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
            comboBurst = ComboBurst(PlayfieldMapping.REFERENCE_WIDTH)
            comboBurst!!.attachAll(bgScene)
        }

        // From the top right corner of the layer, to the left.
        val position = Vector2(-130, 130)

        for (mod in lastMods.values) {
            if (!mod.isUserPlayable) {
                continue
            }

            val icon = ModIcon(mod)
            icon.anchor = Anchor.TopRight
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
            val unrankedSprite = Sprite(SkinTextures.get("play-unranked"))
            this.unrankedSprite = unrankedSprite
            unrankedSprite.anchor = Anchor.TopCenter
            unrankedSprite.origin = Anchor.Center
            unrankedSprite.setPosition(0f, 80f)
            fgScene += unrankedSprite
        }

        if (context.isFlashlight) {
            val flashlightSprite = FlashLightEntity(context.flashlight!!)
            this.flashlightSprite = flashlightSprite
            fgScene.add(0, flashlightSprite)
        }

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        val hud = GameplayHUD(this)
        this.hud = hud

        hud.setEditMode(isHUDEditorMode)
        hud.setSkinData(OsuSkin.get().hudSkinData)

        skipBtn = null
        if (skipTime > 1) {
            val paddingBottom = if (Multiplayer.isConnected) Multiplayer.roomScene!!.chat.buttonHeight else 0f

            val skipBtn = AnimatedSprite(skinFrames("play-skip", orStatic = true), 60f)
            this.skipBtn = skipBtn
            skipBtn.anchor = Anchor.BottomRight
            skipBtn.origin = Anchor.BottomRight
            // The chat's height is in window pixels, the HUD's are scaled.
            skipBtn.y = (-paddingBottom / hud.factor).px
            skipBtn.alpha = 0.7f
            hud += skipBtn
        }

        var playname = OsuSettings.onlineUsername

        if (context.isAutoplay || replaying) {
            val metadata = playableBeatmap.metadata
            playname = if (replaying) gameplay.scoring.replayStat!!.playerName else "osu!"

            if (!Settings[OsuSettings.HideReplayMarquee]) {
                val replayText = Text()
                this.replayText = replayText
                replayText.text = "Watching " + playname + " play " + metadata.artist + " - " + metadata.title + " [" + metadata.version + "]"
                replayText.setPosition(GameplayHUD.REFERENCE_WIDTH + 5f, 140f)
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

        if (!startedFromHUDEditor && (context.isAutoplay || replaying)) {
            val replaySettingsPanel = ReplaySettingsPanel()
            this.replaySettingsPanel = replaySettingsPanel
            // Designed in dp, so it's not in the scaled HUD, it's added with it in start().

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
                if (!breakAnimator.isBreak()) {
                    backgroundLayer.alpha = brightness
                }
            }

            seekControl.updateSeekPosition(
                gameplayClock.time,
                (objects[0].startTime / 1000).toFloat(),
                (objects[objects.size - 1].endTime / 1000).toFloat()
            )

            seekControl.onSeek = { targetTime ->
                updateThread { seekTo(targetTime) }
            }
        }

        if (Multiplayer.isMultiplayer) {
            RoomAPI.notifyBeatmapLoaded()
        } else {
            gameplay.isReadyToStart = true
        }
    }

    /**
     * Starts gameplay. This is used by the game loader once all necessary preprocessing is done.
     *
     * @param beatmapOffset The offset of the beatmap set, in milliseconds, from its options.
     */
    fun start(beatmapOffset: Int) {

        val music = OsuGame.music

        // Hook up the framed beatmap clock as the scene's clock. Use MusicService as the audio source.
        if (music != null) {
            gameplayClock.changeSource(MusicClock(music))
        }

        // Ensure user-defined offset has time to be applied.
        gameplayClock.userGlobalOffset = OsuSettings.offset / 1000
        gameplayClock.userBeatmapOffset = beatmapOffset / 1000f

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

        if (!isHUDEditorMode && !replaying && !context.isAutoplay && !context.isAutopilot) {
            Input.dispatchHistoricalMoves = true
        }

        root += hud
        replaySettingsPanel?.let { root += it }
        Gameplay.replaceCurrentScene(gameUIScene)

        if (isHUDEditorMode) {
            Toasts.show(Res.string.hudEditor_back_for_menu)
        }
    }

    fun getComboColor(hitObject: HitObject): Color4 {
        if (context.isSynesthesia) {
            return ModSynesthesia.getColorFor(playableBeatmap.controlPoints.getClosestBeatDivisor(hitObject.startTime))
        }

        return comboColors!![hitObject.comboIndexWithOffsets % comboColors!!.size]
    }

    private fun update(dt: Float) {
        if (!gameplay.isReadyToStart || isFinished) {
            return
        }

        previousFrameTime = Platform.uptimeMillis

        if (Multiplayer.isMultiplayer) {
            val mSecElapsed = (dt / context.timing.speedMultiplier * 1000).toLong()
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

            if (currentSpeedMultiplier != context.timing.speedMultiplier) {
                context.timing.speedMultiplier = currentSpeedMultiplier
                gameplayClock.rate = currentSpeedMultiplier

                if (videoEnabled && video != null) {
                    video!!.setPlaybackSpeed(currentSpeedMultiplier)
                }

                val music = OsuGame.music
                music.speed = modRate
                music.setPitchRate(replaySettingsRate)
            }
        }


        replaySettingsPanel?.playbackControl?.seekControl?.updateSeekPosition(
            gameplayClock.time,
            (objects[0].startTime / 1000).toFloat(),
            (objects[objects.size - 1].endTime / 1000).toFloat()
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
                    event.setTrackPosition(movement.x, movement.y, objectPlayfield.mapping, context.isHardRock)

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
                    event.setTrackPosition(
                        lastMovement.x * t + movement.x * (1 - t),
                        lastMovement.y * t + movement.y * (1 - t),
                        objectPlayfield.mapping,
                        context.isHardRock
                    )

                    cursors[i]!!.addEvent(event)
                }
            }
        }

        if (context.isAutoplay || context.isAutopilot) {
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

                sprite.setPressed(cursor.getLatestNonUpEvent() != null)
            }
        }

        if (context.isFlashlight) {
            val flashlightSprite = flashlightSprite!!

            if (!context.isAutoplay && !context.isAutopilot) {
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

        while (timingControlPointIndex + 1 < timingControlPoints.size) {
            val nextTimingPoint = timingControlPoints[timingControlPointIndex + 1]

            if (nextTimingPoint.time > mSecPassed) {
                break
            }

            activeTimingPoint = nextTimingPoint
            ++timingControlPointIndex
        }

        while (effectControlPointIndex + 1 < effectControlPoints.size) {
            val nextEffectPoint = effectControlPoints[effectControlPointIndex + 1]

            if (nextEffectPoint.time > mSecPassed) {
                break
            }

            activeEffectPoint = nextEffectPoint
            ++effectControlPointIndex
        }

        val activeTimingPoint = activeTimingPoint!!

        val timing = context.timing
        timing.beatLength = activeTimingPoint.msPerBeat / 1000
        timing.isKiai = activeEffectPoint!!.isKiai
        timing.currentBeatTime = max(0.0, gameplayClock.time - activeTimingPoint.time / 1000) % timing.beatLength

        if (!isGameOver) {
            val breakAnimator = breakAnimator!!

            if (breakPeriodIndex < breakPeriods.size) {
                if (!breakAnimator.isBreak() && breakPeriods[breakPeriodIndex].startTime / 1000 <= gameplayClock.time) {
                    val period = breakPeriods[breakPeriodIndex++]

                    gameStarted = false
                    breakAnimator.init(period.duration / 1000)
                    if (context.isFlashlight) {
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

                if (context.isFlashlight) {
                    flashlightSprite!!.onBreak(false)
                }
            }
        }

        val expiredObjects = expiredObjects

        if (objectIndex >= objects.size && areObjectsJudged(activeObjects) && context.isFlashlight) {
            flashlightSprite!!.onBreak(true)
        }

        if (gameStarted && healthProcessor.drain(dt, distToNextObject)) {
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

        if (judgeableObject != null && (context.isAutoplay || context.isAutopilot)) {
            autoCursor!!.moveToObject(judgeableObject, gameplayClock.time, this)
        }

        if (videoEnabled && video != null && gameplayClock.time >= videoOffset) {
            val video = video!!

            if (!videoStarted) {
                video.play()
                video.setPlaybackSpeed(context.timing.speedMultiplier)
                videoStarted = true
            }

            if (video.alpha < 1.0f)
                video.alpha = min(video.alpha + 0.03f, 1.0f)
        }

        // Follow points are added as they start to fade in, apart from the objects: that can be before the object
        // they start from spawns.
        while (followPointIndex < objects.size - 1) {
            val start = objects[followPointIndex]

            if (gameplayClock.time < FollowPointConnection.fadeInStartTime(start)) {
                break
            }

            val end = objects[++followPointIndex]

            // A seek went past them.
            if (gameplayClock.time >= end.startTime.toFloat() / 1000) {
                continue
            }

            if (start !is Spinner && end !is Spinner && !start.isLastInCombo) {
                FollowPointConnection.addConnection(objectPlayfield.underlay, start, end)
            }
        }

        var shouldBePunished = false

        while (objectIndex < objects.size) {
            val obj = objects[objectIndex]

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

                gameplayCircle.init(this, objectPlayfield, obj, comboColor)
                addObject(gameplayCircle)

                if (context.isAutoplay) {
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

                gameplaySpinner.init(this, bgScene, objectPlayfield, obj, rps, stat)
                addObject(gameplaySpinner)

                if (context.isAutoplay || context.isAutopilot) {
                    gameplaySpinner.setAutoPlay()
                }

                gameplaySpinner.setId(++lastObjectId)
                if (replaying) {
                    gameplaySpinner.setReplayData(replay!!.objectData!![gameplaySpinner.getId()])
                }

                gameObject = gameplaySpinner
            } else if (obj is Slider) {
                val gameplaySlider = GameObjectPool.getInstance().slider

                gameplaySlider.init(this, objectPlayfield, stat, obj, playableBeatmap.controlPoints,
                    comboColor, sliderBorderColor!!, getSliderPath(sliderIndex), getSliderRenderPath(sliderIndex))

                ++sliderIndex
                addObject(gameplaySlider)

                if (context.isAutoplay) {
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

            gameObject?.updateJudgement(lifetimeDt)
        }

        if (context.isPerfect && stat.hit100 + stat.hit50 + stat.misses > 0) {
            gameover()

            if (!Multiplayer.isMultiplayer) {
                gameplay.retry()
            }
        }

        val mutedMod = context.muted

        // 节拍器
        if (metronome != null) {
            metronome!!.update(gameplayClock.time, activeTimingPoint)

            if (mutedMod != null) {
                metronome!!.setVolume(1 - mutedMod.volumeAt(stat.combo))
            }
        }

        if (gameplayClock.time >= 0 && mutedMod != null) {
            OsuGame.music.volume =
                OsuSettings.musicVolume * mutedMod.volumeAt(stat.combo)
        }

        if (shouldBePunished || (!isGameOver && objectIndex >= objects.size && areObjectsJudged(activeObjects) && leadOut > 2)) {

            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                gameplayClock.seek(initialStartTimeValue)
                prepareAttempt(null, null, false)
                applyBackground()
                stat.reset()
                skip(true)
                return
            }

            finish()
        } else if (objectIndex >= objects.size && areObjectsJudged(activeObjects)) {
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
                        objectPlayfield.mapping.width, objectPlayfield.mapping.height) < 250 * 250) {
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

    // Ends the attempt once its objects are judged: its replay is saved and its results are shown.
    private fun finish() {
        isFinished = true

        BeatmapSkinManager.setSkinEnabled(false)
        GameObjectPool.getInstance().purge()
        activeObjects.clear()
        expiredObjects.clear()

        var replayPath: String? = null
        stat.time = currentTimeMillis()
        if (replay != null && !replaying) {
            val ctime = currentTimeMillis().toString()
            replayPath = GameFolders.corePath + "Scores/" +
                MD5Calculator.getStringMD5(loaded.beatmapInfo.filename + ctime) +
                ctime.substring(0, min(3, ctime.length)) + ".odr"
            replay!!.setStat(stat)
            replay!!.save(replayPath)
        }
        resetPlayfieldSizeScale()
        cancelVideoLoading()

        val scoringScene = gameplay.scoring
        if (!startedFromHUDEditor) {
            if (replaying)
                scoringScene.load(scoringScene.replayStat!!, null, OsuGame.music, replayPath, null, loaded.beatmapInfo)
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
                scoringScene.load(stat, loaded.beatmapInfo, OsuGame.music, replayPath, parsedBeatmap.md5, null)
            }
            OsuGame.music.volume = 0.2f
            Gameplay.replaceCurrentScene(scoringScene.scene!!)
        } else {
            Gameplay.returnToPreviousScene()
        }

        // The next attempt loads the beatmap again.
        gameplay.onFinished()

        // Resume difficulty calculation.
        DifficultyCalculationManager.calculateDifficulties()
        Input.dispatchHistoricalMoves = false

        releaseVideo()
    }

    private fun updateActiveObjects(deltaTime: Float) {
        judgeableObject = searchJudgeableObject(0)

        for (i in 0 until activeObjects.size) {
            val obj = activeObjects[i]
            obj.updateJudgement(deltaTime)

            // Advance to the next judgeable object if the current judgeable object is judged.
            // In remove slider lock mode, do this as soon as the current judgeable object is hit instead.
            if (isObjectHittable(obj) && (obj.isJudged() || (Multiplayer.isRemoveSliderLock() && obj.isStartHit()))) {
                judgeableObject = searchJudgeableObject(i + 1)
            }

            if (gameplayClock.time >= obj.lifetimeEnd) {
                expiredObjects.add(obj)
            }
        }
    }

    private fun updatePassiveObjects(deltaTime: Float) {

        hud.onGameplayUpdate(this, deltaTime)

        breakAnimator!!.update(deltaTime)

        countdownAnimator?.update(deltaTime)
    }

    // Whether every active object got its final judgement, the ones left only showing their effects.
    private fun areObjectsJudged(activeObjects: List<GameObject>): Boolean {
        for (i in activeObjects.indices) {
            if (!activeObjects[i].hasFinalJudgement()) {
                return false
            }
        }
        return true
    }

    private fun searchJudgeableObject(startIndex: Int): GameObject? {

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

    /**
     * Leaves the play, see [Gameplay.quit].
     */
    fun quit() {
        gameplay.quit()
    }

    /**
     * Ends this attempt as the play is left: its sounds and its video stop, and the music goes back to the preview of
     * the selected beatmap.
     */
    internal fun exit() {
        isFinished = true

        if (sceneBorder != null) {
            sceneBorder!!.removeSelf()
            sceneBorder = null
        }

        stopLoopingSamples()
        activeObjects.clear()
        expiredObjects.clear()
        if (::hud.isInitialized) {
            hud.removeSelf()
        }
        replaySettingsPanel?.removeSelf()

        cancelVideoLoading()

        val mSecPassed = gameplayClock.time * 1000
        val selectedBeatmap = BeatmapSelection.beatmap
        val music = OsuGame.music

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

        resetPlayfieldSizeScale()
    }

    /**
     * Lets this attempt go as another one replaces it, from the pause menu: nothing of it plays anymore.
     */
    internal fun release() {
        isFinished = true

        stopLoopingSamples()
        cancelVideoLoading()
        releaseVideo()
        gameplayClock.changeSource(null)
    }

    //CB打击处理
    private fun registerHit(objectId: Int, score: Int, endCombo: Boolean): String {
        return registerHit(objectId, score, endCombo, true)
    }

    private fun registerHit(objectId: Int, score: Int, endCombo: Boolean, incrementCombo: Boolean): String {
        return registerHit(objectId, JudgementResult.fromScore(score), endCombo, incrementCombo)
    }

    private fun registerHit(objectId: Int, result: JudgementResult, endCombo: Boolean, incrementCombo: Boolean = true,
                            sliderTail: Boolean = false): String {

        if (isGameOver) {
            return "hit0"
        }

        if (result == JudgementResult.Miss) {
            playComboBreak()
        }

        val scoreName = if (sliderTail)
            scoreProcessor.applySliderTail(result, endCombo, incrementCombo)
        else
            scoreProcessor.applyObject(result, endCombo, incrementCombo)

        if (objectId != -1 && replay != null && !replaying) {
            replay!!.addObjectScore(objectId, result.resultType)
        }

        if (result == JudgementResult.Miss && context.isSuddenDeath) {
            stat.changeHp(-1.0f)
            gameover()
        }

        if (objectId != -1) {
            updatePPValue(objectId)
        }

        return scoreName
    }

    private fun playComboBreak() {
        if (stat.combo > 30) {
            SkinSounds.getBeatmap("combobreak", 1)?.play()
        }
    }


    override fun onCircleHit(id: Int, accuracy: Float, pos: Vector2, endCombo: Boolean, forcedScore: Byte, color: Color4): String? {
        val acc = accuracy

        if (context.isAutoplay) {
            autoCursor!!.click()
            hud.onGameplayTouchDown(parsedBeatmap.hitObjects.objects[id].startTime.toFloat() / 1000)
        }

        val accuracy = abs(acc)
        val writeReplay = replay != null && !replaying
        if (writeReplay) {
            val sacc = (acc * 1000).toInt().toShort()
            replay!!.addObjectResult(id, sacc, null)
        }

        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        val isCursorTrackingActive = !context.isAutoplay && !context.isAutopilot
        val nearestCursorId = if (isCursorTrackingActive) getNearestCursorId(pos.x, pos.y) else -1

        if (context.isFlashlight && isCursorTrackingActive) {
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId
                val latestNonUpEvent = cursors[mainCursorId]!!.getLatestNonUpEvent()

                if (latestNonUpEvent != null) {
                    flashlightSprite!!.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y)
                }
            }
        }
        GameplayVibrations.circle()

        val result = HitJudging.judge(accuracy, playableBeatmap.hitWindow, forcedScore)

        if (result == JudgementResult.Miss) {
            registerHit(id, result, endCombo)
            return "hit0"
        }

        val scoreName = registerHit(id, result, endCombo)

        if (nearestCursorId >= 0) {
            val aimErrorCursorEvent = cursors[nearestCursorId]!!.getLatestNonUpEvent()

            if (aimErrorCursorEvent != null) {
                val objectRadius = parsedBeatmap.hitObjects.objects[id].gameplayRadius.toFloat()
                hud.onAimJudgement(pos, aimErrorCursorEvent.playfieldPosition, objectRadius)
            }
        }

        hud.onNoteHit(stat)
        return scoreName
    }

    override fun onSliderHit(id: Int, score: Int, judgementPos: Vector2, endCombo: Boolean,
                             color: Color4, type: Int, incrementCombo: Boolean): String? {

        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        val isCursorTrackingActive = !context.isAutoplay && !context.isAutopilot
        val isFlashlightActive = context.isFlashlight && isCursorTrackingActive
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
            registerHit(id, 0, endCombo)
            return "hit0"
        }

        // Nested object was missed.
        if (score == -1) {
            playComboBreak()

            if (context.isSuddenDeath) {
                stat.changeHp(-1.0f)
                gameover()
            }
            scoreProcessor.applySliderPartMiss()
            return null
        }

        var scoreName = "hit0"

        when (type) {
            GameObjectListener.SLIDER_START -> {
                if (incrementCombo) {
                    scoreName = scoreProcessor.applySliderPart(SliderPart.Head)

                    if (nearestCursorId >= 0) {
                        val aimErrorCursorEvent = cursors[nearestCursorId]!!.getLatestNonUpEvent()

                        if (aimErrorCursorEvent != null) {
                            val objectRadius = parsedBeatmap.hitObjects.objects[id].gameplayRadius.toFloat()
                            hud.onAimJudgement(judgementPos, aimErrorCursorEvent.playfieldPosition, objectRadius)
                        }
                    }

                    if (context.isAutoplay) {
                        hud.onGameplayTouchDown(parsedBeatmap.hitObjects.objects[id].startTime.toFloat() / 1000)
                    }
                }
            }

            GameObjectListener.SLIDER_REPEAT -> {
                if (incrementCombo) {
                    scoreName = scoreProcessor.applySliderPart(SliderPart.Repeat)
                }
            }

            GameObjectListener.SLIDER_TICK -> {
                if (incrementCombo) {
                    scoreName = scoreProcessor.applySliderPart(SliderPart.Tick)
                }
            }

            GameObjectListener.SLIDER_END -> {
                // The slider end hit is registered before updatePPValue, which reads the slider end hit count.
                scoreName = registerHit(id, JudgementResult.fromScore(score), endCombo, incrementCombo, sliderTail = true)
            }
        }

        hud.onNoteHit(stat)
        return scoreName
    }

    override fun onSpinnerStart(id: Int) {
        if (context.isAutoplay) {
            autoCursor!!.click()
            hud.onGameplayTouchDown(parsedBeatmap.hitObjects.objects[id].startTime.toFloat() / 1000)
        }
    }

    override fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int): String? {
        if (score == 1000) {
            scoreProcessor.applySpinnerBonus()
            return null
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

        GameplayVibrations.spinner()

        if (score == 0) {
            registerHit(id, 0, endCombo)
            return "hit0"
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
                hitWindow.greatWindow + 1
            }

            50 -> {
                scoreName = registerHit(id, 50, endCombo)
                hitWindow.okWindow + 1
            }

            else -> {
                scoreName = "hit0"
                hitWindow.mehWindow + 1
            }
        }.toFloat() / 1000

        hud.onAccuracyRegister(HitObjectType.Spinner, accuracy)
        hud.onNoteHit(stat)
        return scoreName
    }

    override fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?) {
        var volume = 1f
        val muted = context.muted

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
        for (i in 0 until activeObjects.size) {
            activeObjects[i].playLoopingSamples()
        }
    }

    private fun stopLoopingSamples() {
        for (i in 0 until activeObjects.size) {
            activeObjects[i].stopLoopingSamples()
        }
    }

    override fun addObject(`object`: GameObject?) {
        activeObjects.add(`object`!!)
    }

    override fun isObjectHittable(`object`: GameObject?): Boolean {
        return `object` === judgeableObject
    }

    override fun getCursor(index: Int): Cursor {
        return cursors[index]!!
    }

    private fun onPointerEvent(event: PointerEvent): Boolean {
        val offset = if (previousFrameTime > 0)
            (event.time - previousFrameTime) * context.timing.speedMultiplier
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

        val sprite = if (!context.isAutoplay && !context.isAutopilot && cursorSprites != null)
            cursorSprites!![id]
        else
            null

        val position = playfield.toLocalSpaceFromScene(event.x, event.y)
        val cursorEvent = CursorEvent.obtain(event, position.x, position.y, objectPlayfield.mapping, context.isHardRock)

        cursorEvent.trackTime = gameplayClock.time * 1000
        cursorEvent.offset = offset.toDouble()

        sprite?.setPosition(cursorEvent.position.x, cursorEvent.position.y)

        if (action == PointerEvent.Action.Down) {

            sprite?.setShowing(true)

            if (!context.isAutoplay) {
                hud.onGameplayTouchDown(eventTime / 1000f)
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
            (currentTime - previousFrameTime) * context.timing.speedMultiplier
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

    override fun pause() {

        if (paused) {
            return
        }

        if (isHUDEditorMode) {
            hud.onBackPress()
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
            OsuGame.music.setFrequencyForcefully(101f)
            return
        }

        if (video != null && videoStarted) {
            video!!.pause()
        }

        stopLoopingSamples()

        if (!context.isAutoplay && !context.isAutopilot && !replaying) {
            removeAllCursors()
        }

        gameplayClock.pause()
        paused = true

        gameUIScene.ignoreUpdate = true
        hud.ignoreUpdate = true

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
        val music = OsuGame.music

        if (context.isPerfect) {
            video?.pause()

            gameplayClock.pause()
            paused = true
            gameUIScene.ignoreUpdate = true
            hud.ignoreUpdate = true
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

        // The layers of the playfield are in osu!pixels: their objects move as they would in the space gameplay is
        // laid out in, with the mapping of the playfield.
        private fun applyEffectToLayer(layer: Container, mapping: PlayfieldMapping? = null) {
            if (layer.alpha > 0) {
                layer.alpha = max(0f, layer.alpha - 0.007f)
            }

            for (i in 0 until layer.childCount) {
                val entity = layer[i]

                // Hit objects fill the layer, their pieces fall as the layer's own children do.
                if (entity is GameObject) {
                    applyEffectToPieces(entity, mapping)
                    continue
                }

                applyEffectToEntity(entity, mapping)
            }
        }

        private fun applyEffectToPieces(gameObject: GameObject, mapping: PlayfieldMapping?) {
            for (i in 0 until gameObject.childCount) {
                applyEffectToEntity(gameObject[i], mapping)
            }
        }

        private fun applyEffectToEntity(entity: Component, mapping: PlayfieldMapping?) {
            val y = entity.y.resolve(0f)
            val resolutionY = if (mapping != null) y * mapping.scaleY + mapping.offsetY else y
            val newResolutionY = if (resolutionY < 0f) resolutionY * 0.6f else resolutionY * 1.01f
            val newY = if (mapping != null) (newResolutionY - mapping.offsetY) / mapping.scaleY else newResolutionY

            entity.setPosition(entity.x.resolve(0f), newY)

            if (entity.rotationZ == 0f) {
                entity.rotationZ = entity.rotationZ + Random.Default.nextDouble(-0.02, 0.02).toFloat() * 180 / MathUtils.PI_F
            } else if (entity.rotationZ > 0) {
                entity.rotationZ = entity.rotationZ + 0.01f * 180 / MathUtils.PI_F
            } else {
                entity.rotationZ = entity.rotationZ - 0.01f * 180 / MathUtils.PI_F
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
                applyEffectToLayer(objectPlayfield.objects, objectPlayfield.mapping)
                applyEffectToLayer(objectPlayfield.judgements, objectPlayfield.mapping)
                applyEffectToLayer(objectPlayfield.underlay, objectPlayfield.mapping)
                applyEffectToLayer(bgScene)

                val decreasedFrequency = max(101f, music.frequency - 300)
                val decreasedSpeed = context.timing.speedMultiplier * (1 - (initialFrequency - decreasedFrequency) / initialFrequency)

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
                hud.ignoreUpdate = true

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
        hud.ignoreUpdate = false

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
        val mehWindow = hitWindow.mehWindow / 1000

        if (abs(acc) <= mehWindow) {
            if (type != HitObjectType.Spinner) {
                stat.addHitOffset(acc)
            }

            if (replaying) {
                gameplay.scoring.replayStat!!.addHitOffset(acc)
            }
        }

        hud.onAccuracyRegister(type!!, acc.toFloat())
    }


    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?) {
        onTrackingSliders(false)
        if (context.isAutoplay) {
            autoCursor!!.onSliderEnd()
        }
        if (replay != null && !replaying) {
            val acc = accuracy.toShort()
            replay!!.addObjectResult(id, acc, tickSet!!.copy())
        }
    }

    override fun onTrackingSliders(isTrackingSliders: Boolean) {
        if (context.isAutoplay && isTrackingSliders) {
            autoCursor!!.onSliderTracking()
        }
        if (context.isFlashlight) {
            flashlightSprite!!.onTrackingSliders(isTrackingSliders)
        }
    }

    override fun onUpdatedAutoCursor(pX: Float, pY: Float) {
        if (context.isFlashlight) {
            flashlightSprite!!.onMouseMove(pX, pY)
        }
    }

    override fun updateAutoBasedPos(pX: Float, pY: Float) {
        if (context.isAutoplay || context.isAutopilot) {
            // The cursor is in the space gameplay is laid out in.
            val position = objectPlayfield.mapping.toResolution(pX, pY)
            autoCursor!!.setPosition(position.x, position.y, this)
        }
    }

    override val playfieldMapping
        get() = objectPlayfield.mapping

    private fun getNearestCursorId(pX: Float, pY: Float): Int {
        var nearestDistance = Float.POSITIVE_INFINITY
        var id = -1

        for (i in cursors.indices) {
            val latestEvent = cursors[i]!!.getLatestNonUpEvent()

            if (latestEvent != null) {
                val distance = Utils.squaredDistance(pX, pY, latestEvent.playfieldPosition.x, latestEvent.playfieldPosition.y)

                if (distance < nearestDistance) {
                    id = i
                    nearestDistance = distance
                }
            }
        }

        return id
    }

    private fun getSliderPath(index: Int): SliderPath = loaded.sliderPaths[index]

    private fun getSliderRenderPath(index: Int): Path = loaded.sliderRenderPaths[index]

    /**
     * The time that gameplay started relative to the start of the [Beatmap], in seconds.
     */
    override val initialStartTime: Float
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
        stat.time = currentTimeMillis()
        if (replay != null && !replaying) {
            val replay = replay!!

            //write misses to replay
            for (obj in activeObjects) {
                // Judged objects stay alive until their effects end.
                if (obj.hasFinalJudgement()) {
                    continue
                }

                stat.registerHit(0, false, false)
                replay.addObjectScore(obj.getId(), ResultType.MISS)
            }
            while (objectIndex < objects.size) {
                ++objectIndex
                stat.registerHit(0, false, false)
                replay.addObjectScore(++lastObjectId, ResultType.MISS)
            }

            val currentTime = currentTimeMillis().toString()
            val odrFilename = MD5Calculator.getStringMD5(loaded.beatmapInfo.filename + currentTime) + currentTime.substring(0, min(3, currentTime.length)) + ".odr"

            replay.setStat(stat)
            replay.save(GameFolders.scorePath + odrFilename)

            if (stat.totalScoreWithMultiplier > 0 && ModAutoplay::class !in stat.mod) {
                stat.replayFilename = odrFilename
                stat.beatmapMD5 = loaded.beatmapInfo.md5

                val scoreInfo = stat.toScoreInfo()

                async {
                    try {
                        DatabaseManager.scoreInfoTable.insertScore(scoreInfo)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to save score to database", e)
                    }
                }
            }

            Toasts.showLong(Res.string.message_save_replay_successful)
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
        val droidTimedDifficultyAttributes = loaded.timedDifficulty.droid
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
        val standardTimedDifficultyAttributes = loaded.timedDifficulty.standard
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
        if (root.bounds.width <= 0f) {
            return
        }

        val playfieldSize = OsuSettings.playfieldSize

        // The space gameplay is laid out in, which the playfield fills.
        val spaceWidth = objectPlayfield.mapping.width
        val spaceHeight = objectPlayfield.mapping.height

        // The point of the playfield that ends up at the center of the window.
        val centerX = spaceWidth * Interpolation.linear(
            Interpolation.linear(0f, 0.5f, playfieldSize),
            Interpolation.linear(1f, 0.5f, playfieldSize),
            1 - OsuSettings.playfieldHorizontalPosition
        )

        val centerY = spaceHeight * Interpolation.linear(
            Interpolation.linear(0f, 0.5f, playfieldSize),
            Interpolation.linear(1f, 0.5f, playfieldSize),
            1 - OsuSettings.playfieldVerticalPosition
        )

        playfield.scale = playfieldSize
        playfield.translationX = (spaceWidth / 2f - centerX * playfieldSize).px
        playfield.translationY = (spaceHeight / 2f - centerY * playfieldSize).px
    }

    private fun resetPlayfieldSizeScale() {
        playfield.scale = 1f
        playfield.translationX = Dimension.Zero
        playfield.translationY = Dimension.Zero
    }

    private fun estimateMaximumActiveObjects(): Int {
        // Estimate the maximum number of simultaneously active objects to pre-size the lists and minimize
        // array reallocations.
        val lifetimeEnds = DoubleMinHeap(max(1, objects.size / 4))
        var estimatedMaxActiveObjects = 0

        for (`object` in objects) {
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

            // Objects live until the effects of their judgement end.
            lifetimeEnds.add(lifetimeEnd + JudgementPiece.DURATION * 1000)
            estimatedMaxActiveObjects = max(estimatedMaxActiveObjects, lifetimeEnds.size)
        }

        return estimatedMaxActiveObjects
    }

    //region Seeking

    private fun seekTo(targetTimeSeconds: Float) {
        if (isFinished) {
            return
        }

        val expiredObjects = expiredObjects
        val breakAnimator = breakAnimator!!

        val clampedTime = MathUtils.clamp(
            targetTimeSeconds,
            (objects[0].startTime / 1000).toFloat(),
            (objects[objects.size - 1].endTime / 1000).toFloat()
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

        // The judgements went with their objects, their particles are the playfield's.
        objectPlayfield.particles.clear()

        // Remove follow points spawned for objects before the seek point.
        FollowPointConnection.clearAll(objectPlayfield.underlay)

        // Reset spawn counters.
        objectIndex = 0
        followPointIndex = 0
        sliderIndex = 0
        lastObjectId = -1
        gameStarted = false
        leadOut = 0f
        scoreProcessor.resetCombo()
        healthProcessor.reset()
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

        val effectControlPoints = effectControlPoints

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


        while (breakPeriodIndex < breakPeriods.size && breakPeriods[breakPeriodIndex].endTime <= targetMs) {
            breakPeriodIndex++
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

        if (breakPeriodIndex < breakPeriods.size) {
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
        if (context.isAutoplay || replaying) {
            if (context.isAutoplay) {
                for (i in objects.indices) {
                    val tapTime = objects[i].startTime.toFloat() / 1000f

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

            if (targetSpeedMultiplier != context.timing.speedMultiplier) {
                context.timing.speedMultiplier = targetSpeedMultiplier
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

        var localTimingIdx = 0
        var localBreakIdx = 0
        val objectData = if (replaying) replay!!.objectData else null

        // How far continuous HP drain has been applied, and which object-to-object rate segment
        // it's currently within ([objects[rateSegIdx].startTime - timePreempt,
        // objects[rateSegIdx + 1].startTime - timePreempt)). Both advance with elapsed
        // time and is independent of which object's own judgement has been applied yet.
        var drainCursorMs = objects[0].startTime - objects[0].timePreempt
        var rateSegIdx = 0

        for (i in objects.indices) {
            val obj = objects[i]

            val judgementTimeMs = getJudgementTimeMs(i, obj, objectData)

            if (judgementTimeMs > targetMs) {
                break
            }

            val drainTargetMs = min(judgementTimeMs, targetMs.toDouble())

            while (drainCursorMs < drainTargetMs && rateSegIdx < objects.size - 1) {
                val segFromObj = objects[rateSegIdx]
                val segToObj = objects[rateSegIdx + 1]

                // Advance local timing point index to this segment's own "to" object.
                while (localTimingIdx + 1 < timingControlPoints.size && timingControlPoints[localTimingIdx + 1].time <= segToObj.startTime) {
                    localTimingIdx++
                }

                val msPerBeat = timingControlPoints[localTimingIdx].msPerBeat
                val distToNextObject = max(segToObj.startTime - segFromObj.startTime, msPerBeat / 2) / 1000

                val segEndMs = segToObj.startTime - segToObj.timePreempt
                val stopAtMs = min(segEndMs, drainTargetMs)

                if (stopAtMs > drainCursorMs) {
                    // Advance past breaks that fully precede the current segment.
                    while (localBreakIdx < breakPeriods.size && breakPeriods[localBreakIdx].endTime <= drainCursorMs) {
                        localBreakIdx++
                    }

                    val effectiveSecs = HealthProcessor.drainingSeconds(drainCursorMs, stopAtMs, breakPeriods, localBreakIdx)

                    if (healthProcessor.drainSegment(effectiveSecs, distToNextObject)) {
                        return
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
        if (abs(accSeconds) <= hitWindow.mehWindow / 1000) {
            stat.addHitOffset(accSeconds)
        }
    }

    private fun applyCircleResult(data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val result = JudgementResult.fromResultType(data?.result ?: ResultType.HIT300.id)

        if (result != JudgementResult.Miss) {
            reconstructHitOffset(if (data != null) data.accuracy / 1000.0 else 0.0)
        }

        scoreProcessor.applyObject(result, endCombo)
    }

    private fun applySliderResult(slider: Slider, data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val result = JudgementResult.fromResultType(data?.result ?: ResultType.HIT300.id)

        if (result == JudgementResult.Miss) {
            scoreProcessor.applyObject(result, endCombo)
            return
        }

        // Slider head: HIT300 implies all ticks were hit; autoplay (data==null) always hits.
        // For HIT50/HIT100, reconstruct whether the head was actually within the hit window,
        // mirroring GameplaySlider.onSliderHeadHit.
        val accSeconds = if (data != null) data.accuracy / 1000.0 else 0.0
        val headHit: Boolean

        if (data == null || result == JudgementResult.Great) {
            headHit = true
        } else {
            val replayVersion = context.replayVersion
            val mehWindowSecs = hitWindow.mehWindow / 1000.0
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
            scoreProcessor.applySliderPart(SliderPart.Head)
        }

        // Ticks and repeats from tickSet.
        val nested = slider.nestedHitObjects

        // Skip head (index 0) and tail (last index).
        for (i in 1 until nested.size - 1) {
            val wasHit = data == null || (data.tickSet != null && data.tickSet!![i - 1])

            if (!wasHit) {
                scoreProcessor.applySliderPartMiss()
                continue
            }

            val nestedObj = nested[i]

            if (nestedObj is SliderTick) {
                scoreProcessor.applySliderPart(SliderPart.Tick)
            } else if (nestedObj is SliderRepeat) {
                scoreProcessor.applySliderPart(SliderPart.Repeat)
            }
        }

        // Slider tail: combo is only awarded when the player was tracking at the endpoint.
        // Unlike circles, the result score reflects ticks hit, not timing accuracy.
        val tailTracked = data == null || (data.tickSet != null && data.tickSet!![nested.size - 2])

        scoreProcessor.applySliderTail(result, endCombo, tailTracked)
    }

    private fun applySpinnerResult(spinner: Spinner, data: Replay.ReplayObjectData?, endCombo: Boolean) {
        val duration = spinner.duration.toFloat() / 1000
        var needRotations = (2 + 2 * playableBeatmap.difficulty.od / 10f) * duration

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
            scoreProcessor.applySpinnerRotation()
        }

        for (s in 0 until bonus) {
            scoreProcessor.applySpinnerBonus()
        }

        applyCircleResult(data, endCombo)
    }

    //endregion

    private inner class GameUIScene : Scene() {
        val game
            get() = this@GameplaySession

        private val stopwatch = Stopwatch()
        private var lastRootWidth = 0f
        private var lastRootHeight = 0f
        private var areModIconsAnimated = false

        init {
            clock = gameplayClock
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            val panel = replaySettingsPanel
            if (panel != null && panel.parent != null
                && (event !is PointerEvent || event.action != PointerEvent.Action.Down || panel.containsScenePoint(event.x, event.y))
                && panel.onInputEvent(event)) {
                return true
            }

            if (hud.onInputEvent(event)) {
                return true
            }

            if (event is PointerEvent) {
                // Pointer events come from the main thread while the gameplay state belongs to the update thread.
                return Verktex.withTreeLock { onPointerEvent(event) }
            }

            if (super.onInputEvent(event)) {
                return true
            }

            // Back and the menu key pause the play, or resume it, from the update thread, which owns the
            // gameplay state.
            if (event is KeyboardEvent && (event.isBackPress ||
                    event.action == KeyboardEvent.Action.Down && event.key == KeyboardEvent.Key.Menu)) {
                updateThread { if (game.isPaused) game.resume() else game.pause() }
                return true
            }
            return false
        }

        override fun onUpdate(clock: FrameClock) {
            val deltaTimeSec = clock.elapsedFrameTime

            // The playfield follows the size of the window, which is only known after the first layout.
            if (root.bounds.width != lastRootWidth || root.bounds.height != lastRootHeight) {
                lastRootWidth = root.bounds.width
                lastRootHeight = root.bounds.height
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
                this@GameplaySession.update(gameplayClock.elapsedFrameTime)

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
            val hudWidth = hud.bounds?.width ?: GameplayHUD.REFERENCE_WIDTH
            val distance = hudWidth + 5 + textWidth
            val x = replayText.x.resolve(0f) - distance / 40f * deltaTimeSec

            replayText.setPosition(if (x < -textWidth) hudWidth + 5f else x, 140f)
        }
    }

    companion object {
        const val CursorCount = Replay.CursorCount

        private const val TAG = "GameplaySession"

        // The lives Easy gives once health runs out.
        private const val EasyLives = 3

        /**
         * The attempt shown in the current scene, null when gameplay isn't the current scene.
         */
        val current: GameplaySession?
            get() = (Verktex.scene as? GameUIScene)?.game
    }
}
