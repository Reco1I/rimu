package com.osudroid.desktop

import com.osudroid.OsuPlatform
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.ui.v2.SplashScene
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.settings.RESOLUTION_SCALE_KEY
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.async
import com.osudroid.utils.delayed
import com.reco1l.framework.net.Http
import com.reco1l.framework.net.OkHttpClientImplementation
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.DesktopGameHost
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import java.io.File

/**
 * The game's window on desktop, which starts the game as `MainActivity` does on Android.
 */
class OsuDesktop : DesktopGameHost(title = "osu!droid", vsync = true) {

    override fun onEngineInitialized() {
        OsuPlatform.implementation = DesktopOsuPlatform()
        Http.implementation = OkHttpClientImplementation()
        Config.loadConfig()
        StringTable.implementation = DesktopStringTable()

        // There are no toasts on desktop yet, their messages go to the log.
        ToastLogger.init { message, _ -> Logger.i("Toast", message) }

        createGameDirectories()
        OnlineManager.getInstance().init()
        DesktopDatabase.load()

        GlobalManager.getInstance().songService = SongService().apply { onBind() }
        GlobalManager.getInstance().isHeadless = false
    }

    override fun onEngineStarted() {
        Verktex.pushScene(SplashScene)

        async {
            GlobalManager.getInstance().init()

            // The "Show FPS" setting shows the engine's performance overlay.
            Settings[VerktexSettings.ShowPerformanceOverlay] = Config.isShowFPS()

            // The resolution scale setting is stored as a percentage.
            Settings[VerktexSettings.ResolutionScale] = Config.getInt(RESOLUTION_SCALE_KEY, 100) / 100f

            GlobalManager.getInstance().loadingProgress = 50
            Config.loadSkins()
            DifficultyCalculationManager.checkForOutdatedStarRatings()
            OsuPlatform.implementation.loadBeatmapLibrary()

            SplashScene.playWelcomeAnimation()

            delayed(2500) {
                GlobalManager.getInstance().info = ""
                GlobalManager.getInstance().loadingProgress = 100
                Verktex.pushScene(MainScene)
                SongSelect.loadBeatmaps()
            }
        }
    }

    private fun createGameDirectories() {
        File(Config.getBeatmapPath()).mkdirs()
        File(Config.getSkinTopPath()).mkdirs()
    }
}

fun main() {
    OsuDesktop().run()
}
