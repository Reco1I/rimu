package ru.nsu.ccfit.zuev.osu;

import android.content.Context;

import com.osudroid.data.BeatmapInfo;
import com.osudroid.data.AndroidDatabase;
import com.osudroid.ui.v2.Skin;
import com.osudroid.ui.v2.SkinSounds;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class GlobalManager {
    private static GlobalManager instance;
    private GameScene gameScene;
    private ScoringScene scoring;
    private MainActivity mainActivity;
    private int loadingProgress;
    private String info;
    private SongService songService;
    private BeatmapInfo selectedBeatmap;
    private SaveServiceObject saveServiceObject;
    private Context context;

    public static GlobalManager getInstance() {
        if (instance == null) {
            instance = new GlobalManager();
        }
        return instance;
    }

    public BeatmapInfo getSelectedBeatmap() {
        return selectedBeatmap;
    }

    public void setSelectedBeatmap(BeatmapInfo selectedBeatmap) {
        this.selectedBeatmap = selectedBeatmap;
    }

    public void init() {
        AndroidDatabase.load(mainActivity);
        saveServiceObject = (SaveServiceObject) mainActivity.getApplication();
        songService = saveServiceObject.getSongService();
        SongService.initAudio();
        setLoadingProgress(10);
        setInfo("Loading skin...");
        Skin.load(Config.getSkinPath());
        SkinSounds.preload();
        setLoadingProgress(30);
        setGameScene(new GameScene());
        setLoadingProgress(40);
        setScoring(new ScoringScene(getGameScene()));
        getGameScene().setScoringScene(getScoring());
        if (songService != null) {
            songService.stop();
        }
    }

    public GameScene getGameScene() {
        return gameScene;
    }

    public void setGameScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    public ScoringScene getScoring() {
        return scoring;
    }

    public void setScoring(ScoringScene scoring) {
        this.scoring = scoring;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /**
     * The context to reach resources, assets and preferences with. It's the main activity, or the
     * context set with {@link #setContext(Context)} while the game runs without it.
     */
    public Context getContext() {
        return mainActivity != null ? mainActivity : context;
    }

    /**
     * Sets the context used while the game runs without the main activity, see {@link #isHeadless()}.
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Whether the game runs without the main activity, e.g. scenes shown in the UI test browser.
     * Anything that needs the activity itself (opening other activities, finishing it, fragments)
     * must be skipped then.
     */
    public boolean isHeadless() {
        return mainActivity == null;
    }

    public int getLoadingProgress() {
        return loadingProgress;
    }

    public void setLoadingProgress(int loadingProgress) {
        this.loadingProgress = loadingProgress;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public SongService getSongService() {
        return songService;
    }

    public void setSongService(SongService songService) {
        this.songService = songService;
    }

    public SaveServiceObject getSaveServiceObject() {
        return saveServiceObject;
    }

    public void setSaveServiceObject(SaveServiceObject saveServiceObject) {
        this.saveServiceObject = saveServiceObject;
    }

}
