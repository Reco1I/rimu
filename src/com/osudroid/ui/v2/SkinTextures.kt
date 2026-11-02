package com.osudroid.ui.v2

import com.osudroid.ui.ISkinnable
import com.osudroid.ui.skinning.StringSkinData
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIContainer
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import java.io.File

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
        val file = File(Config.getCachePath(), MD5Calculator.getStringMD5(url))

        return Textures[key] ?: Textures.load(key, file.absolutePath, isInternal = false)
    }
}

/**
 * Looks up skin assets through Verktex [Textures], preferring the custom skin folder and falling back to
 * the default skin's assets.
 *
 * Beatmap skins aren't handled here yet, those are still managed by the legacy resource manager.
 */
object SkinTextures {

    private const val KEY_PREFIX = "skin:"

    private val missingTextures = mutableSetOf<String>()


    /**
     * Gets a skin texture by name, loading it if needed.
     */
    operator fun get(name: String): GraphicsTexture.Region? = lookup(name, name)

    /**
     * Gets a skin texture named with a configurable prefix, such as the hit circle font, loading it if needed.
     */
    operator fun get(prefix: StringSkinData, name: String): GraphicsTexture.Region? {
        return lookup("${prefix.currentValue}-$name".replace("\\", ""), "${prefix.defaultValue}-$name")
    }

    /**
     * Unloads all skin textures and notifies the [ISkinnable] components of the current scene. To be
     * called after the skin has changed.
     */
    fun onSkinChanged() {
        Textures.unloadAll { key, _ -> key.startsWith(KEY_PREFIX) }
        missingTextures.clear()
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

    private fun lookup(customName: String, defaultName: String): GraphicsTexture.Region? {
        val key = KEY_PREFIX + customName

        Textures[key]?.let { return it }

        if (key in missingTextures) {
            return null
        }

        val customPath = Config.getSkinPath() + customName + ".png"
        val defaultPath = "gfx/$defaultName.png"

        val region = when {
            Platform.provider.exists(customPath, true) -> Textures.load(key, customPath, isInternal = false)
            Platform.provider.exists(defaultPath, false) -> Textures.load(key, defaultPath, isInternal = true)
            else -> null
        }

        if (region == null) {
            missingTextures += key
        }

        return region
    }
}
