package com.osudroid.settings

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okio.Path.Companion.toPath

/**
 * Backs the settings up to `osudroid.cfg` in the game's folder, and restores them from it. The
 * [sensitive][OsuSettingKey.isSensitive] settings stay out of it.
 *
 * The file is a JSON object of the settings by name, as the backups of the old preferences were, so
 * those can be restored too.
 */
object SettingsBackup {

    private val json = Json { prettyPrint = true }

    private val file
        get() = (GameFolders.corePath + "osudroid.cfg").toPath()


    /**
     * Writes the backup, returning whether it succeeded.
     */
    @JvmStatic
    fun export(): Boolean = try {
        val settings = Settings.toJson().filterKeys { OsuSettings.all[it]?.isSensitive != true }
        Platform.fileSystem.write(file) { writeUtf8(json.encodeToString(JsonObject.serializer(), JsonObject(settings))) }
        true
    } catch (e: Exception) {
        Logger.e("SettingsBackup", "Couldn't back up the settings", e)
        false
    }

    /**
     * Restores the backup, returning whether there was one and it could be read.
     */
    @JvmStatic
    fun import(): Boolean = try {
        if (!Platform.fileSystem.exists(file)) {
            false
        } else {
            val backup = json.parseToJsonElement(Platform.fileSystem.read(file) { readUtf8() }).jsonObject
            val values = HashMap<String, JsonElement>()

            for ((name, value) in backup) {
                val key = OsuSettings.all[name]
                if (key?.isSensitive == true) {
                    continue
                }

                val bounds = key?.bounds
                if (bounds == null || value !is JsonPrimitive) {
                    values[name] = value
                    continue
                }

                // Numbers are kept in their range; anything else where one is expected is skipped.
                val number = value.content.toFloatOrNull()
                if (number == null) {
                    Logger.w("SettingsBackup", "Skipping \"$name\", expected a number but got $value")
                    continue
                }

                val clamped = number.coerceIn(bounds.first.toFloat(), bounds.last.toFloat())
                values[name] = if (key.default is Float || value.intOrNull == null) JsonPrimitive(clamped) else JsonPrimitive(clamped.toInt())
            }

            Settings.putAll(values)
            Settings.flush()
            true
        }
    } catch (e: Exception) {
        Logger.e("SettingsBackup", "Couldn't restore the settings", e)
        false
    }
}
