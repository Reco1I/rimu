package com.osudroid.ui.v2

import com.osudroid.ui.skinning.OsuSkin
import com.osudroid.ui.skinning.SkinConfigurationLoader
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.Theme
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

        val configuration = SkinConfigurationLoader.load(skinFolder)
        OsuSkin.current = configuration

        // Applies the accent color of the skin to the theme of the UI.
        Verktex.theme = Theme(palette = Theme.ColorPalette(configuration.theme.accentColor))
    }
}
