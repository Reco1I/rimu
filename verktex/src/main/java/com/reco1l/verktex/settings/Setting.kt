package com.reco1l.verktex.settings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate property type for settings.
 */
class Setting<V : Any>(

    /**
     * The key of the setting.
     */
    val key: String,

    /**
     * The default value of the setting.
     */
    val defaultValue: V

) : ReadWriteProperty<Any, V> {

    private var storedValue: V? = null


    override fun getValue(thisRef: Any, property: KProperty<*>): V {
        if (storedValue == null || Settings.updateCaches) {
            storedValue = Settings[key, defaultValue]
        }
        return storedValue!!
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
        Settings[key, value]
    }

}