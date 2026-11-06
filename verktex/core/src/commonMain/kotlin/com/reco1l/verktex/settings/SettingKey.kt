package com.reco1l.verktex.settings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * A setting stored by [Settings] under [name], whose value is [default] until it's set. The value is
 * stored as JSON with [serializer], so any type kotlinx.serialization can encode works, e.g. a
 * small `@Serializable` data class or enum.
 *
 * It can be used as a property delegate: `var showFps by VerktexSettings.ShowPerformanceOverlay`.
 *
 * Declare one with [setting].
 */
open class SettingKey<T>(
    val name: String,
    val default: T,
    val serializer: KSerializer<T>,
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = Settings[this]

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        Settings[this] = value
    }

    override fun toString() = "SettingKey($name)"
}

/**
 * Declares a setting stored under [name] with the given [default].
 */
inline fun <reified T> setting(name: String, default: T) = SettingKey(name, default, serializer<T>())
