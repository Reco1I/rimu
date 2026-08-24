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
        xs = 0.425f.dip,
        sm = 0.45f.dip,
        md = 0.475f.dip,
        lg = 0.5f.dip,
        xl = 0.525f.dip,
    ),

    /**
     * The gap units of the theme.
     */
    val gap: FixedSizeBreakpoints = FixedSizeBreakpoints(
        xs = 0.5f.dip,
        sm = 0.75f.dip,
        md = 0.25f.dip,
        lg = 1.5f.dip,
        xl = 2f.dip,
    ),

    val typography: Typography = Typography(
        h1 = FontSettings(
            size = 1.5f.dip,
            family = "torus",
            weight = Font.Weight.Bold
        ),
        h2 = FontSettings(
            size = 1.25f.dip,
            family = "torus",
            weight = Font.Weight.SemiBold
        ),
        h3 = FontSettings(
            size = 1.125f.dip,
            family = "torus",
            weight = Font.Weight.SemiBold
        ),
        body = FontSettings(
            size = 1f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
        smallBody = FontSettings(
            size = 0.875f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
        caption = FontSettings(
            size = 0.875f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
        smallCaption = FontSettings(
            size = 0.75f.dip,
            family = "nunito",
            weight = Font.Weight.Normal
        ),
    ),

    val controlStyle: ControlStyle = ControlStyle(
        minimumHeight = 2.5f.dip,
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