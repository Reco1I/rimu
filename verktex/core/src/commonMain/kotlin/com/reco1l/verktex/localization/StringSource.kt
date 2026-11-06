package com.reco1l.verktex.localization

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Where [Localization] takes the strings of each locale from.
 */
interface StringSource {

    /**
     * The locales this source has strings for, as normalized tags (see [Localization.normalize]).
     */
    val locales: Set<String>

    /**
     * The string with the given [key] in [locale], or null if this source doesn't have it.
     */
    fun get(locale: String, key: String): String?

    /**
     * The array of strings with the given [key] in [locale], or null if this source doesn't have it.
     */
    fun getArray(locale: String, key: String): List<String>?
}

/**
 * Reads the strings of each locale from a `<locale>.json` file in [directory], through
 * [Platform.provider]. A file is a JSON object whose values are strings, or arrays of strings:
 *
 * ```json
 * { "greeting": "Hello", "days": ["Monday", "Tuesday"] }
 * ```
 *
 * Files are read the first time their locale is needed.
 */
open class JsonStringSource(
    private val directory: String = "strings",
    private val isExternal: Boolean = false,
) : StringSource {

    private class Table(val strings: Map<String, String>, val arrays: Map<String, List<String>>)

    private val lock = SynchronizedObject()

    // Null for the locales without a file.
    private val tables = HashMap<String, Table?>()

    private var cachedLocales: Set<String>? = null


    override val locales: Set<String>
        get() = synchronized(lock) {
            cachedLocales ?: (readNames() ?: emptyList())
                .filter { it.endsWith(".json") }
                .map { Localization.normalize(it.removeSuffix(".json")) }
                .toSet()
                .also { cachedLocales = it }
        }

    override fun get(locale: String, key: String) = table(locale)?.strings?.get(key)

    override fun getArray(locale: String, key: String) = table(locale)?.arrays?.get(key)


    /**
     * The content of the file of [locale], or null if it has none.
     */
    protected open fun read(locale: String): ByteArray? {
        if (!isPlatformAvailable()) return null
        return Platform.provider.read("$directory/$locale.json", isExternal)
    }

    /**
     * The names of the files in the [directory], or null if they can't be listed.
     */
    protected open fun readNames(): List<String>? {
        if (!isPlatformAvailable()) return null
        return Platform.provider.list(directory, isExternal)
    }


    private fun table(locale: String): Table? = synchronized(lock) {
        if (locale in tables) {
            return tables[locale]
        }

        val table = try {
            read(locale)?.let { parse(it.decodeToString()) }
        } catch (e: Exception) {
            Logger.e("JsonStringSource", "Couldn't read the strings of $locale in $directory", e)
            null
        }

        tables[locale] = table
        table
    }

    private fun parse(text: String): Table {
        val strings = HashMap<String, String>()
        val arrays = HashMap<String, List<String>>()

        for ((key, value) in Json.parseToJsonElement(text).jsonObject) {
            when (value) {
                is JsonPrimitive -> strings[key] = value.content
                is JsonArray -> arrays[key] = value.map { (it as JsonPrimitive).content }
                else -> Unit
            }
        }

        return Table(strings, arrays)
    }

    private fun isPlatformAvailable() = try {
        Platform.provider
        true
    } catch (_: IllegalStateException) {
        false
    }
}
