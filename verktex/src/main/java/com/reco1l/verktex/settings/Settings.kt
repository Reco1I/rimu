package com.reco1l.verktex.settings

/**
 * A class that handles user and system settings.
 */
object Settings {

    /**
     * The implementation of the settings to save them into storage.
     */
    var implementation: Implementation = Implementation.Memory


    /**
     * A map containing all settings.
     */
    val settings: Map<String, Any>
        field = mutableMapOf<String, Any>()


    /**
     * Whether to update the caches or not. This is useful for bindables that cache its value in
     * order to not iterate over the entire map each time they are accessed.
     */
    var updateCaches = true
        private set


    private val observers = mutableMapOf<String, MutableList<Observer>>()

    //region Operators

    operator fun set(key: String, value: Boolean) = changeSetting(key, value)
    operator fun set(key: String, value: Number) = changeSetting(key, value)
    operator fun set(key: String, value: String) = changeSetting(key, value)
    operator fun set(key: String, value: Set<String>) = changeSetting(key, value)
    operator fun set(key: String, value: Set<Number>) = changeSetting(key, value)

    operator fun <T : Any> get(key: String, default: T): T {
        @Suppress("UNCHECKED_CAST")
        return settings[key] as? T ?: default
    }

    //endregion

    /**
     * Called when a setting is changed.
     *
     * @param key The key of the setting.
     * @param value The new value of the setting.
     */
    private fun changeSetting(key: String, value: Any) {
        settings[key] = value
        updateCaches = true
        implementation.saveToStorage()
        observers[key]?.forEach { it(value) }
    }


    /**
     * Registers an observer for a setting.
     */
    fun on(key: String, observer: Observer): Observer {
        observers.getOrPut(key) { mutableListOf() }.add(observer)
        return observer
    }

    /**
     * Unregisters an observer for a setting.
     */
    fun off(key: String, observer: Observer) {
        observers[key]?.remove(observer)
    }

    /**
     * Unregisters all observers for a setting.
     */
    fun off(key: String) {
        observers.remove(key)
    }


    /**
     * An observer is a function that is called when a setting is changed.
     */
    typealias Observer = (value: Any) -> Unit

    /**
     * Registers an observer for a setting.
     */
    interface Implementation {
        /**
         * Called when the settings set should be saved to storage.
         */
        fun saveToStorage()

        companion object {
            /**
             * A default implementation that does nothing than keeping them into memory.
             */
            val Memory = object : Implementation {
                override fun saveToStorage() = Unit
            }
        }
    }
}


