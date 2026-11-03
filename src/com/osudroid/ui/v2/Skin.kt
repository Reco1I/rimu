package com.osudroid.ui.v2

import com.osudroid.ui.skinning.IniReader
import com.osudroid.ui.skinning.SkinConverter
import java.io.File
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.GlobalManager
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

        SkinJsonReader.getReader().supplyJson(readConfiguration(skinFolder) ?: JSONObject())
    }

    private fun readConfiguration(folder: File): JSONObject? {
        val jsonFile = File(folder, "skin.json")
        val iniFile = File(folder, "skin.ini")

        if (jsonFile.exists()) {
            try {
                val json = JSONObject(OsuSkin.readFull(jsonFile))

                // Backfill any skin.ini derived keys missing
                if (iniFile.exists()) {
                    IniReader(iniFile).use { SkinConverter.mergeMissing(json, SkinConverter.convertToJson(it)) }
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

        GlobalManager.getInstance().setInfo("Reading skin.ini...")

        val json = try {
            IniReader(iniFile).use { SkinConverter.convertToJson(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        SkinConverter.ensureOptionalTexture(File(folder, "sliderendcircle.png"))
        SkinConverter.ensureOptionalTexture(File(folder, "sliderendcircleoverlay.png"))

        SkinConverter.ensureTexture(File(folder, "selection-mods.png"))
        SkinConverter.ensureTexture(File(folder, "selection-random.png"))
        SkinConverter.ensureTexture(File(folder, "selection-options.png"))

        return json
    }
}
