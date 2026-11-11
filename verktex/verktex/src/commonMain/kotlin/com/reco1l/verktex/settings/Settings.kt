package com.reco1l.verktex.settings

import com.reco1l.verktex.Logger
import com.reco1l.verktex.PlatformStorage
import com.reco1l.verktex.Platform
import kotlin.jvm.JvmStatic
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okio.Path

/**
 * The settings of the user, stored as a JSON object in `settings.json` in the
 * [data directory][PlatformStorage.dataDirectory]. Each setting is declared as a [SettingKey].
 *
 * The file is read on first use. Changes are saved a moment after the last one (see [update]),
 * and right away by [flush], which the engine calls when it's paused or disposed. The file is
 * replaced atomically, so it's never left half written.
 *
 * Keys the application doesn't declare are kept as they are, and a value that can't be read as
 * its key's type reads as the default. Without a platform (e.g. in tests) or while [isReadOnly],
 * nothing is written.
 */
object Settings {

    /**
     * Whether the settings are only read, never written to the file. Changes still apply until
     * the application exits. For processes that share the file with the application, e.g. a UI
     * test browser, so they don't change it.
     */
    var isReadOnly = false

    /**
     * Whether there was no settings file when they were read, e.g. on the first start. An
     * application can import the settings it kept elsewhere before then.
     */
    val isNew: Boolean
        get() = synchronized(lock) {
            load()
            wasNew
        }


    private val json = Json { prettyPrint = true }

    private val lock = SynchronizedObject()

    private var values: MutableMap<String, JsonElement>? = null

    // The decoded values, by the name of their key.
    private val cache = HashMap<String, Any?>()

    private class Observer(val key: SettingKey<*>, val callback: (Any?) -> Unit)

    private var observers = mapOf<String, List<Observer>>()

    private var wasNew = false

    private var isLoadedFromStorage = false

    private var isDirty = false

    private var lastChange = TimeSource.Monotonic.markNow()


    //region Values

    /**
     * The value of [key], or its default if it isn't set or can't be read as its type.
     */
    @JvmStatic
    operator fun <T> get(key: SettingKey<T>): T = synchronized(lock) {
        if (key.name in cache) {
            @Suppress("UNCHECKED_CAST")
            return cache[key.name] as T
        }

        val element = load()[key.name]
        val value = if (element == null) key.default else try {
            json.decodeFromJsonElement(key.serializer, element)
        } catch (e: Exception) {
            Logger.w("Settings", "\"${key.name}\" can't be read as its type, using its default: $element")
            key.default
        }

        cache[key.name] = value
        value
    }

    @JvmStatic
    operator fun <T> set(key: SettingKey<T>, value: T) {
        val changed = synchronized(lock) {
            val element = json.encodeToJsonElement(key.serializer, value)
            val values = load()

            if (values[key.name] == element) {
                false
            } else {
                values[key.name] = element
                cache[key.name] = value
                markDirty()
                true
            }
        }

        if (changed) {
            observers[key.name]?.forEach { it.callback(value) }
        }
    }

    /**
     * Whether [key] has a value stored, rather than its default.
     */
    @JvmStatic
    fun contains(key: SettingKey<*>): Boolean = synchronized(lock) { key.name in load() }

    /**
     * Removes the value of [key], so it reads as its default.
     */
    @JvmStatic
    fun remove(key: SettingKey<*>) {
        val removed = synchronized(lock) {
            cache.remove(key.name)
            (load().remove(key.name) != null).also { if (it) markDirty() }
        }

        if (removed) {
            observers[key.name]?.forEach { it.callback(key.default) }
        }
    }

    //endregion

    //region Observers

    /**
     * Calls [observer] with the new value of [key] whenever it changes. Returns it, to remove it
     * with [removeObserver].
     */
    fun <T> observe(key: SettingKey<T>, observer: (T) -> Unit): (T) -> Unit {
        @Suppress("UNCHECKED_CAST")
        val entry = Observer(key, observer as (Any?) -> Unit)
        synchronized(lock) {
            observers = observers + (key.name to (observers[key.name].orEmpty() + entry))
        }
        return observer
    }

