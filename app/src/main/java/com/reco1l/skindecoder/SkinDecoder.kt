package com.reco1l.skindecoder

import com.reco1l.framework.kotlin.between
import com.reco1l.framework.kotlin.decapitalize
import com.reco1l.framework.data.isExtension
import com.reco1l.framework.data.readUTF8Lines
import com.reco1l.framework.kotlin.Regexs.ALPHANUMERIC
import com.reco1l.framework.kotlin.Regexs.DECIMAL
import com.reco1l.framework.kotlin.Regexs.INTEGER
import com.reco1l.framework.kotlin.Regexs.INTEGER_ARRAY
import com.reco1l.framework.kotlin.isBetween
import com.reco1l.framework.kotlin.takeIfMatches
import com.reco1l.skindecoder.data.SkinData
import com.reco1l.skindecoder.exceptions.InvalidSkinException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.File
import java.io.InputStream

/**
 * Basic INI reader meant only for osu! skin.ini format.
 * It only supports [Boolean], [Int], [Float] and [IntArray] types.
 *
 * @author Reco1l
 *
 */
class SkinDecoder : Closeable
{

    // This buffer will be used along the parsing and set back to `null` once it finished.
    private var currentBuffer: BufferedSource? = null

    @OptIn(ExperimentalSerializationApi::class)
    private val format = Json {

        coerceInputValues = true
        ignoreUnknownKeys = true
        useAlternativeNames = false

        // The skin.ini file has the .ini naming convention for commands name with uppercase first
        // letter meanwhile the data classes used for de-serialization doesn't, here we're handling
        // that behaviour.
        namingStrategy = JsonNamingStrategy { _, _, name -> name.decapitalize() }
    }


    /**
     * Decode the `skin.ini` file.
     */
    fun decode(file: File): SkinData
    {
        if (!file.isExtension(EXTENSION))
            throw InvalidSkinException("Not an INI file.")

        return decode(file.inputStream())
    }

    /**
     * Decode the `skin.ini` file from an [InputStream].
     */
    fun decode(stream: InputStream): SkinData
    {
        // Clearing previous buffer if there's any, this will cancel the current decoding.
        close()

        val buffer = stream.source().buffer()
        currentBuffer = buffer

        // Map tree for INI sections and commands.
        val map = mutableMapOf<String,
                // Command key and command value mapping.
                MutableMap<String, JsonElement>>()

        var currentSection: String? = null

        buffer.readUTF8Lines loop@{ rawLine ->

            val line = rawLine.substringBefore(" //").trim()

            // Means the line refers to a section declaration: [Section]
            if (line isBetween '['..']')
            {
                currentSection = line.between('[', ']')
                return@loop
            }

            // Ignoring values if the current sections hasn't been declared yet or if the delimiter
            // isn't present in the line.
            if (currentSection == null || ':' !in line)
                return@loop

            val key = line.substringBefore(':').takeIfMatches(ALPHANUMERIC)
                ?:
                return@loop

            val value = decodeValue(line.substringAfter(':').trim())
                ?:
                return@loop

            map.getOrPut(currentSection!!) { mutableMapOf() }[key] = value
        }

        if (currentBuffer == buffer)
            currentBuffer = null

        return format.decodeFromJsonElement(buildJsonObject {

            // Mapping sections into JsonObjects.
            map.forEach { put(it.key, JsonObject(it.value)) }
        })
    }

    private fun decodeValue(input: String) = when
    {
        // Integers
        input matches INTEGER -> input.toIntOrNull()?.let { JsonPrimitive(it) }

        // Decimals
        input matches DECIMAL -> input.toFloatOrNull()?.let { JsonPrimitive(it) }

        // Integer arrays
        input matches INTEGER_ARRAY ->
        {
            // At this point should have format enough to be considered as an integer array.
            val sequence = input.split(',').map { it.trim() }

            buildJsonArray {
                // We consider as invalid array in case one of the values is wrong, because of
                // the regular expression this should never happen.
                sequence.forEach { add(it.toIntOrNull() ?: return null) }
            }
        }

        // Strings
        else -> JsonPrimitive(input)
    }


    /**
     * Close the buffer and cancel the decoding.
     */
    override fun close()
    {
        currentBuffer?.close()
        currentBuffer = null
    }


    companion object
    {
        /**
         * The `ini` extension constant.
         */
        const val EXTENSION = "ini"
    }
}