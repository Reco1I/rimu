package com.reco1l.verktex.localization

import kotlin.concurrent.Volatile

/**
 * The language of the text, and the strings in it.
 *
 * Strings are looked up by key in the [sources], for each locale of the [fallbackChain] in turn, so a
 * string missing from a translation shows in a more general locale, then in [DEFAULT_LOCALE]. The
 * built-in source reads `strings/<locale>.json` from the assets, see [JsonStringSource].
 *
 * Text that should follow the language is a [LocalizedString]. Components showing one resolve it
 * again when the [locale] changes.
 */
object Localization {

    /**
     * The locale strings fall back to when the others don't have them.
     */
    const val DEFAULT_LOCALE = "en"


    /**
     * The current locale, as a normalized BCP 47 tag (see [normalize]), e.g. `pt-BR`.
     */
    var locale: String = DEFAULT_LOCALE
        set(value) {
            val normalized = normalize(value)
            if (field == normalized) return

            field = normalized
            fallbackChain = fallbackChainOf(normalized)
            version++

            listeners.values.forEach { it() }
        }

    /**
     * The locales strings are looked up in, from the current one to [DEFAULT_LOCALE], e.g.
     * `pt-BR`, `pt`, `en`.
     */
    @Volatile
    var fallbackChain = fallbackChainOf(DEFAULT_LOCALE)
        private set

    /**
     * Increased whenever the strings could have changed: the locale or the sources. Values resolved
     * with an older version must be resolved again.
     */
    @Volatile
    var version = 0
        private set

    /**
     * The sources strings are looked up in, the ones added last first.
     */
    @Volatile
    var sources: List<StringSource> = listOf(JsonStringSource())
        private set

    /**
     * The locales any source has strings for.
     */
    val availableLocales: Set<String>
        get() = sources.flatMapTo(LinkedHashSet()) { it.locales }


    @Volatile
    private var listeners = mapOf<Any, () -> Unit>()


    /**
     * Adds a source of strings, looked up before the ones added earlier.
     */
    fun addSource(source: StringSource) {
        sources = listOf(source) + sources
        version++
    }

    fun removeSource(source: StringSource) {
        sources = sources - source
        version++
    }


    /**
     * The string with the given [key] in the current locale, or the key itself if no source has it.
     */
    fun get(key: String): String = getOrNull(key) ?: key

    /**
     * The string with the given [key] in the current locale, or null if no source has it.
     */
    fun getOrNull(key: String): String? {
        val sources = sources
        for (locale in fallbackChain) {
            for (source in sources) {
                source.get(locale, key)?.let { return it }
            }
        }
        return null
    }

    /**
     * The array of strings with the given [key] in the current locale, empty if no source has it.
     */
    fun getArray(key: String): List<String> {
        val sources = sources
        for (locale in fallbackChain) {
            for (source in sources) {
                source.getArray(locale, key)?.let { return it }
            }
        }
        return emptyList()
    }

    /**
     * Formats [args] as [pattern] says, see [formatString].
     */
    fun format(pattern: String, vararg args: Any?): String = formatString(pattern, *args)


    /**
     * Calls [listener] after the locale changed. Adding one with the same [key] replaces it.
     */
    fun addLocaleListener(key: Any, listener: () -> Unit) {
        listeners = listeners + (key to listener)
    }

    fun removeLocaleListener(key: Any) {
        listeners = listeners - key
    }


    /**
     * Normalizes a locale tag into the BCP 47 form the sources are keyed by: `pt_BR` and Android's
     * `pt-rBR` become `pt-BR`, the language is lowercase and the region uppercase.
     */
    fun normalize(tag: String): String {
        val parts = tag.trim().replace('_', '-').split('-').filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            return DEFAULT_LOCALE
        }

        return parts.mapIndexed { i, part ->
            when {
                i == 0 -> part.lowercase()
                // Android's region qualifier, e.g. the "rBR" of "pt-rBR".
                part.length == 3 && part[0] == 'r' && part[1].isUpperCase() -> part.substring(1)
                part.length == 2 -> part.uppercase()
                // A script, e.g. "Hans".
                part.length == 4 -> part.lowercase().replaceFirstChar { it.uppercaseChar() }
                else -> part
            }
        }.joinToString("-")
    }

    /**
     * The locales to look strings up in for [tag], from the most specific to [DEFAULT_LOCALE].
     */
    fun fallbackChainOf(tag: String): List<String> {
        val parts = normalize(tag).split('-')
        val chain = ArrayList<String>(parts.size + 1)

        for (count in parts.size downTo 1) {
            chain += parts.subList(0, count).joinToString("-")
        }
        if (DEFAULT_LOCALE !in chain) {
            chain += DEFAULT_LOCALE
        }
        return chain
    }
}
