package com.reco1l.verktex

import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsShader

/**
 * Contains built-in resources that are automatically loaded by the engine.
 * These resources include shaders and fonts that can be used in the application.
 */
object BuiltIn {

    /**
     * Built-in shaders.
     */
    object Shaders {

        /**
         * A shader for rendering solid-colored quads.
         */
        lateinit var SolidQuad: GraphicsShader

        /**
         * A shader for rendering textured quads.
         */
        lateinit var TextureQuad: GraphicsShader
    }

    /**
     * Built-in font families.
     */
    object Fonts {

        /**
         * The default font family used by the engine which is [Nunito Sans](https://fonts.google.com/specimen/Nunito+Sans).
         */
        lateinit var Default: FontFamily

        /**
         * [Font Awesome](https://fontawesome.com/) icon font family for regular icons.
         */
        lateinit var FontAwesomeIconRegular: FontFamily

        /**
         * [Font Awesome](https://fontawesome.com/) icon font family for solid icons.
         */
        lateinit var FontAwesomeIconSolid: FontFamily

        /**
         * [Font Awesome](https://fontawesome.com/) icon font family for brand icons.
         */
        lateinit var FontAwesomeIconBrands: FontFamily
    }
}