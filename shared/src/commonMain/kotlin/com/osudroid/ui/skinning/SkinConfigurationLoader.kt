package com.osudroid.ui.skinning

import com.osudroid.utils.LenientJson
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okio.Path

/**
 * Reads the configuration of a skin folder from its `skin.json` and `skin.ini`.
 *
 * A skin.json says what the skin wants, and what it doesn't say comes from the skin.ini, key by key.
 * A skin with a skin.ini alone has what it converts to ([LegacySkinIni.toConfiguration]), and one
 * with neither the default configuration. A file that can't be read is skipped, as if it were
 * missing.
 */
object SkinConfigurationLoader {

    const val JSON_FILE = "skin.json"

    const val INI_FILE = "skin.ini"

    fun load(folder: Path): SkinConfiguration {
        val ini = readIni(folder / INI_FILE)
        val json = readJson(folder / JSON_FILE)

        if (json != null) {
            try {
                return decode(json, ini)
            } catch (e: Exception) {
                Logger.e(TAG, "The skin.json of $folder can't be read.", e)
            }
        }

        return ini?.toConfiguration() ?: SkinConfiguration.Default
    }

    /**
     * Decodes a skin.json, filling what it lacks from [ini] when given.
     */
    fun decode(json: JsonObject, ini: LegacySkinIni?): SkinConfiguration {
        if (ini == null) {
            return decodeSkinConfiguration(json)
        }

        val iniJson = SkinJson.encodeToJsonElement(SkinConfiguration.serializer(), ini.toConfiguration()).jsonObject
        return decodeSkinConfiguration(mergeMissing(json, iniJson))
    }

    /**
     * Reads the skin.json at [file] as it is, or `null` when there's none or it isn't valid.
     */
    fun readJson(file: Path): JsonObject? = read(file) { LenientJson.parseToJsonElement(it).jsonObject }

    private fun readIni(file: Path): LegacySkinIni? = read(file, LegacySkinIni::decode)

    private fun <T> read(file: Path, decode: (String) -> T): T? {
        val fileSystem = Platform.fileSystem

        if (!fileSystem.exists(file)) {
            return null
        }

        return try {
            decode(fileSystem.read(file) { readUtf8() })
        } catch (e: Exception) {
            Logger.e(TAG, "$file can't be read.", e)
            null
        }
    }

    private const val TAG = "SkinConfigurationLoader"
}
