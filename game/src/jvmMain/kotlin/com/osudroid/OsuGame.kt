package com.osudroid

import com.osudroid.audio.MusicService
import com.osudroid.mods.ModNightCore
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.calculateRate
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.ui.v2.Skin
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SplashScene
import com.reco1l.verktex.Platform
import kotlin.jvm.JvmStatic
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.game.GameScene

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
