package com.osudroid.ui.v2

import com.reco1l.verktex.Logger
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import com.osudroid.settings.GameFolders
import com.osudroid.ui.ISkinnable
import com.osudroid.ui.skinning.SkinFont
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.texture.FramesReference
import com.reco1l.verktex.texture.TextureReference
import com.reco1l.verktex.texture.TextureStore
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.container.Container
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import com.osudroid.ui.skinning.OsuSkin
import kotlin.jvm.JvmStatic
import okio.Path.Companion.toPath

/**
 * Looks up textures downloaded to the cache folder, such as avatars, in their own [store].
 */
object CachedTextures {

    /**
     * The store of the downloaded textures.
     */
    val store: TextureStore
        get() = Textures.store("cache")

    /**
     * The [store] key of a downloaded file.
     */
    fun keyOf(url: String) = MD5Calculator.getStringMD5(url)

    /**
     * Gets the texture of an already downloaded file, loading it if needed.
     */
    operator fun get(url: String): Texture.Region? {
        val key = keyOf(url)
        val file = GameFolders.cachePath.toPath() / MD5Calculator.getStringMD5(url)

        return store[key] ?: store.load(key, file.toString(), isInternal = false)
    }
}

/**
 * Looks up skin assets, preferring the custom skin folder and falling back to the default skin's assets, in the
 * [skinStore]. Textures of the beatmap skin, in the [beatmapStore], take priority over those while it's enabled.
 */
object SkinTextures {
    private const val TAG = "SkinTextures"


    /**
     * The textures of the skin, unloaded all at once when it changes. Their keys are their names.
     */
    val skinStore: TextureStore = Textures.store("skin")

    /**
     * The textures of the beatmap skin, unloaded all at once when the beatmap changes.
     */
    val beatmapStore: TextureStore = Textures.store("beatmap-skin")

    private val frameNameRegex = Regex("(.+?)-?\\d+")

    private val missingTextures = mutableSetOf<String>()

    // The files of the beatmap skin by texture name, each one is loaded when it's first used.
    private val beatmapFiles = mutableMapOf<String, String>()

    // The names in beatmapFiles whose file is an @2x image.
    private val hdBeatmapFiles = mutableSetOf<String>()

    // The names in beatmapFiles that are the hit circle's textures standing in for the slider's.
    private val aliasedBeatmapFiles = mutableSetOf<String>()

    // The overlays of the slider's circles, by the circle they go over.
    private val sliderCircleOverlays = mapOf(
        "sliderstartcircleoverlay" to "sliderstartcircle",
        "sliderendcircleoverlay" to "sliderendcircle"
    )

    private const val HD_SUFFIX = "@2x"

    // What a skin without its own slider head and tail uses, as osu!stable does.
    private val skinFallbacks = mapOf(
        "sliderstartcircle" to "hitcircle",
        "sliderstartcircleoverlay" to "hitcircleoverlay",
        "sliderendcircle" to "hitcircle",
        "sliderendcircleoverlay" to "hitcircleoverlay"
    )

    // Textures a skin other than the default one doesn't show unless it has them.
    private val optionalTextures = setOf("scorebar-marker", "scorebar-ki", "scorebar-kidanger", "scorebar-kidanger2")

    // The slider tail of an osu!stable skin, which isn't shown unless the skin has it.
    private val stableOptionalTextures = setOf("sliderendcircle", "sliderendcircleoverlay")

    private val GAMEPLAY_TEXTURES = arrayOf(
        "hitcircle", "hitcircleoverlay", "approachcircle",
        "sliderstartcircle", "sliderstartcircleoverlay", "sliderendcircle", "sliderendcircleoverlay",
        "reversearrow", "sliderscorepoint", "lighting",
        "spinner-background", "spinner-circle", "spinner-metre", "spinner-approachcircle", "spinner-spin",
        "spinner-clear", "spinner-top", "spinner-bottom", "spinner-glow", "spinner-middle", "spinner-middle2",
        "spinner-osu", "cursor", "cursormiddle", "cursortrail",
        "scorebar-bg", "scorebar-marker", "scorebar-ki", "scorebar-kidanger", "scorebar-kidanger2",
        "particle50", "particle100", "particle300", "sliderpoint10", "sliderpoint30",
        // Shown by breaks and the leaderboard while playing.
        "section-pass", "section-fail", "play-unranked", "play-warningarrow", "back-arrow", "ranking-X-small", "ranking-XH-small", "ranking-S-small", "ranking-SH-small",
        "ranking-A-small", "ranking-B-small", "ranking-C-small", "ranking-D-small"
    )

