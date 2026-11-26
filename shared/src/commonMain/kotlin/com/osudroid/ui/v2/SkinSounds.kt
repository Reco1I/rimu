package com.osudroid.ui.v2

import com.reco1l.verktex.Platform
import com.reco1l.verktex.audio.SoundStore
import com.reco1l.verktex.audio.Sounds
import kotlin.jvm.JvmStatic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
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

    private val lock = SynchronizedObject()

    // Where the samples are loaded: the skin's (or the default ones standing in for them) are released together
    // when the skin changes, the beatmap skin's when the beatmap does.
    private val skinStore = Sounds.store("skin")
    private val beatmapStore = Sounds.store("beatmap-skin")
    private val defaultStore = Sounds.store("default-skin")


    /**
     * Gets a skin sound by name, a silent one if it doesn't exist.
     */
    @JvmStatic
    operator fun get(name: String) = getOrNull(name) ?: BassSoundProvider.EMPTY

    /**
     * Gets a skin sound by name, null if it doesn't exist.
     */
    @JvmStatic
    fun getOrNull(name: String): BassSoundProvider? {
        return synchronized(lock) {
            sounds[name]?.let { return it }

            if (name in missingSounds) {
                return null
            }

            val sound = loadFromSkin(name) ?: loadFromAssets(name, skinStore)

            if (sound == null) {
                missingSounds += name
            } else {
                sounds[name] = sound
            }

            return sound
        }
    }

    /**
     * Gets a sound of the default skin, which a custom skin can't replace or silence.
     */
    @JvmStatic
    fun getDefault(name: String): BassSoundProvider {
        return synchronized(lock) {
            return defaultSounds.getOrPut(name) { loadFromAssets(name, defaultStore) ?: BassSoundProvider.EMPTY }
        }
    }

    /**
     * Gets a sound preferring the one of the beatmap skin, null if it doesn't exist.
     */
    @JvmStatic
    fun getBeatmapOrNull(name: String): BassSoundProvider? {
        return synchronized(lock) {
            if (BeatmapSkinManager.isSkinEnabled()) {
                beatmapSounds[name]?.let { return it }
            }

            return getOrNull(name)
        }
    }

    /**
     * Gets a sound of a sample set preferring the one of the beatmap skin, null if it doesn't exist.
     */
    @JvmStatic
    fun getBeatmap(name: String, set: Int) = getBeatmapOrNull(if (set >= 2) name + set else name) ?: getOrNull(name)

    // What gameplay plays, loaded with the play (preloadGameplay) instead of when first played, which would stall a
    // frame of the play. Menus load theirs when they're first played.
    private val gameplaySounds = buildList {
        for (set in arrayOf("normal", "soft", "drum")) {
            for (sound in arrayOf("hitnormal", "hitwhistle", "hitfinish", "hitclap", "slidertick", "sliderslide", "sliderwhistle")) {
                add("$set-$sound")
            }
        }
        addAll(listOf("spinnerspin", "spinnerbonus", "combobreak", "failsound", "sectionpass", "sectionfail", "applause", "comboburst"))
        for (i in 0 until 10) {
            add("comboburst-$i")
        }
    }

    /**
     * Loads the sounds gameplay plays, so that they don't have to be loaded when they're first played. To be called
     * while a play loads, once the beatmap skin is loaded.
     */
    @JvmStatic
    fun preloadGameplay() {
        for (name in gameplaySounds) {
            getOrNull(name)
        }
    }

    /**
     * Releases all the sounds of the skin, those of the new skin are loaded as they're played. To be called after
     * the skin has changed.
     */
    @JvmStatic
    fun onSkinChanged() {
        synchronized(lock) {
            // Whoever still holds one plays nothing rather than a released sample.
            sounds.values.forEach { it.free() }
            sounds.clear()
            missingSounds.clear()
            skinStore.unloadAll()
        }
    }

    /**
     * Loads a sound of the beatmap skin. Files that aren't a variation of a known sound are ignored.
     */
    @JvmStatic
    fun loadBeatmapSound(path: String) {
        synchronized(lock) {
            val name = path.toPath().name.substringBeforeLast('.')

            if (name.isEmpty()) {
                return
            }

            val baseName = beatmapFrameRegex.find(name)?.value

            if (baseName != null && getOrNull(baseName) == null) {
                return
            }

            beatmapStore.loadSampleResource(name, path)?.let { beatmapSounds[name] = BassSoundProvider(it) }
        }
    }

    /**
     * Releases the sounds of the beatmap skin.
     */
    @JvmStatic
    fun clearBeatmapSounds() {
        synchronized(lock) {
            beatmapSounds.values.forEach { it.free() }
            beatmapSounds.clear()
            beatmapStore.unloadAll()
        }
    }


    private fun loadFromSkin(name: String): BassSoundProvider? {
        for (extension in extensions) {
            val file = SkinFolders.path.toPath() / "$name.$extension"
            val metadata = Platform.fileSystem.metadataOrNull(file)

            if (metadata != null && metadata.isRegularFile && (metadata.size ?: 0L) > 0) {
                skinStore.loadSampleResource(name, file.toString())?.let { return BassSoundProvider(it) }
            }
        }

        return null
    }

    private fun loadFromAssets(name: String, store: SoundStore): BassSoundProvider? {
        for (extension in extensions) {
            if (!Platform.provider.exists("sfx/$name.$extension", false)) {
                continue
            }

            store.loadSampleResource(name, "sfx/$name.$extension", isExternal = false)?.let { return BassSoundProvider(it) }
        }

        return null
    }
}
