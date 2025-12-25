package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.fonts.Font

/**
 * A theme is a set of colors and styles that can be applied to an entity.
 */
open class Theme(

    /**
     * The color palette of the theme.
     */
    val palette: ColorPalette = ColorPalette(DEFAULT_ACCENT_COLOR),

    /**
     * The radius units of the theme.
     */
    val radius: FixedSizeBreakpoints = FixedSizeBreakpoints(
        xs = 6.8f.dip,
        sm = 7.2f.dip,
        md = 7.6f.dip,
        lg = 8f.dip,
        xl = 8.4f.dip,
    ),

    /**
     * The gap units of the theme.
     */
    val gap: FixedSizeBreakpoints = FixedSizeBreakpoints(
        xs = 8f.dip,
        sm = 12f.dip,
        md = 16f.dip,
        lg = 24f.dip,
        xl = 32f.dip,
    ),

    val typography: Typography = Typography(
        h1 = FontSettings(
            size = 24f.dip,
            family = "torus",
            weight = Font.Weight.Bold
        ),
        h2 = FontSettings(
            size = 20f.dip,
            family = "torus",
            weight = Font.Weight.SemiBold
        ),
        h3 = FontSettings(
            size = 18f.dip,
            family = "torus",
            weight = Font.Weight.SemiBold
        ),
        body = FontSettings(
            size = 16f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
        smallBody = FontSettings(
            size = 14f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
        caption = FontSettings(
            size = 14f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
        smallCaption = FontSettings(
            size = 12f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
    ),

    val controlStyle: ControlStyle = ControlStyle(
        minimumHeight = 40f.dip,
    ),

) {

    /**
     * A class that holds control style information.
     */
    open class ControlStyle(
        val minimumHeight: Dimension,
    )

    /**
     * A class that holds typography information.
     */
    open class Typography(
        val h1: FontSettings,
        val h2: FontSettings,
        val h3: FontSettings,
        val body: FontSettings,
        val smallBody: FontSettings,
        val caption: FontSettings,
        val smallCaption: FontSettings
    )

    /**
     * A class that holds color palette information.
     *
     * The color palette is used all across the UI components, the algorithm used to generate the
     * palette is a simple shade of the accent color.
     */
    open class ColorPalette(
        val accent: Color4
    ) {
        val surface = accent.darken(5f)

        val buttonBackgroundSolid = accent
        val buttonBackgroundSoft = accent.darken(2.5f)
        val buttonBackgroundSurface = accent.darken(4.5f)

        val badgeBackgroundSolid = accent
        val badgeBackgroundSoft = accent.darken(2.5f)
        val badgeBackgroundSurface = accent.darken(4.5f)

        val controlInactiveBackground = accent.darken(3f)
        val controlActiveBackground = accent.darken(1f)

        val divider = accent.darken(4f)

        /**
         * Color of text and icons drawn over solid backgrounds, which use the light accent color.
         */
        val contentOnSolid = accent.darken(5f)
    }

    /**
     * A class that holds size breakpoints.
     */
    open class FixedSizeBreakpoints(
        val xs: Dimension.Fixed,
        val sm: Dimension.Fixed,
        val md: Dimension.Fixed,
        val lg: Dimension.Fixed,
        val xl: Dimension.Fixed,
    )

    /**
     * A class that holds font settings.
     */
    open class FontSettings(
        val size: Dimension.Fixed,
        val family: String,
        val color: Color4 = Color4.White,
        val weight: Font.Weight = Font.Weight.Normal,
        val style: Font.Style = Font.Style.Normal
    )


    companion object {

        /**
         * The default accent color used in the theme.
         */
        val DEFAULT_ACCENT_COLOR = Color4(0xFFC2CAFF)
    }
}