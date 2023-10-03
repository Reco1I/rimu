package com.reco1l.skindecoder

import com.reco1l.framework.extensions.Regexs
import com.reco1l.framework.extensions.between
import com.reco1l.framework.extensions.decapitalize
import com.reco1l.framework.extensions.isExtension
import com.reco1l.framework.extensions.sanitize
import com.reco1l.framework.graphics.Color4
import com.reco1l.skindecoder.data.SkinData
import com.reco1l.skindecoder.exceptions.InvalidSkinException
import com.reco1l.skindecoder.serializers.ColorSerializer
import com.reco1l.skindecoder.serializers.NumericBooleanSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties
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
    private var buffer: BufferedSource? = null

    // Using '.properties' format to decode which is similar to '.ini'
    @OptIn(ExperimentalSerializationApi::class)
    private val format = Properties(SerializersModule {

        contextual(Color4::class, ColorSerializer)
        contextual(Boolean::class, NumericBooleanSerializer)
    })

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
    @OptIn(ExperimentalSerializationApi::class)
    fun decode(stream: InputStream): SkinData
    {
        // Clearing previous buffer if there's any, this will cancel the current decoding.
        close()
        buffer = stream.source().buffer()

        // Map used to store the sections and its values
        val map = mutableMapOf<String, MutableMap<String, Any>>()

        var currentLine: String?
        var currentSection: String? = null

        // Nullability check in `buffer` just in case it was closed while decoding.
        while (buffer?.readUtf8Line().also { currentLine = it } != null)
        {

            // Trimming it before the C++ comment delimiter
            val line = currentLine?.substringBefore(" //")?.sanitize() ?: continue

            // [SectionName]
            if (line.startsWith('[') && line.endsWith(']'))
            {
                val name = line.between('[', ']')?.sanitize()
                    // Adapting keys to Kotlin's properties name convention.
                    ?.decapitalize()

                currentSection = name
                continue
            }

            // Ignoring values if the current sections hasn't been set.
            if (currentSection == null)
                continue

            // Extracting the key
            val key = line.substringBefore(':').takeIf { Regexs.ALPHANUMERIC.matches(it) }
                // Adapting keys to Kotlin's properties name convention.
                ?.decapitalize() ?: continue

            // Extracting the value
            val value = decodeValue(line.substringAfter(':').trim()) ?: continue

            // Creating the section map if it wasn't created yet.
            map.getOrPut(currentSection) { mutableMapOf() }[key] = value
        }

        // We finished using the buffer
        close()

        // Decoding using kotlinx.serialization
        return format.decodeFromMap(SkinData.serializer(), map)
    }

    private fun decodeValue(input: String): Any?
    {

        // Integer
        if (Regexs.INTEGER.matches(input))
            return input.toIntOrNull()

        // Decimal
        if (Regexs.DECIMAL.matches(input))
            return input.toFloatOrNull()

        // IntArray
        if (Regexs.INTEGER_ARRAY.matches(input))
        {
            val sequence = input.split(',').takeUnless { it.isEmpty() }
                ?: return null

            // We use this instead of map() because we want a primitive Int array.
            return IntArray(sequence.size) {
                // We consider as invalid array in case one of the values is wrong, because of
                // the regular expression this should never happen.
                sequence[it].trim().toIntOrNull() ?: return null
            }
        }

        // String
        return input
    }


    /**
     * Close the buffer and cancel the decoding.
     */
    override fun close()
    {
        buffer?.close()
        buffer = null
    }


    companion object
    {
        /**
         * The `ini` extension constant.
         */
        const val EXTENSION = "ini"
    }
}