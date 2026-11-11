package com.osudroid.ui.v2

import com.osudroid.OsuGameHost
import com.osudroid.ui.skinning.IniReader
import com.osudroid.ui.skinning.convertToJson
import com.osudroid.ui.skinning.mergeMissing
import com.osudroid.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinJsonReader
import com.reco1l.verktex.Platform
import okio.Path
import okio.Path.Companion.toPath

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
        val skinFolder = folder.toPath()

        if (!Platform.fileSystem.exists(skinFolder)) {
            return
        }

        SkinJsonReader.getReader().supplyJson(readConfiguration(skinFolder) ?: JsonObject(emptyMap()))
    }

    private fun readConfiguration(folder: Path): JsonObject? {
        val fileSystem = Platform.fileSystem
        val jsonFile = folder / "skin.json"
        val iniFile = folder / "skin.ini"

        if (fileSystem.exists(jsonFile)) {
            try {
                val json = LenientJson.parseToJsonElement(OsuSkin.readFull(jsonFile.toString())).jsonObject

                // Backfill any skin.ini derived keys missing
                if (fileSystem.exists(iniFile)) {
                    return IniReader(iniFile.toString()).use { mergeMissing(json, convertToJson(it)) }
                }

                return json
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        if (!fileSystem.exists(iniFile)) {
            return null
        }

        SplashScene.info = ("Reading skin.ini...")

        val json = try {
            IniReader(iniFile.toString()).use { convertToJson(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        OsuGameHost.current.checkSkinTexture((folder / "sliderendcircle.png").toString(), optional = true)
        OsuGameHost.current.checkSkinTexture((folder / "sliderendcircleoverlay.png").toString(), optional = true)

        OsuGameHost.current.checkSkinTexture((folder / "selection-mods.png").toString(), optional = false)
        OsuGameHost.current.checkSkinTexture((folder / "selection-random.png").toString(), optional = false)
        OsuGameHost.current.checkSkinTexture((folder / "selection-options.png").toString(), optional = false)

        return json
    }
}
