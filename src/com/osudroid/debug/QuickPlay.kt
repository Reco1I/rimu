package com.osudroid.debug

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import android.content.Intent
import com.reco1l.verktex.Logger
import com.osudroid.mods.ModAutoplay
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.texture.Textures
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

/**
 * TEMPORARY: starts a beatmap right after the game loads, skipping the menus, to test gameplay
 * without navigating the UI. Only in debug builds. Remove once gameplay on Verktex is stable.
 *
 * ```
 * adb shell am start -S -n ru.nsu.ccfit.zuev.osuplus.debug/ru.nsu.ccfit.zuev.osu.MainActivity \
 *     --es quickplay "abracadabra normal" [--ez autoplay false]
 * ```
 *
 * The [EXTRA_BEATMAP] query matches every word against "artist - title [version]", ignoring case.
 * Empty, it plays the selected beatmap or the first one of the library. Autoplay is on unless
 * [EXTRA_AUTOPLAY] is false.
 */
object QuickPlay {

    const val EXTRA_BEATMAP = "quickplay"
    const val EXTRA_AUTOPLAY = "autoplay"

    private const val TAG = "QuickPlay"


    /**
     * Starts the beatmap asked for by [intent], if any.
     *
     * @return Whether a beatmap was started.
     */
    @JvmStatic
    fun startIfRequested(intent: Intent?): Boolean {
        if (!BuildConfig.DEBUG || intent == null) {
            return false
        }

        val query = intent.getStringExtra(EXTRA_BEATMAP) ?: return false
        // Once only, not again when the activity is recreated.
        intent.removeExtra(EXTRA_BEATMAP)

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
        if (intent.getBooleanExtra(EXTRA_AUTOPLAY, true)) {
            mods.put(ModAutoplay::class)
        }

        Logger.i(TAG, "Playing ${beatmap.artist} - ${beatmap.title} [${beatmap.version}] with ${mods.values}")

        BeatmapSelection.beatmap = beatmap
        // Song select loads it otherwise, the game scene expects it.
        beatmap.backgroundPath?.let { Textures.load("::background", it) }
        OsuGame.createGameScene().startGame(beatmap, null, mods)
        return true
    }
}
