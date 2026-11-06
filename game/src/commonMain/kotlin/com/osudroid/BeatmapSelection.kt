package com.osudroid

import com.osudroid.data.BeatmapInfo
import kotlin.concurrent.Volatile
import kotlin.jvm.JvmStatic

/**
 * The beatmap the player has selected: the one song select shows, the menu plays and gameplay
 * starts. It's the only copy of it, every scene reads and changes this one.
 */
object BeatmapSelection {

    /**
     * The selected beatmap, null with an empty library or before one is picked.
     */
    @JvmStatic
    var beatmap: BeatmapInfo? = null
        set(value) {
            if (field != value) {
                field = value
                listeners.values.forEach { it(value) }
            }
        }

    @Volatile
    private var listeners = mapOf<Any, (BeatmapInfo?) -> Unit>()


    /**
     * Calls [listener] with the new beatmap whenever it changes. Adding one with the same [key]
     * replaces it.
     */
    fun addListener(key: Any, listener: (BeatmapInfo?) -> Unit) {
        listeners = listeners + (key to listener)
    }

    fun removeListener(key: Any) {
        listeners = listeners - key
    }
}