    private val GAMEPLAY_ANIMATIONS = arrayOf(
        "sliderb", "sliderfollowcircle", "followpoint", "scorebar-colour", "play-skip",
        "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
    )


    /**
     * Goes up whenever a name may look up another texture than before without [Textures] changing,
     * such as when the files of the beatmap skin are registered or it's enabled, see [SkinTexture].
     */
    var revision = 0
        private set

    /**
     * To be called when the beatmap skin is enabled or disabled.
     */
    @JvmStatic
    fun onBeatmapSkinToggled() {
        revision++
    }


    /**
     * Gets a skin texture by name, loading it if needed.
     */
    @JvmStatic
    operator fun get(name: String): Texture.Region? = lookup(name, name)

    /**
     * Gets a texture of a skin font, named with its prefix, loading it if needed.
     */
    @JvmStatic
    operator fun get(font: SkinFont, name: String): Texture.Region? {
        return lookup("${font.prefix}-$name".replace("\\", ""), "${font.defaultPrefix}-$name")
    }

    /**
     * Gets the frames of an animated skin texture, an empty list if it isn't animated.
     */
    @JvmStatic
    fun frames(name: String): List<Texture.Region> {
        // Found once for each skin: finding them reads storage (the skin's files, the game's assets), and they're
        // resolved again whenever any texture is loaded, by every component showing them (dozens of follow points).
        // The loader looks them up while the game updates.
        val names = synchronized(frameNamesLock) {
            if (frameNamesRevision != revision) {
                frameNames.clear()
                frameNamesRevision = revision
            }
            frameNames[name]
        } ?: findFrames(name).map { it.first }.also { synchronized(frameNamesLock) { frameNames[name] = it } }

        val frames = ArrayList<Texture.Region>(names.size)
        for (i in names.indices) {
            frames += get(names[i]) ?: return frames
        }
        return frames
    }

    // The frames of each animation found by findFrames, by name, for the skin of frameNamesRevision.
    private val frameNames = HashMap<String, List<String>>()
    private var frameNamesRevision = -1
    private val frameNamesLock = SynchronizedObject()

    // The frames of the animated texture [name], with the name of each.
    private fun findFrames(name: String): List<Pair<String, Texture.Region>> {
        val frames = mutableListOf<Pair<String, Texture.Region>>()

        // The frames of a beatmap skin replace the whole animation, as its single image does.
        if (BeatmapSkinManager.isSkinEnabled()) {
            val delimiter = when {
                "$name-0" in beatmapFiles -> "-"
                "${name}0" in beatmapFiles -> ""
                name in beatmapFiles -> return frames
                else -> null
            }

            if (delimiter != null) {
                while ("$name$delimiter${frames.size}" in beatmapFiles) {
                    frames += "$name$delimiter${frames.size}".let { frameName -> frameName to (get(frameName) ?: return frames) }
                }

                return frames
            }
        }

        // The same goes for the skin: its animation, or its single image, replaces the default one, and the
        // frames it doesn't have aren't taken from the default skin.
        val skinDelimiter = when {
            isInSkin("$name-0") -> "-"
            isInSkin("${name}0") -> ""
            isInSkin(name) -> return frames
            else -> null
        }

        if (skinDelimiter != null) {
            while (isInSkin("$name$skinDelimiter${frames.size}")) {
                frames += "$name$skinDelimiter${frames.size}".let { frameName -> frameName to (get(frameName) ?: return frames) }
            }

            return frames
        }

        val delimiter = if (assetExists("gfx/$name-0.png")) "-" else ""

        while (assetExists("gfx/$name$delimiter${frames.size}.png")) {
            frames += "$name$delimiter${frames.size}".let { frameName -> frameName to (get(frameName) ?: return frames) }
        }

        return frames
    }

