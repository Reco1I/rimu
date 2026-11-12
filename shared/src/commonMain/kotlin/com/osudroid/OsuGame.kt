package com.osudroid

import com.osudroid.beatmaps.LibraryImport
import com.osudroid.audio.MusicService
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.data.DatabaseManager
import com.osudroid.mods.ModNightCore
import com.osudroid.resources.Res
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.async
import com.osudroid.utils.calculateRate
import com.osudroid.utils.delayed
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.ui.v2.Skin
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SplashScene
import com.osudroid.ui.v2.mainmenu.MainScene
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import com.reco1l.verktex.ui.toast.Toasts
import kotlin.jvm.JvmStatic
import okio.IOException
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * The game: its startup and the services every scene shares.
 */
object OsuGame {

    /**
     * The music of the game. Replaced by the UI test browser with one that plays nothing.
     */
    @JvmStatic
    var music = MusicService().apply { playlist = LibraryManager }
        set(value) {
            value.playlist = LibraryManager
            field = value
        }

    /**
     * Whether the game runs without its host, e.g. scenes shown in the UI test browser. Anything that
     * needs the host itself (opening other activities, finishing it, fragments) must be skipped then.
     * The host clears it once it's up.
     */
    @JvmStatic
    var isHeadless = true

    /**
     * The main menu, null until the game has started.
     */
    @JvmStatic
    var mainScene: MainScene? = null
        private set

    /**
     * Creates the [GameScene] of a new play. Replaced by the UI test browser with one that plays
     * nothing.
     */
    @JvmStatic
    var createGameScene: () -> GameScene = ::GameScene


    /**
     * Plays [music] at the speed and pitch of [mods].
     */
    @JvmStatic
    fun applyModsToMusic(mods: ModHashMap) {
        music.speed = mods.values.calculateRate(Double.POSITIVE_INFINITY)
        music.setAdjustPitch(ModNightCore::class in mods)
    }

    /**
     * Starts the game, as each host does once the engine is initialized: loads the settings, shows the
     * splash while the database, the skin and the library load in the background, and then the main
     * menu, telling the host with [OsuGameHost.onGameStarted].
     */
    @JvmStatic
    fun start() {
        val host = OsuGameHost.current

        OsuSettings.load()
        createGameFolders()
        OnlineManager.getInstance().init()
        isHeadless = false

        val splashScene = SplashScene()
        Verktex.pushScene(splashScene)

        async {
            DatabaseManager.open()
            init()

            // The "Show FPS" setting shows the engine's performance overlay.
            Settings[VerktexSettings.ShowPerformanceOverlay] = Settings[OsuSettings.ShowFps]

            // The resolution scale setting is stored as a percentage.
            Settings[VerktexSettings.ResolutionScale] = Settings[OsuSettings.ResolutionScale] / 100f

            SplashScene.progress = 50
            LibraryImport.importSkins()
            SkinFolders.load()
            DifficultyCalculationManager.checkForOutdatedStarRatings()
            LibraryImport.loadLibrary()

            splashScene.playWelcomeAnimation()

            delayed(2500) {
                SplashScene.info = ""
                SplashScene.progress = 100

                // The splash can't be gone back to.
                val menu = MainScene()
                mainScene = menu
                Verktex.resetScenes(menu)

                Platform.forceMaxRefreshRate = Settings[OsuSettings.ForceMaxRefreshRate]

                host.onGameStarted()
            }
        }
    }

    // The beatmaps folder, the default one when the one chosen can't be created, and the skins one.
    private fun createGameFolders() {
        val fileSystem = Platform.fileSystem

        fun create(path: String) = try {
            fileSystem.createDirectories(path.toPath())
            true
        } catch (e: IOException) {
            Logger.e("OsuGame", "Failed to create $path", e)
            false
        }

        val beatmapsExisted = fileSystem.exists(GameFolders.beatmapPath.toPath())

        if (!create(GameFolders.beatmapPath)) {
            GameFolders.beatmapPath = GameFolders.corePath + "Songs/"

            if (!create(GameFolders.beatmapPath)) {
                Toasts.showLong(Res.string.message_error_createdir.format(GameFolders.beatmapPath))
            }
        }

        // Keeps the game's files out of the gallery and the music players on Android.
        if (!beatmapsExisted) {
            try {
                fileSystem.write(GameFolders.corePath.toPath() / ".nomedia") {}
            } catch (e: IOException) {
                Logger.e("OsuGame", "Failed to create the .nomedia file", e)
            }
        }

        create(SkinFolders.topPath)
    }

    /**
     * Loads the skin. The host opens the database before.
     */
    @JvmStatic
    fun init() {
        Platform.audioDriver.enumerateDevices()
        SplashScene.progress = 10
        SplashScene.info = "Loading skin..."
        Skin.load(SkinFolders.path)
        SkinSounds.preload()
        SplashScene.progress = 40
        music.stop()
    }
}
