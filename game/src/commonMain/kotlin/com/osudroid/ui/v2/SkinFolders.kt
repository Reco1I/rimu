package com.osudroid.ui.v2

import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings
import kotlin.jvm.JvmStatic
import okio.Path.Companion.toPath

/**
 * The folders skins are in: the one in use, and the one with all of them.
 */
object SkinFolders {

    /**
     * The folder of the skin in use.
     */
    @JvmStatic
    var path: String
        get() = GameFolders.folder(Settings[OsuSettings.SkinPath]) { GameFolders.corePath + "Skin/" }
        set(value) {
            Settings[OsuSettings.SkinPath] = value
        }

    /**
     * The folder with the skins to choose from.
     */
    @JvmStatic
    var topPath: String
        get() = GameFolders.folder(Settings[OsuSettings.SkinTopPath]) { path }
        set(value) {
            Settings[OsuSettings.SkinTopPath] = value
        }

    /**
     * The skins found by [load], by the name of their folder.
     */
    @JvmStatic
    val skins: Map<String, String>
        get() = folders

    private var folders = mapOf<String, String>()


    /**
     * Finds the skins in [topPath].
     */
    @JvmStatic
    fun load() {
        val folders = (Platform.fileSystem.listOrNull(topPath.toPath()) ?: emptyList()).filter {
            Platform.fileSystem.metadataOrNull(it)?.isDirectory == true && !it.name.startsWith(".")
        }
        this.folders = folders.associate { it.name to it.toString() }
    }

    /**
     * Adds a skin, e.g. one just imported.
     */
    @JvmStatic
    fun add(name: String, path: String) {
        folders = folders + (name to path)
    }
}
