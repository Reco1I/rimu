package com.osudroid.utils

import com.osudroid.mods.*
import com.reco1l.verktex.math.toFixed
import kotlin.collections.iterator
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.SerializationException

/**
 * A map of [Mod]s by their type with additional functionalities.
 *
 * Like a concurrent map, it can be read and iterated while other threads change it: every change
 * replaces the underlying map, so reads see either the old or the new one, and iterations see the
 * [Mod]s there were when they started.
 */
open class ModHashMap() : MutableMap<KClass<out Mod>, Mod> {

    private val lock = SynchronizedObject()

    @Volatile
    private var map: Map<KClass<out Mod>, Mod> = emptyMap()
        set(value) {
            field = value
            version++
        }

    /**
     * Increases on every change of the [Mod]s in this [ModHashMap], so that a component showing them
     * can tell when they changed by comparing it with the one it showed. Changing the settings of a
     * [Mod] doesn't change it.
     */
    @Volatile
    var version = 0
        private set

    constructor(map: Map<out KClass<out Mod>, Mod>) : this() {
        putAll(map)
    }

    constructor(mods: Collection<Mod>) : this(mods as Iterable<Mod>)

    constructor(mods: Iterable<Mod>?) : this() {
        mods?.forEach { put(it::class, it) }
    }

    //region Map

    override val size get() = map.size

    override fun isEmpty() = map.isEmpty()

    override fun containsKey(key: KClass<out Mod>) = map.containsKey(key)

    override fun containsValue(value: Mod) = map.containsValue(value)

    override fun get(key: KClass<out Mod>) = map[key]

    override val entries: MutableSet<MutableMap.MutableEntry<KClass<out Mod>, Mod>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<KClass<out Mod>, Mod>>() {
            override val size get() = this@ModHashMap.size

            override fun add(element: MutableMap.MutableEntry<KClass<out Mod>, Mod>) =
                throw UnsupportedOperationException("Mods are added with put.")

            override fun iterator() = SnapshotIterator { key, value -> Entry(key, value) }
        }

    override val keys: MutableSet<KClass<out Mod>>
        get() = object : AbstractMutableSet<KClass<out Mod>>() {
            override val size get() = this@ModHashMap.size

            override fun add(element: KClass<out Mod>) = throw UnsupportedOperationException("Mods are added with put.")

            override fun iterator() = SnapshotIterator { key, _ -> key }
        }

    override val values: MutableCollection<Mod>
        get() = object : AbstractMutableCollection<Mod>() {
            override val size get() = this@ModHashMap.size

            override fun add(element: Mod) = throw UnsupportedOperationException("Mods are added with put.")

            override fun iterator() = SnapshotIterator { _, value -> value }
        }

