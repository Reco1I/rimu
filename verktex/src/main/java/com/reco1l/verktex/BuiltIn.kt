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
         * A shader drawing only the interpolated vertex color. It's much cheaper per pixel than
         * [UI], which matters for large areas such as fullscreen gradients.
         */
        lateinit var Color: GraphicsShader

        /**
         * A shader for rounded boxes and text in the same batches, see [com.reco1l.verktex.graphics.VertexLayout.UI].
         */
        lateinit var UI: GraphicsShader

        /**
         * A shader for rendering textured quads.
         */
        lateinit var TextureQuad: GraphicsShader

        /**
         * A shader for rendering external (video) textures, sampled through their transform.
         */
        lateinit var Video: GraphicsShader

        /**
         * A shader blurring a texture along one axis, see [com.reco1l.verktex.ui.container.UICaptureContainer.blurRadius].
         */
        lateinit var Blur: GraphicsShader

        /**
         * A shader for rendering particles, see [com.reco1l.verktex.ui.UIParticleSystem].
         */
        lateinit var Particle: GraphicsShader
    }

    /**
     * Built-in font families.
     */
    object Fonts {

        /**
         * The default font family used by the engine which is [Nunito Sans](https://fonts.google.com/specimen/Nunito+Sans).
         */
        lateinit var Default: FontFamily

        lateinit var Torus: FontFamily

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