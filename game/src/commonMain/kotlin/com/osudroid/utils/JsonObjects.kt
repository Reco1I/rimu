@file:JvmName("JsonObjects")

package com.osudroid.utils

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive

// Lookups that coerce values the way Android's org.json does, so data that was read with it gives the
// same results: numbers can be strings, booleans can be "true" or "false" strings in any case, and
// anything missing or of another type gives the fallback.

/**
 * Parses JSON as leniently as Android's org.json: comments, trailing commas and unquoted strings are
 * accepted.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
val LenientJson = Json {
    isLenient = true
    allowComments = true
    allowTrailingComma = true
}

/**
 * Whether [key] is missing or null.
 */
fun JsonObject.isNull(key: String) = this[key].let { it == null || it is JsonNull }

/**
 * Gets the value of [key] as a string. Like org.json, a null value gives `"null"`.
 */
@JvmOverloads
fun JsonObject.optString(key: String, fallback: String = "") = this[key].asString() ?: fallback

fun JsonObject.optInt(key: String, fallback: Int = 0) = this[key].asDouble()?.toInt() ?: fallback

fun JsonObject.optLong(key: String, fallback: Long = 0L) = this[key].asLong() ?: fallback

@JvmOverloads
fun JsonObject.optDouble(key: String, fallback: Double = Double.NaN) = this[key].asDouble() ?: fallback

@JvmOverloads
fun JsonObject.optBoolean(key: String, fallback: Boolean = false) = this[key].asBoolean() ?: fallback

fun JsonObject.optJsonObject(key: String) = this[key] as? JsonObject

fun JsonObject.optJsonArray(key: String) = this[key] as? JsonArray

@JvmOverloads
fun JsonArray.optString(index: Int, fallback: String = "") = getOrNull(index).asString() ?: fallback

fun JsonArray.optJsonObject(index: Int) = getOrNull(index) as? JsonObject


private fun JsonElement?.asString(): String? = when (this) {
    null -> null
    is JsonNull -> "null"
    is JsonPrimitive -> content
    else -> toString()
}

private fun JsonElement?.asDouble(): Double? {
    if (this !is JsonPrimitive || this is JsonNull) {
        return null
    }
    if (!isString && (content == "true" || content == "false")) {
        return null
    }
    return content.trim().toDoubleOrNull()
}

private fun JsonElement?.asLong(): Long? {
    if (this !is JsonPrimitive || this is JsonNull) {
        return null
    }
    return content.trim().toLongOrNull() ?: asDouble()?.toLong()
}

private fun JsonElement?.asBoolean(): Boolean? {
    if (this !is JsonPrimitive || this is JsonNull) {
        return null
    }
    return when {
        content.equals("true", ignoreCase = true) -> true
        content.equals("false", ignoreCase = true) -> false
        else -> null
    }
}

/**
 * Puts [value] if it isn't null, as org.json's `put` removes the key for a null value instead.
 */
fun JsonObjectBuilder.putIfNotNull(key: String, value: Any?) {
    when (value) {
        null -> Unit
        is JsonElement -> put(key, value)
        is String -> put(key, JsonPrimitive(value))
        is Number -> put(key, JsonPrimitive(value))
        is Boolean -> put(key, JsonPrimitive(value))
        else -> put(key, JsonPrimitive(value.toString()))
    }
}
