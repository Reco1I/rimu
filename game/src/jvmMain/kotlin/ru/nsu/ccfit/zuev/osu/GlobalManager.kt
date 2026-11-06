package ru.nsu.ccfit.zuev.osu

import com.osudroid.data.BeatmapInfo
import com.osudroid.ui.v2.Skin
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.audio.MusicService
import com.reco1l.verktex.Platform
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene

/**
 * Created by Fuuko on 2015/4/24.
 */
class GlobalManager {
    lateinit var gameScene: GameScene
    lateinit var scoring: ScoringScene
    var loadingProgress = 0
    var info: String? = null

    /**
     * The music of the game. Replaced by the UI test browser with one that plays nothing.
     */
    var music = MusicService().apply { playlist = LibraryManager }
        set(value) {
            value.playlist = LibraryManager
            field = value
        }

    var selectedBeatmap: BeatmapInfo? = null

    /**
     * The [gameScene], or null before [init] created it.
     */
    val gameSceneOrNull: GameScene?
        get() = if (::gameScene.isInitialized) gameScene else null

    /**
     * The [scoring] scene, or null before [init] created it.
     */
    val scoringOrNull: ScoringScene?
        get() = if (::scoring.isInitialized) scoring else null

    /**
     * Whether the game runs without its host, e.g. scenes shown in the UI test browser. Anything that
     * needs the host itself (opening other activities, finishing it, fragments) must be skipped then.
     * The platform clears it once the game's host is up.
     */
    var isHeadless = true

    /**
     * Loads the skin and creates the gameplay scenes. The platform opens the database before.
     */
    fun init() {
        Platform.audioDriver.enumerateDevices()
        loadingProgress = 10
        info = "Loading skin..."
        Skin.load(Config.getSkinPath())
        SkinSounds.preload()
        loadingProgress = 30
        gameScene = GameScene()
        loadingProgress = 40
        scoring = ScoringScene(gameScene)
        gameScene.setScoringScene(scoring)
        music.stop()
    }

    companion object {
        private var instance: GlobalManager? = null

        @JvmStatic
        fun getInstance(): GlobalManager {
            if (instance == null) {
                instance = GlobalManager()
            }
            return instance!!
        }
    }
}
