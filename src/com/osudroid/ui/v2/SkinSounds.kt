package com.osudroid.ui.v2

import java.io.File
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager

/**
 * Looks up skin sounds, preferring the custom skin folder and falling back to the default skin's assets. Sounds of
 * the beatmap skin take priority over those when requested through [getBeatmapOrNull] or [getBeatmap].
 */
object SkinSounds {

    private val extensions = arrayOf("wav", "mp3", "ogg")

    private val sounds = HashMap<String, BassSoundProvider>()
    private val defaultSounds = HashMap<String, BassSoundProvider>()
    private val beatmapSounds = HashMap<String, BassSoundProvider>()
    private val missingSounds = HashSet<String>()

    private val beatmapFrameRegex = Regex("[^\\d.]+")


    /**
     * Gets a skin sound by name, a silent one if it doesn't exist.
     */
    @JvmStatic
    operator fun get(name: String) = getOrNull(name) ?: BassSoundProvider.EMPTY

    /**
     * Gets a skin sound by name, null if it doesn't exist.
     */
    @JvmStatic
    @Synchronized
    fun getOrNull(name: String): BassSoundProvider? {
        sounds[name]?.let { return it }

        if (name in missingSounds) {
            return null
        }

        val sound = loadFromSkin(name) ?: loadFromAssets(name)

        if (sound == null) {
            missingSounds += name
        } else {
            sounds[name] = sound
        }

        return sound
    }

    /**
     * Gets a sound of the default skin, which a custom skin can't replace or silence.
     */
    @JvmStatic
    @Synchronized
    fun getDefault(name: String): BassSoundProvider {
        return defaultSounds.getOrPut(name) { loadFromAssets(name) ?: BassSoundProvider.EMPTY }
    }

    /**
     * Gets a sound preferring the one of the beatmap skin, null if it doesn't exist.
     */
    @JvmStatic
    @Synchronized
    fun getBeatmapOrNull(name: String): BassSoundProvider? {
        if (BeatmapSkinManager.isSkinEnabled()) {
            beatmapSounds[name]?.let { return it }
        }

        return getOrNull(name)
    }

    /**
     * Gets a sound of a sample set preferring the one of the beatmap skin, null if it doesn't exist.
     */
    @JvmStatic
    fun getBeatmap(name: String, set: Int) = getBeatmapOrNull(if (set >= 2) name + set else name) ?: getOrNull(name)

    /**
     * Loads all the sounds of the current skin, so that they don't have to be loaded when they're first played.
     */
    @JvmStatic
    fun preload() {
        val assets = GlobalManager.getInstance().mainActivity.assets.list("sfx") ?: return

        for (asset in assets) {
            getOrNull(asset.substringBeforeLast('.'))
        }

        getOrNull("comboburst")

        for (i in 0 until 10) {
            getOrNull("comboburst-$i")
        }
    }

    /**
     * Releases all the sounds of the skin and loads the ones of the current skin. To be called after the skin
     * has changed.
     */
    @JvmStatic
    fun onSkinChanged() {
        synchronized(this) {
            sounds.values.forEach { it.free() }
            sounds.clear()
            missingSounds.clear()
        }

        preload()
    }

    /**
     * Loads a sound of the beatmap skin. Files that aren't a variation of a known sound are ignored.
     */
    @JvmStatic
    @Synchronized
    fun loadBeatmapSound(file: File) {
        val name = file.name.substringBeforeLast('.')

        if (name.isEmpty()) {
            return
        }

        val baseName = beatmapFrameRegex.find(name)?.value

        if (baseName != null && getOrNull(baseName) == null) {
            return
        }

        val sound = BassSoundProvider()

        if (sound.prepare(file.path)) {
            beatmapSounds[name] = sound
        }
    }

    /**
     * Releases the sounds of the beatmap skin.
     */
    @JvmStatic
    @Synchronized
    fun clearBeatmapSounds() {
        beatmapSounds.values.forEach { it.free() }
        beatmapSounds.clear()
    }


    private fun loadFromSkin(name: String): BassSoundProvider? {
        for (extension in extensions) {
            val file = File(Config.getSkinPath(), "$name.$extension")

            if (file.isFile && file.length() > 0) {
                val sound = BassSoundProvider()

                if (sound.prepare(file.path)) {
                    return sound
                }
            }
        }

        return null
    }

    private fun loadFromAssets(name: String): BassSoundProvider? {
        for (extension in extensions) {
            val sound = BassSoundProvider()

            if (sound.prepareAsset("sfx/$name.$extension")) {
                return sound
            }
        }

        return null
    }
}