    override fun putAll(from: Map<out KClass<out Mod>, Mod>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun remove(key: KClass<out Mod>): Mod? = synchronized(lock) {
        val previous = map[key] ?: return null
        map = LinkedHashMap(map).apply { remove(key) }
        previous
    }

    override fun clear() = synchronized(lock) {
        map = emptyMap()
    }

    /**
     * Iterates the [Mod]s there were when it was created, removing them from the [ModHashMap].
     */
    private inner class SnapshotIterator<T>(private val transform: (KClass<out Mod>, Mod) -> T) : MutableIterator<T> {
        private val iterator = map.entries.iterator()
        private var last: KClass<out Mod>? = null

        override fun hasNext() = iterator.hasNext()

        override fun next(): T {
            val (key, value) = iterator.next()
            last = key
            return transform(key, value)
        }

        override fun remove() {
            val key = checkNotNull(last) { "next has not been called." }
            this@ModHashMap.remove(key)
            last = null
        }
    }

    private inner class Entry(override val key: KClass<out Mod>, override val value: Mod) : MutableMap.MutableEntry<KClass<out Mod>, Mod> {
        override fun setValue(newValue: Mod) = put(key, newValue)!!
    }

    //endregion

    /**
     * Inserts the specified [Mod] into this [ModHashMap].
     *
     * Do note that this method will remove any [Mod]s that are incompatible with the specified [Mod].
     *
     * @param mod The [Mod] to insert.
     * @return The [Mod] instance that was previously in this [ModHashMap], or `null` if there was no such [Mod].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> put(mod: T) = put(mod::class, mod) as? T

    /**
     * Inserts a new instance of the specified [Mod] type into this [ModHashMap].
     *
     * Do note that this method will remove any [Mod]s that are incompatible with the specified [Mod].
     *
     * @param mod The [Mod] type to insert.
     * @return The [Mod] instance that was previously in this [ModHashMap], or `null` if there was no such [Mod].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> put(mod: KClass<out T>) = put(mod, mod.createModInstance()) as? T

    override fun put(key: KClass<out Mod>, value: Mod): Mod? = synchronized(lock) {
        // Ensure the mod class corresponds to the mod itself.
        if (key != value::class) {
            throw IllegalArgumentException("The key class must correspond to the value class.")
        }

        val newMap = LinkedHashMap(map)

        // Check if there are any mods that are incompatible with the new mod. Both directions are checked since
        // isCompatibleWith is not guaranteed to be symmetric between two mods.
        val iterator = newMap.iterator()
        while (iterator.hasNext()) {
            val (_, mod) = iterator.next()

            if (!value.isCompatibleWith(mod) || !mod.isCompatibleWith(value)) {
                iterator.remove()
            }
        }

        val previous = newMap.put(key, value)
        map = newMap
        previous
    }

    /**
     * Checks if this [ModHashMap] contains a [Mod] of the specified type.
     *
     * @param key The [Mod] type to check for.
     * @return `true` if this [ModHashMap] contains a [Mod] of the specified type, `false` otherwise.
     */
    operator fun contains(key: KClass<out Mod>) = containsKey(key)

    /**
     * Checks if this [ModHashMap] contains a [Mod].
     *
     * Unlike other [contains] methods, this checks for both the type and the instance of the [Mod].
     *
     * @param value The [Mod] to check for.
     * @return `true` if this [ModHashMap] contains the [Mod], `false` otherwise.
     */
    operator fun contains(value: Mod) = value::class in this && this[value::class] == value

    /**
     * Gets a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to get.
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> ofType(key: KClass<out T>) = get(key) as? T

    /**
     * Gets a [Mod] of the specified type from this [ModHashMap].
     *
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    inline fun <reified T : Mod> ofType() = ofType(T::class)

    /**
     * Removes a [Mod] from this [ModHashMap].
     *
     * @param mod The [Mod] to remove.
     * @return The removed [Mod] instance, or `null` if the [Mod] does not exist in this [ModHashMap].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> remove(mod: T) = if (this[mod::class] == mod) remove(mod::class) as? T else null

    /**
     * Removes a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to remove.
     * @return The removed [Mod] instance, or `null` if the [Mod] does not exist in this [ModHashMap].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> removeOfType(key: KClass<out T>) = remove(key) as? T

    /**
     * Serializes all [Mod]s in this [ModHashMap] into a list of [APIMod]s, contained within a JSON string.
     *
     * The result can be deserialized using [deserializeMods].
     *
     * @param includeNonUserPlayable Whether to include [Mod]s whose [Mod.isUserPlayable] is `false`. Defaults to `true`.
     * @param includeIrrelevantMods Whether to include [Mod]s whose [Mod.isRelevant] is `false`. Defaults to `false`.
     * @return The list of [APIMod]s as a JSON string.
     * @throws SerializationException If there is an error during serialization.
     */
    @JvmOverloads
    @Throws(SerializationException::class)
    fun serializeMods(includeNonUserPlayable: Boolean = true, includeIrrelevantMods: Boolean = false) =
        values.serialize(includeNonUserPlayable, includeIrrelevantMods)

    /**
     * Converts the container [Mod]s in this [ModHashMap] to their [String] representative.
     */
    fun getContainerModString() = buildString {
        val difficultyAdjust = ofType<ModDifficultyAdjust>()
        val customSpeed = ofType<ModCustomSpeed>()
        val flashlight = ofType<ModFlashlight>()

        if (difficultyAdjust == null && customSpeed == null && flashlight == null) {
            return@buildString
        }

        if (customSpeed != null) {
            append("x" + customSpeed.trackRateMultiplier.toFixed(2) + "|")
        }

        difficultyAdjust?.let {
            if (it.ar != null) {
                append("AR" + it.ar!!.toFixed(1) + "|")
            }

            if (it.od != null) {
                append("OD" + it.od!!.toFixed(1) + "|")
            }

            if (it.cs != null) {
                append("CS" + it.cs!!.toFixed(1) + "|")
            }

            if (it.hp != null) {
                append("HP" + it.hp!!.toFixed(1) + "|")
            }
        }

        if (flashlight != null && flashlight.followDelay != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
            append("FLD" + flashlight.followDelay.toFixed(2) + "|")
        }
    }.substringBeforeLast('|')

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModHashMap) {
            return false
        }

        if (size != other.size) {
            return false
        }

        for ((k, v) in this) {
            if (v != other[k]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 0

        for ((_, v) in this) {
            result = 31 * result + v.hashCode()
        }

        return result
    }

    /**
     * Converts this [ModHashMap] to a string representation that can be used to display [Mod]s to the user.
     *
     * @param includeNonUserPlayable Whether to include non-user-playable [Mod]s in the string representation.
     * @return The string representation of the [Mod]s in this [ModHashMap].
     */
    @JvmOverloads
    fun toDisplayModString(includeNonUserPlayable: Boolean = true) = buildString {
        modStringOrder.forEach {
            if (!includeNonUserPlayable && !it.isUserPlayable) {
                return@forEach
            }

            if (it::class in this@ModHashMap) {
                append(this@ModHashMap[it::class]!!.toString() + ",")
            }
        }

        if (isEmpty()) {
            append('-')
        } else {
            deleteAt(length - 1)
        }
    }

    /**
     * Creates a deep copy of this [ModHashMap] and all [Mod]s inside of this [ModHashMap].
     */
    fun deepCopy() = ModHashMap().also {
        for ((k, v) in this) {
            it[k] = v.deepCopy()
        }
    }

    companion object {
        /**
         * The order in which mods should be displayed.
         *
         * This is intentionally kept to keep the order consistent with what players are used to.
         */
        private val modStringOrder = arrayOf(
            ModAutoplay(),
            ModRelax(),
            ModAutopilot(),
            ModEasy(),
            ModNoFail(),
            ModHardRock(),
            ModDifficultyAdjust(),
            ModMirror(),
            ModRandom(),
            ModHidden(),
            ModApproachDifferent(),
            ModFreezeFrame(),
            ModTraceable(),
            ModFlashlight(),
            ModDoubleTime(),
            ModNightCore(),
            ModHalfTime(),
            ModCustomSpeed(),
            ModWindDown(),
            ModWindUp(),
            ModPrecise(),
            ModReallyEasy(),
            ModPerfect(),
            ModSuddenDeath(),
            ModMuted(),
            ModSynesthesia(),
            ModScoreV2(),
            ModReplayV6()
        )
    }
}