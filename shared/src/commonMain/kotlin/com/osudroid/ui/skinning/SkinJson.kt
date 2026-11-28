@file:OptIn(ExperimentalSerializationApi::class)

package com.osudroid.ui.skinning

import com.osudroid.utils.LenientJson
import com.reco1l.verktex.data.Color4
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The JSON skins are read and written with: as lenient as [LenientJson], ignoring the keys rimu!
 * doesn't read, and writing the default values, so that a configuration converted from a skin.ini
 * fills every key a skin.json lacks.
 */
val SkinJson = Json(LenientJson) {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    // Indented by four spaces, as skin.json files always were.
    prettyPrint = true
    prettyPrintIndent = "    "
}

/**
 * Decodes a skin.json, its values coerced first ([coercedTo]).
 */
fun decodeSkinConfiguration(json: JsonObject): SkinConfiguration {
    val serializer = SkinConfiguration.serializer()
    return SkinJson.decodeFromJsonElement(serializer, json.coercedTo(serializer.descriptor))
}

/**
 * A colour as a hexadecimal string, `#RRGGBB`, or `#AARRGGBB` when it isn't opaque. Reads `#RGB`
 * too. Invalid ones throw, [coercedTo] removes them before.
 */
object HexColorSerializer : KSerializer<Color4> {

    override val descriptor = PrimitiveSerialDescriptor("com.osudroid.ui.skinning.HexColor", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = Color4(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Color4) {
        val rgb = listOf(value.red8, value.green8, value.blue8)
        val channels = if (value.alpha8 == 255) rgb else listOf(value.alpha8) + rgb

        encoder.encodeString(channels.joinToString("", prefix = "#") { it.toString(16).uppercase().padStart(2, '0') })
    }

    fun isValid(value: String) = try {
        Color4(value)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}

/**
 * This object with its values coerced as Android's org.json coerced them, for what [descriptor]
 * reads: numbers and booleans can be strings, booleans `"true"` and `"false"` in any case, and
 * values of another type (or invalid colours) are removed, so their properties take their default
 * values. A list of colours drops the invalid ones. Keys [descriptor] doesn't have are kept.
 */
fun JsonObject.coercedTo(descriptor: SerialDescriptor): JsonObject {
    val coerced = LinkedHashMap<String, JsonElement>(this)

    for (index in 0 until descriptor.elementsCount) {
        val name = descriptor.getElementName(index)
        val value = this[name] ?: continue

        val result = value.coercedTo(descriptor.getElementDescriptor(index))
        if (result == null) coerced.remove(name) else coerced[name] = result
    }

    return JsonObject(coerced)
}

private fun JsonElement.coercedTo(descriptor: SerialDescriptor): JsonElement? {
    if (this is JsonNull) {
        return if (descriptor.isNullable) this else null
    }

    if (descriptor.isHexColor()) {
        return (this as? JsonPrimitive)?.takeIf { HexColorSerializer.isValid(it.content) }
    }

    return when (descriptor.kind) {
        PrimitiveKind.BOOLEAN -> asBoolean()?.let(::JsonPrimitive)
        PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.SHORT, PrimitiveKind.BYTE ->
            asDouble()?.let { JsonPrimitive(it.toLong()) }
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> asDouble()?.let(::JsonPrimitive)
        PrimitiveKind.STRING -> (this as? JsonPrimitive)?.let { JsonPrimitive(it.content) }
        StructureKind.CLASS, StructureKind.OBJECT -> (this as? JsonObject)?.coercedTo(descriptor)
        StructureKind.LIST -> {
            val array = this as? JsonArray ?: return null
            val item = descriptor.getElementDescriptor(0)

            when {
                item.isHexColor() -> JsonArray(array.filter { it is JsonPrimitive && HexColorSerializer.isValid(it.content) })
                item.kind == StructureKind.CLASS -> JsonArray(array.map { (it as? JsonObject)?.coercedTo(item) ?: it })
                else -> array
            }
        }
        else -> this
    }
}

private fun SerialDescriptor.isHexColor() =
    serialName.removeSuffix("?") == HexColorSerializer.descriptor.serialName

private fun JsonElement.asBoolean(): Boolean? {
    val content = (this as? JsonPrimitive)?.content ?: return null
    return when {
        content.equals("true", ignoreCase = true) -> true
        content.equals("false", ignoreCase = true) -> false
        else -> null
    }
}

private fun JsonElement.asDouble(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    if (!primitive.isString && (primitive.content == "true" || primitive.content == "false")) {
        return null
    }
    return primitive.content.trim().toDoubleOrNull()
}
