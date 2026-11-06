package ru.nsu.ccfit.zuev.skins

import com.reco1l.verktex.data.Color4

import com.reco1l.verktex.Platform
import kotlin.jvm.JvmStatic
import okio.Path.Companion.toPath

import ru.nsu.ccfit.zuev.osu.Config
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures

class BeatmapSkinManager private constructor() {

    val sliderColor = Color4(1f, 1f, 1f)
    private var skinname = ""

    fun loadBeatmapSkin(beatmapFolder: String) {
        if (!Config.isUseCustomSkins() && !Config.isUseCustomSounds()) {
            clearSkin()
            return
        }
        skinEnabled = true
        if (skinname == beatmapFolder) {
            return
        }
        clearSkin()
        skinname = beatmapFolder
        val folderFile = beatmapFolder.toPath()
        val endsWithExtensions = arrayOf(".wav", ".mp3", ".ogg", ".png", ".jpg")
        val folderFiles = (Platform.fileSystem.listOrNull(folderFile) ?: emptyList()).filter { file ->
            val filename = file.name.lowercase()
            endsWithExtensions.any { filename.endsWith(it) }
        }
        for (f in folderFiles) {
            val metadata = Platform.fileSystem.metadataOrNull(f)
            if (metadata == null || !metadata.isRegularFile) {
                continue
            }
            if (Config.isUseCustomSounds()
                    && (f.name.lowercase().matches(Regex(".*[.]wav"))
                    || f.name.lowercase().matches(Regex(".*[.]mp3"))
                    || f.name.lowercase().matches(Regex(".*[.]ogg")))
                && (metadata.size ?: 0L) >= 1024) {
                SkinSounds.loadBeatmapSound(f.toString())
            } else if (Config.isUseCustomSkins()
                    && (f.name.lowercase().matches(Regex(".*[.]png"))
                    || f.name.lowercase().matches(Regex(".*[.]jpg")))) {
                SkinTextures.loadBeatmapTexture(f.toString())
            }

        }
    }

    fun clearSkin() {
        if (skinname.isEmpty()) {
            return
        }
        skinname = ""
        SkinSounds.clearBeatmapSounds()
        SkinTextures.clearBeatmapTextures()
    }

    companion object {
        private val instance = BeatmapSkinManager()
        private var skinEnabled = true

        @JvmStatic
        fun getInstance(): BeatmapSkinManager {
            return instance
        }

        @JvmStatic
        fun isSkinEnabled(): Boolean {
            return skinEnabled
        }

        @JvmStatic
        fun setSkinEnabled(skinEnabled: Boolean) {
            this.skinEnabled = skinEnabled
        }
    }
}
