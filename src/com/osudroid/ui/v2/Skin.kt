package com.osudroid.ui.v2

import com.osudroid.ui.skinning.IniReader
import com.osudroid.ui.skinning.convertToJson
import com.osudroid.ui.skinning.ensureOptionalTexture
import com.osudroid.ui.skinning.ensureTexture
import com.osudroid.ui.skinning.mergeMissing
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.Theme
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
     * Applies the accent color of a skin to the theme of the UI.
     */
    @JvmStatic
    fun applyAccentColor(color: Color4) {
        Verktex.theme = Theme(palette = Theme.ColorPalette(color))
    }

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
                    IniReader(iniFile).use { mergeMissing(json, convertToJson(it)) }
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
            IniReader(iniFile).use { convertToJson(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        ensureOptionalTexture(File(folder, "sliderendcircle.png"))
        ensureOptionalTexture(File(folder, "sliderendcircleoverlay.png"))

        ensureTexture(File(folder, "selection-mods.png"))
        ensureTexture(File(folder, "selection-random.png"))
        ensureTexture(File(folder, "selection-options.png"))

        return json
    }
}
