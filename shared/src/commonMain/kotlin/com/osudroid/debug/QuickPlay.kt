package com.osudroid.debug

import ru.nsu.ccfit.zuev.osu.game.GameScene
import com.osudroid.BeatmapSelection
import com.osudroid.mods.ModAutoplay
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.texture.Textures
import kotlin.jvm.JvmStatic
import ru.nsu.ccfit.zuev.osu.LibraryManager

/**
 * TEMPORARY: starts a beatmap right after the game loads, skipping the menus, to test gameplay
 * without navigating the UI. Only in debug builds. Remove once gameplay on Verktex is stable.
 *
 * Each host passes it what it was started with:
 *
 * ```
 * adb shell am start -S -n ru.nsu.ccfit.zuev.osuplus.debug/ru.nsu.ccfit.zuev.osu.MainActivity \
 *     --es quickplay "abracadabra normal" [--ez autoplay false]
 *
 * ./gradlew :desktopApp:run --args="--quickplay 'abracadabra normal' [--no-autoplay]"
 * ```
 */
object QuickPlay {

    private const val TAG = "QuickPlay"


    /**
     * Starts the beatmap that matches [query]: every word of it against "artist - title [version]",
     * ignoring case. Empty, it plays the selected beatmap or the first one of the library.
     *
     * @param autoplay Whether it's played with autoplay.
     * @return Whether a beatmap was started.
     */
    @JvmStatic
    fun start(query: String, autoplay: Boolean): Boolean {
        if (!Platform.application.isDebug) {
            return false
        }

        val beatmaps = LibraryManager.getLibrary().flatMap { it.beatmaps }
        val words = query.lowercase().split(' ').filter { it.isNotBlank() }

        val beatmap = if (words.isEmpty()) {
            BeatmapSelection.beatmap ?: beatmaps.firstOrNull()
        } else {
            beatmaps.firstOrNull { beatmap ->
                val name = "${beatmap.artist} - ${beatmap.title} [${beatmap.version}]".lowercase()
                words.all { it in name }
            }
        }

        if (beatmap == null) {
            Logger.w(TAG, "No beatmap matches \"$query\"")
            return false
        }

        val mods = ModHashMap()
        if (autoplay) {
            mods.put(ModAutoplay::class)
        }

        Logger.i(TAG, "Playing ${beatmap.artist} - ${beatmap.title} [${beatmap.version}] with ${mods.values}")

        BeatmapSelection.beatmap = beatmap
        // Song select loads it otherwise, the game scene expects it.
        beatmap.backgroundPath?.let { Textures.load("::background", it) }
        GameScene().startGame(beatmap, null, mods)
        return true
    }
}
