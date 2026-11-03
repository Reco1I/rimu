package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
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
            borderWidth = if (variant == Variant.Outline) 1.5f.dip else Dimension.Zero
            color = if (variant == Variant.Solid) theme.palette.contentOnSolid else Color4.White

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

            val size = theme.badgeStyle[density]
            fontSize = size.fontSize
            padding = size.padding
            iconSpacing = size.iconSpacing
            radius = size.radius

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
     * The densities of the badge, their sizes are defined by [Theme.badgeStyle].
     */
    enum class Density {
        Small,
        Medium
    }
}


inline fun UIContainer.labeledBadge(builder: UILabeledBadge.() -> Unit): UILabeledBadge {
    return UILabeledBadge().apply(builder).also(::plusAssign)
}

/**
 * A badge that displays a value next to a label.
 */
open class UILabeledBadge : UILinearContainer() {

    /**
     * The variant of the badge, which determines its visual style. Only [UIBadge.Variant.Solid] and
     * [UIBadge.Variant.Soft] have a distinct look, the others are drawn as soft.
     */
    var variant = UIBadge.Variant.Soft
        set(value) {
            if (field == value) return
            field = value
            style()
        }

    /**
     * The size of the badge.
     */
    var density = UIBadge.Density.Medium
        set(value) {
            if (field == value) return
            field = value
            style()
        }


    /**
     * The component displaying the label of the badge.
     */
    val labelComponent = UIText().apply {
        alignment = Anchor.Center
        backgroundColor = Color4.Black.copy(alpha = 0.1f)
    }

    /**
     * The component displaying the value of the badge.
     */
    val valueComponent = UIText().apply {
        alignment = Anchor.Center
    }

    //region Delegation

    var label by labelComponent::text
    var value by valueComponent::text

    //endregion


    init {
        orientation = Orientation.Horizontal

        +labelComponent
        +valueComponent

        style = Style { theme ->
            val isSolid = variant == UIBadge.Variant.Solid
            val textColor = if (isSolid) theme.palette.contentOnSolid else Color4.White

            backgroundColor = if (isSolid) theme.palette.badgeBackgroundSolid else theme.palette.badgeBackgroundSoft
            labelComponent.color = textColor
            valueComponent.color = textColor

            val size = theme.badgeStyle[density]
            val componentPadding = size.padding
            val fontSize = size.fontSize
            radius = size.radius

            labelComponent.radius = radius
            labelComponent.padding = componentPadding
            valueComponent.padding = componentPadding
            labelComponent.fontSize = fontSize
            valueComponent.fontSize = fontSize
        }
    }
}