    /**
     * Whether spinners are the new style ones (`spinner-top`, `spinner-middle`...) rather than the old style
     * ones (`spinner-background`, `spinner-metre`...), as osu!stable decides: a skin with a
     * `spinner-background` or made for a version before 2.0 has old style spinners. The default skin has new
     * style ones unless it has a `spinner-background` itself.
     */
    @JvmStatic
    fun isNewStyleSpinner(): Boolean {
        if (BeatmapSkinManager.isSkinEnabled()) {
            if ("spinner-background" in beatmapFiles) {
                return false
            }

            if ("spinner-top" in beatmapFiles) {
                return true
            }
        }

        if (isInSkin("spinner-background")) {
            return false
        }

        return isDefaultSkin() || OsuSkin.current.utils.version >= 2f
    }

    /**
     * Resolves the textures gameplay uses, so they're loaded while the game loads instead of when they're
     * first shown. To be called once the beatmap skin is loaded.
     */
    @JvmStatic
    fun preloadGameplay() {
        for (name in GAMEPLAY_TEXTURES) {
            get(name)
        }

        for (name in GAMEPLAY_ANIMATIONS) {
            if (frames(name).isEmpty()) {
                get(name)
            }
        }

        val skin = OsuSkin.current

        for (digit in 0..9) {
            get(SkinFont.HitCircle, digit.toString())
            get(SkinFont.Score, digit.toString())
            get(SkinFont.Combo, digit.toString())
        }

        for (name in arrayOf("comma", "dot", "percent", "x")) {
            get(SkinFont.Score, name)
            get(SkinFont.Combo, name)
        }
    }

    /**
     * Registers a texture of the beatmap skin. Files that don't replace a known texture are ignored.
     */
    @JvmStatic
    fun loadBeatmapTexture(path: String) {
        revision++
        val fileName = path.toPath().name.substringBeforeLast('.').lowercase()
        val name = fileName.removeSuffix(HD_SUFFIX)

        // An image made at twice the resolution is used only if there isn't one at the normal resolution.
        if (name != fileName) {
            if (name in beatmapFiles && name !in hdBeatmapFiles) {
                return
            }
            hdBeatmapFiles += name
        } else {
            hdBeatmapFiles -= name
        }

        if (!isKnown(name)) {
            // A single image replacing all the frames of an animation.
            val delimiter = when {
                isKnown("$name-0") -> "-"
                isKnown("${name}0") -> ""
                else -> {
                    val baseName = frameNameRegex.matchEntire(name)?.groupValues?.get(1)

                    if (baseName != null && (isKnown(baseName) || isKnown("$baseName-0") || isKnown("${baseName}0"))) {
                        beatmapFiles[name] = path
                    }

                    return
                }
            }

            var frame = 0

            while (isKnown("$name$delimiter$frame")) {
                beatmapFiles.getOrPut("$name$delimiter$frame") { path }
                frame++
            }

            return
        }

        beatmapFiles[name] = path
        aliasedBeatmapFiles -= name

        if (name == "hitcircle" || name == "hitcircleoverlay") {
            for ((sliderName, circleName) in skinFallbacks) {
                if (circleName == name && sliderName !in beatmapFiles) {
                    beatmapFiles[sliderName] = path
                    aliasedBeatmapFiles += sliderName
                }
            }
        }
    }

    /**
     * Releases the textures of the beatmap skin.
     */
    @JvmStatic
    fun clearBeatmapTextures() {
        beatmapFiles.clear()
        aliasedBeatmapFiles.clear()
        hdBeatmapFiles.clear()
        revision++
        beatmapStore.unloadAll()
    }

    /**
     * Unloads all skin textures and notifies the [ISkinnable] components of the current scene. To be
     * called after the skin has changed.
     */
    fun onSkinChanged() {
        skinStore.unloadAll()
        missingTextures.clear()
        revision++
        notifySkinChanged(Verktex.scene)
    }

    private fun notifySkinChanged(component: Component) {
        if (component is ISkinnable) {
            component.onSkinChanged()
        }

        if (component is Container) {
            component.forEach(::notifySkinChanged)
        }
    }

    private fun isKnown(name: String): Boolean {
        return isSkinFile("$name.png")
            || assetExists("gfx/$name.png")
            || assetExists("$name.png")
    }