    fun <T> removeObserver(key: SettingKey<T>, observer: (T) -> Unit) {
        synchronized(lock) {
            val remaining = observers[key.name].orEmpty().filter { it.callback !== observer }
            observers = if (remaining.isEmpty()) observers - key.name else observers + (key.name to remaining)
        }
    }

    //endregion

    //region JSON

    /**
     * All the settings as a JSON object, e.g. for a backup.
     */
    @JvmStatic
    fun toJson(): JsonObject = synchronized(lock) { JsonObject(load().toMap()) }

    /**
     * Sets the settings in [values], by name, keeping the others. Values are read as their key's
     * type when used, so they can come from elsewhere, e.g. strings for booleans.
     */
    @JvmStatic
    fun putAll(values: Map<String, JsonElement>) {
        if (values.isEmpty()) return

        val changedNames = synchronized(lock) {
            val stored = load()
            val changed = values.filter { (name, value) -> stored[name] != value }.keys

            stored.putAll(values)
            changed.forEach(cache::remove)
            if (changed.isNotEmpty()) markDirty()
            changed
        }

        for (name in changedNames) {
            observers[name]?.forEach { it.callback(get(it.key)) }
        }
    }

    //endregion

    //region Storage

    /**
     * Saves the changes now if there are any.
     */
    @JvmStatic
    fun flush() {
        val (file, text) = synchronized(lock) {
            if (!isDirty || isReadOnly) return
            val file = file() ?: return
            isDirty = false
            file to json.encodeToString(JsonObject.serializer(), JsonObject(load().toMap()))
        }

        try {
            val fileSystem = Platform.fileSystem
            val temporary = file.parent!! / (file.name + ".tmp")

            file.parent?.let(fileSystem::createDirectories)
            fileSystem.write(temporary) { writeUtf8(text) }
            fileSystem.atomicMove(temporary, file)
        } catch (e: Exception) {
            Logger.e("Settings", "Couldn't save the settings to $file", e)
            synchronized(lock) { isDirty = true }
        }
    }

    /**
     * Saves the changes once none were made for a moment. Called on every update by the engine.
     */
    fun update() {
        val isDue = synchronized(lock) { isDirty && lastChange.elapsedNow() >= SAVE_DELAY }
        if (isDue) {
            flush()
        }
    }

    /**
     * Forgets the settings read, so the file is read again on next use. Only for tests.
     */
    internal fun reset() = synchronized(lock) {
        values = null
        cache.clear()
        isDirty = false
        wasNew = false
        isLoadedFromStorage = false
    }

    /**
     * The folder the file is saved in instead of the platform's data directory. Only for tests.
     */
    internal var directory: Path? = null

    private fun markDirty() {
        isDirty = true
        lastChange = TimeSource.Monotonic.markNow()
    }

    // The file, or null without a platform to tell where it goes.
    private fun file(): Path? = try {
        (directory ?: Platform.storage.dataDirectory) / FILE_NAME
    } catch (_: IllegalStateException) {
        null
    }

    private fun load(): MutableMap<String, JsonElement> {
        values?.let { if (isLoadedFromStorage) return it }

        // Without a platform yet the values are kept in memory, and the file is read once there is
        // one, below them.
        val file = file() ?: return values ?: LinkedHashMap<String, JsonElement>().also { values = it }
        val loaded = LinkedHashMap<String, JsonElement>()

        try {
            if (Platform.fileSystem.exists(file)) {
                val text = Platform.fileSystem.read(file) { readUtf8() }
                loaded.putAll(json.parseToJsonElement(text).jsonObject)
            } else {
                wasNew = true
            }
        } catch (e: Exception) {
            // A broken file is replaced on the next save rather than stopping the application.
            Logger.e("Settings", "Couldn't read the settings from $file, using the defaults", e)
        }

        values?.let {
            loaded.putAll(it)
            cache.clear()
        }

        values = loaded
        isLoadedFromStorage = true
        return loaded
    }

    private const val FILE_NAME = "settings.json"

    private val SAVE_DELAY = 1.seconds

    //endregion
}
