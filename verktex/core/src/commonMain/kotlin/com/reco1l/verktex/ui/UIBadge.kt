package com.reco1l.verktex.ui

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Border
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
            updateBorder()
            restyle()
        }

    /**
     * The size of the badge.
     */
    var density = Density.Medium
        set(value) {
            if (field == value) return
            field = value
            restyle()
        }


    init {
        color = themed { if (variant == Variant.Solid) palette.contentOnSolid else Color4.White }

        backgroundColor = themed {
            when (variant) {
                Variant.Solid -> palette.badgeBackgroundSolid
                Variant.Soft -> palette.badgeBackgroundSoft
                Variant.Surface -> palette.badgeBackgroundSurface
                Variant.Outline -> Color4.Transparent
            }
        }

        fontSize = themed { badgeStyle[density].fontSize }
        padding = themed { badgeStyle[density].padding }
        iconSpacing = themed { badgeStyle[density].iconSpacing }
        radius = themed { badgeStyle[density].radius }

        updateBorder()
    }

    private fun updateBorder() {
        border = if (variant == Variant.Outline) Border(1.5f.dip, themed { palette.badgeBackgroundSolid }) else null
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
            restyle()
        }

    /**
     * The size of the badge.
     */
    var density = UIBadge.Density.Medium
        set(value) {
            if (field == value) return
            field = value
            restyle()
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

        backgroundColor = themed {
            if (variant == UIBadge.Variant.Solid) palette.badgeBackgroundSolid else palette.badgeBackgroundSoft
        }
        radius = themed { badgeStyle[density].radius }

        for (component in listOf(labelComponent, valueComponent)) {
            component.color = themed { if (variant == UIBadge.Variant.Solid) palette.contentOnSolid else Color4.White }
            component.padding = themed { badgeStyle[density].padding }
            component.fontSize = themed { badgeStyle[density].fontSize }
        }
        labelComponent.radius = themed { badgeStyle[density].radius }
    }
}