    private fun lookupBeatmap(name: String): Texture.Region? {
        if (!BeatmapSkinManager.isSkinEnabled()) {
            return null
        }

        val path = beatmapFiles[name] ?: return null
        val density = if (path.substringBeforeLast('.').endsWith(HD_SUFFIX, ignoreCase = true)) 2f else 1f

        return beatmapStore[name] ?: run {
            onLoad("beatmap-skin:$name")
            beatmapStore.load(name, path, isInternal = false, density = density)
        }
    }

    private var isTrackingPlay = false

    /**
     * The textures loaded since a play started, see [startPlayTracking]: each one is read from storage on the
     * update thread, a stall, so they should be preloaded ([preloadGameplay]).
     */
    var loadsWhilePlaying = 0
        private set

    /**
     * Counts and logs the textures loaded from now on, until [stopPlayTracking], as a play starts.
     */
    fun startPlayTracking() {
        isTrackingPlay = true
        loadsWhilePlaying = 0
    }

    fun stopPlayTracking() {
        isTrackingPlay = false
    }

    private fun onLoad(key: String) {
        if (isTrackingPlay) {
            loadsWhilePlaying++
            Logger.w(TAG, "Texture loaded while playing: $key")
        }
    }

    private fun lookup(customName: String, defaultName: String): Texture.Region? {
        if (isHiddenSliderOverlay(defaultName)) {
            return null
        }

        lookupBeatmap(defaultName)?.let { return it }

        val key = customName

        skinStore[key]?.let { return it }

        if (key in missingTextures) {
            return null
        }

        onLoad(key)

        val region = when {
            isOptional(customName) -> null
            else -> loadFromSkin(key, customName)
                ?: skinFallbacks[customName]?.let { loadFromSkin(key, it) }
                ?: loadDefault(key, defaultName)
        }

        if (region == null) {
            missingTextures += key
        }

        return region
    }

    private fun loadFromSkin(key: String, name: String): Texture.Region? {
        skinFile("$name.png")?.let { path ->
            return skinStore.load(key, path, isInternal = false)
        }

        val hdPath = skinFile("$name$HD_SUFFIX.png")

        if (hdPath != null) {
            return skinStore.load(key, hdPath, isInternal = false, density = 2f)
        }

        return null
    }

    private fun loadDefault(key: String, name: String): Texture.Region? {
        val defaultPath = "gfx/$name.png"

        // Assets that aren't skinnable, such as UI icons, are at the root.
        val assetPath = "$name.png"

        return when {
            assetExists(defaultPath) -> skinStore.load(key, defaultPath, isInternal = true)
            assetExists(assetPath) -> skinStore.load(key, assetPath, isInternal = true)
            else -> null
        }
    }

    /**
     * Whether the skin folder has the texture, at either resolution.
     */
    private fun isInSkin(name: String): Boolean {
        return isSkinFile("$name.png") || isSkinFile("$name$HD_SUFFIX.png")
    }

    // The files of each folder of the skin (its root is ""), listed once for each skin: checking for each file reads
    // the storage, slow on Android's shared storage, and a skin's animations have hundreds of frames. By their name in
    // lower case, which they're looked up by, as osu!stable and Android's shared storage do. Fonts can be in a
    // subfolder (HitCirclePrefix: fonts/default).
    private val skinFiles = HashMap<String, Map<String, String>>()
    private var skinFilesFolder: String? = null
    private var skinFilesRevision = -1
    private val skinFilesLock = SynchronizedObject()

    private fun isSkinFile(fileName: String) = skinFile(fileName) != null

    // The path of the file of the skin named [fileName], relative to its folder, whatever its case: null if it has
    // none.
    private fun skinFile(fileName: String): String? {
        val folder = SkinFolders.path
        val separator = fileName.lastIndexOf('/')
        val directory = if (separator >= 0) fileName.substring(0, separator) else ""
        val name = fileName.substring(separator + 1)

        val files = synchronized(skinFilesLock) {
            if (skinFilesFolder != folder || skinFilesRevision != revision) {
                skinFiles.clear()
                skinFilesFolder = folder
                skinFilesRevision = revision
            }
            skinFiles[directory.lowercase()]
        } ?: run {
            val path = if (directory.isEmpty()) folder.toPath() else folder.toPath() / directory
            val listed = Platform.fileSystem.listOrNull(path)?.associate { it.name.lowercase() to it.name } ?: emptyMap()

            synchronized(skinFilesLock) { skinFiles[directory.lowercase()] = listed }
            listed
        }

        val actual = files[name.lowercase()] ?: return null
        return if (directory.isEmpty()) folder + actual else "$folder$directory/$actual"
    }

