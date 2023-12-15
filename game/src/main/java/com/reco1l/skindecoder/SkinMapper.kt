package com.reco1l.skindecoder

import com.reco1l.framework.data.isExtension
import com.reco1l.ini.Ini
import com.reco1l.ini.IniParser
import com.reco1l.skindecoder.data.SkinData
import com.reco1l.skindecoder.data.SkinDataGeneral
import com.reco1l.skindecoder.exceptions.InvalidSkinException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.Closeable
import java.io.File
import java.io.InputStream

class SkinMapper
{

    private val format = Json {

        coerceInputValues = true
        ignoreUnknownKeys = true
        useAlternativeNames = false
    }

    /**
     * Decode the `skin.ini` file.
     */
    fun decode(file: File): SkinData
    {
        if (!file.isExtension(IniParser.EXTENSION))
            throw InvalidSkinException("Not an INI file.")

        return decode(file.inputStream())
    }

    /**
     * Decode the `skin.ini` file from an [InputStream].
     */
    fun decode(stream: InputStream): SkinData
    {
        IniParser(stream).use {

            return format.decodeFromJsonElement(it.decode().encodeToJson())
        }
    }
}