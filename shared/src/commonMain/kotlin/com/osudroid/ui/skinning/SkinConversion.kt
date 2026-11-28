package com.osudroid.ui.skinning

import kotlinx.serialization.json.JsonObject

/**
 * Recursively fills [target] with keys from [source] it's missing, without overwriting existing ones.
 *
 * @return The filled copy of [target].
 */
fun mergeMissing(target: JsonObject, source: JsonObject): JsonObject {
    val merged = LinkedHashMap(target)

    for ((key, sourceValue) in source) {
        if (!target.containsKey(key)) {
            merged[key] = sourceValue
            continue
        }

        val targetValue = target[key]
        if (sourceValue is JsonObject && targetValue is JsonObject) {
            merged[key] = mergeMissing(targetValue, sourceValue)
        }
    }

    return JsonObject(merged)
}
