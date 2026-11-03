package ru.nsu.ccfit.zuev.osu.game;

import static kotlinx.coroutines.JobKt.ensureActive;

import android.graphics.PointF;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import kotlin.Unit;
import kotlin.random.Random;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import com.osudroid.audio.SongServiceClock;
import com.osudroid.beatmaps.constants.HitObjectType;
import com.reco1l.verktex.time.SongClock;
import com.reco1l.verktex.time.FrameStableClock;
import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import com.acivev.VibratorManager;
import com.edlplan.framework.math.FMath;
import com.edlplan.osu.support.slider.SliderBody;
import com.edlplan.framework.utils.functionality.SmartIterator;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.game.Cursor;
import com.osudroid.game.CursorEvent;
import com.osudroid.game.replay.ReplaySettingsPanel;
import com.osudroid.multiplayer.api.RoomAPI;
import com.osudroid.beatmaps.DifficultyCalculationManager;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.ui.v2.GameLoaderScene;
import com.osudroid.ui.v2.multi.LobbyScene;
import com.osudroid.data.DatabaseManager;
import com.osudroid.ui.v2.game.SliderTickSprite;
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard;
import com.osudroid.ui.v2.modmenu.ModIcon;
import com.osudroid.utils.Execution;
import android.util.Log;
import com.osudroid.ui.v2.SkinSounds;
import com.osudroid.ui.v2.SkinTextures;
import com.reco1l.verktex.Verktex;
import com.reco1l.verktex.data.ClipMode;
import com.reco1l.verktex.data.Dimension;
import com.reco1l.verktex.data.DimensionKt;
import com.reco1l.verktex.graphics.BlendFactor;
import com.reco1l.verktex.graphics.BlendMode;
import com.reco1l.verktex.input.Input;
import com.reco1l.verktex.input.InputEvent;
import com.reco1l.verktex.input.PointerEvent;
import com.reco1l.verktex.math.Easing;
import com.reco1l.verktex.texture.Textures;
import com.reco1l.verktex.time.FrameClock;
import com.reco1l.verktex.ui.ChoreographyKt;
import com.reco1l.verktex.ui.UIComponent;
import com.reco1l.verktex.ui.UIScene;
import com.reco1l.verktex.ui.UIText;
import com.reco1l.verktex.ui.container.UIContainer;
import com.reco1l.verktex.ui.shape.UIBox;
import com.reco1l.verktex.ui.UIAnimatedSprite;
import com.reco1l.verktex.ui.UISprite;
import com.reco1l.verktex.data.Anchor;
import com.osudroid.ui.v2.UIVideo;
import com.osudroid.resources.R;
import com.osudroid.ui.v2.game.FollowPointConnection;
import com.osudroid.ui.v2.hud.GameplayHUD;
import com.osudroid.ui.v2.hud.elements.HUDPPCounter;
import com.osudroid.multiplayer.Multiplayer;

import com.reco1l.verktex.data.Color4;
import com.osudroid.GameMode;
import com.osudroid.beatmaps.Beatmap;
import com.osudroid.beatmaps.ComboColor;
import com.osudroid.beatmaps.DroidPlayableBeatmap;
import com.osudroid.beatmaps.HitWindow;
import com.osudroid.beatmaps.constants.BeatmapCountdown;
import com.osudroid.beatmaps.hitobjects.HitCircle;
import com.osudroid.beatmaps.hitobjects.HitObject;
import com.osudroid.beatmaps.hitobjects.Slider;
import com.osudroid.beatmaps.hitobjects.Spinner;
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderRepeat;
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick;
import com.osudroid.beatmaps.sections.BeatmapDifficulty;
import com.osudroid.beatmaps.timings.BreakPeriod;
import com.osudroid.beatmaps.timings.EffectControlPoint;
import com.osudroid.beatmaps.timings.TimingControlPoint;
import com.osudroid.difficulty.BeatmapDifficultyCalculator;
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes;
import com.osudroid.difficulty.attributes.DroidPerformanceAttributes;
import com.osudroid.difficulty.attributes.PerformanceAttributes;
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes;
import com.osudroid.difficulty.attributes.StandardPerformanceAttributes;
import com.osudroid.difficulty.attributes.TimedDifficultyAttributes;
import com.osudroid.difficulty.calculator.DroidPerformanceCalculationParameters;
import com.osudroid.difficulty.calculator.PerformanceCalculationParameters;
import com.osudroid.difficulty.calculator.StandardPerformanceCalculationParameters;
import com.osudroid.game.GameplayHitSampleInfo;
import com.osudroid.math.Interpolation;
import com.osudroid.mods.*;
import com.osudroid.utils.ModHashMap;
import com.osudroid.utils.ModUtils;
import com.reco1l.verktex.time.Stopwatch;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import ru.nsu.ccfit.zuev.audio.effect.Metronome;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import com.osudroid.ui.v2.game.BreakAnimator;
import com.osudroid.ui.v2.game.ComboBurst;
import com.osudroid.ui.v2.game.Countdown;
import com.osudroid.ui.v2.game.FlashLightEntity;
import com.osudroid.ui.v2.game.AutoCursor;
import com.osudroid.ui.v2.game.CursorEntity;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import com.osudroid.ui.v2.game.PauseMenu;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osu.scoring.TouchType;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager;

public class GameScene implements GameObjectListener {
    public static final int CursorCount = 10;
    private final int maximumActiveCursorCount = 3;
    private Cursor[] cursors = new Cursor[CursorCount];
    public String audioFilePath = null;
    private GameUIScene scene;
    private UIContainer root;
    private UIContainer backgroundLayer;
    private UIContainer playfield;
    private UIContainer bgScene, mgScene, fgScene;
    private UIScene oldScene;
    private FailAnimation failAnimation;
    private PauseMenu pauseMenu;
    private UIText replayText;
    private UIBox sceneBorder;
    private UIComponent beatmapBackground;
    private float beatmapBackgroundWidth;
    private float beatmapBackgroundHeight;
    private Beatmap parsedBeatmap;
    private DroidPlayableBeatmap playableBeatmap;
    private BeatmapInfo lastBeatmapInfo;
    private ScoringScene scoringScene;
    private TimingControlPoint[] timingControlPoints;
    private int timingControlPointIndex;
    private EffectControlPoint[] effectControlPoints;
    private int effectControlPointIndex;
    private TimingControlPoint activeTimingPoint;
    private EffectControlPoint activeEffectPoint;
    private int lastObjectId = -1;
    private float leadOut = 0;
    private HitObject[] objects;
    private int objectIndex;
    private int postSeekFrameCount;
    private ArrayList<Color4> comboColors;
    private boolean comboWasMissed = false;
    private boolean comboWas100 = false;
    private ArrayList<GameObject> activeObjects;
    private ArrayList<GameObject> expiredObjects;
    private final Set<GameObject> processedExpiredObjects = Collections.newSetFromMap(new IdentityHashMap<>());
    private GameObject judgeableObject;
    private BreakPeriod[] breakPeriods;
    private int breakPeriodIndex;
    private Metronome metronome;
    private float scale;
    public StatisticV2 stat;
    private boolean gameStarted;
    private final FrameStableClock gameplayClock;
    private float initialStartTime;
    private int totalLength = Integer.MAX_VALUE;
    private boolean paused;
    private UISprite skipBtn;
    private float skipTime;
    private double distToNextObject;
    private CursorEntity[] cursorSprites;
    private AutoCursor autoCursor;
    private FlashLightEntity flashlightSprite;
    private int mainCursorId = -1;
    private Replay replay;
    private boolean replaying;
    private String replayFilePath;
    private UIBox dimRectangle = null;
    private ComboBurst comboBurst;
    private int failcount = 0;
    private Color4 sliderBorderColor;
    private SliderPath[] sliderPaths = null;
    private SliderBody.RenderPathCache[] sliderRenderPaths = null;
    private int sliderIndex = 0;
    private UISprite unrankedSprite;
    private final ArrayList<ModIcon> modIcons = new ArrayList<>();
    private final ArrayList<IModApplicableToTrackRate> rateAdjustingMods = new ArrayList<>();

    @Nullable

    public HitWindow hitWindow;
    private ModHashMap lastMods;

    private final AtomicInteger loadingRequestId = new AtomicInteger(0);
    private CompletableFuture<?> loadingPipeline;
    private Job gameLoadingJob;

    private PerformanceAttributes performanceAttributes;
    private PerformanceCalculationParameters performanceCalculationParameters;
    private volatile TimedDifficultyAttributes<DroidDifficultyAttributes>[] droidTimedDifficultyAttributes;
    private volatile TimedDifficultyAttributes<StandardDifficultyAttributes>[] standardTimedDifficultyAttributes;

    @Nullable
    private Job ppCalculationJob;

    private ReplaySettingsPanel replaySettingsPanel;

    // Game

    /**
     * Whether the game is over.
     */
    private boolean isGameOver = false;

    /**
     * The break time animator.
     */
    private BreakAnimator breakAnimator;

    /**
     * The countdown animator.
     */
    public Countdown countdownAnimator;

    /**
     * Whether the game is ready to start.
     */
    public boolean isReadyToStart = false;


    // UI

    /**
     * The gameplay HUD
     */
    public GameplayHUD hud;

    /**
     * Whether the HUD editor mode is enabled.
     */
    public boolean isHUDEditorMode = false;

    /**
     * Whether the game started in HUD editor mode.
     */
    public boolean startedFromHUDEditor = false;


    // Timing

    /**
     * The time at which the last frame was rendered with respect to {@link SystemClock#uptimeMillis()}.
     * <br>
     * If 0, a frame has not been rendered yet.
     */
    private long previousFrameTime;

    /**
     * The start time of the first object in seconds.
     */
    public float firstObjectStartTime;

    /**
     * The end time of the last object in seconds.
     */
    public float lastObjectEndTime;

    // Video support

    /**
     * The current video loading {@link Job}.
     */
    @Nullable
    private Job videoLoadingJob;

    /**
     * Whether video is enabled.
     */
    private boolean videoEnabled;

    /**The video sprite*/
    private UIVideo video;

    /**Video offset aka video start time in seconds*/
    private float videoOffset;

    /**Whether the video has started*/
    private boolean videoStarted;


    // Multiplayer

    /**Indicates the last time that the user pressed the back button, used to reset {@code backPressCount}*/
    private float lastBackPressTime = -1f;

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    public boolean hasFailed = false;

    /**Indicates that the player has requested skip*/
    private boolean isSkipRequested = false;

    /**Real time elapsed in milliseconds since the game has started*/
    private long realTimeElapsed = 0;

    /**Real time elapsed in milliseconds since the latest statistic data was sent*/
    private long statisticDataTimeElapsed = 0;

    /**Last score data chunk sent to server, used to determine if the data was changed.*/
    private ScoreBoardItem lastScoreSent = null;


    public GameScene() {
        createScene();
    }

    private void createScene() {
        root = new UIContainer();
        root.setWidth(Dimension.FillAvailable);
        root.setHeight(Dimension.FillAvailable);
        root.setBackgroundColor(Color4.Black);

        backgroundLayer = new UIContainer();
        backgroundLayer.setWidth(Dimension.FillAvailable);
        backgroundLayer.setHeight(Dimension.FillAvailable);

        playfield = new UIContainer();
        playfield.setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        playfield.setScaleOrigin(Anchor.TopLeft);

        bgScene = createLayer();
        mgScene = createLayer();
        mgScene.setClipping(ClipMode.Bounds);
        fgScene = createLayer();

        playfield.plusAssign(bgScene);
        playfield.plusAssign(mgScene);
        playfield.plusAssign(fgScene);

        root.plusAssign(backgroundLayer);
        root.plusAssign(playfield);

        createScene();
        scene.setBackground(root);
    }

    private UIContainer createLayer() {
        var layer = new UIContainer();
        layer.setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        return layer;
    }

    public void setScoringScene(final ScoringScene sc) {
        scoringScene = sc;
    }

    public void setOldScene(final UIScene oscene) {
        oldScene = oscene;
    }

    private void loadBackground() {
        if (dimRectangle != null) {
            dimRectangle.removeSelf();
            dimRectangle = null;
        }

        if (sceneBorder != null) {
            sceneBorder.removeSelf();
            sceneBorder = null;
        }

        releaseVideo();

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        var textureRegion = Config.isSafeBeatmapBg() || playableBeatmap.getEvents().backgroundFilename == null
                ? SkinTextures.get("menu-background")
                : Textures.INSTANCE.get("::background");

        if (textureRegion == null) {
            Color4 backgroundColor = playableBeatmap.getEvents().getBackgroundColor();

            var box = new UIBox();
            box.setWidth(Dimension.FillAvailable);
            box.setHeight(Dimension.FillAvailable);
            box.setBackgroundColor(backgroundColor != null ? backgroundColor : Color4.Black);

            beatmapBackground = box;
            beatmapBackgroundWidth = 0f;
            beatmapBackgroundHeight = 0f;
        } else {
            beatmapBackground = new UISprite(textureRegion);
            beatmapBackgroundWidth = textureRegion.getWidth();
            beatmapBackgroundHeight = textureRegion.getHeight();
        }
    }

    public void loadVideo(BeatmapInfo beatmapInfo) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getNumber("bgbrightness", 25f) / 100f;
        var videoFilename = playableBeatmap.getEvents().videoFilename;
        videoEnabled = brightness > 0.02f && Config.getBoolean("enableVideo", false) && videoFilename != null;

        if (!videoEnabled) {
            cancelVideoLoading();
            releaseVideo();
            return;
        }

        if (video != null) {
            return;
        }

        if (videoLoadingJob != null && !videoLoadingJob.isCompleted()) {
            return;
        }

        // Check if video file exists before attempting to load.
        var videoFile = new File(beatmapInfo.getAbsoluteSetDirectory() + "/" + videoFilename);

        if (!videoFile.exists()) {
            return;
        }

