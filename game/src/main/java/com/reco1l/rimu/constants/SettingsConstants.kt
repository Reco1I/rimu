package com.reco1l.rimu.constants

import com.reco1l.rimu.data.Skin

/**
 * Whenever you need this for develop.
 */
object BuildSettings
{
    /**
     * Disables every possible NSFW feature, for example backgrounds.
     */
    const val SFW_MODE = false
}

enum class RimuSetting(val default: Any)
{

    // UI

    /**
     * The UI scale factor, by default `1.0`.
     */
    UI_SCALE(1f),

    /**
     * The skin key, by default [Skin.BASE].
     */
    UI_SKIN(Skin.BASE),

    /**
     * Determines if we use the beatmap skin instead of the user skin, by default `true`.
     */
    UI_USE_BEATMAP_SKIN(true),


    // Music

    /**
     * The music volume, by default `1.0`
     */
    MUSIC_VOLUME(1f);

}