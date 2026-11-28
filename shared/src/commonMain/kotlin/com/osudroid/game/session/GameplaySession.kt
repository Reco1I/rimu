package com.osudroid.game.session

import com.osudroid.game.Gameplay
import com.osudroid.game.loading.LoadedPlay
import com.osudroid.game.loading.PlayRequest
import com.osudroid.scoring.Statistic
import com.reco1l.verktex.math.Path
import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.game.GameplayVibrations
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.Platform
import com.osudroid.audio.MusicClock
import com.osudroid.beatmaps.Beatmap
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
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.data.DatabaseManager
import com.osudroid.data.toScoreInfo
import com.osudroid.difficulty.BeatmapDifficultyCalculator
import com.osudroid.difficulty.attributes.DroidPerformanceAttributes
import com.osudroid.difficulty.attributes.PerformanceAttributes
import com.osudroid.difficulty.attributes.StandardPerformanceAttributes
import com.osudroid.difficulty.calculator.DroidPerformanceCalculationParameters
import com.osudroid.difficulty.calculator.PerformanceCalculationParameters
import com.osudroid.difficulty.calculator.StandardPerformanceCalculationParameters
import com.osudroid.game.Cursor
import com.osudroid.game.CursorEvent
import com.osudroid.game.GameplayContext
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.Playfield
import com.osudroid.game.SliderPath
import com.osudroid.game.replay.ReplayControls
import com.osudroid.math.Interpolation
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import com.osudroid.mods.*
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.resources.Res
import com.osudroid.ui.v2.BeatmapBackground
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skinFrames
import com.osudroid.ui.v2.game.BreakAnimator
import com.osudroid.ui.v2.game.ComboBurst
import com.osudroid.ui.v2.game.Countdown
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
import com.osudroid.utils.calculateRate
import com.osudroid.utils.async
import com.osudroid.utils.runSafe
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.time.FrameStableClock
import com.reco1l.verktex.time.SongClock
import com.reco1l.verktex.time.Stopwatch
import com.reco1l.verktex.ui.AnimatedSprite
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.ScalingContainer
import com.reco1l.verktex.ui.text.Text
import kotlin.math.abs
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
import com.osudroid.game.objects.GameObject
import com.osudroid.game.objects.GameObjectListener
import com.osudroid.game.objects.PiecePool
import com.osudroid.game.objects.PoolSizes
import kotlin.time.measureTime
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import com.osudroid.ui.skinning.OsuSkin
import com.osudroid.utils.currentTimeMillis
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
    // What the background brightness and breaks dim: the game's background, or the layer of the video over it.
    private var dimmedBackground: Component = BeatmapBackground

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

    private lateinit var controlPoints: ControlPointTracker
    private lateinit var breaks: BreakController
    private lateinit var scoreReconstruction: ScoreReconstruction
    private var multiplayer = MultiplayerSync()
    private var leadOut = 0f
    private var postSeekFrameCount = 0
    private var comboColors: ArrayList<Color4>? = null
    private val activeObjects = ArrayList<GameObject>()
    private val expiredObjects = ArrayList<GameObject>()
    // By identity: GameObject doesn't override equals or hashCode.
    private val processedExpiredObjects: MutableSet<GameObject> = HashSet()
    private var judgeableObject: GameObject? = null
    private var metronome: Metronome? = null
    private var scale = 0f
    override lateinit var stat: Statistic

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
    private lateinit var cursorPresenter: CursorPresenter
    private lateinit var spawner: ObjectSpawner

    // The replay played back into the cursors, null unless one is watched.
    private var replayInput: ReplayInputSource? = null
    private var replay: Replay? = null
    var replaying = false
        private set
    private var comboBurst: ComboBurst? = null
    private var sliderBorderColor: Color4? = null
    private var unrankedSprite: Sprite? = null
    private val modIcons = ArrayList<ModIcon>()
    private val rateAdjustingMods = ArrayList<IModApplicableToTrackRate>()

    override val hitWindow: HitWindow
        get() = playableBeatmap.hitWindow

    private var performanceAttributes: PerformanceAttributes? = null
    private var performanceCalculationParameters: PerformanceCalculationParameters? = null

    override var replayControls: ReplayControls? = null
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
     * The layer the [hud] is in, which the HUD's entrance animates: the HUD scales itself to the window, as a
     * [ScalingContainer], so its own scale can't be animated.
     */
    val hudLayer = Container().apply {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
    }

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

    /**
     * The video of the beatmap, shown as the background.
     */
    private val video = VideoController(onReady = { applyBackground() })


    // Multiplayer

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    var hasFailed = false
        private set



    init {
        gameUIScene = createScene()
    }

    private fun createScene(): GameUIScene {
        root = Container()
        root.width = Dimension.FillAvailable
        root.height = Dimension.FillAvailable

        backgroundLayer = Container()
        backgroundLayer.width = Dimension.FillAvailable
        backgroundLayer.height = Dimension.FillAvailable

        // Gameplay is laid out in a space 1280 wide, as osu!droid's, scaled to the width of the window.
        gameplaySpace = ScalingContainer()
        gameplaySpace.referenceSize = Vec2(Playfield.SPACE_WIDTH, 720f)

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
            Playfield.SPACE_WIDTH,
            Playfield.SPACE_WIDTH * window.windowHeight / window.windowWidth.coerceAtLeast(1)
        )

        playfield += bgScene
        playfield += objectPlayfield
        playfield += fgScene

        gameplaySpace += playfield

        root += backgroundLayer
        root += gameplaySpace

        // The timelines of the hit objects and the HUD are on the gameplay clock, not the scene's.
        root.clock = gameplayClock

        // The root is the scene's background, laid out over the whole scene instead of in its safe area.
        val scene = GameUIScene()
        scene.background = Background.Component(root)
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

        applyBackgroundStyle()
    }

    // The background is the game's, which shows the selected beatmap's, in the color of the beatmap's events where it
    // has no texture.
    private fun applyBackgroundStyle() {
        BeatmapBackground.fallbackColor = playableBeatmap.events.backgroundColor ?: Color4.Black
    }

    /**
     * Loads the video of the beatmap, or releases it, as the settings changed.
     */
    fun loadVideo() {
        video.load(loaded.beatmapInfo, playableBeatmap)
    }

    private fun cancelVideoLoading() {
        video.cancelLoading()
    }

    internal fun releaseVideo() {
        video.release()
    }

    private fun applyBackground() {
        // Guard: video sprite exists but dimensions not yet reported by ExoPlayer.
        // onVideoSizeChanged will trigger another applyBackground call when they arrive.
        val loadedVideo = video.video

        if (loadedVideo != null && (loadedVideo.videoWidth == 0 || loadedVideo.videoHeight == 0)) {
            return
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        val brightness = OsuSettings.backgroundBrightness

        val playfieldSize = OsuSettings.playfieldSize

        val shownVideo = video.shown
        var sceneBorder = this.sceneBorder

        if (sceneBorder == null && Settings[OsuSettings.DisplayPlayfieldBorder] && playfieldSize < 1f) {
            sceneBorder = Container().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                border = Border(Dimension.Fixed.Pixels(5f), Color4.White)
                alpha = Interpolation.linear(0.2f, 0.8f, brightness)
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
            }

            playfield.add(0, sceneBorder)
            this.sceneBorder = sceneBorder
        }

        backgroundLayer.clearChildren()

        // The beatmap's background is the game's, a video is shown over it, which hides it.
        if (shownVideo != null) {
            val videoWidth = shownVideo.videoWidth.toFloat()
            val videoHeight = shownVideo.videoHeight.toFloat()

            if (videoWidth > 0f && videoHeight > 0f) {
                // Fit in the window through the size instead of the scale, the size is relative to its parent.
                val factor = root.bounds.width / videoWidth

                shownVideo.anchor = Anchor.Center
                shownVideo.origin = Anchor.Center
                shownVideo.setSize(videoWidth * factor, videoHeight * factor)
            }

            backgroundLayer += shownVideo
            BeatmapBackground.setBrightness(0f)
            dimmedBackground = backgroundLayer
        } else {
            dimmedBackground = BeatmapBackground
        }

        // Dimmed by fading it over the black of the window rather than covering it with black, which
        // costs a fullscreen layer to the GPU, and a fully dimmed background isn't drawn at all.
        BeatmapBackground.clearTransforms()
        dimmedBackground.alpha = brightness

        if (breakAnimator != null) {
            breakAnimator!!.setDimBrightness(brightness)
            breakAnimator!!.setDimmedLayer(dimmedBackground)
        }
    }

    /**
     * Prepares this attempt while the loader is shown: its background, its music, its clock and its replay.
     *
     * @param isNewBeatmap Whether the beatmap isn't the one of the attempt before, whose skin is loaded then.
     * @return Whether the attempt can be played.
     */
    fun prepare(scope: CoroutineScope, isNewBeatmap: Boolean): Boolean {
        if (isHUDEditorMode || OsuSkin.current.hud.hasElement(HUDElements.pp_counter.name)) {
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
            if (gameplay.session === this && ::spawner.isInitialized && spawner.objectIndex > 0) {
                updatePPValue(spawner.objectIndex - 1)
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

        if (::breaks.isInitialized) {
            breaks.reset()
        }
        if (::spawner.isInitialized) {
            spawner.reset()
        }
        video.reset(offset = playableBeatmap.events.videoStartTime / 1000f)

        val poolSizes = PoolSizes.of(objects)
        val estimatedMaxActiveObjects = max(10, poolSizes.activeObjects)

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
        gameplayClock.seek(min(gameplayClock.time, video.offset))

        sliderBorderColor = null
        if (playableBeatmap.colors.sliderBorderColor != null) {
            sliderBorderColor = playableBeatmap.colors.sliderBorderColor
        }

        if ((OsuSkin.current.slider.borderColor != null)) {
            sliderBorderColor = OsuSkin.current.slider.borderColor
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

        if (comboColors.isEmpty() || OsuSkin.current.comboColors.forceOverride) {
            comboColors.clear()
            comboColors.addAll(OsuSkin.current.comboColors.resolved)
        }

        scope?.ensureActive()

        // TODO replay
        replaying = false
        var replay: Replay? = Replay(true)
        this.replay = replay
        replay!!.setObjectCount(hitObjects.size)
        replay.setBeatmap(loaded.beatmapInfo.fullBeatmapsetName, loaded.beatmapInfo.fullBeatmapName, playableBeatmap.md5)

        if (replayPath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            val replayStat = Statistic()
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
        this.context = context

        controlPoints = ControlPointTracker(
            timingControlPoints, effectControlPoints,
            playableBeatmap.controlPoints.timing.defaultControlPoint, playableBeatmap.controlPoints.effect.defaultControlPoint,
            context.timing
        )

        SkinTextures.preloadGameplay()
        SkinSounds.preloadGameplay()

        // The pools keep what they have on a retry, the skin being the same.
        val keepPools: Boolean
        val preloadTime = measureTime {
            keepPools = PiecePool.instance.preload(poolSizes, context.isTraceable)
            FollowPointConnection.renew(poolSizes.followPoints + 4, keep = keepPools)
            GameplaySliderTick.renew(poolSizes.sliderTicks + 4, keep = keepPools)
        }
        Logger.i(TAG, "Pools filled in $preloadTime (kept: $keepPools): $poolSizes")

        // Resetting variables before starting the game.
        Multiplayer.finalData = null
        hasFailed = false
        multiplayer = MultiplayerSync()
        leadOut = 0f
        isGameOver = false

        paused = false
        gameStarted = false
        return true
    }

    override val scene: Scene
        get() = gameUIScene

    override val replayId: Int
        get() = gameplay.results.replayID

    /**
     * Takes the job loading the video, to stop it with the loading of the play.
     */
    fun takeVideoLoadingJob(): Job? {
        return video.takeLoadingJob()
    }

    /**
     * Makes the statistics, the cursors, the HUD and the rest of the scene of this attempt, once it's [prepare]d. It's
     * ready to start then, or once every player is in multiplayer.
     */
    fun prepareScene() {
        val lastMods = mods

        stat = Statistic()
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
        scoreReconstruction = ScoreReconstruction(
            objects, timingControlPoints, breakPeriods, hitWindow, playableBeatmap.difficulty.od, context.replayVersion,
            stat, scoreProcessor, healthProcessor
        )
        previousFrameTime = 0

        metronome = null
        if ((OsuSettings.metronomeSwitch == 1 && context.isNightCore)
            || (context.isMuted && context.muted!!.enableMetronome)
            || OsuSettings.metronomeSwitch == 2) {
            metronome = Metronome()
        }

        cursorPresenter = CursorPresenter(
            cursors, context, objectPlayfield.cursors,
            showsCursors = (replaying || Settings[OsuSettings.ShowCursor]) && !context.isAutoplay && !context.isAutopilot
        )

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
            comboBurst = ComboBurst(Playfield.SPACE_WIDTH)
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

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        val hud = GameplayHUD(this)
        this.hud = hud

        spawner = ObjectSpawner(
            listener = this,
            loaded = loaded,
            playfield = objectPlayfield,
            stat = stat,
            events = hud,
            replayObjectData = if (replaying) replay!!.objectData else null,
            comboColor = ::getComboColor,
            sliderBorderColor = { sliderBorderColor!! }
        )

        if (replaying) {
            replayInput = ReplayInputSource(replay!!, cursors, context.isHardRock, hud::onGameplayTouchDown)
        }

        hud.setEditMode(isHUDEditorMode)
        hud.setSkinData(OsuSkin.current.hud)

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
            playname = if (replaying) gameplay.results.replayStat!!.playerName else "osu!"

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

        breaks = BreakController(breakPeriods, breakAnimator,
            onBreakStart = {
                gameStarted = false

                if (context.isFlashlight) {
                    cursorPresenter.flashlight!!.onBreak(true)
                }

                multiplayer.showChat()
                hud.onBreakStateChange(true)
            },
            onBreakEnd = {
                // Ensure the chat is dismissed if it's still shown
                multiplayer.hideChat()

                gameStarted = true
                hud.onBreakStateChange(false)

                if (context.isFlashlight) {
                    cursorPresenter.flashlight!!.onBreak(false)
                }
            }
        )

        if (!startedFromHUDEditor && (context.isAutoplay || replaying)) {
            val replayControls = ReplayControls()
            this.replayControls = replayControls
            // Designed in dp, so it's not in the scaled HUD, it's added with it in start().

            replayControls.onPauseToggle = { isPaused ->
                updateThread {
                    if (isPaused) {
                        gameplayClock.pause()
                        stopLoopingSamples()

                        video.pause()
                    } else if (!paused) {
                        if (!gameplayClock.isRunning) {
                            gameplayClock.start()
                        }

                        playLoopingSamples()

                        video.resume()
                    }
                }
            }

            replayControls.onBackgroundBrightnessChanged = { brightness ->
                breakAnimator.setDimBrightness(brightness)

                // We do not want
                if (!breakAnimator.isBreak()) {
                    dimmedBackground.alpha = brightness
                }
            }

            replayControls.updateSeekPosition(
                gameplayClock.time,
                (objects[0].startTime / 1000).toFloat(),
                (objects[objects.size - 1].endTime / 1000).toFloat()
            )

            replayControls.onSeek = { targetTime ->
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
        gameplayClock.changeSource(MusicClock(music))

        // Ensure user-defined offset has time to be applied.
        gameplayClock.userGlobalOffset = OsuSettings.offset / 1000
        gameplayClock.userBeatmapOffset = beatmapOffset / 1000f

        val firstObjectTimePreempt = playableBeatmap.hitObjects.objects[0].timePreempt.toFloat() / 1000
        gameplayClock.seek(min(gameplayClock.time, firstObjectStartTime - firstObjectTimePreempt))
        initialStartTimeValue = gameplayClock.time

        music.volume = OsuSettings.musicVolume

        gameplayClock.start()
        SkinTextures.startPlayTracking()

        if (skipTime <= 1 && Multiplayer.isConnected) {
            Multiplayer.roomScene!!.chat.hide()
        }

        applyPlayfieldSizeScale()
        applyBackground()

        replayControls?.defaultBackgroundBrightness = OsuSettings.backgroundBrightness

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

        hudLayer += hud
        root += hudLayer
        replayControls?.let { root += it }
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
        profile.start()
        val time = gameplayClock.time

        multiplayer.update(dt, context.timing.speedMultiplier, stat)
        profile.mark("multiplayer")

        if (!isGameOver) {
            updateRate()
        }

        replayControls?.updateSeekPosition(time, firstObjectStartTime, lastObjectEndTime)

        replayInput?.update(time, dt)
        cursorPresenter.update(dt, replaying, stat.combo)
        controlPoints.update(time)
        profile.mark("input and cursors")

        if (!isGameOver) {
            breaks.update(time)
        }

        if (spawner.isDone && areObjectsJudged(activeObjects) && context.isFlashlight) {
            cursorPresenter.flashlight!!.onBreak(true)
        }

        if (gameStarted && healthProcessor.drain(dt, spawner.distanceToNextObject) && onHealthDepleted()) {
            return
        }
        profile.mark("breaks and health")

        updateComboBurst()
        removeExpiredObjects()
        profile.mark("expiry")

        updatePassiveObjects(dt)
        profile.mark("HUD")
        updateActiveObjects(dt)
        profile.mark("judgement")

        if (judgeableObject != null && (context.isAutoplay || context.isAutopilot)) {
            cursorPresenter.autoCursor!!.moveToObject(judgeableObject, time, this)
        }

        video.update(time, context.timing.speedMultiplier)
        profile.mark("autoplay and video")

        spawner.spawnFollowPoints(time)
        profile.mark("follow points")

        val isPastMusic = spawner.spawn(time, controlPoints.timingPoint, totalLength, ::onObjectLifetimeStart)
        profile.mark("spawn")

        if (context.isPerfect && stat.hit100 + stat.hit50 + stat.misses > 0) {
            gameover()

            if (!Multiplayer.isMultiplayer) {
                gameplay.retry()
            }
        }

        updateMetronomeAndMuted()

        if (updateEnd(dt, isPastMusic)) {
            return
        }

        if (updateSkipButton()) {
            return
        }

        if (postSeekFrameCount > 0) {
            postSeekFrameCount--
        }

        profile.end("end")
    }

    // Logs what a slow step of gameplay spent its time on, in debug builds.
    private val profile = UpdateProfile(TAG, "gameplay step")

    // Follows the rate of the mods changing it over time and of the replay controls.
    private fun updateRate() {
        val modRate = rateAdjustingMods.calculateRate((gameplayClock.time * 1000).toDouble())
        val replaySettingsRate = replayControls?.rate ?: 1f
        val currentSpeedMultiplier = modRate * replaySettingsRate

        if (currentSpeedMultiplier != context.timing.speedMultiplier) {
            context.timing.speedMultiplier = currentSpeedMultiplier
            gameplayClock.rate = currentSpeedMultiplier

            video.setPlaybackSpeed(currentSpeedMultiplier)

            val music = OsuGame.music
            music.speed = modRate
            music.setPitchRate(replaySettingsRate)
        }
    }

    // Called once health ran out and no life is left: the play fails, though it goes on in multiplayer.
    // Returns whether the frame ends there.
    private fun onHealthDepleted(): Boolean {
        if (Multiplayer.isMultiplayer) {
            if (!hasFailed) {
                Toasts.show("You failed but you can continue playing.")
            }
            hasFailed = true
            return false
        }

        gameover()
        return true
    }

    private fun updateComboBurst() {
        val comboBurst = comboBurst ?: return

        if (stat.combo == 0) {
            comboBurst.breakCombo()
        } else {
            comboBurst.checkAndShow(stat.combo)
        }
    }

    private fun removeExpiredObjects() {
        processedExpiredObjects.clear()

        if (expiredObjects.isEmpty()) {
            return
        }

        for (i in 0 until expiredObjects.size) {
            val obj = expiredObjects[i]

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire()
            }
        }

        activeObjects.removeAll(expiredObjects)
        expiredObjects.clear()
    }

    private fun onObjectLifetimeStart() {
        gameStarted = true

        if (unrankedSprite != null) {
            unrankedSprite!!.choreographAt(1.5f) { sequence -> sequence
                .scaleTo(1.5f, 0.5f)
                .fadeOut(0.5f) }

            // Make it null to avoid multiple entity modifier registration
            unrankedSprite = null
        }
    }

    // The metronome, and the volume of the music under Muted.
    private fun updateMetronomeAndMuted() {
        val mutedMod = context.muted

        if (metronome != null) {
            metronome!!.update(gameplayClock.time, controlPoints.timingPoint)

            if (mutedMod != null) {
                metronome!!.setVolume(1 - mutedMod.volumeAt(stat.combo))
            }
        }

        if (gameplayClock.time >= 0 && mutedMod != null) {
            OsuGame.music.volume =
                OsuSettings.musicVolume * mutedMod.volumeAt(stat.combo)
        }
    }

    // Ends the attempt two seconds after its last object is judged, or once an object starts after the music.
    // Returns whether the frame ends there, as the HUD editor plays the attempt again.
    private fun updateEnd(dt: Float, isPastMusic: Boolean): Boolean {
        val isJudged = spawner.isDone && areObjectsJudged(activeObjects)

        if (isPastMusic || (!isGameOver && isJudged && leadOut > 2)) {

            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                gameplayClock.seek(initialStartTimeValue)
                prepareAttempt(null, null, false)
                applyBackground()
                stat.reset()
                skip(true)
                return true
            }

            finish()
        } else if (isJudged) {
            gameStarted = false
            leadOut += dt
        }

        return false
    }

    // Removes the skip button once the intro is over, or skips once it's pressed. Returns whether the frame ends there.
    private fun updateSkipButton(): Boolean {
        if (gameplayClock.time > skipTime - 1f && skipBtn != null) {
            multiplayer.hideChat()
            skipBtn!!.removeSelf()
            skipBtn = null
        } else if (skipBtn != null) {
            // Pressed within 250 pixels of the space from its bottom right corner, in osu!pixels.
            val area = objectPlayfield.visibleArea
            val radius = 250 * area.width / Playfield.SPACE_WIDTH

            for (i in cursors.indices) {
                val latestDownEvent = cursors[i]!!.getLatestNonUpEvent()

                if (latestDownEvent != null && Utils.squaredDistance(latestDownEvent.playfieldPosition.x,
                        latestDownEvent.playfieldPosition.y, area.z, area.w) < radius * radius) {
                    if (Multiplayer.isConnected) {
                        multiplayer.requestSkip {
                            SkinSounds.get("menuhit").play()
                            skipBtn!!.isVisible = false
                        }
                        return true
                    }
                    if (skipBtn != null) {
                        skipBtn!!.removeSelf()
                        skipBtn = null
                    }
                    skip()
                    return true
                }
            }
        }

        return false
    }

    // Ends the attempt once its objects are judged: its replay is saved and its results are shown.
    private fun finish() {
        isFinished = true
        logPoolMisses()

        BeatmapSkinManager.setSkinEnabled(false)
        PiecePool.instance.purge()
        activeObjects.clear()
        expiredObjects.clear()

        var replayPath: String? = null
        stat.time = currentTimeMillis()
        if (replay != null && !replaying) {
            val ctime = currentTimeMillis().toString()
            replayPath = GameFolders.corePath + "Scores/" +
                MD5Calculator.getStringMD5(loaded.beatmapInfo.filename + ctime) +
                ctime.substring(0, min(3, ctime.length)) + Replay.FILE_EXTENSION
            replay!!.setStat(stat)
            replay!!.save(replayPath)
        }
        resetPlayfieldSizeScale()
        cancelVideoLoading()

        val results = gameplay.results
        if (!startedFromHUDEditor) {
            if (replaying)
                // The replay watched, so it can be watched again.
                results.load(results.replayStat!!, null, OsuGame.music, this.replayPath, null, loaded.beatmapInfo)
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
                results.load(stat, loaded.beatmapInfo, OsuGame.music, replayPath, parsedBeatmap.md5, null)
            }
            OsuGame.music.volume = 0.2f
            Gameplay.replaceCurrentScene(results.scene!!)
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
        multiplayer.hideChat()

        if (gameplayClock.time > skipTime - 1f && !force) {
            return
        }

        SkinSounds.get("menuhit").play()

        val difference = skipTime - gameplayClock.time
        gameplayClock.seek(skipTime)

        updateThread {
            updatePassiveObjects(difference)

            video.seekTo(gameplayClock.time)

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
        if (!isFinished) {
            logPoolMisses()
        }
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
        hudLayer.removeSelf()
        replayControls?.removeSelf()

        cancelVideoLoading()

        val mSecPassed = gameplayClock.time * 1000
        val selectedBeatmap = BeatmapSelection.beatmap
        val music = OsuGame.music

        if (selectedBeatmap != null && !Multiplayer.isMultiplayer) {
            // osu!stable restarts the song back to preview time when the player is in the last 10 seconds *or* 2% of the beatmap.
            val continuePreview = mSecPassed < totalLength - 10000 && mSecPassed / totalLength < 0.98f
            val position = music.position

            if (Settings[OsuSettings.PlayMusicPreview]) {
                async {
                    music.stop()
                    music.preLoad(selectedBeatmap.audioPath)

                    // Song select starts it over at the preview point whenever it ends.
                    val previewPoint = music.previewPoint(selectedBeatmap)
                    music.playPreview(if (continuePreview) position else previewPoint, previewPoint, OsuSettings.musicVolume)
                }
            }
        }

        resetPlayfieldSizeScale()
    }

    /**
     * Lets this attempt go as another one replaces it, from the pause menu: nothing of it plays anymore.
     */
    internal fun release() {
        if (!isFinished) {
            logPoolMisses()
        }
        isFinished = true

        stopLoopingSamples()
        cancelVideoLoading()
        releaseVideo()
        gameplayClock.changeSource(null)
    }

    // What the pools had to create while playing, each a stall: the pools should be filled with it beforehand.
    private fun logPoolMisses() {
        SkinTextures.stopPlayTracking()
        Logger.i(TAG, "Created while playing: " + PiecePool.instance.missSummary() +
            ", follow points: " + FollowPointConnection.misses + ", slider ticks: " + GameplaySliderTick.misses +
            ", textures: " + SkinTextures.loadsWhilePlaying)
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


    override fun onCircleJudged(id: Int, hitOffset: Float, position: Vector2, endCombo: Boolean, forcedResult: ResultType?): String? {
        if (context.isAutoplay) {
            cursorPresenter.autoCursor!!.click()
            hud.onGameplayTouchDown(parsedBeatmap.hitObjects.objects[id].startTime.toFloat() / 1000)
        }

        if (replay != null && !replaying) {
            replay!!.addObjectResult(id, (hitOffset * 1000).toInt().toShort(), null)
        }

        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        val isCursorTrackingActive = !context.isAutoplay && !context.isAutopilot
        val nearestCursorId = if (isCursorTrackingActive) cursorPresenter.nearestCursor(position.x, position.y) else -1

        if (context.isFlashlight && isCursorTrackingActive && nearestCursorId >= 0) {
            cursorPresenter.followCursor(nearestCursorId)
        }
        GameplayVibrations.circle()

        val result = HitJudging.judge(hitOffset, playableBeatmap.hitWindow, forcedResult)
        val scoreName = registerHit(id, result, endCombo)

        if (result == JudgementResult.Miss) {
            return scoreName
        }

        registerAimJudgement(id, position, nearestCursorId)

        hud.onNoteHit(stat)
        return scoreName
    }

    override fun onSliderPartJudged(id: Int, part: SliderPart, isHit: Boolean, position: Vector2): String? {
        val nearestCursorId = followSliderCursor(position, isHeadHit = isHit && part == SliderPart.Head)

        GameplayVibrations.slider()

        // A missed part only breaks the combo.
        if (!isHit) {
            playComboBreak()

            if (context.isSuddenDeath) {
                stat.changeHp(-1.0f)
                gameover()
            }
            scoreProcessor.applySliderPartMiss()
            return null
        }

        val scoreName = scoreProcessor.applySliderPart(part)

        if (part == SliderPart.Head) {
            registerAimJudgement(id, position, nearestCursorId)

            if (context.isAutoplay) {
                hud.onGameplayTouchDown(parsedBeatmap.hitObjects.objects[id].startTime.toFloat() / 1000)
            }
        }

        hud.onNoteHit(stat)
        return scoreName
    }

    override fun onSliderJudged(id: Int, result: JudgementResult, position: Vector2, endCombo: Boolean, isTracked: Boolean): String? {
        followSliderCursor(position, isHeadHit = false)

        GameplayVibrations.slider()

        // Whole slider was missed.
        if (result == JudgementResult.Miss) {
            return registerHit(id, result, endCombo)
        }

        // The slider end hit is registered before updatePPValue, which reads the slider end hit count.
        val scoreName = registerHit(id, result, endCombo, isTracked, sliderTail = true)

        hud.onNoteHit(stat)
        return scoreName
    }

    // Makes Flashlight follow the cursor nearest to a part of a slider judged, returning it when it's needed for the
    // aim of a slider head hit, -1 otherwise or for none.
    private fun followSliderCursor(position: Vector2, isHeadHit: Boolean): Int {
        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        val isCursorTrackingActive = !context.isAutoplay && !context.isAutopilot
        val isFlashlightActive = context.isFlashlight && isCursorTrackingActive

        if (!isFlashlightActive && !(isHeadHit && isCursorTrackingActive)) {
            return -1
        }

        val nearestCursorId = cursorPresenter.nearestCursor(position.x, position.y)

        if (isFlashlightActive && nearestCursorId >= 0) {
            cursorPresenter.followCursor(nearestCursorId)
        }

        return nearestCursorId
    }

    // Tells the HUD how far from the center of the object [id] the cursor that hit it was.
    private fun registerAimJudgement(id: Int, position: Vector2, cursorId: Int) {
        if (cursorId < 0) {
            return
        }

        val aimErrorCursorEvent = cursors[cursorId]!!.getLatestNonUpEvent() ?: return
        val objectRadius = parsedBeatmap.hitObjects.objects[id].gameplayRadius.toFloat()

        hud.onAimJudgement(position, aimErrorCursorEvent.playfieldPosition, objectRadius)
    }

    override fun onSpinnerStart(id: Int) {
        if (context.isAutoplay) {
            cursorPresenter.autoCursor!!.click()
            hud.onGameplayTouchDown(parsedBeatmap.hitObjects.objects[id].startTime.toFloat() / 1000)
        }
    }

    override fun onSpinnerBonus() {
        scoreProcessor.applySpinnerBonus()
    }

    override fun onSpinnerJudged(id: Int, result: JudgementResult, endCombo: Boolean, spins: Int): String? {
        if (replay != null && !replaying) {
            // The spins, and the result in the last two bits.
            val resultCode = when (result) {
                JudgementResult.Great -> 3
                JudgementResult.Ok -> 2
                JudgementResult.Meh -> 1
                JudgementResult.Miss -> 0
            }
            replay!!.addObjectResult(id, (spins * 4 + resultCode).toShort(), null)
        }

        GameplayVibrations.spinner()

        val scoreName = registerHit(id, result, endCombo)

        if (result == JudgementResult.Miss) {
            return scoreName
        }

        // Simulate a hit for hit error meter registration.
        val accuracy = when (result) {
            JudgementResult.Great -> 0.0
            JudgementResult.Ok -> hitWindow.greatWindow + 1
            else -> hitWindow.okWindow + 1
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

        val sprite = cursorPresenter.sprite(id)

        val position = objectPlayfield.toPlayfield(event.x, event.y)
        val cursorEvent = CursorEvent.obtain(event, position.x, position.y, objectPlayfield.visibleArea, context.isHardRock)

        cursorEvent.trackTime = gameplayClock.time * 1000
        cursorEvent.offset = offset.toDouble()

        sprite?.setPosition(cursorEvent.playfieldPosition.x, cursorEvent.playfieldPosition.y)

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

            cursorPresenter.sprite(i)?.setShowing(false)
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
            if (multiplayer.onBackPress()) {
                // Room being null can happen when the player disconnects from socket while playing
                multiplayer.submitFinalScore(stat)

                Multiplayer.log("Player left the match.")
                quit()
            }
            return
        }

        if (isGameOver) {
            // Finishing the game over animation now.
            OsuGame.music.setFrequencyForcefully(101f)
            return
        }

        video.pause()

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
            }
            multiplayer.submitFinalScore(stat)
            quit()
            return
        }

        stopLoopingSamples()
        val music = OsuGame.music

        if (context.isPerfect) {
            video.pause()

            gameplayClock.pause()
            paused = true
            gameUIScene.ignoreUpdate = true
            hud.ignoreUpdate = true
            return
        }

        SkinSounds.get("failsound").play()
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

        // Things fall away from the top of the screen, which is at [top] in the layer: the top of the part of the
        // playfield that's shown for its layers, in osu!pixels.
        private fun applyEffectToLayer(layer: Container, top: Float = 0f) {
            if (layer.alpha > 0) {
                layer.alpha = max(0f, layer.alpha - 0.007f)
            }

            for (i in 0 until layer.childCount) {
                val entity = layer[i]

                // Hit objects fill the layer, their pieces fall as the layer's own children do.
                if (entity is GameObject) {
                    applyEffectToPieces(entity, top)
                    continue
                }

                applyEffectToEntity(entity, top)
            }
        }

        private fun applyEffectToPieces(gameObject: GameObject, top: Float) {
            for (i in 0 until gameObject.childCount) {
                applyEffectToEntity(gameObject[i], top)
            }
        }

        private fun applyEffectToEntity(entity: Component, top: Float) {
            val y = entity.y.resolve(0f) - top
            val newY = top + if (y < 0f) y * 0.6f else y * 1.01f

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
                val top = objectPlayfield.visibleArea.y
                applyEffectToLayer(objectPlayfield.objects, top)
                applyEffectToLayer(objectPlayfield.judgements, top)
                applyEffectToLayer(objectPlayfield.underlay, top)
                applyEffectToLayer(objectPlayfield.spinners, top)
                applyEffectToLayer(bgScene)

                val decreasedFrequency = max(101f, music.frequency - 300)
                val decreasedSpeed = context.timing.speedMultiplier * (1 - (initialFrequency - decreasedFrequency) / initialFrequency)

                video.setPlaybackSpeed(decreasedSpeed)

                music.setFrequencyForcefully(decreasedFrequency)
            } else {
                video.pause()

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
            && replayControls?.isPlaybackPaused != true) {
            gameplayClock.start()

            video.resume()

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
                gameplay.results.replayStat!!.addHitOffset(acc)
            }
        }

        hud.onAccuracyRegister(type!!, acc.toFloat())
    }


    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?) {
        onTrackingSliders(false)
        if (context.isAutoplay) {
            cursorPresenter.autoCursor!!.onSliderEnd()
        }
        if (replay != null && !replaying) {
            val acc = accuracy.toShort()
            replay!!.addObjectResult(id, acc, tickSet!!.copy())
        }
    }

    override fun onTrackingSliders(isTrackingSliders: Boolean) {
        if (context.isAutoplay && isTrackingSliders) {
            cursorPresenter.autoCursor!!.onSliderTracking()
        }
        if (context.isFlashlight) {
            cursorPresenter.flashlight!!.onTrackingSliders(isTrackingSliders)
        }
    }

    override fun onUpdatedAutoCursor(pX: Float, pY: Float) {
        if (context.isFlashlight) {
            cursorPresenter.flashlight!!.onMouseMove(pX, pY)
        }
    }

    override fun updateAutoBasedPos(pX: Float, pY: Float) {
        if (context.isAutoplay || context.isAutopilot) {
            cursorPresenter.autoCursor!!.setPosition(pX, pY, this)
        }
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
            spawner.skipRemaining { id ->
                stat.registerHit(0, false, false)
                replay.addObjectScore(id, ResultType.MISS)
            }

            val currentTime = currentTimeMillis().toString()
            val replayFilename = MD5Calculator.getStringMD5(loaded.beatmapInfo.filename + currentTime) + currentTime.substring(0, min(3, currentTime.length)) + Replay.FILE_EXTENSION

            replay.setStat(stat)
            replay.save(GameFolders.scorePath + replayFilename)

            if (stat.totalScoreWithMultiplier > 0 && ModAutoplay::class !in stat.mod) {
                stat.replayFilename = replayFilename
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
        if (Settings[OsuSettings.HideInGameUI] || !isHUDEditorMode && !OsuSkin.current.hud.hasElement(HUDElements.pp_counter.name)) {
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
        val spaceWidth = gameplaySpace.measuredSize.x
        val spaceHeight = gameplaySpace.measuredSize.y

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

    //region Seeking

    private fun seekTo(targetTimeSeconds: Float) {
        if (isFinished) {
            return
        }

        val expiredObjects = expiredObjects
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

        gameStarted = false
        leadOut = 0f
        scoreProcessor.resetCombo()
        healthProcessor.reset()
        isGameOver = false
        hasFailed = false

        // Reset auto cursor modifiers.
        cursorPresenter.autoCursor?.stopMoving()

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
        controlPoints.seekTo(targetMs)

        // Reconstruct scoring state up to the seek target.
        val reconstruction = scoreReconstruction.reconstruct(targetMs, if (replaying) replay!!.objectData else null)

        // Objects are spawned again from the first one not judged.
        spawner.seekTo(reconstruction.objectCount)

        if (reconstruction.isStarted) {
            gameStarted = true
        }

        // Reset any in-progress break animation, then re-initialize if the seek target is inside a break.
        val isInBreak = breaks.seekTo(targetMs)

        if (isInBreak) {
            gameStarted = false
        }

        hud.onBreakStateChange(isInBreak)

        hud.onNoteHit(stat)
        hud.onSeek()

        // Replay all touch-down events up to the seek target so HUD elements
        // that depend on touch history can reconstruct their state correctly. A replay's cursor movements are
        // played back from the start again.
        if (context.isAutoplay) {
            for (i in objects.indices) {
                val tapTime = objects[i].startTime.toFloat() / 1000f

                if (tapTime > clampedTime) {
                    break
                }

                hud.onGameplayTouchDown(tapTime)
            }
        } else {
            replayInput?.seekTo(clampedTime)
        }

        updatePPValue(spawner.objectIndex - 1)

        // For variable-rate mods (WindUp/WindDown), the rate at the seek target may differ from
        // the current rate. FramedBeatmapClock.seek() subtracts (userGlobalOffset * rate) from the
        // seek position to compute the audio byte offset, so the rate must be updated first.
        if (!rateAdjustingMods.isEmpty()) {
            val modRate = rateAdjustingMods.calculateRate(targetMs.toDouble())

            val replaySettingsRate = replayControls?.rate ?: 1f

            val targetSpeedMultiplier = modRate * replaySettingsRate

            if (targetSpeedMultiplier != context.timing.speedMultiplier) {
                context.timing.speedMultiplier = targetSpeedMultiplier
                gameplayClock.rate = targetSpeedMultiplier
            }
        }

        // Seek the beatmap clock (also seeks the audio source).
        gameplayClock.seek(clampedTime)

        // Seek video.
        video.seekTo(clampedTime)

        // Suppress hitsounds for slider objects (head, ticks, repeats) that have already passed the seek target.
        // Objects are spawned in the same frame as seekTo runs, but updated in the next frame,
        // so the flag must survive 2 update frames.
        postSeekFrameCount = 2
    }

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

        override fun onEnter() {
            super.onEnter()
            applyBackgroundStyle()
        }

        // The scenes after gameplay show the game's background as everywhere else.
        override fun onLeave() {
            super.onLeave()
            BeatmapBackground.fallbackColor = Color4.Black
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            val controls = replayControls?.takeIf { it.parent != null }

            if (controls != null && event is PointerEvent) {
                // Its gestures are timed on the update thread.
                fun controlsTake() = Verktex.withTreeLock { controls.onInputEvent(event) }

                if (event.action != PointerEvent.Action.Down) {
                    // Only the fingers whose press it took.
                    if (controlsTake()) return true
                } else {
                    // Its bar and popups come first, then the HUD (e.g. the back button), and every other press is
                    // a gesture of the controls (gameplay ignores touches while replaying), but on autoplay's skip
                    // button, which is pressed through the cursors.
                    if (controls.takesPressFirst(event.x, event.y) && controlsTake()) return true
                    if (hud.onInputEvent(event)) return true
                    if (skipBtn?.containsScenePoint(event.x, event.y) != true && controlsTake()) return true
                }
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
            sceneProfile.start()

            do {
                // The scene's clock only paces the animations, it must be advanced here. Each step moves it
                // at most a frame-stable amount, more steps are taken while it is catching up.
                gameplayClock.processFrame()
                this@GameplaySession.update(gameplayClock.elapsedFrameTime)
                sceneProfile.mark("gameplay steps")

                for (i in cursors.indices) {
                    cursors[i]!!.reset(previousFrameTime, gameplayClock.time * 1000)
                }

                super.onUpdate(clock)
                sceneProfile.mark("components")
                // 10ms of allowance when catching up.
            } while (gameplayClock.requiresCatchUp && stopwatch.elapsedSeconds < 0.01f)

            sceneProfile.end("end")
        }

        private val sceneProfile = UpdateProfile(TAG, "scene update")

        private fun updateReplayText(deltaTimeSec: Float) {
            val replayText = replayText!!
            val textWidth = replayText.bounds.width
            val hudWidth = hud.bounds.width
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
