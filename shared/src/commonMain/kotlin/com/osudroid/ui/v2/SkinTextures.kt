package com.osudroid.ui.v2

import com.osudroid.settings.GameFolders
import com.osudroid.ui.ISkinnable
import com.osudroid.ui.skinning.StringSkinData
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.texture.FramesReference
import com.reco1l.verktex.texture.TextureReference
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIContainer
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.jvm.JvmStatic
import okio.Path.Companion.toPath

/**
 * Looks up textures downloaded to the cache folder, such as avatars, through Verktex [Textures].
 */
object CachedTextures {

    /**
     * The [Textures] key of a downloaded file.
     */
    fun keyOf(url: String) = "cache:" + MD5Calculator.getStringMD5(url)

    /**
     * Gets the texture of an already downloaded file, loading it if needed.
     */
    operator fun get(url: String): GraphicsTexture.Region? {
        val key = keyOf(url)
        val file = GameFolders.cachePath.toPath() / MD5Calculator.getStringMD5(url)

        return Textures[key] ?: Textures.load(key, file.toString(), isInternal = false)
    }
}

/**
 * Looks up skin assets through Verktex [Textures], preferring the custom skin folder and falling back to
 * the default skin's assets. Textures of the beatmap skin take priority over those while it's enabled.
 */
object SkinTextures {

    private const val KEY_PREFIX = "skin:"

    private const val BEATMAP_KEY_PREFIX = "beatmap:"

    private val frameNameRegex = Regex("(.+?)-?\\d+")

    private val missingTextures = mutableSetOf<String>()

    // The files of the beatmap skin by texture name, each one is loaded when it's first used.
    private val beatmapFiles = mutableMapOf<String, String>()

    // The names in beatmapFiles whose file is an @2x image.
    private val hdBeatmapFiles = mutableSetOf<String>()

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

    // Textures that are always the default skin's, under another name.
    private val defaultAliases = mapOf("defaultapproachcircle" to "approachcircle")

    private val GAMEPLAY_TEXTURES = arrayOf(
        "hitcircle", "hitcircleoverlay", "approachcircle", "defaultapproachcircle",
        "sliderstartcircle", "sliderstartcircleoverlay", "sliderendcircle", "sliderendcircleoverlay",
        "reversearrow", "sliderscorepoint", "lighting",
        "spinner-background", "spinner-circle", "spinner-metre", "spinner-approachcircle", "spinner-spin",
        "spinner-clear", "spinner-top", "spinner-bottom", "spinner-glow", "spinner-middle", "spinner-middle2",
        "spinner-osu", "cursor", "cursormiddle", "cursortrail",
        "scorebar-bg", "scorebar-marker", "scorebar-ki", "scorebar-kidanger", "scorebar-kidanger2",
        "particle50", "particle100", "particle300"
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
    operator fun get(name: String): GraphicsTexture.Region? = lookup(name, name)

    /**
     * Gets a skin texture named with a configurable prefix, such as the hit circle font, loading it if needed.
     */
    @JvmStatic
    operator fun get(prefix: StringSkinData, name: String): GraphicsTexture.Region? {
        return lookup("${prefix.currentValue}-$name".replace("\\", ""), "${prefix.defaultValue}-$name")
    }

    /**
     * Gets the frames of an animated skin texture, an empty list if it isn't animated.
     */
    @JvmStatic
    fun frames(name: String): List<GraphicsTexture.Region> {
        val frames = mutableListOf<GraphicsTexture.Region>()

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
                    frames += get("$name$delimiter${frames.size}") ?: return frames
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
                frames += get("$name$skinDelimiter${frames.size}") ?: return frames
            }

            return frames
        }

        val delimiter = if (Platform.provider.exists("gfx/$name-0.png", false)) "-" else ""

        while (Platform.provider.exists("gfx/$name$delimiter${frames.size}.png", false)) {
            frames += get("$name$delimiter${frames.size}") ?: return frames
        }

        return frames
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

        val skin = OsuSkin.get()

        for (digit in 0..9) {
            get(skin.hitCirclePrefix, digit.toString())
            get(skin.scorePrefix, digit.toString())
            get(skin.comboPrefix, digit.toString())
        }

        for (name in arrayOf("comma", "dot", "percent", "x")) {
            get(skin.scorePrefix, name)
            get(skin.comboPrefix, name)
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

        if (name == "hitcircle") {
            beatmapFiles.getOrPut("sliderstartcircle") { path }
            beatmapFiles.getOrPut("sliderendcircle") { path }
        }

        if (name == "hitcircleoverlay") {
            beatmapFiles.getOrPut("sliderstartcircleoverlay") { path }
            beatmapFiles.getOrPut("sliderendcircleoverlay") { path }
        }
    }

    /**
     * Releases the textures of the beatmap skin.
     */
    @JvmStatic
    fun clearBeatmapTextures() {
        beatmapFiles.clear()
        hdBeatmapFiles.clear()
        revision++
        Textures.unloadAll { key, _ -> key.startsWith(BEATMAP_KEY_PREFIX) }
    }

