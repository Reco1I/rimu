package com.reco1l.verktex.fonts

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.px

/**
 * Represents a family of fonts, which can be used to retrieve specific [Font] instances based on their
 * size, weight, and style.
 *
 * The [FontFamily] class is responsible for managing the creation and caching of [Font] instances,
 * ensuring that the same font is reused when requested with the same parameters.
 *
 * In Verktex engine font rendering is delegated to the platform, and this class serves as a bridge
 * between the engine and the platform-specific font rendering implementation. e.g., on Android, it
 * uses Android's Typeface API to render fonts.
 *
 * Due to this, fonts could be **rendered differently** on different platforms, and the same font family
 * may look different on Android, iOS, or other platforms.
 */
class FontFamily(

    /**
     * The name of the font family, which is used to identify it and retrieve specific [Font] instances.
     */
    val name: String,
    /**
     * The source responsible for creating [Font] instances based on their descriptors.
     */
    private val source: Source
) {

    private val fonts = mutableMapOf<Font.Descriptor, Font>()


    /**
     * Retrieves a [Font] instance based on the provided size, weight, and style.
     */
    operator fun get(size: Dimension.Fixed, weight: Font.Weight = Font.Weight.Normal, style: Font.Style = Font.Style.Normal): Font {
        // Every font size follows the user's font scale.
        val scaledSize = if (Verktex.fontScale == 1f) size else (size.toPixels() * Verktex.fontScale).px
        return get(Font.Descriptor(this, scaledSize, weight, style))
    }

    /**
     * Retrieves a [Font] instance based on the provided [Font.Descriptor].
     */
    operator fun get(descriptor: Font.Descriptor): Font {
        return fonts.getOrPut(descriptor) { source.onCreateFont(descriptor) }
    }


    /**
     * Disposes of all [Font] instances managed by this [FontFamily], releasing their associated
     * resources, such as textures.
     *
     * After calling this method, the [FontFamily] will no longer be usable, and any references to its fonts should be discarded.
     */
    fun dispose() {
        while (fonts.isNotEmpty()) {
            val font = fonts.values.first()
            fonts.remove(font.descriptor)
            font.dispose()
        }
    }


    /**
     * This interface defines a contract for creating [Font] instances based on a given [Font.Descriptor].
     */
    interface Source {

        /**
         * This function should create a new [Font] instance based on the provided [Font.Descriptor].
         */
        fun onCreateFont(descriptor: Font.Descriptor): Font

    }
}


