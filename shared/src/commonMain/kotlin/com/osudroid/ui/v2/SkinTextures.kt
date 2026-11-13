package com.osudroid.ui.v2

import com.osudroid.settings.GameFolders
import com.osudroid.ui.ISkinnable
import com.osudroid.ui.skinning.StringSkinData
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.texture.TextureReference
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIContainer
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
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

        // The frames of a beatmap skin replace the whole animation.
        if (BeatmapSkinManager.isSkinEnabled()) {
            val delimiter = when {
                "$name-0" in beatmapFiles -> "-"
                "${name}0" in beatmapFiles -> ""
                else -> null
            }

            if (delimiter != null) {
                while ("$name$delimiter${frames.size}" in beatmapFiles) {
                    frames += get("$name$delimiter${frames.size}") ?: return frames
                }

                return frames
            }
        }

        val delimiter = if (get("$name-0") != null) "-" else ""

        while (true) {
            frames += get("$name$delimiter${frames.size}") ?: return frames
        }
    }

    /**
     * Registers a texture of the beatmap skin. Files that don't replace a known texture are ignored.
     */
    @JvmStatic
    fun loadBeatmapTexture(path: String) {
        revision++
        val name = path.toPath().name.substringBeforeLast('.').lowercase()

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

        return Textures[key] ?: Textures.load(key, path, isInternal = false)
    }

    private fun lookup(customName: String, defaultName: String): GraphicsTexture.Region? {
        lookupBeatmap(defaultName)?.let { return it }

        val key = KEY_PREFIX + customName

        Textures[key]?.let { return it }

        if (key in missingTextures) {
            return null
        }

        val customPath = SkinFolders.path + customName + ".png"
        val defaultPath = "gfx/$defaultName.png"

        // Assets that aren't skinnable, such as UI icons, are at the root.
        val assetPath = "$defaultName.png"

        val region = when {
            Platform.provider.exists(customPath, true) -> Textures.load(key, customPath, isInternal = false)
            Platform.provider.exists(defaultPath, false) -> Textures.load(key, defaultPath, isInternal = true)
            Platform.provider.exists(assetPath, false) -> Textures.load(key, assetPath, isInternal = true)
            else -> null
        }

        if (region == null) {
            missingTextures += key
        }

        return region
    }
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