    /**
     * Unloads all skin textures and notifies the [ISkinnable] components of the current scene. To be
     * called after the skin has changed.
     */
    fun onSkinChanged() {
        Textures.unloadAll { key, _ -> key.startsWith(KEY_PREFIX) }
        missingTextures.clear()
        revision++
        notifySkinChanged(Verktex.scene)
    }

    private fun notifySkinChanged(component: UIComponent) {
        if (component is ISkinnable) {
            component.onSkinChanged()
        }

        if (component is UIContainer) {
            component.forEach(::notifySkinChanged)
        }
    }

    private fun isKnown(name: String): Boolean {
        return Platform.fileSystem.metadataOrNull(SkinFolders.path.toPath() / "$name.png")?.isRegularFile == true
            || Platform.provider.exists("gfx/$name.png", false)
            || Platform.provider.exists("$name.png", false)
    }

    private fun lookupBeatmap(name: String): GraphicsTexture.Region? {
        if (!BeatmapSkinManager.isSkinEnabled()) {
            return null
        }

        val path = beatmapFiles[name] ?: return null
        val key = BEATMAP_KEY_PREFIX + name
        val density = if (path.substringBeforeLast('.').endsWith(HD_SUFFIX, ignoreCase = true)) 2f else 1f

        return Textures[key] ?: Textures.load(key, path, isInternal = false, density = density)
    }

    private fun lookup(customName: String, defaultName: String): GraphicsTexture.Region? {
        lookupBeatmap(defaultName)?.let { return it }

        val key = KEY_PREFIX + customName

        Textures[key]?.let { return it }

        if (key in missingTextures) {
            return null
        }

        val defaultAlias = defaultAliases[defaultName]

        val region = when {
            defaultAlias != null -> loadDefault(key, defaultAlias)
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

    private fun loadFromSkin(key: String, name: String): GraphicsTexture.Region? {
        val path = SkinFolders.path + name + ".png"

        if (Platform.provider.exists(path, true)) {
            return Textures.load(key, path, isInternal = false)
        }

        val hdPath = SkinFolders.path + name + HD_SUFFIX + ".png"

        if (Platform.provider.exists(hdPath, true)) {
            return Textures.load(key, hdPath, isInternal = false, density = 2f)
        }

        return null
    }

    private fun loadDefault(key: String, name: String): GraphicsTexture.Region? {
        val defaultPath = "gfx/$name.png"

        // Assets that aren't skinnable, such as UI icons, are at the root.
        val assetPath = "$name.png"

        return when {
            Platform.provider.exists(defaultPath, false) -> Textures.load(key, defaultPath, isInternal = true)
            Platform.provider.exists(assetPath, false) -> Textures.load(key, assetPath, isInternal = true)
            else -> null
        }
    }

    /**
     * Whether the skin folder has the texture, at either resolution.
     */
    private fun isInSkin(name: String): Boolean {
        return Platform.provider.exists(SkinFolders.path + name + ".png", true)
            || Platform.provider.exists(SkinFolders.path + name + HD_SUFFIX + ".png", true)
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

    private fun isDefaultSkin() = SkinFolders.path.trimEnd('/') == SkinFolders.topPath.trimEnd('/')
}

/**
 * Refers to the skin texture named [name], see [SkinTextures.get], following it as the skin or the
 * beatmap skin changes. With a [prefix], it's the one named with that configurable prefix.
 */
data class SkinTexture(val name: String, val prefix: StringSkinData? = null) : TextureReference.Lookup() {

    // Both only go up, so their sum changes whenever either does.
    override val revision: Int
        get() = Textures.revision + SkinTextures.revision

    override fun lookup() = if (prefix != null) SkinTextures[prefix, name] else SkinTextures[name]
}

/**
 * Refers to the skin texture named [name], following it as the skin changes.
 */
fun skinTexture(name: String) = SkinTexture(name)

/**
 * Refers to the skin texture named [name] with a configurable [prefix], following it as the skin changes.
 */
fun skinTexture(prefix: StringSkinData, name: String) = SkinTexture(name, prefix)

/**
 * Refers to the frames of the animated skin texture named [name], see [SkinTextures.frames], following
 * them as the skin or the beatmap skin changes. With [orStatic], a skin where it isn't animated gives
 * its single texture as the only frame.
 */
data class SkinFrames(val name: String, val orStatic: Boolean = false) : FramesReference.Lookup() {

    override val revision: Int
        get() = Textures.revision + SkinTextures.revision

    override fun lookup(): List<GraphicsTexture.Region> {
        val frames = SkinTextures.frames(name)

        return if (orStatic && frames.isEmpty()) listOfNotNull(SkinTextures[name]) else frames
    }
}

/**
 * Refers to the frames of the animated skin texture named [name], following them as the skin changes.
 * With [orStatic], a skin where it isn't animated gives its single texture as the only frame.
 */
fun skinFrames(name: String, orStatic: Boolean = false) = SkinFrames(name, orStatic)
