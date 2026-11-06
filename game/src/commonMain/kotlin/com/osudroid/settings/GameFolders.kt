package com.osudroid.settings

import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings
import kotlin.jvm.JvmStatic

/**
 * The folders of the game, each a path ending with `/`. The ones the player can choose are stored in
 * [OsuSettings], the others follow them.
 */
object GameFolders {

    /**
     * The game's folder when none was chosen: `osu!droid` in the storage the user sees, or in the
     * private folder of the application without one.
     */
    @JvmStatic
    val defaultCorePath: String
        get() = "${Platform.storage.externalDirectory ?: Platform.storage.dataDirectory}/osu!droid/"

    /**
     * The game's folder, where the others are by default.
     */
    @JvmStatic
    var corePath: String
        get() = folder(Settings[OsuSettings.CorePath]) { defaultCorePath }
        set(value) {
            Settings[OsuSettings.CorePath] = value
        }

    /**
     * The folder with the beatmap sets.
     */
    @JvmStatic
    var beatmapPath: String
        get() = folder(Settings[OsuSettings.BeatmapPath]) { corePath + "Songs/" }
        set(value) {
            Settings[OsuSettings.BeatmapPath] = value
        }

    /**
     * The folder with the replays of the scores.
     */
    @JvmStatic
    val scorePath: String
        get() = corePath + "Scores/"

    /**
     * The folder for files that can be deleted, such as downloaded avatars.
     */
    @JvmStatic
    val cachePath: String
        get() = Platform.storage.cacheDirectory.toString()


    /**
     * [path] with a trailing `/`, or [default] if it's empty.
     */
    internal inline fun folder(path: String, default: () -> String): String {
        val folder = path.ifEmpty(default)
        return if (folder.endsWith('/')) folder else "$folder/"
    }
}
