package com.osudroid.ui.v2.skin

import com.osudroid.ui.v2.skin.legacy.LegacySkin

/**
 * A source of the pieces a [SkinComponent] is drawn with.
 */
interface GameplaySkin {

    /**
     * Whether this skin has its own look for [component]: a texture skin has its textures, the default skin draws it.
     */
    fun provides(component: SkinComponent<*>): Boolean

    /**
     * Creates a piece for [component], null if this skin can't draw it at all. A skin may create pieces it doesn't
     * [provide][provides], from what it falls back to (the default textures of a texture skin).
     */
    fun <T : Any> create(component: SkinComponent<T>): T?
}

/**
 * Finds the piece of each [SkinComponent], component by component as osu!lazer does: from the first of [skins] that
 * [provides][GameplaySkin.provides] it, otherwise from the first that can create it at all. A texture skin missing a
 * component's textures then gets it from the default skin, which is drawn in code.
 */
class SkinPieces(private val skins: List<GameplaySkin>) {

    /**
     * Creates the piece of [component].
     *
     * @throws IllegalStateException If no skin can draw it.
     */
    fun <T : Any> create(component: SkinComponent<T>): T =
        createOrNull(component) ?: error("No skin can draw $component")

    /**
     * Creates the piece of [component], null if no skin can draw it, as the hit lighting of a skin without its
     * texture.
     */
    fun <T : Any> createOrNull(component: SkinComponent<T>): T? {
        for (i in skins.indices) {
            val skin = skins[i]
            if (skin.provides(component)) {
                skin.create(component)?.let { return it }
            }
        }

        for (i in skins.indices) {
            skins[i].create(component)?.let { return it }
        }

        return null
    }

    companion object {

        /**
         * The pieces of the skin in use: the custom or beatmap skin's textures, then the default skin.
         */
        val current = SkinPieces(listOf(LegacySkin, DefaultSkin))
    }
}
