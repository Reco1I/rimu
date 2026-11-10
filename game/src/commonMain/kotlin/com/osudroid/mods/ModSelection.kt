package com.osudroid.mods

import com.osudroid.utils.ModHashMap
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/**
 * The mods the player has selected: the ones the mod menu shows, song select rates beatmaps with and
 * gameplay starts with. It's the only copy of them, every scene reads and changes this one.
 */
object ModSelection {

    /**
     * The selected mods. Read them here, but change them through [add], [remove] and [clear] so that
     * the listeners know.
     */
    @JvmStatic
    val mods = ModHashMap()

    @Volatile
    private var listeners = mapOf<Any, (List<Mod>) -> Unit>()


    /**
     * Selects [mod], unselecting the mods incompatible with it.
     */
    @JvmStatic
    fun add(mod: Mod) {
        if (mod in mods) {
            return
        }

        val before = mods.values.toList()
        mods.put(mod)

        notifyChanged(before.filter { it !in mods } + mod)
    }

    @JvmStatic
    fun remove(mod: Mod) = remove(mod::class)

    @JvmStatic
    fun remove(modClass: KClass<out Mod>) {
        val removed = mods.remove(modClass) ?: return
        notifyChanged(listOf(removed))
    }

    /**
     * Unselects every mod but those [keep] tells to keep.
     */
    @JvmStatic
    fun clear(keep: (Mod) -> Boolean = { false }) {
        val removed = mods.values.filter { !keep(it) }

        if (removed.isEmpty()) {
            return
        }

        removed.forEach { mods.remove(it::class) }
        notifyChanged(removed)
    }

    /**
     * Tells the listeners that the settings of [mod], a selected mod, changed.
     */
    @JvmStatic
    fun notifyChanged(mod: Mod) = notifyChanged(listOf(mod))

    private fun notifyChanged(changed: List<Mod>) {
        listeners.values.forEach { it(changed) }
    }


    /**
     * Calls [listener] with the mods that were selected, unselected or changed, on the thread that
     * changed them. Adding one with the same [key] replaces it.
     */
    fun addListener(key: Any, listener: (changed: List<Mod>) -> Unit) {
        listeners = listeners + (key to listener)
    }

    fun removeListener(key: Any) {
        listeners = listeners - key
    }
}
