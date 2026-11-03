package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
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

    /**
     * The sizes of buttons.
     */
    val buttonStyle: ButtonStyle = ButtonStyle(
        small = ButtonStyle.Size(
            height = 32f.dip,
            horizontalPadding = 12f.dip,
            fontSize = typography.smallBody.size,
            iconSize = typography.smallBody.size,
            iconSpacing = gap.xs,
            radius = radius.sm,
        ),
        medium = ButtonStyle.Size(
            height = 40f.dip,
            horizontalPadding = gap.md,
            fontSize = typography.body.size,
            iconSize = typography.body.size,
            iconSpacing = gap.md,
            radius = radius.md,
        ),
        large = ButtonStyle.Size(
            height = 52f.dip,
            horizontalPadding = gap.lg,
            fontSize = typography.h3.size,
            iconSize = 20f.dip,
            iconSpacing = gap.lg,
            radius = radius.lg,
        ),
        elevation = Elevation(
            bezel = Elevation.Bezel(
                width = 1f.px,
                top = Color4.White.copy(alpha = 0.06f),
                bottom = Color4.Black.copy(alpha = 0.2f),
            ),
            shadow = Shadow(
                color = Color4.Black.copy(alpha = 0.3f),
                offsetY = 1f.dip,
                blur = 2f.dip,
            ),
        ),
    ),

    /**
     * The style of cards, including the ones of modals and dialogs.
     */
    val cardStyle: CardStyle = CardStyle(
        elevation = Elevation(
            bezel = Elevation.Bezel(
                width = 1f.px,
                top = Color4.White.copy(alpha = 0.06f),
                bottom = Color4.Black.copy(alpha = 0.2f),
            ),
            shadow = Shadow(
                color = Color4.Black.copy(alpha = 0.35f),
                offsetY = 2f.dip,
                blur = 8f.dip,
            ),
        ),
    ),

    /**
     * The sizes of badges. Badges are read-only, so unlike controls they don't need a comfortable
     * touch target and are sized after their text.
     */
    val badgeStyle: BadgeStyle = BadgeStyle(
        small = BadgeStyle.Size(
            fontSize = typography.smallCaption.size,
            padding = Padding(6f.dip, 2f.dip),
            iconSpacing = 4f.dip,
            radius = radius.xs,
        ),
        medium = BadgeStyle.Size(
            fontSize = typography.caption.size,
            padding = Padding(8f.dip, 3f.dip),
            iconSpacing = 6f.dip,
            radius = radius.sm,
        ),
    ),

) {

    /**
     * A class that holds control style information.
     */
    open class ControlStyle(
        val minimumHeight: Dimension,
    )

    /**
     * A class that holds card style information.
     */
    open class CardStyle(

        /**
         * How cards look raised. Null for flat cards.
         */
        val elevation: Elevation? = null,
    ) {

        /**
         * Returns this style without [elevation], for flat themes.
         */
        fun withoutElevation() = CardStyle()
    }

    /**
     * How a component looks raised from what is below it, like Windows 11 controls.
     */
    data class Elevation(

        /**
         * The border, lighter on top and darker at the bottom. Null for none.
         */
        val bezel: Bezel? = null,

        /**
         * The shadow below the component. Null for none.
         */
        val shadow: Shadow? = null,
    ) {

        /**
         * The colors of a bezel, the border fades from [top] to [bottom].
         */
        data class Bezel(
            val width: Dimension.Fixed,
            val top: Color4,
            val bottom: Color4,
        )
    }

    /**
     * A class that holds button style information, with a [Size] for each [UIButton.Density].
     */
    open class ButtonStyle(
        val small: Size,
        val medium: Size,
        val large: Size,

        /**
         * How filled buttons look raised. Null for flat buttons.
         */
        val elevation: Elevation? = null,
    ) {

        /**
         * Returns this style without [elevation], for flat themes.
         */
        fun withoutElevation() = ButtonStyle(small, medium, large)

        /**
         * Returns the size used for the given density.
         */
        operator fun get(density: UIButton.Density) = when (density) {
            UIButton.Density.Small -> small
            UIButton.Density.Medium -> medium
            UIButton.Density.Large -> large
        }

        /**
         * The size definitions of a button density.
         */
        open class Size(
            val height: Dimension.Fixed,
            val horizontalPadding: Dimension.Fixed,
            val fontSize: Dimension.Fixed,
            val iconSize: Dimension.Fixed,
            val iconSpacing: Dimension.Fixed,
            val radius: Dimension.Fixed,
        )
    }

    /**
     * A class that holds badge style information, with a [Size] for each [UIBadge.Density].
     */
    open class BadgeStyle(
        val small: Size,
        val medium: Size,
    ) {

        /**
         * Returns the size used for the given density.
         */
        operator fun get(density: UIBadge.Density) = when (density) {
            UIBadge.Density.Small -> small
            UIBadge.Density.Medium -> medium
        }

        /**
         * The size definitions of a badge density.
         */
        open class Size(
            val fontSize: Dimension.Fixed,
            val padding: Padding,
            val iconSpacing: Dimension.Fixed,
            val radius: Dimension.Fixed,
        )
    }

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


/**
 * Makes this component look raised with the given [elevation], or removes its bezel and shadow when
 * null. The border width is left as it is without a bezel.
 */
fun UIComponent.applyElevation(elevation: Theme.Elevation?) {
    val bezel = elevation?.bezel

    if (bezel != null) {
        borderWidth = bezel.width
        borderColor = bezel.top
        borderColorBottom = bezel.bottom
    } else {
        borderColorBottom = null
    }

    shadow = elevation?.shadow
}
