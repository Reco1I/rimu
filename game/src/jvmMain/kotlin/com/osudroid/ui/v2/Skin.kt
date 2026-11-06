package com.osudroid.ui.v2

import com.osudroid.OsuGameHost
import com.osudroid.ui.skinning.IniReader
import com.osudroid.ui.skinning.convertToJson
import com.osudroid.ui.skinning.mergeMissing
import java.io.File
import com.osudroid.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinJsonReader

/**
 * Loads the configuration of a skin folder, its textures and sounds are looked up through [SkinTextures] and
 * [SkinSounds].
 */
object Skin {

    /**
     * Loads the configuration of the skin in the given folder.
     */
    @JvmStatic
    fun load(folder: String) {
        val skinFolder = File(folder)

        if (!skinFolder.exists()) {
            return
        }

        SkinJsonReader.getReader().supplyJson(readConfiguration(skinFolder) ?: JsonObject(emptyMap()))
    }

    private fun readConfiguration(folder: File): JsonObject? {
        val jsonFile = File(folder, "skin.json")
        val iniFile = File(folder, "skin.ini")

        if (jsonFile.exists()) {
            try {
                val json = LenientJson.parseToJsonElement(OsuSkin.readFull(jsonFile.path)).jsonObject

                // Backfill any skin.ini derived keys missing
                if (iniFile.exists()) {
                    return IniReader(iniFile.path).use { mergeMissing(json, convertToJson(it)) }
                }

                return json
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        if (!iniFile.exists()) {
            return null
        }

        SplashScene.info = ("Reading skin.ini...")

        val json = try {
            IniReader(iniFile.path).use { convertToJson(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        OsuGameHost.current.checkSkinTexture(File(folder, "sliderendcircle.png").path, optional = true)
        OsuGameHost.current.checkSkinTexture(File(folder, "sliderendcircleoverlay.png").path, optional = true)

        OsuGameHost.current.checkSkinTexture(File(folder, "selection-mods.png").path, optional = false)
        OsuGameHost.current.checkSkinTexture(File(folder, "selection-random.png").path, optional = false)
        OsuGameHost.current.checkSkinTexture(File(folder, "selection-options.png").path, optional = false)

        return json
    }
}
