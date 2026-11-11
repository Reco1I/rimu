package com.osudroid.resources

import com.reco1l.verktex.Platform
import com.reco1l.verktex.localization.Localization

/**
 * The languages the game is translated to, from the language pack.
 */
object Languages {

    /**
     * The value of [com.osudroid.settings.OsuSettings.Language] that follows the language of the system.
     */
    const val SYSTEM = "system"

    /**
     * The languages to choose from, by locale tag, with their names in themselves.
     */
    val names = linkedMapOf(
        "en" to "English",
        "de" to "Deutsch",
        "es" to "Español",
        "fr" to "Français",
        "id" to "Bahasa Indonesia",
        "it" to "Italiano",
        "ja" to "日本語",
        "ko" to "한국어",
        "nl" to "Nederlands",
        // As Android names it, which the setting kept before.
        "pt-rBR" to "Português (Brasil)",
        "ru" to "Русский",
        "th" to "ไทย",
        "vi" to "Tiếng Việt",
        "zh" to "中文 (简体)",
    )


    /**
     * Shows the game in the language of [tag], or the system's one for [SYSTEM] or an empty tag.
     */
    fun apply(tag: String) {
        Localization.locale = if (tag == SYSTEM || tag.isBlank()) Platform.systemLocale else tag
    }
}