        videoLoadingJob = Execution.async(scope -> {
            try {
                var video = new UIVideo(beatmapInfo.getAbsoluteSetDirectory() + "/" + videoFilename);
                video.setAlpha(0f);

                ensureActive(scope.getCoroutineContext());

                this.video = video;

                // applyBackground is called from the onReady callback once ExoPlayer reports the video dimensions via
                // onVideoSizeChanged, rather than blocking here.
                // If dimensions are already known (fast local file), the callback fires immediately.
                video.setOnReady(() -> Execution.updateThread(this::applyBackground));
            } catch (Exception e) {
                video = null;
                Log.e("GameScene", "Error while loading video background.", e);
            }

            videoLoadingJob = null;
        });
    }

    private void cancelVideoLoading() {
        if (videoLoadingJob != null) {
            videoLoadingJob.cancel(new CancellationException("Video loading job cancelled"));
            videoLoadingJob = null;
        }
    }

    private void releaseVideo() {
        if (video != null) {
            video.release();
            video = null;
            videoStarted = false;
        }
    }

    private void applyBackground() {
        // Guard: video sprite exists but dimensions not yet reported by ExoPlayer.
        // onVideoSizeChanged will trigger another applyBackground call when they arrive.
        if (video != null && (video.getVideoWidth() == 0 || video.getVideoHeight() == 0)) {
            return;
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getNumber("bgbrightness", 25f) / 100f;

        float playfieldSize = Config.getPlayfieldSize();

        var video = this.video;
        var sceneBorder = this.sceneBorder;

        if (sceneBorder == null && Config.isDisplayPlayfieldBorder() && playfieldSize < 1f) {
            sceneBorder = new UIBox() {
                {
                    setAnchor(Anchor.Center);
                    setOrigin(Anchor.Center);
                    setBorderWidth(new Dimension.Fixed.Pixels(5f));
                    setBorderColor(new Color4(1f, 1f, 1f, 1f));
                    setAlpha(Interpolation.linear(0.2f, 0.8f, brightness));
                    setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                }
            };

            playfield.add(0, sceneBorder);
            this.sceneBorder = sceneBorder;
        }

        var showVideo = videoEnabled && video != null;
        UIComponent background = showVideo ? video : beatmapBackground;

        float backgroundWidth = showVideo ? video.getVideoWidth() : beatmapBackgroundWidth;
        float backgroundHeight = showVideo ? video.getVideoHeight() : beatmapBackgroundHeight;

        backgroundLayer.clearChildren();

        if (background != null) {
            if (backgroundWidth > 0f && backgroundHeight > 0f) {
                // Fit in the window through the size instead of the scale, the size is relative to its parent.
                float windowWidth = root.getBounds().getWidth();
                float windowHeight = root.getBounds().getHeight();

                float factor = Config.isKeepBackgroundAspectRatio()
                        ? windowHeight / backgroundHeight
                        : windowWidth / backgroundWidth;

                background.setAnchor(Anchor.Center);
                background.setOrigin(Anchor.Center);
                background.setSize(backgroundWidth * factor, backgroundHeight * factor);
            }

            backgroundLayer.plusAssign(background);
        }

        if (dimRectangle == null) {
            dimRectangle = new UIBox();
            dimRectangle.setWidth(Dimension.FillAvailable);
            dimRectangle.setHeight(Dimension.FillAvailable);
            dimRectangle.setBackgroundColor(Color4.Black);
        }

        dimRectangle.setAlpha(1f - brightness);
        backgroundLayer.plusAssign(dimRectangle);

        if (breakAnimator != null) {
            breakAnimator.setDimBrightness(brightness);
            breakAnimator.setDimRectangle(dimRectangle);
        }

        // Storyboards are drawn here once they're available again, on top of the background and below the playfield.
    }

    private boolean loadGame(final BeatmapInfo beatmapInfo, final String rFile, final ModHashMap mods, @Nullable CoroutineScope scope) {
        if (!SecurityUtils.verifyFileIntegrity(GlobalManager.getInstance().getMainActivity())) {
            ToastLogger.showText(com.osudroid.resources.R.string.file_integrity_tampered, true);
            return false;
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFilePath = Config.getCachePath() + "/" +
                    MD5Calculator.getStringMD5(rFile) + ".odr";
            Log.i("GameScene", "ReplayFile = " + replayFilePath);
            if (!OnlineFileOperator.downloadFile(rFile, this.replayFilePath)) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_cantdownload, true);
                return false;
            }
        } else
            this.replayFilePath = rFile;

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        boolean shouldParseBeatmap = parsedBeatmap == null || !parsedBeatmap.getMd5().equals(beatmapInfo.getMD5());

        if (shouldParseBeatmap) {
            try {
                parsedBeatmap = BeatmapCache.getBeatmap(beatmapInfo, true, GameMode.Droid, scope);
            } catch (IOException | IllegalArgumentException e) {
                Log.e("GameScene", "startGame: " + e.getMessage());
                ToastLogger.showText(e.getMessage(), true);
                return false;
            }
        }

        if (parsedBeatmap == null) {
            return false;
        }

        if (!parsedBeatmap.getMd5().equals(beatmapInfo.getMD5())) {
            ToastLogger.showText(com.osudroid.resources.R.string.file_integrity_tampered, true);
            return false;
        }

        if (parsedBeatmap.getHitObjects().objects.isEmpty()) {
            ToastLogger.showText("Empty Beatmap", true);
            return false;
        }

        // Ensure that only relevant mods are applied.
        mods.values().removeIf(m -> !m.isRelevant());

        boolean differentPlayableBeatmap = shouldParseBeatmap || lastMods == null || !lastMods.equals(mods);

        var playableBeatmap = differentPlayableBeatmap
            ? parsedBeatmap.createDroidPlayableBeatmap(mods.values())
            : this.playableBeatmap;

        this.playableBeatmap = playableBeatmap;

        if (isHUDEditorMode || OsuSkin.get().getHUDSkinData().hasElement(HUDPPCounter.class)) {
            final var finalParsedBeatmap = parsedBeatmap;

            // Calculate timed difficulty attributes
            switch (Config.getDifficultyAlgorithm()) {
                case droid -> {
                    performanceAttributes = new DroidPerformanceAttributes();
                    performanceCalculationParameters = new DroidPerformanceCalculationParameters();

                    if (droidTimedDifficultyAttributes == null || differentPlayableBeatmap) {
                        final var finalPlayableBeatmap = playableBeatmap;

                        ppCalculationJob = Execution.async(ppScope -> {
                            droidTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(
                                finalParsedBeatmap, finalPlayableBeatmap, ppScope
                            );

                            Execution.updateThread(() -> {
                                if (objectIndex > 0) {
                                    updatePPValue(objectIndex - 1);
                                }
                            });
                        });
                    }
                }

                case standard -> {
                    performanceAttributes = new StandardPerformanceAttributes();
                    performanceCalculationParameters = new StandardPerformanceCalculationParameters();

                    if (standardTimedDifficultyAttributes == null || differentPlayableBeatmap) {
                        final var modValues = mods.values();

                        ppCalculationJob = Execution.async(ppScope -> {
                            standardTimedDifficultyAttributes = BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                                finalParsedBeatmap, modValues, ppScope
                            );

                            Execution.updateThread(() -> {
                                if (objectIndex > 0) {
                                    updatePPValue(objectIndex - 1);
                                }
                            });
                        });
                    }
                }
            }
        }

        // Load backgrounds early to minimize waiting time.
        loadBackground();
        loadVideo(beatmapInfo);

        rateAdjustingMods.clear();

        for (var mod : mods.values()) {
            if (mod instanceof IModApplicableToTrackRate rateMod) {
                rateAdjustingMods.add(rateMod);
            }
        }

        // TODO skin manager
        if (shouldParseBeatmap) {
            BeatmapSkinManager.getInstance().loadBeatmapSkin(playableBeatmap.getBeatmapsetPath());
        }

        var breaks = playableBeatmap.getEvents().breaks;

        if (shouldParseBeatmap || breakPeriods == null || breakPeriods.length != breaks.size()) {
            breakPeriods = new BreakPeriod[breaks.size()];
            System.arraycopy(breaks.toArray(), 0, breakPeriods, 0, breakPeriods.length);
        }

        try {
            var musicFile = new File(beatmapInfo.getAudioPath());

            if (!musicFile.exists()) {
                throw new FileNotFoundException(musicFile.getPath());
            }

            audioFilePath = musicFile.getPath();

        } catch (final Exception e) {
            Log.e("GameScene", "Load Music: " + e.getMessage());
            ToastLogger.showText(e.getMessage(), true);
            return false;
        }

        var hitObjects = playableBeatmap.getHitObjects().objects;
        var firstObject = hitObjects.get(0);
        scale = firstObject.getScreenSpaceGameplayScale();

        GameHelper.setOverallDifficulty(playableBeatmap.getDifficulty().od);
        GameHelper.setHealthDrain(playableBeatmap.getDifficulty().hp);
        GameHelper.setSpeedMultiplier(ModUtils.calculateRate(mods.values(), Double.NEGATIVE_INFINITY));

        GameHelper.setOriginalTimePreempt((float) BeatmapDifficulty.difficultyRange(
            playableBeatmap.getDifficulty().getAR(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ));

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GlobalManager.getInstance().getSongService().preLoad(audioFilePath, GameHelper.getSpeedMultiplier(),
            GameHelper.getSpeedMultiplier() != 1f &&
                (Config.isShiftPitchInRateChange() || mods.contains(ModNightCore.class) || mods.contains(ModOldNightCore.class)));

        gameplayClock.setRate(GameHelper.getSpeedMultiplier());

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        totalLength = GlobalManager.getInstance().getSongService().getLength();
        judgeableObject = null;
        breakPeriodIndex = 0;
        objectIndex = 0;
        lastObjectId = -1;
        hitWindow = playableBeatmap.getHitWindow();
        videoStarted = false;
        videoOffset = playableBeatmap.getEvents().videoStartTime / 1000f;

        if (shouldParseBeatmap || objects == null || objects.length != hitObjects.size()) {
            objects = new HitObject[hitObjects.size()];
        }

        if (differentPlayableBeatmap || objects[0] == null) {
            System.arraycopy(hitObjects.toArray(), 0, objects, 0, objects.length);
        }

        firstObjectStartTime = (float) firstObject.startTime / 1000;
        lastObjectEndTime = (float) objects[objects.length - 1].getEndTime() / 1000;

        int estimatedMaxActiveObjects = Math.max(10, estimateMaximumActiveObjects());

        if (activeObjects != null) {
            activeObjects.clear();
            activeObjects.ensureCapacity(estimatedMaxActiveObjects);
        } else {
            activeObjects = new ArrayList<>(estimatedMaxActiveObjects);
        }

        if (expiredObjects != null) {
            expiredObjects.clear();
            expiredObjects.ensureCapacity(estimatedMaxActiveObjects);
        } else {
            expiredObjects = new ArrayList<>(estimatedMaxActiveObjects);
        }

        float firstObjectTimePreempt = (float) firstObject.timePreempt / 1000;
        float skipTargetTime = firstObjectStartTime - Math.max(2f, firstObjectTimePreempt);

        gameplayClock.setGameplayStartTime(skipTargetTime);
        gameplayClock.seek(Math.min(0, skipTargetTime));
        skipTime = skipTargetTime - 1;

        // Some beatmaps specify a current lead-in time, which overrides the default lead-in time above.
        float leadIn = playableBeatmap.getGeneral().audioLeadIn / 1000f;
        if (leadIn > 0) {
            gameplayClock.seek(Math.min(gameplayClock.getTime(), firstObjectStartTime - leadIn));
        }

        // Ensure the video has time to start.
        // Even when video is not activated, apply offset anyway to ensure that everyone in multiplayer starts at the
        // same time regardless of the setting.
        gameplayClock.seek(Math.min(gameplayClock.getTime(), videoOffset));

        sliderBorderColor = null;
        if (playableBeatmap.getColors().getSliderBorderColor() != null) {
            sliderBorderColor = playableBeatmap.getColors().getSliderBorderColor();
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor()) {
            sliderBorderColor = OsuSkin.get().getSliderBorderColor();
        }

        if (sliderBorderColor == null) {
            sliderBorderColor = BeatmapSkinManager.getInstance().getSliderColor();
        }

        comboColors = new ArrayList<>();
        for (ComboColor comboColor : playableBeatmap.getColors().comboColors) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            comboColors.add(comboColor.getColor());
        }

        if (comboColors.isEmpty() || OsuSkin.get().isForceOverrideComboColor()) {
            comboColors.clear();
            comboColors.addAll(OsuSkin.get().getComboColor());
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        var timingControlPointManager = playableBeatmap.getControlPoints().timing;
        var effectControlPointManager = playableBeatmap.getControlPoints().effect;

        if (shouldParseBeatmap || timingControlPoints == null) {
            timingControlPoints = new TimingControlPoint[timingControlPointManager.controlPoints.size()];
            System.arraycopy(timingControlPointManager.controlPoints.toArray(), 0, timingControlPoints, 0, timingControlPoints.length);
        }

        if (shouldParseBeatmap || effectControlPoints == null) {
            effectControlPoints = new EffectControlPoint[effectControlPointManager.controlPoints.size()];
            System.arraycopy(effectControlPointManager.controlPoints.toArray(), 0, effectControlPoints, 0, effectControlPoints.length);
        }

        activeTimingPoint = timingControlPoints.length > 0 ? timingControlPoints[0] : timingControlPointManager.defaultControlPoint;
        activeEffectPoint = effectControlPoints.length > 0 ? effectControlPoints[0] : effectControlPointManager.defaultControlPoint;
        timingControlPointIndex = 0;
        effectControlPointIndex = 0;

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(0);
        GameHelper.setSamplesMatchPlaybackRate(playableBeatmap.getGeneral().samplesMatchPlaybackRate);

        GameObjectPool.getInstance().purge();

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        FollowPointConnection.renew(16);
        SliderTickSprite.renew(16);

        // TODO replay
        replaying = false;
        replay = new Replay(true);
        replay.setObjectCount(hitObjects.size());
        replay.setBeatmap(beatmapInfo.getFullBeatmapsetName(), beatmapInfo.getFullBeatmapName(), playableBeatmap.getMd5());

        if (replayFilePath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            var replayStat = new StatisticV2();
            replayStat.setMod(mods);
            replay.setStat(replayStat);

            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            replaying = replay.load(replayFilePath, true);

            if (!replaying) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_invalid, true);
                return false;
            }

            // In older versions, replay uploads are separated from scores, which means that they may not be uploaded
            // for reasons independent of score uploads (e.g., network failure). When this happens, replays may be very
            // off such that it causes gameplay to appear very wrong (e.g., a score has Hard Rock/Mirror mod while its
            // replay does not). While this can theoretically happen to any score data (not just mods), checking for
            // mods for the time being is enough to dislodge major inconsistencies in gameplay.
            // Checking for mod existence (not equality) is enough for this case since affected mods do not have
            // customizations.
            for (var mod : mods.values()) {
                if (!replay.getStat().getMod().containsKey(mod.getClass())) {
                    ToastLogger.showText(com.osudroid.resources.R.string.replay_invalid, true);
                    return false;
                }
            }

            GameHelper.setReplayVersion(replay.replayVersion);
        } else if (mods.contains(ModAutoplay.class)) {
            replay = null;
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GameObjectPool.getInstance().preload();

        sliderIndex = 0;

        if (sliderPaths == null || sliderRenderPaths == null || differentPlayableBeatmap) {
            calculateAllSliderPaths(scope);
        }

        lastMods = mods;
        lastBeatmapInfo = beatmapInfo;

        // Resetting variables before starting the game.
        Multiplayer.finalData = null;
        hasFailed = false;
        lastBackPressTime = -1f;
        isSkipRequested = false;
        realTimeElapsed = 0;
        statisticDataTimeElapsed = 0;
        leadOut = 0;
        lastScoreSent = null;
        isGameOver = false;

        paused = false;
        gameStarted = false;
        return true;
    }

    public UIScene getScene() {
        return scene;
    }

    public void restartGame() {
        startGame(null, null, null);
    }


    public void startGame(BeatmapInfo beatmapInfo, String replayFile, ModHashMap mods) {
        startGame(beatmapInfo, replayFile, mods, false);
    }

    public void startGame(BeatmapInfo beatmapInfo, String replayFile, ModHashMap mods, boolean isHUDEditor) {
        isReadyToStart = false;
        isHUDEditorMode = isHUDEditor;
        startedFromHUDEditor = isHUDEditor;
        resetPlayfieldSizeScale();

        createScene();
        failcount = 0;
        mainCursorId = -1;
        gameplayClock.changeSource(null);
        modIcons.clear();

        final String rfile = beatmapInfo != null ? replayFile : this.replayFilePath;
        final int requestId = loadingRequestId.incrementAndGet();

        BeatmapInfo beatmapToUse = beatmapInfo != null ? beatmapInfo : lastBeatmapInfo;
        boolean isRestart = beatmapInfo == null && replayFile == null && mods == null;
        ModHashMap modsToUse;

        if (isHUDEditor) {
            modsToUse = new ModHashMap();
            modsToUse.put(ModAutoplay.class);
        } else {
            modsToUse = mods != null ? mods.deepCopy() : lastMods;
        }

        GameLoaderScene scene = new GameLoaderScene(this, beatmapToUse, modsToUse, isRestart);
        Verktex.withTreeLock(() -> {
            Verktex.pushScene(scene);
            return Unit.INSTANCE;
        });

        SkinSounds.get("failsound").stop();

        var pipeline = cancelLoading(false)
                .thenCompose((ignored) -> DifficultyCalculationManager.stopCalculation())
                .thenRun(() -> {
                    if (requestId != loadingRequestId.get()) {
                        return;
                    }

                    gameLoadingJob = Execution.async((scope) -> {
                        boolean succeeded = false;
                        boolean cancelled = false;

                        try {
                            if (requestId != loadingRequestId.get()) {
                                return;
                            }

                            succeeded = loadGame(beatmapToUse, rfile, modsToUse, scope);

                            if (succeeded && requestId == loadingRequestId.get()) {
                                prepareScene();
                            }
                        } catch (Exception e) {
                            if (e instanceof CancellationException) {
                                cancelled = true;
                                throw e;
                            }

                            Log.e("GameScene", "Failed to load gameplay", e);
                        } finally {
                            if (requestId == loadingRequestId.get()) {
                                if (!succeeded && !cancelled) {
                                    Execution.updateThread(this::quit);
                                }

                                gameLoadingJob = null;
                            }
                        }
                    });
                });

        loadingPipeline = pipeline;

        pipeline.whenComplete((ignored, error) -> {
            if (loadingPipeline == pipeline) {
                loadingPipeline = null;
            }
        });
    }

    public CompletableFuture<Unit> cancelLoading() {
        return cancelLoading(true);
    }

    private CompletableFuture<Unit> cancelLoading(boolean invalidatePendingStart) {
        if (invalidatePendingStart) {
            loadingRequestId.incrementAndGet();
        }

        var gameLoadingJob = this.gameLoadingJob;
        var videoLoadingJob = this.videoLoadingJob;
        var ppCalculationJob = this.ppCalculationJob;
        var loadingPipeline = this.loadingPipeline;

        this.gameLoadingJob = null;
        this.videoLoadingJob = null;
        this.ppCalculationJob = null;

        var jobCancellation = CompletableFuture.allOf(
            Execution.stopAsync(gameLoadingJob),
            Execution.stopAsync(videoLoadingJob),
            Execution.stopAsync(ppCalculationJob)
        );

        var pipelineDrain = loadingPipeline != null
            ? loadingPipeline.exceptionally((error) -> null)
            : CompletableFuture.completedFuture(Unit.INSTANCE);

        return CompletableFuture.allOf(jobCancellation, pipelineDrain).thenApply((ignored) -> Unit.INSTANCE);
    }

    private void replaceCurrentScene(UIScene newScene) {
        Verktex.withTreeLock(() -> {
            Verktex.popScene();
            Verktex.pushScene(newScene);
            return Unit.INSTANCE;
        });
    }

    private void returnToPreviousScene() {
        Verktex.withTreeLock(() -> {
            Verktex.popScene();
            return Unit.INSTANCE;
        });
    }

    private void prepareScene() {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        stat = new StatisticV2();
        stat.setMod(lastMods);
        stat.migrateLegacyMods(parsedBeatmap.getDifficulty());
        stat.calculateModScoreMultiplier(parsedBeatmap.getDifficulty());
        stat.canFail = !stat.getMod().contains(ModNoFail.class)
                && !stat.getMod().contains(ModRelax.class)
                && !stat.getMod().contains(ModAutopilot.class)
                && !stat.getMod().contains(ModAutoplay.class);

        float difficultyScoreMultiplier = 1 + Math.min(parsedBeatmap.getDifficulty().od, 10) / 10f +
                Math.min(parsedBeatmap.getDifficulty().hp, 10) / 10f;

        // The maximum CS of osu!droid mapped to osu!standard is ~17.62.
        difficultyScoreMultiplier += (Math.min(parsedBeatmap.getDifficulty().gameplayCS, 17.62f) - 3) / 4f;

        stat.setDiffModifier(difficultyScoreMultiplier);
        stat.setBeatmapNoteCount(objects.length);
        stat.setBeatmapMaxCombo(parsedBeatmap.getMaxCombo());

        GameHelper.setHardRock(lastMods.ofType(ModHardRock.class));
        GameHelper.setDoubleTime(lastMods.ofType(ModDoubleTime.class));
        GameHelper.setNightCore(lastMods.contains(ModNightCore.class) ? lastMods.ofType(ModNightCore.class) : lastMods.ofType(ModOldNightCore.class));
        GameHelper.setHalfTime(lastMods.ofType(ModHalfTime.class));
        GameHelper.setHidden(lastMods.ofType(ModHidden.class));
        GameHelper.setTraceable(lastMods.ofType(ModTraceable.class));
        GameHelper.setFlashlight(lastMods.ofType(ModFlashlight.class));
        GameHelper.setRelax(lastMods.ofType(ModRelax.class));
        GameHelper.setAutopilot(lastMods.ofType(ModAutopilot.class));
        GameHelper.setAutoplay(lastMods.ofType(ModAutoplay.class));
        GameHelper.setSuddenDeath(lastMods.ofType(ModSuddenDeath.class));
        GameHelper.setPerfect(lastMods.ofType(ModPerfect.class));
        GameHelper.setSynesthesia(lastMods.ofType(ModSynesthesia.class));
        GameHelper.setScoreV2(lastMods.ofType(ModScoreV2.class));
        GameHelper.setEasy(lastMods.ofType(ModEasy.class));
        GameHelper.setMuted(lastMods.ofType(ModMuted.class));
        GameHelper.setFreezeFrame(lastMods.ofType(ModFreezeFrame.class));
        GameHelper.setApproachDifferent(lastMods.ofType(ModApproachDifferent.class));

        int cursorCount = replaying && replay != null ? replay.cursorMoves.size() : CursorCount;

        cursors = new Cursor[cursorCount];

        for (int i = 0; i < cursorCount; i++) {
            cursors[i] = new Cursor();
        }

        comboWas100 = false;
        comboWasMissed = false;
        previousFrameTime = 0;

        metronome = null;
        if ((Config.getMetronomeSwitch() == 1 && GameHelper.isNightCore())
                || (GameHelper.isMuted() && GameHelper.getMuted().isEnableMetronome())
                || Config.getMetronomeSwitch() == 2) {
            metronome = new Metronome();
        }

        distToNextObject = 0;

        // TODO passive objects
        if ((replaying || Config.isShowCursor()) && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
            cursorSprites = new CursorEntity[cursorCount];
            for (int i = 0; i < cursorCount; i++) {
                cursorSprites[i] = new CursorEntity();
                cursorSprites[i].attachToScene(fgScene);
            }
        } else {
            cursorSprites = null;
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor = new AutoCursor();
            autoCursor.attachToScene(fgScene);
        }

        final var countdown = playableBeatmap.getGeneral().countdown;
        if (Config.isCorovans() && countdown != BeatmapCountdown.NoCountdown) {
            float cdSpeed = countdown.speed;
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH;
            if (cdSpeed != 0 && firstObjectStartTime - gameplayClock.getTime() >= cdSpeed * Countdown.COUNTDOWN_LENGTH) {
                countdownAnimator = new Countdown(bgScene, cdSpeed, 0, firstObjectStartTime - gameplayClock.getTime());
            }
        }

        if (Config.isComboburst()) {
            comboBurst = new ComboBurst(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            comboBurst.attachAll(bgScene);
        }

        var position = new PointF(Config.getRES_WIDTH() - 130, 130);

        for (var mod : lastMods.values()) {
            if (!mod.isUserPlayable()) {
                continue;
            }

            var icon = new ModIcon(mod);
            icon.setPosition(position.x, position.y);
            icon.setOrigin(Anchor.Center);
            icon.setSize(68, 66);
            icon.setScale(1.2f);

            fgScene.plusAssign(icon);
            modIcons.add(icon);

            position.x -= 25f;
        }

        boolean hasUnrankedMod = SmartIterator.wrap(lastMods.values().iterator()).applyFilter(m -> !m.isRanked()).hasNext();
        if (hasUnrankedMod || Config.isRemoveSliderLock()) {
            unrankedSprite = new UISprite(SkinTextures.get("play-unranked"));
            unrankedSprite.setAnchor(Anchor.TopCenter);
            unrankedSprite.setOrigin(Anchor.Center);
            unrankedSprite.setPosition(0, 80);
            fgScene.plusAssign(unrankedSprite);
        }

        if (GameHelper.isFlashlight()) {
            flashlightSprite = new FlashLightEntity(GameHelper.getFlashlight());
            fgScene.add(0, flashlightSprite);
        }

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        if (hud != null) {
            hud.removeSelf();
        }

        hud = new GameplayHUD();

        if (!replaying && !GameHelper.isAutoplay()) {
            // Since block areas are saved in device pixels, we need to map them to scaled pixels.
            final var displayMetrics = new DisplayMetrics();
            GlobalManager.getInstance().getMainActivity().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

            final float ratio = (float) Config.getRES_WIDTH() / displayMetrics.widthPixels;

            for (final var area : DatabaseManager.getBlockAreaTable().getAll()) {
                // Attach the block area to the HUD so that it does not get scaled with the playfield.
                var areaBox = new UIBox() {
                    {
                        setRadius(new Dimension.Fixed.Pixels(2f));
                        setBackgroundColor(new Color4(30f / 255f, 30f / 255f, 41f / 255f));
                        setAlpha(0.15f);

                        setPosition(
                            Interpolation.linear(0f, area.getX(), ratio),
                            Interpolation.linear(0f, area.getY(), ratio)
                        );

                        setSize(
                            Interpolation.linear(0f, area.getWidth(), ratio),
                            Interpolation.linear(0f, area.getHeight(), ratio)
                        );
                    }

                    @Override
                    public boolean onInputEvent(@NonNull InputEvent event) {
                        if (event instanceof PointerEvent pointer && pointer.getAction() == PointerEvent.Action.Down
                                && containsScenePoint(pointer.getX(), pointer.getY())) {
                            int id = pointer.getPointerId();

                            if (id >= 0 && id < getCursorsCount()) {
                                cursors[id].mouseBlocked = true;
                            }
                        }

                        return false;
                    }
                };

                hud.plusAssign(areaBox);
            }
        }

        hud.setEditMode(isHUDEditorMode);
        hud.setSkinData(OsuSkin.get().getHUDSkinData());

        skipBtn = null;
        if (skipTime > 1) {
            float paddingBottom = Multiplayer.isConnected() ? Multiplayer.roomScene.getChat().getButtonHeight() : 0f;

            skipBtn = new UIAnimatedSprite(SkinTextures.frames("play-skip"), OsuSkin.get().getAnimationFramerate());
            skipBtn.setOrigin(Anchor.BottomRight);
            skipBtn.setPosition(Config.getRES_WIDTH(), Config.getRES_HEIGHT() - paddingBottom);
            skipBtn.setAlpha(0.7f);
            hud.plusAssign(skipBtn);
        }

        String playname = Config.getOnlineUsername();

        if (GameHelper.isAutoplay() || replaying) {
            var metadata = playableBeatmap.getMetadata();
            playname = replaying ? GlobalManager.getInstance().getScoring().getReplayStat().getPlayerName() : "osu!";

            if (!Config.isHideReplayMarquee()) {
                replayText = new UIText();
                replayText.setText("Watching " + playname + " play " + metadata.artist + " - " + metadata.title + " [" + metadata.version + "]");
                replayText.setPosition(Config.getRES_WIDTH() + 5, 140);
                replayText.setAlpha(0.7f);
                hud.add(0, replayText);
            }
        } else if (Multiplayer.room != null && Multiplayer.room.isTeamVersus()) {
            //noinspection DataFlowIssue
            playname = Multiplayer.player.getTeam().toString();
        }
        stat.setPlayerName(playname);

        breakAnimator = new BreakAnimator(fgScene, stat, hud);

        if (!startedFromHUDEditor && (GameHelper.isAutoplay() || replaying)) {
            hud.plusAssign(replaySettingsPanel = new ReplaySettingsPanel());

            var seekControl = replaySettingsPanel.getPlaybackControl().getSeekControl();

            seekControl.setOnPauseToggle(isPaused -> {
                if (isPaused) {
                    gameplayClock.pause();
                    stopLoopingSamples();

                    if (video != null && videoStarted) {
                        video.pause();
                    }
                } else {
                    if (!gameplayClock.isRunning()) {
                        gameplayClock.start();
                    }

                    playLoopingSamples();

                    if (video != null && videoStarted) {
                        video.play();
                    }
                }

                return Unit.INSTANCE;
            });

            replaySettingsPanel.getVisualSettingsControl().setOnBackgroundBrightnessChanged(brightness -> {
                breakAnimator.setDimBrightness(brightness);

                // We do not want
                if (!breakAnimator.isBreak() && dimRectangle != null) {
                    dimRectangle.setAlpha(1f - brightness);
                }

                return Unit.INSTANCE;
            });

            seekControl.updateSeekPosition(
                gameplayClock.getTime(),
                (float) (objects[0].startTime / 1000),
                (float) (objects[objects.length - 1].getEndTime() / 1000)
            );

            seekControl.setOnSeek(targetTime -> {
                Execution.updateThread(() -> seekTo(targetTime));
                return Unit.INSTANCE;
            });
        }

        if (Multiplayer.isMultiplayer) {
            RoomAPI.INSTANCE.notifyBeatmapLoaded();
        } else {
            isReadyToStart = true;
        }
    }

    /**
     * Starts gameplay. This is used by the game loader once all necessary preprocessing is done.
     */
    public void start() {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        var songService = GlobalManager.getInstance().getSongService();

        // Hook up the framed beatmap clock as the scene's clock. Use SongService as the audio source.
        if (songService != null) {
            gameplayClock.changeSource(new SongServiceClock(songService));
        }

        // Ensure user-defined offset has time to be applied.
        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(lastBeatmapInfo.getSetDirectory());
        gameplayClock.setUserGlobalOffset(Config.getOffset() / 1000);
        gameplayClock.setUserBeatmapOffset(props != null ? props.getOffset() / 1000f : 0);

        var firstObjectTimePreempt = (float) playableBeatmap.getHitObjects().objects.get(0).timePreempt / 1000;
        gameplayClock.seek(Math.min(gameplayClock.getTime(), firstObjectStartTime - firstObjectTimePreempt));
        initialStartTime = gameplayClock.getTime();

        if (songService != null) {
            songService.setVolume(Config.getBgmVolume());
        }

        gameplayClock.start();

        if (skipTime <= 1 && Multiplayer.isConnected()) {
            Multiplayer.roomScene.getChat().hide();
        }

        applyPlayfieldSizeScale();
        applyBackground();

        if (replaySettingsPanel != null) {
            replaySettingsPanel.getVisualSettingsControl().setDefaultBackgroundBrightness(Config.getBackgroundBrightness());
        }

        if (!isHUDEditorMode && !Config.isShowScoreboard()) {
            hud.detachChild(e -> e instanceof HUDLeaderboard);
        }

        if (!isHUDEditorMode && !replaying && !GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
        Input.setDispatchHistoricalMoves(true);
        }

        root.plusAssign(hud);
        replaceCurrentScene(scene);

        if (isHUDEditorMode) {
            ToastLogger.showText(R.string.hudEditor_back_for_menu, false);
        }
    }

    public Color4 getComboColor(HitObject hitObject) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap != null && GameHelper.isSynesthesia()) {
            return ModSynesthesia.getColorFor(playableBeatmap.getControlPoints().getClosestBeatDivisor(hitObject.startTime));
        }

        return comboColors.get(hitObject.getComboIndexWithOffsets() % comboColors.size());
    }

    private void update(final float dt) {
        if (!isReadyToStart) {
            return;
        }

        previousFrameTime = SystemClock.uptimeMillis();

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        if (Multiplayer.isMultiplayer)
        {
            long mSecElapsed = (long) (dt / GameHelper.getSpeedMultiplier() * 1000);
            realTimeElapsed += mSecElapsed;
            statisticDataTimeElapsed += mSecElapsed;

            // Sending statistics data every 3000ms if data was changed
            if (statisticDataTimeElapsed > 3000)
            {
                statisticDataTimeElapsed %= 3000;

                if (Multiplayer.isConnected())
                {
                    var liveScore = stat.toBoardItem();

                    if (!Objects.equals(liveScore, lastScoreSent))
                    {
                        lastScoreSent = liveScore;
                        Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitLiveScore(lastScoreSent.toJson())));
                    }
                }
            }
        }

        final float mSecPassed = gameplayClock.getTime() * 1000;

        if (!isGameOver) {
            float modRate = ModUtils.calculateRateWithTrackRateMods(rateAdjustingMods, mSecPassed);

            float replaySettingsRate = replaySettingsPanel != null
                ? replaySettingsPanel.getPlaybackControl().getRateControl().getRate()
                : 1f;

            float currentSpeedMultiplier = modRate * replaySettingsRate;

            if (currentSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(currentSpeedMultiplier);
                gameplayClock.setRate(currentSpeedMultiplier);

                if (videoEnabled && video != null) {
                    video.setPlaybackSpeed(currentSpeedMultiplier);
                }

                var songService = GlobalManager.getInstance().getSongService();
                songService.setSpeed(modRate);
                songService.setPitchRate(replaySettingsRate);
            }
        }

        if (replaySettingsPanel != null) {
            replaySettingsPanel.getPlaybackControl().getSeekControl().updateSeekPosition(
                gameplayClock.getTime(),
                (float) (objects[0].startTime / 1000),
                (float) (objects[objects.length - 1].getEndTime() / 1000)
            );
        }

        if (replaying) {
            final float replayTimeMs = gameplayClock.getTime() * 1000;
            final float replayCatchUpTimeMs = (gameplayClock.getTime() + dt / 4) * 1000;
            int cIndex;

            for (int i = 0; i < replay.cursorIndex.length; i++) {
                if (replay.cursorMoves.size() <= i) {
                    break;
                }

                var moveArray = replay.cursorMoves.get(i);

                cIndex = replay.cursorIndex[i];
                Replay.ReplayMovement movement = null;

                // Emulating moves
                while (
                    cIndex < moveArray.size && (movement = moveArray.movements[cIndex]).getTime() <= replayCatchUpTimeMs
                ) {
                    var event = CursorEvent.obtain();

                    event.systemTime = movement.getTime();
                    event.trackTime = movement.getTime();
                    event.offset = 0;
                    event.position.set(movement.getX(), movement.getY());

                    if (movement.getTouchType() == TouchType.DOWN) {
                        event.action = PointerEvent.Action.Down;
                        replay.lastMoveIndex[i] = -1;
                        hud.onGameplayTouchDown(movement.getTime() / 1000f);
                    } else if (movement.getTouchType() == TouchType.MOVE) {
                        event.action = PointerEvent.Action.Move;
                        replay.lastMoveIndex[i] = cIndex;
                    } else {
                        event.action = PointerEvent.Action.Up;
                    }
                    cursors[i].addEvent(event);
                    replay.cursorIndex[i]++;
                    cIndex++;
                }

                // Interpolating cursor movements
                if (movement != null && movement.getTouchType() == TouchType.MOVE && replay.lastMoveIndex[i] >= 0) {
                    final int lIndex = replay.lastMoveIndex[i];
                    final Replay.ReplayMovement lastMovement = moveArray.movements[lIndex];
                    int movementTime = movement.getTime();
                    int lastMovementTime = lastMovement.getTime();
                    int duration = lastMovementTime - movementTime;

                    if (duration == 0) {
                        continue;
                    }

                    float t = (replayTimeMs - movementTime) / duration;

                    var event = CursorEvent.obtain();

                    // We don't exactly need systemTime to be accurate here since it's not used for anything important
                    // in replays.
                    event.systemTime = (long) mSecPassed;
                    event.trackTime = (long) mSecPassed;
                    event.offset = 0;
                    event.action = PointerEvent.Action.Move;
                    event.position.set(
                        lastMovement.getX() * t + movement.getX() * (1 - t),
                        lastMovement.getY() * t + movement.getY() * (1 - t)
                    );

                    cursors[i].addEvent(event);
                }
            }
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.update(dt);
        } else if (cursorSprites != null) {
            for (int i = 0; i < cursorSprites.length; i++) {
                var sprite = cursorSprites[i];
                sprite.update(dt);

                var cursor = cursors[i];
                var latestEvent = cursor.getLatestEvent();

                if (replaying && latestEvent != null) {
                    sprite.setPosition(latestEvent.position.x, latestEvent.position.y);
                    sprite.setShowing(!latestEvent.isActionUp());
                }

                if (cursor.getLatestDownEvent() != null) {
                    sprite.click();
                }
            }
        }

        if (GameHelper.isFlashlight()) {
            if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
                // Check if the main cursor is still valid.
                if (mainCursorId >= 0 && !cursors[mainCursorId].isMouseDown()) {
                    mainCursorId = -1;
                }

                // If no cursor is valid, check for the latest pressed cursor.
                if (mainCursorId < 0) {
                    int index = -1;
                    CursorEvent latestDownEvent = null;

                    for (int i = 0; i < cursors.length; ++i) {
                        var c = cursors[i];
                        var latestCursorDownEvent = c.getLatestDownEvent();

                        if (latestCursorDownEvent == null) {
                            continue;
                        }

                        if (latestDownEvent == null || latestDownEvent.systemTime < latestCursorDownEvent.systemTime) {
                            latestDownEvent = latestCursorDownEvent;
                            index = i;
                        }
                    }

                    if (latestDownEvent != null) {
                        mainCursorId = index;
                    }
                }

                if (mainCursorId != -1) {
                    var cursor = cursors[mainCursorId];
                    var latestNonUpEvent = cursor.getLatestNonUpEvent();

                    if (latestNonUpEvent != null) {
                        flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y);
                    }
                }
            }

            flashlightSprite.updateCombo(stat.getCombo());
        }

        while (timingControlPointIndex + 1 < timingControlPoints.length) {
            var nextTimingPoint = timingControlPoints[timingControlPointIndex + 1];

            if (nextTimingPoint.time > mSecPassed) {
                break;
            }

            activeTimingPoint = nextTimingPoint;
            ++timingControlPointIndex;
        }

        while (effectControlPointIndex + 1 < effectControlPoints.length) {
            var nextEffectPoint = effectControlPoints[effectControlPointIndex + 1];

            if (nextEffectPoint.time > mSecPassed) {
                break;
            }

            activeEffectPoint = nextEffectPoint;
            ++effectControlPointIndex;
        }

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(Math.max(0, gameplayClock.getTime() - activeTimingPoint.time / 1000) % GameHelper.getBeatLength());

        if (!isGameOver) {

            if (breakPeriodIndex < breakPeriods.length) {
                if (!breakAnimator.isBreak() && breakPeriods[breakPeriodIndex].startTime / 1000 <= gameplayClock.getTime()) {
                    var period = breakPeriods[breakPeriodIndex++];

                    gameStarted = false;
                    breakAnimator.init(period.getDuration() / 1000);
                    if(GameHelper.isFlashlight()){
                        flashlightSprite.onBreak(true);
                    }

                    if (Multiplayer.isConnected())
                        Multiplayer.roomScene.getChat().show();

                    hud.onBreakStateChange(true);
                }
            }

            if (breakAnimator.isOver()) {

                // Ensure the chat is dismissed if it's still shown
                if (Multiplayer.isConnected()) {
                    Multiplayer.roomScene.getChat().hide();
                }

                gameStarted = true;
                hud.onBreakStateChange(false);

                if(GameHelper.isFlashlight()){
                    flashlightSprite.onBreak(false);
                }
            }
        }

        if (objectIndex >= objects.length && activeObjects.isEmpty() && GameHelper.isFlashlight()) {
            flashlightSprite.onBreak(true);
        }

        if (gameStarted) {
            double rate = 0.375;
            if (playableBeatmap.getDifficulty().hp > 0 && distToNextObject > 0) {
                rate = 1 + playableBeatmap.getDifficulty().hp / (2 * distToNextObject);
            }
            stat.changeHp((float) -rate * 0.01f * dt);

            if (stat.getHp() <= 0 && stat.canFail) {
                if (GameHelper.isEasy() && failcount < 3) {
                    failcount++;
                    stat.changeHp(1f);
                } else {
                    if (Multiplayer.isMultiplayer) {
                        if (!hasFailed) {
                            ToastLogger.showText("You failed but you can continue playing.", false);
                        }
                        hasFailed = true;
                    } else {
                        gameover();
                        return;
                    }
                }
            }
        }

        if (comboBurst != null) {
            if (stat.getCombo() == 0) {
                comboBurst.breakCombo();
            } else {
                comboBurst.checkAndShow(stat.getCombo());
            }
        }

        // Clearing expired objects.
        processedExpiredObjects.clear();

        if (!expiredObjects.isEmpty()) {
            for (int i = 0, size = expiredObjects.size(); i < size; i++) {
                var obj = expiredObjects.get(i);

                if (processedExpiredObjects.add(obj)) {
                    obj.onExpire();
                }
            }

            activeObjects.removeAll(expiredObjects);
            expiredObjects.clear();
        }

        updatePassiveObjects(dt);
        updateActiveObjects(dt);

        if (judgeableObject != null && (GameHelper.isAutoplay() || GameHelper.isAutopilot())) {
            autoCursor.moveToObject(judgeableObject, gameplayClock.getTime(), this);
        }

        if (videoEnabled && video != null && gameplayClock.getTime() >= videoOffset)
        {
            if (!videoStarted) {
                video.play();
                video.setPlaybackSpeed(GameHelper.getSpeedMultiplier());
                videoStarted = true;
            }

            if (video.getAlpha() < 1.0f)
                video.setAlpha(Math.min(video.getAlpha() + 0.03f, 1.0f));
        }

        boolean shouldBePunished = false;

        while (objectIndex < objects.length) {
            var obj = objects[objectIndex];

            // The casts can be simplified, but it is necessary to prevent floating point errors (see how
            // GameplayHitCircle and GameplaySlider track their passed time, where startTime and timePreempt
            // are cast and converted to seconds individually).
            float lifetimeStart = (float) obj.startTime / 1000 - (float) obj.timePreempt / 1000;
            float lifetimeDt = gameplayClock.getTime() - lifetimeStart;

            if (lifetimeDt < 0) {
                break;
            }

            gameStarted = true;
            ++objectIndex;

            if (unrankedSprite != null) {
                ChoreographyKt.choreographAt(unrankedSprite, 1.5f, sequence -> sequence
                        .scaleTo(1.5f, 0.5f)
                        .fadeOut(0.5f));

                // Make it null to avoid multiple entity modifier registration
                unrankedSprite = null;
            }

            if (obj.startTime > totalLength) {
                shouldBePunished = true;
                break;
            }

            final var nextObj = objectIndex < objects.length ? objects[objectIndex] : null;

            distToNextObject = nextObj != null ?
                Math.max(nextObj.startTime - obj.startTime, activeTimingPoint.msPerBeat / 2) / 1000 :
                0;

            hud.onHitObjectLifetimeStart(obj);

            final Color4 comboColor = getComboColor(obj);

            GameObject gameObject = null;

            if (obj instanceof HitCircle parsedCircle) {
                final var gameplayCircle = GameObjectPool.getInstance().getCircle();

                gameplayCircle.init(this, mgScene, parsedCircle, comboColor);
                addObject(gameplayCircle);

                if (GameHelper.isAutoplay()) {
                    gameplayCircle.setAutoPlay();
                }

                gameplayCircle.setId(++lastObjectId);

                if (replaying) {
                    gameplayCircle.setReplayData(replay.objectData[gameplayCircle.getId()]);
                }

                gameObject = gameplayCircle;
            } else if (obj instanceof Spinner parsedSpinner) {
                final float rps = 2 + 2 * playableBeatmap.getDifficulty().od / 10f;
                final var gameplaySpinner = GameObjectPool.getInstance().getSpinner();

                gameplaySpinner.init(this, bgScene, parsedSpinner, rps, stat);
                addObject(gameplaySpinner);

                if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                    gameplaySpinner.setAutoPlay();
                }

                gameplaySpinner.setId(++lastObjectId);
                if (replaying) {
                    gameplaySpinner.setReplayData(replay.objectData[gameplaySpinner.getId()]);
                }

                gameObject = gameplaySpinner;
            } else if (obj instanceof Slider parsedSlider) {
                final var gameplaySlider = GameObjectPool.getInstance().getSlider();

                gameplaySlider.init(this, mgScene, stat, parsedSlider, playableBeatmap.getControlPoints(),
                        comboColor, sliderBorderColor, getSliderPath(sliderIndex), getSliderRenderPath(sliderIndex));

                ++sliderIndex;
                addObject(gameplaySlider);

                if (GameHelper.isAutoplay()) {
                    gameplaySlider.setAutoPlay();
                }

                gameplaySlider.setId(++lastObjectId);
                if (replaying) {
                    gameplaySlider.setReplayData(replay.objectData[gameplaySlider.getId()]);
                    if (gameplaySlider.getReplayData().tickSet == null)
                        gameplaySlider.getReplayData().tickSet = new BitSet();
                }

                gameObject = gameplaySlider;
            }

            if (gameObject != null) {
                gameObject.update(lifetimeDt);
            }

            if (!(obj instanceof Spinner) && nextObj != null && !(nextObj instanceof Spinner) && !obj.isLastInCombo()) {
                FollowPointConnection.addConnection(bgScene, obj, nextObj);
            }
        }

        if (GameHelper.isPerfect() && stat.getHit100() + stat.getHit50() + stat.getMisses() > 0) {
            gameover();

            if (!Multiplayer.isMultiplayer) {
                restartGame();
            }
        }

        var mutedMod = GameHelper.getMuted();

        // 节拍器
        if (metronome != null) {
            metronome.update(gameplayClock.getTime(), activeTimingPoint);

            if (mutedMod != null) {
                metronome.setVolume(1 - mutedMod.volumeAt(stat.getCombo()));
            }
        }

        if (gameplayClock.getTime() >= 0 && mutedMod != null) {
            GlobalManager.getInstance().getSongService().setVolume(
                Config.getBgmVolume() * mutedMod.volumeAt(stat.getCombo())
            );
        }

        if (shouldBePunished || (!isGameOver && objectIndex >= objects.length && activeObjects.isEmpty() && leadOut > 2)) {

            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                gameplayClock.seek(initialStartTime);
                loadGame(lastBeatmapInfo, null, lastMods, null);
                applyBackground();
                stat.reset();
                skip(true);
                return;
            }

            createScene();
            BeatmapSkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            timingControlPoints = null;
            effectControlPoints = null;
            objects = null;
            activeObjects.clear();
            expiredObjects.clear();
            replaySettingsPanel = null;
            breakPeriods = null;
            cursorSprites = null;
            this.playableBeatmap = null;
            performanceAttributes = null;
            performanceCalculationParameters = null;
            droidTimedDifficultyAttributes = null;
            standardTimedDifficultyAttributes = null;
            sliderPaths = null;
            sliderRenderPaths = null;
            String replayPath = null;
            stat.setTime(System.currentTimeMillis());
            if (replay != null && !replaying) {
                String ctime = String.valueOf(System.currentTimeMillis());
                replayPath = Config.getCorePath() + "Scores/"
                        + MD5Calculator.getStringMD5(lastBeatmapInfo.getFilename() + ctime)
                        + ctime.substring(0, Math.min(3, ctime.length())) + ".odr";
                replay.setStat(stat);
                replay.save(replayPath);
            }
            resetPlayfieldSizeScale();
            cancelVideoLoading();

            if (scoringScene != null && !startedFromHUDEditor) {
                if (replaying)
                    scoringScene.load(scoringScene.getReplayStat(), null, GlobalManager.getInstance().getSongService(), replayPath, null, lastBeatmapInfo);
                else {
                    if (stat.getMod().contains(ModAutoplay.class)) {
                        stat.setPlayerName("osu!");
                    }

                    if (Multiplayer.isConnected())
                    {
                        Multiplayer.log("Match ended, moving to results scene.");
                        Multiplayer.roomScene.getChat().show();

                        Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));

                        ToastLogger.showText("Loading room statistics...", false);
                    }
                    scoringScene.load(stat, lastBeatmapInfo, GlobalManager.getInstance().getSongService(), replayPath, parsedBeatmap.getMd5(), null);
                }
                GlobalManager.getInstance().getSongService().setVolume(0.2f);
                replaceCurrentScene(scoringScene.getScene());
            } else {
                returnToPreviousScene();
            }

            // Resume difficulty calculation.
            DifficultyCalculationManager.calculateDifficulties();
            Input.setDispatchHistoricalMoves(false);

            if (video != null) {
                video.release();
                video = null;
                videoStarted = false;
            }

            parsedBeatmap = null;
        } else if (objectIndex >= objects.length && activeObjects.isEmpty()) {
            gameStarted = false;
            leadOut += dt;
        }

        if (gameplayClock.getTime() > skipTime - 1f && skipBtn != null) {
            if (Multiplayer.isConnected()) {
                Multiplayer.roomScene.getChat().hide();
            }
            skipBtn.removeSelf();
            skipBtn = null;
        } else if (skipBtn != null) {
            for (int i = 0; i < cursors.length; ++i) {
                var latestDownEvent = cursors[i].getLatestNonUpEvent();

                if (latestDownEvent != null && Utils.squaredDistance(latestDownEvent.position.x, latestDownEvent.position.y,
                        Config.getRES_WIDTH(), Config.getRES_HEIGHT()) < 250 * 250) {
                    if (Multiplayer.isConnected()) {
                        if (!isSkipRequested) {
                            isSkipRequested = true;
                            SkinSounds.get("menuhit").play();
                            skipBtn.setVisible(false);

                            Execution.async(RoomAPI.INSTANCE::requestSkip);
                            ToastLogger.showText("Skip requested", false);
                        }
                        return;
                    }
                    if (skipBtn != null) {
                        skipBtn.removeSelf();
                        skipBtn = null;
                    }
                    skip();
                    return;
                }
            }
        }

        if (postSeekFrameCount > 0) {
            postSeekFrameCount--;
        }
    }

    private void updateActiveObjects(float deltaTime) {
        judgeableObject = searchJudgeableObject(0);

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);
            obj.update(deltaTime);

            // Advance to the next judgeable object if the current judgeable object is judged.
            // In remove slider lock mode, do this as soon as the current judgeable object is hit instead.
            if (isObjectHittable(obj) && (obj.isJudged() || (Config.isRemoveSliderLock() && obj.isStartHit()))) {
                judgeableObject = searchJudgeableObject(i + 1);
            }

            if (gameplayClock.getTime() >= obj.getLifetimeEnd()) {
                expiredObjects.add(obj);
            }
        }
    }

    private void updatePassiveObjects(float deltaTime) {

        hud.onGameplayUpdate(this, deltaTime);

        breakAnimator.update(deltaTime);

        if (countdownAnimator != null) {
            countdownAnimator.update(deltaTime);
        }
    }

    @Nullable
    private GameObject searchJudgeableObject(int startIndex) {
        for (int i = startIndex, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);

            if (!obj.isJudged()) {
                return obj;
            }
        }

        return null;
    }

    public void skip() {
        skip(false);
    }

    public void skip(boolean force)
    {
        if (Multiplayer.isConnected()) {
            Multiplayer.roomScene.getChat().hide();
        }

        if (gameplayClock.getTime() > skipTime - 1f && !force) {
            return;
        }

        SkinSounds.get("menuhit").play();

        float difference = skipTime - gameplayClock.getTime();
        gameplayClock.seek(skipTime);

        Execution.updateThread(() -> {
            updatePassiveObjects(difference);

            if (videoEnabled && video != null) {
                video.seekTo(Math.max(0, (int) ((gameplayClock.getTime() - videoOffset) * 1000)));
            }

            if (skipBtn != null) {
                skipBtn.removeSelf();
                skipBtn = null;
            }
        });
    }

    private void onExit() {
        BeatmapSkinManager.setSkinEnabled(false);
        GameObjectPool.getInstance().purge();
        stopLoopingSamples();
        if (activeObjects != null) {
            activeObjects.clear();
        }
        if (expiredObjects != null) {
            expiredObjects.clear();
        }
        if (hud != null) {
            hud.removeSelf();
        }
        breakPeriods = null;
        replaySettingsPanel = null;
        objects = null;
        timingControlPoints = null;
        effectControlPoints = null;
        parsedBeatmap = null;
        playableBeatmap = null;
        cursorSprites = null;
        lastMods = null;
        performanceAttributes = null;
        performanceCalculationParameters = null;
        droidTimedDifficultyAttributes = null;
        standardTimedDifficultyAttributes = null;
        sliderPaths = null;
        sliderRenderPaths = null;

        BeatmapSkinManager.getInstance().clearSkin();

        cancelVideoLoading();

        float mSecPassed = gameplayClock.getTime() * 1000;
        var selectedBeatmap = GlobalManager.getInstance().getSelectedBeatmap();
        var songService = GlobalManager.getInstance().getSongService();
        var songMenu = GlobalManager.getInstance().getSongMenu();

        if (songService != null && selectedBeatmap != null && !Multiplayer.isMultiplayer) {
            // osu!stable restarts the song back to preview time when the player is in the last 10 seconds *or* 2% of the beatmap.
            boolean continuePreview = mSecPassed < totalLength - 10000 && mSecPassed / totalLength < 0.98f;
            int previewTime = continuePreview ? songService.getPosition() : selectedBeatmap.getPreviewTime();

            songMenu.playMusic(selectedBeatmap.getAudioPath(), previewTime);

            if (continuePreview) {
                songMenu.startMusicVolumeAnimation(0.3f);
            }
        }

        if (replaying) {
            replayFilePath = null;
        }
    }

    public void quit() {
        // Disable historical event processing for more efficient ACTION_MOVE reports, since frequent reports are
        Input.setDispatchHistoricalMoves(false);


        if (sceneBorder != null) {
            sceneBorder.removeSelf();
            sceneBorder = null;
        }

        onExit();
        resetPlayfieldSizeScale();
        scene = new GameUIScene();

        if (Multiplayer.isMultiplayer) {
            releaseVideo();

            var songService = GlobalManager.getInstance().getSongService();
            if (songService != null) {
                songService.stop();
            }

            var roomScene = Multiplayer.roomScene;

            if (Multiplayer.isConnected() && roomScene != null) {
                returnToPreviousScene();

                if (Verktex.getScene() != roomScene) {
                    roomScene.show();
                }
            } else {
                replaceCurrentScene(new LobbyScene());
            }

            return;
        }
        SkinSounds.get("failsound").stop();
        returnToPreviousScene();
        releaseVideo();

        // Resume difficulty calculation.
        DifficultyCalculationManager.calculateDifficulties();
    }


    public void reset() {
    }

    //CB打击处理
    private String registerHit(final int objectId, final int score, final boolean endCombo) {
        return registerHit(objectId, score, endCombo, true);
    }

    private String registerHit(final int objectId, final int score, final boolean endCombo, final boolean incrementCombo) {

        if (isGameOver) {
            return "hit0";
        }

        boolean writeReplay = objectId != -1 && replay != null && !replaying;
        if (score == 0) {
            if (stat.getCombo() > 30) {
                var sound = SkinSounds.getBeatmap("combobreak", 1);
                if (sound != null) {
                    sound.play();
                }
            }
            comboWasMissed = true;
            stat.registerHit(0, false, false, incrementCombo);
            if (writeReplay) replay.addObjectScore(objectId, ResultType.MISS);
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            if (objectId != -1) {
                updatePPValue(objectId);
            }
            return "hit0";
        }

        String scoreName = "hit300";
        if (score == 50) {
            stat.registerHit(50, false, false, incrementCombo);
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT50);
            scoreName = "hit50";
            comboWas100 = true;
        } else if (score == 100) {
            comboWas100 = true;
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT100);
            if (endCombo && !comboWasMissed) {
                stat.registerHit(100, true, false, incrementCombo);
                scoreName = "hit100k";
            } else {
                stat.registerHit(100, false, false, incrementCombo);
                scoreName = "hit100";
            }
        } else if (score == 300) {
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT300);
            if (endCombo && !comboWasMissed) {
                if (!comboWas100) {
                    stat.registerHit(300, true, true, incrementCombo);
                    scoreName = "hit300g";
                } else {
                    stat.registerHit(300, true, false, incrementCombo);
                    scoreName = "hit300k";
                }
            } else {
                stat.registerHit(300, false, false, incrementCombo);
                scoreName = "hit300";
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }

        if (objectId != -1) {
            updatePPValue(objectId);
        }

        return scoreName;
    }


    public void onCircleHit(int id, final float acc, final PointF pos,
                            final boolean endCombo, byte forcedScore, Color4 color) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        if (GameHelper.isAutoplay()) {
            autoCursor.click();
            hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
        }

        float accuracy = Math.abs(acc);
        boolean writeReplay = replay != null && !replaying;
        if (writeReplay) {
            short sacc = (short) (acc * 1000);
            replay.addObjectResult(id, sacc, null);
        }

        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        boolean isCursorTrackingActive = !GameHelper.isAutoplay() && !GameHelper.isAutopilot();
        int nearestCursorId = isCursorTrackingActive ? getNearestCursorId(pos.x, pos.y) : -1;

        if (GameHelper.isFlashlight() && isCursorTrackingActive) {
           if (nearestCursorId >= 0) {
               mainCursorId = nearestCursorId;
               var latestNonUpEvent = cursors[mainCursorId].getLatestNonUpEvent();

               if (latestNonUpEvent != null) {
                   flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y);
               }
            }
        }
        VibratorManager.INSTANCE.circleVibration();

        if (accuracy > playableBeatmap.getHitWindow().getMehWindow() / 1000 || forcedScore == ResultType.MISS.getId()) {
            createHitEffect(pos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName;
        if (forcedScore == ResultType.HIT300.getId() ||
                forcedScore == 0 && accuracy <= playableBeatmap.getHitWindow().getGreatWindow() / 1000) {
            scoreName = registerHit(id, 300, endCombo);
        } else if (forcedScore == ResultType.HIT100.getId() ||
                forcedScore == 0 && accuracy <= playableBeatmap.getHitWindow().getOkWindow() / 1000) {
            scoreName = registerHit(id, 100, endCombo);
        } else {
            scoreName = registerHit(id, 50, endCombo);
        }

        createBurstEffect(pos, color);
        createHitEffect(pos, scoreName, color);

        if (nearestCursorId >= 0) {
            var aimErrorCursorEvent = cursors[nearestCursorId].getLatestNonUpEvent();

            if (aimErrorCursorEvent != null) {
                float objectRadius = (float) parsedBeatmap.getHitObjects().objects.get(id).getScreenSpaceGameplayRadius();
                hud.onAimJudgement(pos, aimErrorCursorEvent.position, objectRadius);
            }
        }

        hud.onNoteHit(stat);
    }

    public void onSliderReverse(PointF pos, float ang, Color4 color) {
        createBurstEffectSliderReverse(pos, ang, color);
    }

    public void onSliderHit(int id, final int score, final PointF judgementPos, final boolean endCombo,
                            Color4 color, int type, boolean incrementCombo) {
        // Autoplay/Autopilot synthesize their own cursor movement rather than using real touch input, so
        // real touch events (e.g. a resting finger) must not be picked up as the "cursor position" here.
        boolean isCursorTrackingActive = !GameHelper.isAutoplay() && !GameHelper.isAutopilot();
        boolean isFlashlightActive = GameHelper.isFlashlight() && isCursorTrackingActive;
        boolean isSliderHeadHit = type == GameObjectListener.SLIDER_START && score > 0 && isCursorTrackingActive;

        int nearestCursorId = -1;

        if (isFlashlightActive || isSliderHeadHit) {
            nearestCursorId = getNearestCursorId(judgementPos.x, judgementPos.y);
        }

        if (isFlashlightActive && nearestCursorId >= 0) {
            mainCursorId = nearestCursorId;
            var latestNonUpEvent = cursors[mainCursorId].getLatestNonUpEvent();

            if (latestNonUpEvent != null) {
                flashlightSprite.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y);
            }
        }

        VibratorManager.INSTANCE.sliderVibration();

        // Whole slider was missed.
        if (score == 0) {
            createHitEffect(judgementPos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        // Nested object was missed.
        if (score == -1) {
            if (stat.getCombo() > 30) {
                var sound = SkinSounds.getBeatmap("combobreak", 1);
                if (sound != null) {
                    sound.play();
                }
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            stat.registerHit(0, true, false);
            return;
        }

        String scoreName = "hit0";

        switch (type) {
            case GameObjectListener.SLIDER_START:
                if (incrementCombo) {
                    scoreName = "sliderpoint30";
                    stat.registerHit(30, false, false);
                    stat.addSliderHeadHit();
                    createBurstEffectSliderStart(judgementPos, color);

                    if (nearestCursorId >= 0) {
                        var aimErrorCursorEvent = cursors[nearestCursorId].getLatestNonUpEvent();

                        if (aimErrorCursorEvent != null) {
                            float objectRadius = (float) parsedBeatmap.getHitObjects().objects.get(id).getScreenSpaceGameplayRadius();
                            hud.onAimJudgement(judgementPos, aimErrorCursorEvent.position, objectRadius);
                        }
                    }

                    if (GameHelper.isAutoplay()) {
                        hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
                    }
                }
                break;

            case GameObjectListener.SLIDER_REPEAT:
                if (incrementCombo) {
                    scoreName = "sliderpoint30";
                    stat.registerHit(30, false, false);
                    stat.addSliderRepeatHit();
                }
                break;

            case GameObjectListener.SLIDER_TICK:
                if (incrementCombo) {
                    scoreName = "sliderpoint10";
                    stat.registerHit(10, false, false);
                    stat.addSliderTickHit();
                }
                break;

            case GameObjectListener.SLIDER_END:
                // Register the slider end hit to statistics first before calling registerHit so that updatePPValue
                // receives the correct slider end hit count.
                if (incrementCombo) {
                    stat.addSliderEndHit();
                }

                scoreName = registerHit(id, score, endCombo, incrementCombo);

                if (incrementCombo) {
                    createBurstEffectSliderEnd(judgementPos, color);
                }
                break;
        }

        createHitEffect(judgementPos, scoreName, color);

        hud.onNoteHit(stat);
    }

    @Override
    public void onSpinnerStart(int id) {
        if (GameHelper.isAutoplay()) {
            autoCursor.click();
            hud.onGameplayTouchDown((float) parsedBeatmap.getHitObjects().objects.get(id).startTime / 1000);
        }
    }

    public void onSpinnerHit(int id, final int score, final boolean endCombo, int totalScore) {
        if (score == 1000) {
            stat.registerHit(score, false, false);
            return;
        }

        if (replay != null && !replaying) {
            short acc = (short) (totalScore * 4);
            switch (score) {
                case 300:
                    acc += 3;
                    break;
                case 100:
                    acc += 2;
                    break;
                case 50:
                    acc += 1;
                    break;
            }
            replay.addObjectResult(id, acc, null);
        }

        final PointF pos = new PointF((float) Config.getRES_WIDTH() / 2,
                (float) Config.getRES_HEIGHT() / 2);

        VibratorManager.INSTANCE.spinnerVibration();

        if (score == 0) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                    "hit0");
            effect.init(
                    scene,
                    pos,
                    scale,
                    sequence -> sequence.fadeIn(0.15f)
                            .then(0.35f)
                            .fadeOut(0.25f)
            );
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName;

        // Simulate a hit for hit error meter registration.
        float accuracy = (float) switch (score) {
            case 300 -> {
                scoreName = registerHit(id, 300, endCombo);
                yield 0;
            }

            case 100 -> {
                scoreName = registerHit(id, 100, endCombo);
                yield hitWindow.getGreatWindow() + 1;
            }

            case 50 -> {
                scoreName = registerHit(id, 50, endCombo);
                yield hitWindow.getOkWindow() + 1;
            }

            default -> {
                scoreName = "hit0";
                yield hitWindow.getMehWindow() + 1;
            }
        } / 1000;

        createHitEffect(pos, scoreName, null);

        hud.onAccuracyRegister(HitObjectType.Spinner, accuracy);
        hud.onNoteHit(stat);
    }

    @Override
    public void playHitSamples(List<GameplayHitSampleInfo> samples) {
        float volume = 1;
        var muted = GameHelper.getMuted();

        if (muted != null && muted.affectsHitSounds()) {
            volume = muted.volumeAt(stat.getCombo());
        }

        for (int i = 0, size = samples.size(); i < size; ++i) {
            var sample = samples.get(i);
            sample.setVolume(volume);
            sample.play();
        }
    }

    private void playLoopingSamples() {
        if (activeObjects == null) {
            return;
        }

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).playLoopingSamples();
        }
    }

    private void stopLoopingSamples() {
        if (activeObjects == null) {
            return;
        }

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).stopLoopingSamples();
        }
    }

    public void addObject(final GameObject object) {
        activeObjects.add(object);
    }

    @Override
    public boolean isObjectHittable(GameObject object) {
        return object == judgeableObject;
    }

    @Override
    public Cursor getCursor(int index) {
        return cursors[index];
    }

    private boolean onPointerEvent(final PointerEvent event) {
        float offset = previousFrameTime > 0
                ? (event.getTime() - previousFrameTime) * GameHelper.getSpeedMultiplier()
                : 0;
        int eventTime = (int) (gameplayClock.getTime() * 1000 + offset);

        if (replaying || isGameOver) {
            return false;
        }

        var id = event.getPointerId();
        if (id < 0 || id >= getCursorsCount()) {
            return false;
        }

        var cursor = cursors[id];
        var action = event.getAction();

        if (action == PointerEvent.Action.Down) {
            if (cursor.mouseBlocked) {
                cursor.mouseBlocked = false;
                return true;
            }

            int activeCursorCount = 0;

            for (int i = 0; i < cursors.length; ++i) {
                if (activeCursorCount >= maximumActiveCursorCount) {
                    break;
                }

                if (cursors[i].isMouseDown()) {
                    ++activeCursorCount;
                }
            }

            if (activeCursorCount >= maximumActiveCursorCount) {
                return false;
            }
        }

        var sprite = !GameHelper.isAutoplay() && !GameHelper.isAutopilot() && cursorSprites != null
                ? cursorSprites[id]
                : null;

        var playfieldPosition = playfield.toLocalSpaceFromScene(event.getX(), event.getY());
        var cursorEvent = CursorEvent.obtain(event, playfieldPosition.getX(), playfieldPosition.getY());

        cursorEvent.trackTime = gameplayClock.getTime() * 1000;
        cursorEvent.offset = offset;

        if (sprite != null) {
            sprite.setPosition(cursorEvent.position.x, cursorEvent.position.y);
        }

        if (action == PointerEvent.Action.Down) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            if (!GameHelper.isAutoplay()) {
                hud.onGameplayTouchDown(eventTime / 1000f);
            }

            cursor.addEvent(cursorEvent);

            if (replay != null) {
                replay.addPress(eventTime, cursorEvent.trackPosition, id);
            }
        } else if (cursor.isMouseDown() && action == PointerEvent.Action.Move) {

            if (sprite != null) {
                sprite.setShowing(true);
            }

            cursor.addEvent(cursorEvent);

            if (replay != null) {
                replay.addMove(eventTime, cursorEvent.trackPosition, id);
            }

        } else if (cursor.isMouseDown() && action == PointerEvent.Action.Up) {

            if (sprite != null) {
                sprite.setShowing(false);
            }

            cursor.addEvent(cursorEvent);

            if (replay != null) {
                replay.addUp(eventTime, id);
            }

        } else if (action == PointerEvent.Action.Cancel) {
            removeAllCursors();
        } else {
            return false;
        }
        return true;
    }

    private void removeAllCursors() {
        long currentTime = SystemClock.uptimeMillis();
        float offset = previousFrameTime > 0
                ? (currentTime - previousFrameTime) * GameHelper.getSpeedMultiplier()
                : 0;
        float time = gameplayClock.getTime() * 1000 + offset;

        for (int i = 0; i < cursors.length; ++i) {
            var cursor = cursors[i];

            if (cursor.isMouseDown()) {
                var upEvent = CursorEvent.obtain();

                upEvent.systemTime = currentTime;
                upEvent.trackTime = time;
                upEvent.action = PointerEvent.Action.Up;

                cursor.addEvent(upEvent);

                if (replay != null) {
                    replay.addUp((int) time, i);
                }
            }

            if (cursorSprites != null) {
                cursorSprites[i].setShowing(false);
            }
        }
    }

    public void pause() {

        if (paused) {
            return;
        }

        if (isHUDEditorMode) {
            hud.onBackPress();
            return;
        }

        if (Multiplayer.isMultiplayer)
        {
            // Setting a delay of 300ms minimum for the player to tap back button again.
            if (lastBackPressTime > 0 && realTimeElapsed - lastBackPressTime > Math.max(300, Config.getBackButtonPressTime() * 1.5f))
            {
                // Room being null can happen when the player disconnects from socket while playing
                if (Multiplayer.isConnected())
                    Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));

                Multiplayer.log("Player left the match.");
                quit();
                return;
            }

            lastBackPressTime = realTimeElapsed;
            ToastLogger.showText("Tap twice to exit to room.", false);
            return;
        }

        if (isGameOver) {
            // Finishing the game over animation now.
            GlobalManager.getInstance().getSongService().setFrequencyForcefully(101);
            return;
        }

        if (video != null && videoStarted) {
            video.pause();
        }

        stopLoopingSamples();

        if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot() && !replaying) {
            removeAllCursors();
        }

        gameplayClock.pause();
        paused = true;

        scene.setIgnoreUpdate(true);
        hud.setIgnoreUpdate(true);

        showPauseMenu(false);
    }

    private void showPauseMenu(boolean isFail) {
        pauseMenu = new PauseMenu(this, isFail);
        pauseMenu.show(scene);
    }

    public void gameover() {

        if (isGameOver) {
            return;
        }
        isGameOver = true;

        if (!replaying) {
            removeAllCursors();
        }

        if (Multiplayer.isMultiplayer) {
            if (Multiplayer.isConnected()) {
                Multiplayer.log("Player has lost, moving to room scene.");
                Execution.async(() -> Execution.runSafe(() -> RoomAPI.submitFinalScore(stat.toJson())));
            }
            quit();
            return;
        }

        stopLoopingSamples();
        SongService songService = GlobalManager.getInstance().getSongService();

        if (GameHelper.isPerfect()) {
            if (video != null) {
                video.pause();
            }

            gameplayClock.pause();
            paused = true;
            scene.setIgnoreUpdate(true);
            hud.setIgnoreUpdate(true);
            return;
        }

        SkinSounds.get("failsound").play();
        gameStarted = false;

        float initialFrequency = songService.getFrequency();

        // Wind down animation for failing based on osu!stable behavior.
        failAnimation = new FailAnimation(songService, initialFrequency);
    }

    /**
     * The wind down animation played when failing, based on osu!stable behavior.
     */
    private class FailAnimation {
        private final SongService songService;
        private final float initialFrequency;
        private float elapsedTime;

        FailAnimation(SongService songService, float initialFrequency) {
            this.songService = songService;
            this.initialFrequency = initialFrequency;
        }

        private void applyEffectToLayer(UIContainer layer) {
            if (layer.getAlpha() > 0) {
                layer.setAlpha(Math.max(0, layer.getAlpha() - 0.007f));
            }

            for (int i = 0; i < layer.getChildCount(); i++) {
                UIComponent entity = layer.get(i);

                float y = entity.getY().resolve(0f);
                entity.setPosition(entity.getX().resolve(0f), y < 0f ? y * 0.6f : y * 1.01f);

                if (entity.getRotationZ() == 0) {
                    entity.setRotationZ(entity.getRotationZ() + (float) Random.Default.nextDouble(-0.02, 0.02) * 180 / FMath.Pi);
                } else if (entity.getRotationZ() > 0) {
                    entity.setRotationZ(entity.getRotationZ() + 0.01f * 180 / FMath.Pi);
                } else {
                    entity.setRotationZ(entity.getRotationZ() - 0.01f * 180 / FMath.Pi);
                }
            }
        }

        void update(float secondsElapsed) {
            elapsedTime += secondsElapsed;

            // In osu!stable, the update is capped to 60 FPS. This means in higher framerates, the animations
            // need to be slowed down to match 60 FPS.
            float sixtyFPS = 1 / 60f;

            if (elapsedTime < sixtyFPS) {
                return;
            }

            elapsedTime -= sixtyFPS;

            if (songService.getFrequency() > 101) {
                applyEffectToLayer(mgScene);
                applyEffectToLayer(bgScene);

                float decreasedFrequency = Math.max(101, songService.getFrequency() - 300);
                float decreasedSpeed = GameHelper.getSpeedMultiplier() * (1 - (initialFrequency - decreasedFrequency) / initialFrequency);

                if (videoEnabled && video != null) {
                    video.setPlaybackSpeed(decreasedSpeed);
                }

                songService.setFrequencyForcefully(decreasedFrequency);
            } else {
                if (videoEnabled && video != null) {
                    video.pause();
                }

                // Ensure music frequency is reset back to what it was.
                songService.setFrequencyForcefully(initialFrequency);
                gameplayClock.pause();
                paused = true;

                scene.setIgnoreUpdate(true);
                hud.setIgnoreUpdate(true);

                failAnimation = null;

                showPauseMenu(true);
            }
        }
    }

    public void resume() {
        if (!paused) {
            return;
        }

        scene.setIgnoreUpdate(false);
        hud.setIgnoreUpdate(false);

        if (pauseMenu != null) {
            pauseMenu.hide();
            pauseMenu = null;
        }

        paused = false;

        if (isGameOver) {
            quit();
            return;
        }

        if (!gameplayClock.isRunning()
                && (replaySettingsPanel == null || !replaySettingsPanel.getPlaybackControl().getSeekControl().isPlaybackPaused())) {
            gameplayClock.start();

            if (video != null && videoStarted) {
                video.play();
            }

            playLoopingSamples();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    private void createHitEffect(final PointF pos, final String name, Color4 color) {

        var effect = GameObjectPool.getInstance().getEffect(name);
        var isAnimated = effect.hit instanceof UIAnimatedSprite animatedHit && animatedHit.getFrames().length > 1;

        // Reference https://github.com/ppy/osu/blob/ebf637bd3c33f1c886f6bfc81aa9ea2132c9e0d2/osu.Game/Skinning/LegacyJudgementPieceOld.cs

        float fadeInLength = 0.12f;
        float fadeOutLength = 0.6f;
        float fadeOutDelay = 0.5f;

        if (name.equals("hit0")) {
            var rotation = (float) Random.Default.nextDouble(8.6 * 2) - 8.6f;

            if (isAnimated) {
                // Legacy judgements don't play any transforms if they are an animation.
                effect.init(
                    mgScene,
                    pos,
                    scale,
                    sequence -> sequence
                            .fadeIn(fadeInLength)
                            .then(fadeOutDelay)
                            .fadeOut(fadeOutLength)
                );
            } else {
                effect.init(
                    mgScene,
                    pos,
                    scale * 1.6f,
                    sequence -> sequence
                            .fadeIn(fadeInLength)
                            .then(fadeOutDelay)
                            .fadeOut(fadeOutLength),
                    sequence -> sequence
                            .scaleTo(scale, 0.1f, Easing.InQuad)
                            .translateToY(-5)
                            .moveToY(80, fadeOutDelay + fadeOutLength, Easing.InQuad),
                    sequence -> sequence
                            .rotateTo(0)
                            .rotateTo(rotation, fadeInLength)
                            .then()
                            .rotateTo(rotation * 2, fadeOutDelay + fadeOutLength - fadeInLength, Easing.InQuad)
                );
            }

            return;
        }

        if (Config.isHitLighting() && !name.equals("sliderpoint10") && !name.equals("sliderpoint30") && SkinTextures.get("lighting") != null) {

            // Reference https://github.com/ppy/osu/blob/a7e110f6693beca6f6e6a20efb69a6913d58550e/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuJudgement.cs#L71-L88

            var light = GameObjectPool.getInstance().getEffect("lighting");
            light.setBlendMode(new BlendMode.Custom(BlendFactor.SrcAlpha, BlendFactor.DstAlpha, BlendFactor.SrcAlpha, BlendFactor.DstAlpha));
            light.setColor(color);
            light.init(
                bgScene,
                pos,
                scale * 0.8f,
                sequence -> sequence.scaleTo(scale * 1.2f, 0.6f, Easing.OutQuad),
                sequence -> sequence.fadeIn(0.2f).then().fadeOut(1f)
            );
        }

        // Legacy judgements don't play any transforms if they are an animation.
        if (isAnimated) {
            effect.init(
                mgScene,
                pos,
                scale,
                sequence -> sequence.fadeIn(fadeInLength).then(fadeOutDelay).fadeOut(fadeOutLength)
            );
        } else {
            effect.init(
                mgScene,
                pos,
                scale * 0.6f,
                sequence -> sequence.fadeIn(fadeInLength).then(fadeOutDelay).fadeOut(fadeOutLength),
                sequence -> sequence
                        .scaleTo(scale * 1.1f, fadeInLength * 0.8f) // t = 0.8
                        .then(fadeInLength * 0.2f) // t = 1.0
                        .scaleTo(scale * 0.9f, fadeInLength * 0.2f) // t = 1.2
                        .then()

                        // stable dictates scale of 0.9->1 over time 1.0 to 1.4, but we are already at 1.2.
                        // so we need to force the current value to be correct at 1.2 (0.95) then complete the
                        // second half of the transform.
                        .scaleTo(scale * 0.95f).scaleTo(scale, fadeInLength * 0.2f) // t = 1.4
            );
        }
    }

    private void applyBurstEffect(GameEffect effect, PointF pos) {

        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174

        var fadeDuration = 0.24f;

        effect.init(mgScene, pos, scale,
                sequence -> sequence
                        .scaleTo(scale * 1.4f, fadeDuration, Easing.OutQuad)
                        .fadeOut(fadeDuration)
        );
    }

    private void createBurstEffect(final PointF pos, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("hitcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("hitcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderStart(final PointF pos, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderstartcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderstartcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderEnd(final PointF pos, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("sliderendcircle");
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect("sliderendcircleoverlay");
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderReverse(final PointF pos, float ang, final Color4 color) {
        if (!Config.isBurstEffects() ||
                (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
                GameHelper.isTraceable())
            return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect("reversearrow");
        burst1.hit.setRotationZ(ang);
        applyBurstEffect(burst1, pos);
    }

    public int getCursorsCount() {
        return cursors.length;
    }


    public void registerAccuracy(HitObjectType type, final double acc) {
        double mehWindow = hitWindow.getMehWindow() / 1000;

        if (Math.abs(acc) <= mehWindow) {
            if (type != HitObjectType.Spinner) {
                stat.addHitOffset(acc);
            }

            if (replaying) {
                scoringScene.getReplayStat().addHitOffset(acc);
            }
        }

        hud.onAccuracyRegister(type, (float) acc);
    }


    public void onSliderEnd(int id, int accuracy, BitSet tickSet) {
        onTrackingSliders(false);
        if (GameHelper.isAutoplay()) {
            autoCursor.onSliderEnd();
        }
        if (replay != null && !replaying) {
            short acc = (short) (accuracy);
            replay.addObjectResult(id, acc, (BitSet) tickSet.clone());
        }
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        if (GameHelper.isAutoplay()) {
            autoCursor.onSliderTracking();
        }
        if (GameHelper.isFlashlight()) {
            flashlightSprite.onTrackingSliders(isTrackingSliders);
        }
    }

    public void onUpdatedAutoCursor(float pX, float pY) {
        if (GameHelper.isFlashlight()) {
            flashlightSprite.onMouseMove(pX, pY);
        }
    }

    public void updateAutoBasedPos(float pX, float pY) {
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.setPosition(pX, pY, this);
        }
    }

    private int getNearestCursorId(float pX, float pY) {
        float nearestDistance = Float.POSITIVE_INFINITY;
        int id = -1;

        for (int i = 0; i < cursors.length; ++i) {
            var latestEvent = cursors[i].getLatestNonUpEvent();

            if (latestEvent != null) {
                float distance = Utils.squaredDistance(pX, pY, latestEvent.position.x, latestEvent.position.y);

                if (distance < nearestDistance) {
                    id = i;
                    nearestDistance = distance;
                }
            }
        }

        return id;
    }

    private void calculateAllSliderPaths(@Nullable final CoroutineScope scope) {
        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || playableBeatmap.getHitObjects().getSliderCount() == 0) {
            return;
        }

        var sliderPaths = new SliderPath[playableBeatmap.getHitObjects().getSliderCount()];
        var sliderRenderPaths = new SliderBody.RenderPathCache[playableBeatmap.getHitObjects().getSliderCount()];
        int index = 0;

        for (var obj : playableBeatmap.getHitObjects().objects) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            if (!(obj instanceof Slider slider)) {
                continue;
            }

            sliderPaths[index] = GameHelper.convertSliderPath(slider, scope);

            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            float scale = slider.getScreenSpaceGameplayScale();
            float sliderBodyWidth = OsuSkin.get().getSliderBodyWidth() * scale;
            float sliderBorderWidth = OsuSkin.get().getSliderBorderWidth() * scale;
            float sliderHintWidth = OsuSkin.get().getSliderHintWidth() * scale;
            boolean isHintVisible = OsuSkin.get().isSliderHintEnable() &&
                slider.getDistance() > OsuSkin.get().getSliderHintShowMinLength();

            sliderRenderPaths[index] = SliderBody.createCache(
                GameHelper.convertSliderPath(sliderPaths[index], scope),
                sliderBodyWidth,
                sliderBorderWidth,
                isHintVisible,
                sliderHintWidth,
                Config.isSnakingInSliders() || Config.isSnakingOutSliders()
            );

            ++index;
        }


        this.sliderPaths = sliderPaths;
        this.sliderRenderPaths = sliderRenderPaths;
    }

    private SliderPath getSliderPath(int index) {
        if (sliderPaths != null && index < sliderPaths.length && index >= 0){
            return sliderPaths[index];
        }
        else {
            return null;
        }
    }

    private SliderBody.RenderPathCache getSliderRenderPath(int index) {
        if (sliderRenderPaths == null) {
            throw new IllegalStateException(
                "Slider render paths have not been calculated; sliderRenderPaths is null, index=" + index
            );
        }

        if (index < 0 || index >= sliderRenderPaths.length) {
            throw new IndexOutOfBoundsException(
                "Invalid slider render path index " + index + " for sliderRenderPaths.length=" + sliderRenderPaths.length
            );
        }

        var renderPath = sliderRenderPaths[index];

        if (renderPath == null) {
            throw new IllegalStateException(
                "Slider render path at index " + index + " is null after calculation; sliderRenderPaths.length=" + sliderRenderPaths.length
            );
        }

        return renderPath;
    }

    public boolean getReplaying() {
        return replaying;
    }

    public @Nullable DroidPlayableBeatmap getPlayableBeatmap() {
        return playableBeatmap;
    }

    public @Nullable ReplaySettingsPanel getReplaySettingsPanel() {
        return replaySettingsPanel;
    }

    /**
     * The time that gameplay started relative to the start of the {@link Beatmap}, in seconds.
     */
    public float getInitialStartTime() {
        return initialStartTime;
    }

    /**
     * The current elapsed time relative to the start of the {@link Beatmap}, in seconds.
     */
    @Override
    public float getElapsedTime() {
        return gameplayClock.getTime();
    }

    @Override
    public boolean isAfterSeek() {
        return postSeekFrameCount > 0;
    }

    public boolean saveFailedReplay() {
        stat.setTime(System.currentTimeMillis());
        if (replay != null && !replaying) {
            //write misses to replay
            for (GameObject obj : activeObjects) {
                stat.registerHit(0, false, false);
                replay.addObjectScore(obj.getId(), ResultType.MISS);
            }
            while (objectIndex < objects.length) {
                ++objectIndex;
                stat.registerHit(0, false, false);
                replay.addObjectScore(++lastObjectId, ResultType.MISS);
            }

            var currentTime = String.valueOf(System.currentTimeMillis());
            var odrFilename = MD5Calculator.getStringMD5(lastBeatmapInfo.getFilename() + currentTime) + currentTime.substring(0, Math.min(3, currentTime.length())) + ".odr";

            replayFilePath = Config.getScorePath() + odrFilename;
            replay.setStat(stat);
            replay.save(replayFilePath);

            if (stat.getTotalScoreWithMultiplier() > 0 && !stat.getMod().contains(ModAutoplay.class)) {
                stat.setReplayFilename(odrFilename);
                stat.setBeatmapMD5(lastBeatmapInfo.getMD5());

                try {
                    DatabaseManager.getScoreInfoTable().insertScore(stat.toScoreInfo());
                } catch (Exception e) {
                    Log.e("GameScene", "Failed to save score to database", e);
                }
            }

            ToastLogger.showText(StringTable.get(com.osudroid.resources.R.string.message_save_replay_successful), true);
            replayFilePath = null;
            return true;
        }
        else{
            ToastLogger.showText(StringTable.get(com.osudroid.resources.R.string.message_save_replay_failed), true);
            return false;
        }
    }

    private void updatePPValue(int objectId) {
        if (Config.isHideInGameUI() || !isHUDEditorMode && !OsuSkin.get().getHUDSkinData().hasElement(HUDPPCounter.class)) {
            return;
        }

        double pp = switch (Config.getDifficultyAlgorithm()) {
            case droid -> getDroidPPAt(objectId);
            case standard -> getStandardPPAt(objectId);
        };

        stat.setPP(pp);
    }

    private double getDroidPPAt(int objectId) {
        if (droidTimedDifficultyAttributes == null || performanceCalculationParameters == null || objectId < 0 ||
                objectId >= droidTimedDifficultyAttributes.length) {
            return 0;
        }

        var timedAttributes = droidTimedDifficultyAttributes[objectId];

        performanceCalculationParameters.populate(stat, timedAttributes.sliderCount, timedAttributes.sliderTickCount, timedAttributes.sliderRepeatCount);

        BeatmapDifficultyCalculator.calculateDroidPerformance(timedAttributes.attributes,
                (DroidPerformanceCalculationParameters) performanceCalculationParameters,
                (DroidPerformanceAttributes) performanceAttributes);

        return performanceAttributes.total;
    }

    private double getStandardPPAt(int objectId) {
        if (standardTimedDifficultyAttributes == null || performanceCalculationParameters == null || objectId < 0 ||
                objectId >= standardTimedDifficultyAttributes.length) {
            return 0;
        }

        var timedAttributes = standardTimedDifficultyAttributes[objectId];

        performanceCalculationParameters.populate(stat, timedAttributes.sliderCount, timedAttributes.sliderTickCount, timedAttributes.sliderRepeatCount);

        BeatmapDifficultyCalculator.calculateStandardPerformance(timedAttributes.attributes,
                (StandardPerformanceCalculationParameters) performanceCalculationParameters,
                (StandardPerformanceAttributes) performanceAttributes);

        return performanceAttributes.total;
    }

    private void applyPlayfieldSizeScale() {
        float windowWidth = root.getBounds().getWidth();

        if (windowWidth <= 0f) {
            return;
        }

        float playfieldSize = Config.getPlayfieldSize();
        float resolutionWidth = Config.getRES_WIDTH();
        float resolutionHeight = Config.getRES_HEIGHT();

        // The point of the playfield that ends up at the center of the window.
        float centerX = resolutionWidth * Interpolation.linear(
            Interpolation.linear(0f, 0.5f, playfieldSize),
            Interpolation.linear(1f, 0.5f, playfieldSize),
            1 - Config.getPlayfieldHorizontalPosition()
        );

        float centerY = resolutionHeight * Interpolation.linear(
            Interpolation.linear(0f, 0.5f, playfieldSize),
            Interpolation.linear(1f, 0.5f, playfieldSize),
            1 - Config.getPlayfieldVerticalPosition()
        );

        float resolutionScale = windowWidth / resolutionWidth;

        playfield.setScale(resolutionScale * playfieldSize);
        playfield.setTranslationX(DimensionKt.getPx(resolutionScale * (resolutionWidth / 2f - centerX * playfieldSize)));
        playfield.setTranslationY(DimensionKt.getPx(resolutionScale * (resolutionHeight / 2f - centerY * playfieldSize)));
    }

    private void resetPlayfieldSizeScale() {
        playfield.setScale(root.getBounds().getWidth() / Config.getRES_WIDTH());
        playfield.setTranslationX(Dimension.Zero);
        playfield.setTranslationY(Dimension.Zero);
    }

    private int estimateMaximumActiveObjects() {
        if (objects == null) {
            return 0;
        }

        // Estimate the maximum number of simultaneously active objects to pre-size the lists and minimize
        // array reallocations.
        var lifetimeEnds = new PriorityQueue<Double>(Math.max(1, objects.length / 4));
        int estimatedMaxActiveObjects = 0;

        for (var object : objects) {
            double lifetimeStart = object.startTime - object.timePreempt;

            // Remove all objects that have expired by the time this object's lifetime starts.
            while (!lifetimeEnds.isEmpty() && lifetimeEnds.peek() <= lifetimeStart) {
                lifetimeEnds.poll();
            }

            double lifetimeEnd;

            if (object instanceof HitCircle) {
                var hitWindow = object.hitWindow;
                lifetimeEnd = object.startTime + (hitWindow != null ? hitWindow.getMehWindow() : 0);
            } else if (object instanceof Slider slider) {
                lifetimeEnd = Math.max(object.startTime + slider.getHead().hitWindow.getMehWindow(), object.getEndTime());
            } else {
                lifetimeEnd = object.getEndTime();
            }

            lifetimeEnds.add(lifetimeEnd);
            estimatedMaxActiveObjects = Math.max(estimatedMaxActiveObjects, lifetimeEnds.size());
        }

        return estimatedMaxActiveObjects;
    }

    //region Seeking

    private void seekTo(float targetTimeSeconds) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || objects == null) {
            return;
        }

        float clampedTime = FMath.clamp(
            targetTimeSeconds,
            (float) (objects[0].startTime / 1000),
            (float) (objects[objects.length - 1].getEndTime() / 1000)
        );

        float targetMs = clampedTime * 1000;

        // Force-expire all active objects (detach from scene and return to pool).
        for (int i = 0, size = activeObjects.size(); i < size; ++i) {
            var obj = activeObjects.get(i);

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire();
            }
        }

        // Also expire objects that were queued for cleanup but not yet processed.
        for (int i = 0, size = expiredObjects.size(); i < size; ++i) {
            var obj = expiredObjects.get(i);

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire();
            }
        }

        activeObjects.clear();
        expiredObjects.clear();
        processedExpiredObjects.clear();
        judgeableObject = null;

        // Detach any lingering hit effects from the gameplay scene.
        for (int i = mgScene.getChildCount() - 1; i >= 0; --i) {
            var child = mgScene.get(i);

            if (child instanceof UISprite && child.getChoreography() != null) {
                // Hit effects need to be pooled, so we cannot directly detach.
                // finish() fires the effect's fade-out callback, which schedules putEffect() so the GameEffect
                // wrapper is returned to the pool rather than orphaned.
                child.getChoreography().finish();
            }

            // We detach directly so that the next frame is not orphaned with hit effects that were not detached (since
            // GameEffect's callback is scheduled for the next update tick).
            child.removeSelf();
        }

        // Remove follow points spawned for objects before the seek point.
        FollowPointConnection.clearAll(bgScene);

        // Reset spawn counters.
        objectIndex = 0;
        sliderIndex = 0;
        lastObjectId = -1;
        gameStarted = false;
        leadOut = 0;
        comboWasMissed = false;
        comboWas100 = false;
        failcount = 0;
        isGameOver = false;
        hasFailed = false;

        // Reset auto cursor modifiers.
        if (autoCursor != null) {
            autoCursor.stopMoving();
        }

        // Clear pending cursor events.
        for (int i = 0; i < cursors.length; ++i) {
            var cursor = cursors[i];

            if (cursor != null) {
                cursor.reset(SystemClock.uptimeMillis(), 0);
                cursor.latestProcessedDownEventIndex = 0;
                cursor.latestProcessedEventIndex = 0;
            }
        }

        // Advance timing and effect control points to the target time.
        timingControlPointIndex = 0;
        effectControlPointIndex = 0;

        while (timingControlPointIndex + 1 < timingControlPoints.length && timingControlPoints[timingControlPointIndex + 1].time <= targetMs) {
            timingControlPointIndex++;
        }

        while (effectControlPointIndex + 1 < effectControlPoints.length && effectControlPoints[effectControlPointIndex + 1].time <= targetMs) {
            effectControlPointIndex++;
        }

        var controlPoints = playableBeatmap.getControlPoints();

        activeTimingPoint = timingControlPoints.length > 0 ?
            timingControlPoints[timingControlPointIndex] : controlPoints.timing.defaultControlPoint;

        activeEffectPoint = effectControlPoints.length > 0 ?
            effectControlPoints[effectControlPointIndex] : controlPoints.effect.defaultControlPoint;

        // Advance break period index past fully elapsed breaks.
        breakPeriodIndex = 0;

        if (breakPeriods != null) {
            while (breakPeriodIndex < breakPeriods.length && breakPeriods[breakPeriodIndex].endTime <= targetMs) {
                breakPeriodIndex++;
            }
        }

        // Reset replay cursor movement indices.
        if (replaying && replay.cursorIndex != null) {
            Arrays.fill(replay.cursorIndex, 0);
            Arrays.fill(replay.lastMoveIndex, -1);
        }

        // Reconstruct scoring state up to the seek target.
        stat.reset();
        reconstructStatAtTime(targetMs);

        // Reset any in-progress break animation, then re-initialize if the seek target is inside a break.
        breakAnimator.reset();

        if (breakPeriods != null && breakPeriodIndex < breakPeriods.length) {
            var bp = breakPeriods[breakPeriodIndex];

            if (bp.startTime <= targetMs && targetMs < bp.endTime) {
                gameStarted = false;
                float totalDuration = bp.getDuration() / 1000f;
                float elapsedTime = (float) ((targetMs - bp.startTime) / 1000.0);
                breakAnimator.init(totalDuration, elapsedTime);
                breakPeriodIndex++;
                hud.onBreakStateChange(true);
            } else {
                hud.onBreakStateChange(false);
            }
        } else {
            hud.onBreakStateChange(false);
        }

        hud.onNoteHit(stat);
        hud.onSeek();

        // Replay all touch-down events up to the seek target so HUD elements
        // that depend on touch history can reconstruct their state correctly.
        if (GameHelper.isAutoplay() || replaying) {
            if (GameHelper.isAutoplay()) {
                for (int i = 0; i < objects.length; ++i) {
                    float tapTime = (float) objects[i].startTime / 1000f;

                    if (tapTime > clampedTime) {
                        break;
                    }

                    hud.onGameplayTouchDown(tapTime);
                }
            } else {
                int cursorCount = replay.cursorMoves.size();

                for (int i = 0; i < cursorCount; ++i) {
                    var moveArray = replay.cursorMoves.get(i);

                    for (int j = 0; j < moveArray.size; ++j) {
                        var movement = moveArray.movements[j];

                        float tapTime = movement.getTime() / 1000f;

                        if (tapTime > clampedTime) {
                            break;
                        }

                        if (movement.getTouchType() == TouchType.DOWN) {
                            hud.onGameplayTouchDown(tapTime);
                        }
                    }
                }
            }
        }

        updatePPValue(objectIndex - 1);

        // For variable-rate mods (WindUp/WindDown), the rate at the seek target may differ from
        // the current rate. FramedBeatmapClock.seek() subtracts (userGlobalOffset * rate) from the
        // seek position to compute the audio byte offset, so the rate must be updated first.
        if (!rateAdjustingMods.isEmpty()) {
            float modRate = ModUtils.calculateRateWithTrackRateMods(rateAdjustingMods, targetMs);

            float replaySettingsRate = replaySettingsPanel != null
                ? replaySettingsPanel.getPlaybackControl().getRateControl().getRate()
                : 1f;

            float targetSpeedMultiplier = modRate * replaySettingsRate;

            if (targetSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(targetSpeedMultiplier);
                gameplayClock.setRate(targetSpeedMultiplier);
            }
        }

        // Seek the beatmap clock (also seeks the audio source).
        gameplayClock.seek(clampedTime);

        // Seek video.
        if (videoEnabled && video != null) {
            video.seekTo(Math.max(0, (int) ((clampedTime - videoOffset) * 1000)));
        }

        // Suppress hitsounds for slider objects (head, ticks, repeats) that have already passed the seek target.
        // Objects are spawned in the same frame as seekTo runs, but updated in the next frame,
        // so the flag must survive 2 update frames.
        postSeekFrameCount = 2;
    }

    private void reconstructStatAtTime(float targetMs) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || objects == null || objects.length == 0) {
            return;
        }

        var difficulty = playableBeatmap.getDifficulty();
        int localTimingIdx = 0;
        int localBreakIdx = 0;
        var objectData = replaying ? replay.objectData : null;

        // How far continuous HP drain has been applied, and which object-to-object rate segment
        // it's currently within ([objects[rateSegIdx].startTime - timePreempt,
        // objects[rateSegIdx + 1].startTime - timePreempt)). Both advance with elapsed
        // time and is independent of which object's own judgement has been applied yet.
        double drainCursorMs = objects[0].startTime - objects[0].timePreempt;
        int rateSegIdx = 0;

        for (int i = 0; i < objects.length; i++) {
            var obj = objects[i];

            double judgementTimeMs = getJudgementTimeMs(i, obj, objectData);

            if (judgementTimeMs > targetMs) {
                break;
            }

            double drainTargetMs = Math.min(judgementTimeMs, targetMs);

            while (drainCursorMs < drainTargetMs && rateSegIdx < objects.length - 1) {
                var segFromObj = objects[rateSegIdx];
                var segToObj = objects[rateSegIdx + 1];

                // Advance local timing point index to this segment's own "to" object.
                while (localTimingIdx + 1 < timingControlPoints.length && timingControlPoints[localTimingIdx + 1].time <= segToObj.startTime) {
                    localTimingIdx++;
                }

                double msPerBeat = timingControlPoints[localTimingIdx].msPerBeat;
                double distToNextObject = Math.max(segToObj.startTime - segFromObj.startTime, msPerBeat / 2) / 1000;

                double drainRate = difficulty.hp > 0 && distToNextObject > 0
                        ? 1 + difficulty.hp / (2 * distToNextObject)
                        : 0.375;

                double segEndMs = segToObj.startTime - segToObj.timePreempt;
                double stopAtMs = Math.min(segEndMs, drainTargetMs);

                if (stopAtMs > drainCursorMs) {
                    // Advance past breaks that fully precede the current segment.
                    if (breakPeriods != null) {
                        while (localBreakIdx < breakPeriods.length && breakPeriods[localBreakIdx].endTime <= drainCursorMs) {
                            localBreakIdx++;
                        }
                    }

                    double effectiveSecs = calculateEffectiveDrainDuration(drainCursorMs, stopAtMs, localBreakIdx);

                    // Apply drain incrementally so that a large drain section can consume multiple Easy lives.
                    // A one-shot stat.changeHp clamps at 0 and loses the excess, causing at most one Easy revive per drain
                    // section regardless of how deep HP would have gone.
                    float remainingDrain = (float) (drainRate * 0.01 * effectiveSecs);

                    while (remainingDrain > 0) {
                        float currentHp = stat.getHp();

                        if (remainingDrain < currentHp) {
                            stat.changeHp(-remainingDrain);
                            break;
                        }

                        remainingDrain -= currentHp;
                        stat.changeHp(-currentHp);

                        if (!stat.canFail) {
                            break;
                        }

                        if (GameHelper.isEasy() && failcount < 3) {
                            failcount++;
                            stat.changeHp(1f);
                        } else {
                            return;
                        }
                    }

                    drainCursorMs = stopAtMs;
                }

                if (drainCursorMs >= segEndMs) {
                    rateSegIdx++;
                } else {
                    // Capped short of this segment's own end by drainTargetMs - stop crossing
                    // further for now. The remainder of this same segment continues once a
                    // later object's own judgement time lets drain proceed past it.
                    break;
                }
            }

            if (!gameStarted) {
                gameStarted = true;
            }

            var data = (objectData != null && i < objectData.length) ? objectData[i] : null;

            boolean endCombo = obj.isLastInCombo();

            if (obj instanceof HitCircle) {
                applyCircleResult(data, endCombo);
            } else if (obj instanceof Slider slider) {
                sliderIndex++;
                applySliderResult(slider, data, endCombo);
            } else if (obj instanceof Spinner parsedSpinner) {
                applySpinnerResult(parsedSpinner, data, endCombo);
            }

            lastObjectId = i;
            objectIndex = i + 1;
        }
    }

    private double getJudgementTimeMs(int idx, HitObject obj, @Nullable Replay.ReplayObjectData[] objectData) {
        if (obj instanceof Slider || obj instanceof Spinner) {
            return obj.getEndTime();
        }

        // Circle: judged when hit (or miss window expires).
        if (objectData != null && idx < objectData.length) {
            var data = objectData[idx];

            if (data != null && data.result != ResultType.MISS.getId()) {
                return obj.startTime + Math.abs(data.accuracy);
            }
        }

        double mehWindow = obj.hitWindow != null ? obj.hitWindow.getMehWindow() : HitWindow.MISS_WINDOW;

        return obj.startTime + mehWindow;
    }

    private void reconstructHitOffset(double accSeconds) {
        if (Math.abs(accSeconds) <= hitWindow.getMehWindow() / 1000) {
            stat.addHitOffset(accSeconds);
        }
    }

    private void applyCircleResult(@Nullable Replay.ReplayObjectData data, boolean endCombo) {
        byte result = data != null ? data.result : ResultType.HIT300.getId();

        if (result == ResultType.MISS.getId()) {
            comboWasMissed = true;
            stat.registerHit(0, false, false);
        } else {
            reconstructHitOffset(data != null ? data.accuracy / 1000.0 : 0.0);

            if (result == ResultType.HIT50.getId()) {
                stat.registerHit(50, false, false);
                comboWas100 = true;
            } else if (result == ResultType.HIT100.getId()) {
                comboWas100 = true;
                stat.registerHit(100, endCombo && !comboWasMissed, false);
            } else {
                if (endCombo && !comboWasMissed) {
                    stat.registerHit(300, true, !comboWas100);
                } else {
                    stat.registerHit(300, false, false);
                }
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }
    }

    private void applySliderResult(Slider slider, @Nullable Replay.ReplayObjectData data, boolean endCombo) {
        byte result = data != null ? data.result : ResultType.HIT300.getId();

        if (result == ResultType.MISS.getId()) {
            comboWasMissed = true;
            stat.registerHit(0, false, false);

            if (endCombo) {
                comboWas100 = false;
                comboWasMissed = false;
            }

            return;
        }

        // Slider head: HIT300 implies all ticks were hit; autoplay (data==null) always hits.
        // For HIT50/HIT100, reconstruct whether the head was actually within the hit window,
        // mirroring GameplaySlider.onSliderHeadHit.
        double accSeconds = data != null ? data.accuracy / 1000.0 : 0.0;
        boolean headHit;

        if (data == null || result == ResultType.HIT300.getId()) {
            headHit = true;
        } else {
            int replayVersion = GameHelper.getReplayVersion();
            double mehWindowSecs = hitWindow.getMehWindow() / 1000.0;
            double sliderDurationSecs = slider.getDuration() / 1000.0;
            double lateHitThreshold = replayVersion <= 7 ? Math.min(mehWindowSecs, sliderDurationSecs) : mehWindowSecs;

            if (replayVersion >= 6 || mehWindowSecs <= sliderDurationSecs) {
                headHit = -mehWindowSecs <= accSeconds && accSeconds <= lateHitThreshold;
            } else {
                headHit = accSeconds <= sliderDurationSecs;
            }
        }

        if (headHit) {
            reconstructHitOffset(accSeconds);
            stat.registerHit(30, false, false);
            stat.addSliderHeadHit();
        }

        // Ticks and repeats from tickSet.
        var nested = slider.getNestedHitObjects();

        // Skip head (index 0) and tail (last index).
        for (int i = 1, end = nested.size() - 1; i < end; i++) {
            boolean wasHit = data == null || (data.tickSet != null && data.tickSet.get(i - 1));

            if (!wasHit) {
                stat.registerHit(0, true, false);
                continue;
            }

            var nestedObj = nested.get(i);

            if (nestedObj instanceof SliderTick) {
                stat.registerHit(10, false, false);
                stat.addSliderTickHit();
            } else if (nestedObj instanceof SliderRepeat) {
                stat.registerHit(30, false, false);
                stat.addSliderRepeatHit();
            }
        }

        // Slider tail: combo is only awarded when the player was tracking at the endpoint.
        // Unlike circles, the result score reflects ticks hit, not timing accuracy.
        boolean tailTracked = data == null || (data.tickSet != null && data.tickSet.get(nested.size() - 2));
        byte tailResult = data != null ? data.result : ResultType.HIT300.getId();

        if (tailResult == ResultType.HIT50.getId()) {
            comboWas100 = true;
            stat.registerHit(50, false, false, tailTracked);
        } else if (tailResult == ResultType.HIT100.getId()) {
            comboWas100 = true;
            stat.registerHit(100, endCombo && !comboWasMissed, false, tailTracked);
        } else {
            if (endCombo && !comboWasMissed) {
                stat.registerHit(300, true, !comboWas100, tailTracked);
            } else {
                stat.registerHit(300, false, false, tailTracked);
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }

        if (tailTracked) {
            stat.addSliderEndHit();
        }
    }

    private void applySpinnerResult(Spinner spinner, @Nullable Replay.ReplayObjectData data, boolean endCombo) {
        float duration = (float) spinner.getDuration() / 1000;
        float needRotations = (2 + 2 * playableBeatmap.getDifficulty().od / 10f) * duration;

        if (duration < 0.05f) {
            needRotations = 0.1f;
        }

        int preClear;
        int bonus;

        if (data != null) {
            // data.accuracy = totalSpins * 4 + resultCode, where totalSpins = fullRotations (pre-clear, 100 pts each)
            // + (bonusScoreCounter - 1) (bonus, 1000 pts each). Split by needRotations to award the correct amounts.
            int totalSpins = (data.accuracy & 0xFFFF) >> 2;
            preClear = Math.min(totalSpins, (int) Math.ceil(needRotations) - 1);
            bonus = totalSpins - preClear;
        } else {
            // Autoplay always clears the spinner. Reconstruct the pre-clear (100 pts each) and bonus (1000 pts each)
            // rotation split from the spinner's parameters.
            // ceil(needRotations) - 1 rotations are pre-clear; bonus rotations begin at ceil(needRotations) total.
            float totalRotations = 5f * duration;

            preClear = (int) Math.ceil(needRotations) - 1;
            bonus = Math.max(0, (int) totalRotations - (int) Math.ceil(needRotations) + 1);
        }

        for (int s = 0; s < preClear; s++) {
            stat.registerSpinnerHit();
        }

        for (int s = 0; s < bonus; s++) {
            stat.registerHit(1000, false, false);
        }

        applyCircleResult(data, endCombo);
    }

    /**
     * Returns the effective drain duration in seconds for the segment [startMs, endMs], subtracting any time that falls
     * within a break period. {@code startBreakIdx} should be the first break period index whose end time is >= startMs
     * (caller advances this monotonically as segments progress forward in time).
     */
    private double calculateEffectiveDrainDuration(double startMs, double endMs, int startBreakIdx) {
        if (startMs >= endMs) {
            return 0;
        }

        double total = endMs - startMs;

        if (breakPeriods != null) {
            for (int i = startBreakIdx; i < breakPeriods.length; ++i) {
                var bp = breakPeriods[i];

                if (bp.startTime >= endMs) {
                    break;
                }

                double overlap = Math.min(bp.endTime, endMs) - Math.max(bp.startTime, startMs);

                if (overlap > 0) {
                    total -= overlap;
                }
            }
        }

        return Math.max(0, total) / 1000;
    }

    //endregion

    private class GameUIScene extends UIScene {
        private final Stopwatch stopwatch = new Stopwatch();
        private float lastRootWidth;
        private boolean areModIconsAnimated;

        public GameUIScene() {
            super();

            setClock(gameplayClock);
        }

        @Override
        public boolean onInputEvent(@NonNull InputEvent event) {
            if (hud != null && hud.onInputEvent(event)) {
                return true;
            }

            if (event instanceof PointerEvent pointer) {
                // Pointer events come from the main thread while the gameplay state belongs to the update thread.
                return Verktex.withTreeLock(() -> onPointerEvent(pointer));
            }

            return super.onInputEvent(event);
        }

        @Override
        protected void onUpdate(@NonNull FrameClock clock) {
            float deltaTimeSec = clock.getElapsedFrameTime();

            // The playfield follows the size of the window, which is only known after the first layout.
            if (root.getBounds().getWidth() != lastRootWidth) {
                lastRootWidth = root.getBounds().getWidth();
                applyPlayfieldSizeScale();
                applyBackground();
            }

            if (!areModIconsAnimated) {
                areModIconsAnimated = true;

                for (int i = 0, size = modIcons.size(); i < size; i++) {
                    float finalTimeOffset = i * 0.25f;

                    ChoreographyKt.choreographNow(modIcons.get(i), sequence -> sequence
                            .scaleTo(1, 0.25f)
                            .delay(2 - finalTimeOffset)
                            .fadeOut(0.5f)
                            .scaleTo(1.5f, 0.5f));
                }

                modIcons.clear();
            }

            if (failAnimation != null) {
                failAnimation.update(deltaTimeSec);
            }

            if (replayText != null) {
                updateReplayText(deltaTimeSec);
            }

            stopwatch.restart();

            do {
                GameScene.this.update(deltaTimeSec);

                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < cursors.length; ++i) {
                    cursors[i].reset(previousFrameTime, gameplayClock.getTime() * 1000);
                }

                super.onUpdate(clock);
                // 10ms of allowance when catching up.
            } while (gameplayClock.getRequiresCatchUp() && stopwatch.getElapsedSeconds() < 0.01f);
        }

        private void updateReplayText(float deltaTimeSec) {
            float textWidth = replayText.getBounds().getWidth();
            float distance = Config.getRES_WIDTH() + 5 + textWidth;
            float x = replayText.getX().resolve(0f) - distance / 40f * deltaTimeSec;

            replayText.setPosition(x < -textWidth ? Config.getRES_WIDTH() + 5 : x, 140);
        }
    }
}
