package ru.nsu.ccfit.zuev.osu.scoring;

import com.reco1l.verktex.Logger;

import com.edlplan.framework.utils.functionality.SmartIterator;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.data.DatabaseManager;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.ui.v2.modmenu.ModIcon;
import com.osudroid.ui.v2.multi.LobbyScene;
import com.osudroid.utils.Execution;
import com.osudroid.ui.v2.SkinSounds;
import com.osudroid.ui.v2.SkinTextures;
import com.osudroid.ui.v2.StatisticSelector;
import com.osudroid.ui.v2.game.ScoreNumber;
import com.reco1l.verktex.Verktex;
import com.reco1l.verktex.data.Anchor;
import com.reco1l.verktex.data.Color4;
import com.reco1l.verktex.data.Dimension;
import com.reco1l.verktex.input.InputEvent;
import com.reco1l.verktex.input.PointerEvent;
import com.reco1l.verktex.texture.Textures;
import com.reco1l.verktex.ui.TransformsKt;
import com.reco1l.verktex.ui.UIScene;
import com.reco1l.verktex.ui.UISprite;
import com.reco1l.verktex.ui.container.UILinearContainer;
import com.reco1l.verktex.ui.shape.UIBox;
import com.reco1l.verktex.ui.text.UIText;
import com.reco1l.verktex.ui.container.UIContainer;

import com.osudroid.GameMode;
import com.osudroid.beatmaps.Beatmap;
import com.osudroid.difficulty.BeatmapDifficultyCalculator;
import com.osudroid.difficulty.attributes.DifficultyAttributes;
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes;
import com.osudroid.difficulty.attributes.PerformanceAttributes;
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes;
import com.osudroid.mods.ModAutoplay;
import com.osudroid.mods.ModCustomSpeed;
import com.osudroid.mods.ModDifficultyAdjust;
import com.osudroid.mods.ModFlashlight;
import com.osudroid.mods.ModNightCore;
import com.osudroid.mods.ModOldNightCore;
import com.osudroid.ui.SendingPanel;

import org.apache.commons.io.FilenameUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