    // Whether the game has the asset [path]: they never change, and a missing one is costly to tell on Android.
    private val assetsFound = HashMap<String, Boolean>()

    private fun assetExists(path: String): Boolean {
        synchronized(skinFilesLock) { assetsFound[path] }?.let { return it }

        val exists = Platform.provider.exists(path, false)
        synchronized(skinFilesLock) { assetsFound[path] = exists }
        return exists
    }

    /**
     * Whether a texture the skin doesn't have isn't shown, instead of being taken from the default skin.
     */
    private fun isOptional(name: String): Boolean {
        if (isDefaultSkin() || isInSkin(name)) {
            return false
        }

        if (name in optionalTextures) {
            return true
        }

        // Only for osu!stable skins, those configured by a skin.ini alone.
        if (name in stableOptionalTextures) {
            val folder = SkinFolders.path.toPath()
            val fileSystem = Platform.fileSystem
            return fileSystem.exists(folder / "skin.ini") && !fileSystem.exists(folder / "skin.json")
        }

        return false
    }

    /**
     * Whether the overlay of a slider's circle isn't shown: a skin with the slider's circle but not its
     * overlay shows none, the hit circle's overlay is only used when the slider's circle isn't skinned either.
     */
    private fun isHiddenSliderOverlay(name: String): Boolean {
        val circleName = sliderCircleOverlays[name] ?: return false

        if (BeatmapSkinManager.isSkinEnabled() && circleName in beatmapFiles && circleName !in aliasedBeatmapFiles) {
            return name !in beatmapFiles || name in aliasedBeatmapFiles
        }

        return isInSkin(circleName) && !isInSkin(name)
    }

    /**
     * Whether the skin in use, or the beatmap skin while it's enabled, has its own texture with the name,
     * rather than the default skin's.
     */
    @JvmStatic
    fun isSkinned(name: String): Boolean {
        if (BeatmapSkinManager.isSkinEnabled() && name in beatmapFiles && name !in aliasedBeatmapFiles) {
            return true
        }

        return isInSkin(name)
    }

    /**
     * Whether the skin in use is the default skin: no skin folder is chosen.
     */
    fun isDefaultSkin() = SkinFolders.path.trimEnd('/') == SkinFolders.topPath.trimEnd('/')
}

/**
 * Refers to the skin texture named [name], see [SkinTextures.get], following it as the skin or the
 * beatmap skin changes. With a [font], it's that font's texture named so.
 */
data class SkinTexture(val name: String, val font: SkinFont? = null) : TextureReference.Lookup() {

    // Both only go up, so their sum changes whenever either does.
    override val revision: Int
        get() = Textures.revision + SkinTextures.revision

    override fun lookup() = if (font != null) SkinTextures[font, name] else SkinTextures[name]
}

/**
 * Refers to the skin texture named [name], following it as the skin changes.
 */
fun skinTexture(name: String) = SkinTexture(name)

/**
 * Refers to the texture named [name] of the skin font [font], following it as the skin changes.
 */
fun skinTexture(font: SkinFont, name: String) = SkinTexture(name, font)

/**
 * Refers to the frames of the animated skin texture named [name], see [SkinTextures.frames], following
 * them as the skin or the beatmap skin changes. With [orStatic], a skin where it isn't animated gives
 * its single texture as the only frame.
 */
data class SkinFrames(val name: String, val orStatic: Boolean = false) : FramesReference.Lookup() {

    override val revision: Int
        get() = Textures.revision + SkinTextures.revision

    override fun lookup(): List<Texture.Region> {
        val frames = SkinTextures.frames(name)

        return if (orStatic && frames.isEmpty()) listOfNotNull(SkinTextures[name]) else frames
    }
}

/**
 * Refers to the frames of the animated skin texture named [name], following them as the skin changes.
 * With [orStatic], a skin where it isn't animated gives its single texture as the only frame.
 */
fun skinFrames(name: String, orStatic: Boolean = false) = SkinFrames(name, orStatic)
