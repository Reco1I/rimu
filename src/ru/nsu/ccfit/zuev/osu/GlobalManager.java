package ru.nsu.ccfit.zuev.osu;

import com.osudroid.data.BeatmapInfo;
import com.osudroid.data.DatabaseManager;
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
        DatabaseManager.load(mainActivity);
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