import kotlin.Unit;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import com.osudroid.data.BeatmapInfo;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class ScoringScene {
    private final GameScene game;
    private UIScene scene;
    private UIContainer content;
    private SongService songService;
    private StatisticV2 replayStat;
    private int replayID = -1;
    public BeatmapInfo beatmapInfo;

    // Multiplayer
    public StatisticV2 currentStatistic;

    private StatisticSelector selector;


    public ScoringScene(final GameScene pGame) {
        game = pGame;
    }

    private UISprite createSprite(String texture, float x, float y) {
        var sprite = new UISprite(SkinTextures.get(texture));
        sprite.setPosition(x, y);
        return sprite;
    }

    /**
     * Creates a sprite centered in the given position with a size relative to its texture.
     */
    private UISprite createHitSprite(String texture, float centerX, float centerY) {
        var sprite = createSprite(texture, Utils.toRes(centerX), Utils.toRes(centerY));
        sprite.setOrigin(Anchor.Center);
        return sprite;
    }

    private UIText createText(String value, float fontSize) {
        var text = new UIText();
        text.setFontSize(new Dimension.Fixed.Pixels(fontSize));
        text.setText(value);
        return text;
    }

    private float textureWidth(String texture) {
        var region = SkinTextures.get(texture);
        return region != null ? region.getWidth() : 0f;
    }

    private float textureHeight(String texture) {
        var region = SkinTextures.get(texture);
        return region != null ? region.getHeight() : 0f;
    }

    private UISprite createButton(String texture, Runnable onClick) {
        return new UISprite(SkinTextures.get(texture)) {
            private boolean isPressed;

            @Override
            public boolean onInputEvent(@NonNull InputEvent event) {
                if (!(event instanceof PointerEvent pointer)) {
                    return false;
                }

                if (pointer.getAction() == PointerEvent.Action.Down && containsScenePoint(pointer.getX(), pointer.getY())) {
                    isPressed = true;
                    setColor(0.7f, 0.7f, 0.7f);
                    SkinSounds.get("menuback").play();
                    return true;
                }

                if (isPressed && pointer.getAction() == PointerEvent.Action.Up) {
                    isPressed = false;
                    onClick.run();
                    return true;
                }

                return false;
            }
        };
    }

    private void leaveAndRun(Runnable action) {
        Verktex.withTreeLock(() -> {
            Verktex.popScene();
            return Unit.INSTANCE;
        });

        action.run();
    }

    public void load(final StatisticV2 stat, final BeatmapInfo beatmap,
                     final SongService player, final String replayPath, final String mapMD5,
                     final BeatmapInfo beatmapToReplay) {
        var wasShown = scene != null && Verktex.getScene() == scene;
        var previousScene = scene;

        scene = new UIScene();
        songService = player;
        currentStatistic = stat;
        if (replayPath != null && beatmap == null) {
            replayStat = stat;
        }

        var backgroundSprite = new UISprite(Textures.INSTANCE.get("::background") != null
                ? Textures.INSTANCE.get("::background")
                : SkinTextures.get("menu-background"));
        backgroundSprite.setWidth(Dimension.FillAvailable);
        backgroundSprite.setHeight(Dimension.FillAvailable);
        backgroundSprite.setScaleType(UISprite.ScaleType.Crop);
        scene.setBackground(backgroundSprite);

        // Everything is placed in the coordinates of the game's resolution and scaled to fit the window.
        content = new UIContainer();
        content.setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        content.setScaleOrigin(Anchor.TopLeft);
        content.setScale((float) Verktex.INSTANCE.getGraphics().getWindow().getWindowWidth() / Config.getRES_WIDTH());
        scene.plusAssign(content);

        var bgTopRect = new UIBox();
        bgTopRect.setSize(Config.getRES_WIDTH(), Utils.toRes(100));
        bgTopRect.setBackgroundColor(new Color4(0f, 0f, 0f, 0.8f));
        content.plusAssign(bgTopRect);

        beatmapInfo = beatmapToReplay;
        if (beatmapToReplay == null && beatmap != null) {
            beatmapInfo = beatmap;
        }

        float paddingButtom = Multiplayer.isConnected()
                ? Multiplayer.roomScene.getChat().getButtonHeight()
                : 0f;

        UIContainer scaledContainer = new UIContainer();
        scaledContainer.setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        scaledContainer.setPosition(0f, 100f);
        scaledContainer.setScaleOrigin(Anchor.TopLeft);
        scaledContainer.setScale((Config.getRES_HEIGHT() - paddingButtom) / Config.getRES_HEIGHT());
        content.plusAssign(scaledContainer);

        final int x = 0, y = 0;
        var panel = createSprite("ranking-panel", x, y);
        panel.setSize(Utils.toRes(textureWidth("ranking-panel") * 0.9f), Utils.toRes(textureHeight("ranking-panel") * 0.9f));
        scaledContainer.plusAssign(panel);

        scaledContainer.plusAssign(createHitSprite("hit300", 70 + x, 130 + y));
        scaledContainer.plusAssign(createHitSprite("hit100", 70 + x, 130 + 92 + y));
        scaledContainer.plusAssign(createHitSprite("hit50", 70 + x, 130 + 92 * 2 + y));
        scaledContainer.plusAssign(createHitSprite("hit300g", 340 + x, 130 + y));
        scaledContainer.plusAssign(createHitSprite("hit100k", 340 + x, 130 + 92 + y));
        scaledContainer.plusAssign(createHitSprite("hit0", 340 + x, 130 + 92 * 2 + y));

        content.plusAssign(createSprite("ranking-title",
                (float) (Config.getRES_WIDTH() * 5) / 6 - textureWidth("ranking-title") / 2, 0));

        StringBuilder scoreStr = new StringBuilder(String.valueOf(stat.getTotalScoreWithMultiplier()));
        while (scoreStr.length() < 8) {
            scoreStr.insert(0, "0");
        }
        final ScoreNumber scoreNum = new ScoreNumber(Utils.toRes(220 + x),
                Utils.toRes(18 + y), scoreStr.toString(), 1, false);
        scaledContainer.plusAssign(scoreNum);

        final ScoreNumber hit300num = new ScoreNumber(Utils.toRes(138 + x),
                Utils.toRes(110 + y), stat.getHit300() + "x", 1,
                false);
        scaledContainer.plusAssign(hit300num);
        final ScoreNumber hit100num = new ScoreNumber(Utils.toRes(138 + x),
                Utils.toRes(110 + 85 + y), stat.getHit100() + "x",
                1, false);
        scaledContainer.plusAssign(hit100num);
        final ScoreNumber hit50num = new ScoreNumber(Utils.toRes(138 + x),
                Utils.toRes(110 + 85 * 2 + y), stat.getHit50() + "x",
                1, false);
        scaledContainer.plusAssign(hit50num);

        final ScoreNumber hit300knum = new ScoreNumber(Utils.toRes(400 + x),
                Utils.toRes(110 + y), stat.getHit300k() + "x", 1,
                false);
        scaledContainer.plusAssign(hit300knum);
        final ScoreNumber hit100knum = new ScoreNumber(Utils.toRes(400 + x),
                Utils.toRes(110 + 85 + y), stat.getHit100k() + "x",
                1, false);
        scaledContainer.plusAssign(hit100knum);
        final ScoreNumber hit0num = new ScoreNumber(Utils.toRes(400 + x),
                Utils.toRes(110 + 85 * 2 + y), stat.getMisses() + "x",
                1, false);
        scaledContainer.plusAssign(hit0num);

        scaledContainer.plusAssign(createSprite("ranking-maxcombo", Utils.toRes(20 + x), Utils.toRes(332 + y)));
        scaledContainer.plusAssign(createSprite("ranking-accuracy", Utils.toRes(260 + x), Utils.toRes(332 + y)));
        final ScoreNumber maxCombo = new ScoreNumber(Utils.toRes(20 + x),
                Utils.toRes(332 + y + 38), stat.getScoreMaxCombo() + "x", 1,
                false);
        scaledContainer.plusAssign(maxCombo);
        final String accStr = String
                .format(Locale.ENGLISH, "%2.2f%%", stat.getAccuracy() * 100);
        final ScoreNumber accuracy = new ScoreNumber(Utils.toRes(260 + x),
                Utils.toRes(332 + y + 38), accStr, 1, false);
        scaledContainer.plusAssign(accuracy);

        float markX = (float) (Config.getRES_WIDTH() * 5) / 6 - textureWidth("ranking-" + stat.getMark()) / 2;
        final UISprite mark = createSprite("ranking-" + stat.getMark(), markX, 80);

        if (beatmap != null) {
            mark.setAlpha(0);
            mark.setScale(2f);
            TransformsKt.fadeIn(mark, 2f);
            TransformsKt.scaleTo(mark, 1f, 2f);
        }

        content.plusAssign(mark);

        var backBtn = createButton("ranking-back", this::back);
        float buttonX = Config.getRES_WIDTH() - textureWidth("ranking-back") - 10;
        float backY = Config.getRES_HEIGHT() - textureHeight("ranking-back") - 10 - paddingButtom;
        backBtn.setPosition(buttonX, backY);
        content.plusAssign(backBtn);

        var mods = stat.getMod();

        if (!Multiplayer.isMultiplayer && beatmap != null) {
            var retryBtn = createButton("ranking-retry", () -> {
                SkinSounds.get("applause").stop();
                stopMusic();
                scene = null;
                leaveAndRun(() -> game.startGame(null, null, null));
            });
            retryBtn.setPosition(buttonX, backY - textureHeight("ranking-retry") - 10);
            content.plusAssign(retryBtn);
        } else if (!Multiplayer.isMultiplayer && replayPath != null) {
            var replayBtn = createButton("ranking-replay", () -> {
                SkinSounds.get("applause").stop();
                stopMusic();
                scene = null;
                leaveAndRun(() -> game.startGame(beatmapToReplay, replayPath, mods));
            });
            replayBtn.setPosition(buttonX, backY - textureHeight("ranking-replay") - 10);
            content.plusAssign(replayBtn);
        }

        if (stat.isPerfect()) {
            scaledContainer.plusAssign(createSprite("ranking-perfect", 0, Utils.toRes(332 + y + 38) + accuracy.getLetterHeight() + 10));
        }

        var modX = markX - 30;
        var modY = 80 + textureHeight("ranking-" + stat.getMark()) * 2 / 3;
        for (var mod : mods.values()) {
            if (!mod.isUserPlayable()) {
                continue;
            }

            var modIcon = new ModIcon(mod);
            modIcon.setPosition(modX, modY);
            modIcon.setSize(68, 66);
            modX -= 30;
            content.plusAssign(modIcon);
        }

        String infoStr = beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText() + " [" + beatmapInfo.getVersion() + "]";

        String mapperStr = "Beatmap by " + beatmapInfo.getCreator();
        String playerStr = "Played by " + stat.getPlayerName() + " on " +
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date(stat.getTime()));
        playerStr += String.format("  %s(%s)", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE);
        if (mods.contains(ModCustomSpeed.class) ||
            mods.contains(ModDifficultyAdjust.class) ||
            (mods.contains(ModFlashlight.class) && !mods.ofType(ModFlashlight.class).getUsesDefaultSettings())) {

            var customSpeed = mods.ofType(ModCustomSpeed.class);
            var difficultyAdjust = mods.ofType(ModDifficultyAdjust.class);
            var flashlight = mods.ofType(ModFlashlight.class);

            mapperStr += " [";
            if (customSpeed != null && customSpeed.getTrackRateMultiplier() != 1f) {
                mapperStr += String.format(Locale.ENGLISH, "%.2fx,", customSpeed.getTrackRateMultiplier());
            }

            if (difficultyAdjust != null) {
                if (difficultyAdjust.getAr() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "AR%.1f,", difficultyAdjust.getAr());
                }

                if (difficultyAdjust.getOd() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "OD%.1f,", difficultyAdjust.getOd());
                }

                if (difficultyAdjust.getCs() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "CS%.1f,", difficultyAdjust.getCs());
                }

                if (difficultyAdjust.getHp() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "HP%.1f,", difficultyAdjust.getHp());
                }
            }

            if (flashlight != null && !flashlight.getUsesDefaultSettings()) {
                if (flashlight.getFollowDelay() != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
                    mapperStr += String.format(Locale.ENGLISH, "FLD%.2f,", flashlight.getFollowDelay());
                }

                if (flashlight.getSizeMultiplier() != ModFlashlight.DEFAULT_SIZE_MULTIPLIER) {
                    mapperStr += String.format(Locale.ENGLISH, "%.2fx,", flashlight.getSizeMultiplier());
                }
            }

            if (mapperStr.endsWith(",")){
                mapperStr = mapperStr.substring(0, mapperStr.length() - 1);
            }

            mapperStr += "]";
        }
        var infoContainer = new UILinearContainer();
        infoContainer.setOrientation(UILinearContainer.Orientation.Vertical);
        infoContainer.setPosition(Utils.toRes(4), Utils.toRes(2));
        infoContainer.plusAssign(createText(infoStr, 28));
        infoContainer.plusAssign(createText(mapperStr, 21));
        infoContainer.plusAssign(createText(playerStr, 21));

        if (Config.isDisplayScoreStatistics() && !currentStatistic.isTeamStatistic()) {

            StringBuilder ppinfo = new StringBuilder();
            Beatmap beatmapData;

            try {
                beatmapData = BeatmapCache.getBeatmap(
                    beatmapInfo,
                    true,
                    switch (Config.getDifficultyAlgorithm()) {
                        case droid -> GameMode.Droid;
                        case standard -> GameMode.Standard;
                    }
                );
            } catch (Exception e) {
                Logger.e("ScoringScene", "Failed to parse beatmap for difficulty calculation", e);
                beatmapData = null;
            }

            if (beatmapData != null) {
                DifficultyAttributes difficultyAttributes = null;
                PerformanceAttributes performanceAttributes = null;

                // Don't try to load online replay
                if (replayPath != null && beatmapToReplay != null && !replayPath.startsWith("https://")) {
                    var replay = new Replay();
                    replay.setObjectCount(beatmapToReplay.getTotalHitObjectCount());
                    replay.setBeatmap(beatmapToReplay.getFullBeatmapsetName(), beatmapToReplay.getFullBeatmapName(), mapMD5);

                    if (replay.load(replayPath, true)) {
                        // If statistics information is not complete, we populate it with information from replay.
                        if (
                            stat.getSliderHeadHits() == -1 ||
                            stat.getSliderTickHits() == -1 ||
                            stat.getSliderRepeatHits() == -1 ||
                            stat.getSliderEndHits() == -1
                        ) {
                            replay.populateStatistics(beatmapData, stat);
                        }

                        var copiedMods = mods;

                        if (mods.contains(ModNightCore.class) && replay.replayVersion <= 3) {
                            copiedMods = mods.deepCopy();
                            copiedMods.removeOfType(ModNightCore.class);
                            copiedMods.put(ModOldNightCore.class);
                        }

                        switch (Config.getDifficultyAlgorithm()) {
                            case droid -> {
                                var playableBeatmap = beatmapData.createDroidPlayableBeatmap(copiedMods.values());

                                difficultyAttributes = BeatmapDifficultyCalculator.calculateDroidDifficultyForReplay(beatmapData, playableBeatmap);

                                performanceAttributes = BeatmapDifficultyCalculator.calculateDroidPerformanceWithReplayStat(
                                    playableBeatmap, (DroidDifficultyAttributes) difficultyAttributes, replay, stat
                                );
                            }

                            case standard -> {
                                var playableBeatmap = beatmapData.createStandardPlayableBeatmap(copiedMods.values());

                                difficultyAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, playableBeatmap);

                                performanceAttributes = BeatmapDifficultyCalculator.calculateStandardPerformanceWithReplayStat(
                                    playableBeatmap, (StandardDifficultyAttributes) difficultyAttributes,
                                    replay.objectData, stat
                                );
                            }
                        }
                    }
                }

                if (difficultyAttributes == null) {
                    difficultyAttributes = switch (Config.getDifficultyAlgorithm()) {
                        case droid -> BeatmapDifficultyCalculator.calculateDroidDifficulty(beatmapData, mods.values());
                        case standard -> BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, mods.values());
                    };

                    performanceAttributes = switch (Config.getDifficultyAlgorithm()) {
                        case droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                            beatmapData, (DroidDifficultyAttributes) difficultyAttributes, stat
                        );

                        case standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                            beatmapData, (StandardDifficultyAttributes) difficultyAttributes, stat
                        );
                    };
                }

                var maxPerformanceAttributes = switch (Config.getDifficultyAlgorithm()) {
                    case droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                        (DroidDifficultyAttributes) difficultyAttributes
                    );

                    case standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                        (StandardDifficultyAttributes) difficultyAttributes
                    );
                };

                ppinfo.append(String.format(
                    Locale.ENGLISH,
                    "%.2f★ | %.2f/%.2f" + (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid ? "dpp" : "pp"),
                    difficultyAttributes.starRating,
                    performanceAttributes.total,
                    maxPerformanceAttributes.total
                ));
            }

            if (stat.getUnstableRate() > 0) {
                if (beatmapData != null) {
                    ppinfo.append("\n");
                }
                ppinfo.append(String.format(Locale.ENGLISH, "Error: %.2fms - %.2fms avg", stat.getNegativeHitError(), stat.getPositiveHitError()));
                ppinfo.append("\n");
                ppinfo.append(String.format(Locale.ENGLISH, "Unstable Rate: %.2f", stat.getUnstableRate()));
            }

            var ppInfo = createText(ppinfo.toString(), 21);
            ppInfo.setBackgroundColor(new Color4(0f, 0f, 0f, 0.5f));
            ppInfo.setOrigin(Anchor.BottomLeft);
            ppInfo.setPosition(Utils.toRes(240), Config.getRES_HEIGHT() - Utils.toRes(2) - paddingButtom);
            content.plusAssign(ppInfo);
        }
        content.plusAssign(infoContainer);

        // In case the scene was reloaded
        if (Multiplayer.isMultiplayer) {
            updateLeaderboard();
        }

        if (wasShown && previousScene != scene) {
            Verktex.withTreeLock(() -> {
                Verktex.popScene();
                Verktex.pushScene(scene);
                return Unit.INSTANCE;
            });
        }

        if (beatmap != null && beatmap.getMD5().equals(mapMD5)) {
            SkinSounds.get("applause").play();

            // Save score
            if (stat.getTotalScoreWithMultiplier() <= 0 || mods.contains(ModAutoplay.class) ||
                    (Multiplayer.isMultiplayer &&
                        ((Multiplayer.room != null && Multiplayer.room.isTeamVersus()) || game.hasFailed))) {
                return;
            }

            int totalNotes = stat.getHit300() + stat.getHit100() + stat.getHit50() + stat.getMisses();

            // Do not save and submit score if note count does not match, since it indicates a corrupted score
            // (potentially from bugging the gameplay by any unnecessary means).
            if (totalNotes != beatmap.getTotalHitObjectCount()) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_corrupted, true);
                return;
            }

            stat.setReplayFilename(FilenameUtils.getName(replayPath));
            stat.setBeatmapMD5(beatmap.getMD5());

            try {
                DatabaseManager.getScoreInfoTable().insertScore(stat.toScoreInfo());
            } catch (Exception e) {
                Logger.e("GameScene", "Failed to save score to database", e);
            }

            // Upload score
            if (!OnlineManager.getInstance().isStayOnline() ||
                    (Multiplayer.isMultiplayer && !Config.isSubmitScoreOnMultiplayer())) {
                return;
            }

            boolean hasUnrankedMod = SmartIterator.wrap(mods.values().iterator()).applyFilter(m -> !m.isRanked()).hasNext();

            if (hasUnrankedMod || Multiplayer.isRemoveSliderLock()) {
                return;
            }

            var sendingPanel = new SendingPanel(OnlineManager.getInstance().getRank(),
                    OnlineManager.getInstance().getScore(), OnlineManager.getInstance().getAccuracy(),
                    OnlineManager.getInstance().getPP());

            content.plusAssign(sendingPanel);

            OnlineScoring.getInstance().sendRecord(beatmap, stat, sendingPanel, replayPath);
        }
    }

    public void updateLeaderboard() {

        if (Multiplayer.finalData != null) {

            if (selector != null) {
                var oldSelector = selector;
                Execution.updateThread(oldSelector::removeSelf);
            }

            selector = new StatisticSelector(Multiplayer.finalData);

            if (scene != null) {
                content.plusAssign(selector);
            }
        }
    }

    public void back() {
        SkinSounds.get("applause").stop();
        Multiplayer.finalData = null;
        currentStatistic = null;

        if (Multiplayer.isMultiplayer) {
            // Preventing NPEs when player gets disconnected while playing
            if (!Multiplayer.isConnected()) {
                leaveAndRun(() -> Verktex.pushScene(new LobbyScene()));
            } else {
                leaveAndRun(() -> {
                    if (Verktex.getScene() != Multiplayer.roomScene) {
                        Multiplayer.roomScene.show();
                    }
                });
            }
            return;
        }

        replayMusic();
        leaveAndRun(() -> {});
        setReplayID(-1);
    }

    public UIScene getScene() {
        return scene;
    }

    public void stopMusic() {
        if (songService != null) {
            songService.stop();
        }
    }

    public void replayMusic() {
        if (songService != null) {
            songService.stop();
//            songService.preLoadWithLoop(game.filePath);
            songService.preLoad(beatmapInfo.getAudioPath());
            songService.play();
        }
    }

    public StatisticV2 getReplayStat() {
        return replayStat;
    }

    public void setReplayStat(StatisticV2 replayStat) {
        this.replayStat = replayStat;
    }

    public int getReplayID() {
        return replayID;
    }

    public void setReplayID(int id) {
        this.replayID = id;
    }
}
