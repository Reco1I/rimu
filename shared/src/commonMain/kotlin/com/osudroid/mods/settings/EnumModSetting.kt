package com.osudroid.mods.settings

import kotlinx.serialization.json.*

/**
 * A [ModSetting] that represents an [Enum] value.
 */
class EnumModSetting<T : Enum<*>>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T,

    /**
     * The list of possible values for this [EnumModSetting].
     */
    val entries: List<T>

) : ModSetting<T>(name, key, valueFormatter, defaultValue) {
    override fun load(json: JsonObject) {
        if (key != null) {
            value = entries[json[key]?.jsonPrimitive?.intOrNull ?: defaultValue.ordinal]
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key != null) {
            builder.put(key, value.ordinal)
        }
    }
}

/**
 * Creates an [EnumModSetting] whose possible values are all the entries of [T].
 */
@Suppress("FunctionName")
inline fun <reified T : Enum<T>> EnumModSetting(
    name: String,
    key: String? = null,
    noinline valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T
) = EnumModSetting(name, key, valueFormatter, defaultValue, enumValues<T>().toList())
