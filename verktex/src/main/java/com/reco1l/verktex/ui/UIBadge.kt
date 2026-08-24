package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.UIText

inline fun UIContainer.badge(builder: UIBadge.() -> Unit): UIBadge {
    return UIBadge().apply(builder).also(::plusAssign)
}

/**
 * A badge is a small piece of information that can be used to display a value or a status.
 */
open class UIBadge : UIText() {

    /**
     * The variant of the badge, which determines its visual style.
     */
    var variant = Variant.Soft
        set(value) {
            if (field == value) return
            field = value
            style()
        }

    /**
     * The size of the badge.
     */
    var density = Density.Medium
        set(value) {
            if (field == value) return
            field = value
            style()
        }


    init {
        style = Style { theme ->
            when (variant) {
                Variant.Solid -> {
                    backgroundColor = theme.palette.badgeBackgroundSolid
                    borderColor = Color4.Transparent
                }
                Variant.Soft -> {
                    backgroundColor = theme.palette.badgeBackgroundSoft
                    borderColor = Color4.Transparent
                }
                Variant.Surface -> {
                    backgroundColor = theme.palette.badgeBackgroundSurface
                    borderColor = Color4.Transparent
                }
                Variant.Outline -> {
                    backgroundColor = Color4.Transparent
                    borderColor = theme.palette.badgeBackgroundSolid
                }
            }

            when (density) {
                Density.Small -> {
                    fontSize = theme.typography.smallBody.size
                    padding = Padding(theme.gap.sm, theme.gap.xs)
                    iconSpacing = theme.gap.sm
                    radius = theme.radius.sm
                }
                Density.Medium -> {
                    fontSize = theme.typography.body.size
                    padding = Padding(theme.gap.md, theme.gap.sm)
                    iconSpacing = theme.gap.md
                    radius = theme.radius.md
                }
            }

        }
    }


    /**
     * The variants of the button.
     */
    enum class Variant {
        Solid,
        Soft,
        Surface,
        Outline,
    }

    /**
     * The densities of the badge.
     */
    enum class Density {
        Small,
        Medium
    }
}



