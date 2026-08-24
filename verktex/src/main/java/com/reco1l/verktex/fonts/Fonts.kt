package com.reco1l.verktex.fonts

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex

/**
 * The [Fonts] class is responsible for managing fonts and font families in the application.
 * It provides functionality to load, unload, and retrieve fonts and their associated families.
 */
object Fonts {

    private val fontFamilies = mutableMapOf<String, FontFamily>()


    //region Fonts

    /**
     * This function will load a font family from the specified file path.
     *
     * @param key A unique key to associate with the loaded font family.
     * @param path The file path of the font to load.
     * @param isInternal A boolean indicating whether the file path is internal to the application.
     * @return A [FontFamily] representing the loaded font family.
     */
    fun load(key: String, path: String, isInternal: Boolean = false): FontFamily? {

        val existingFontFamily = fontFamilies[key]
        if (existingFontFamily != null) {
            Logger.w("FontManager", "Font with key '$key' already exists...")
            return existingFontFamily
        }

        val fontSource = Platform.provider.createFontFamilySource(path, !isInternal)
            ?: run {
                Logger.w("FontManager", "Failed to create font source for path: $path")
                return null
            }

        val fontFamily = FontFamily(key, fontSource)
        fontFamilies[key] = fontFamily
        return fontFamily
    }

    /**
     * Unloads a font family associated with the specified key from the font manager.
     *
     * @param key The key associated with the font family to unload.
     */
    fun unload(key: String) {
        val fontFamily = fontFamilies.remove(key)
        if (fontFamily == null) {
            Logger.w(
                "FontManager",
                "Font with key '$key' not found in store. Cannot unload."
            )
            return
        }

        Logger.i("FontManager", "Unloading font with key '$key'...")
        fontFamily.dispose()
    }


    operator fun contains(key: String): Boolean {
        return fontFamilies.containsKey(key)
    }

    operator fun get(key: String): FontFamily {
        return fontFamilies[key] ?: throw IllegalArgumentException("Font with key '$key' not found in store.")
    }

    //endregion

}