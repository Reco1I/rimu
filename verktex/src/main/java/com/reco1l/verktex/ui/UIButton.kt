package com.reco1l.verktex.ui

import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.time.FrameClock

fun UIContainer.button(builder: UIButton.() -> Unit) = UIButton().apply(builder).also { +it }

@Suppress("LeakingThis")
open class UIButton : UIClickableContainer() {

    var density = Density.Medium
        set(value) {
            if (field != value) {
                field = value
                style()
            }
        }

    var variant: Variant = Variant.Surface
        set(value) {
            if (field != value) {
                field = value
                style()
            }
        }


    //region State

    /**
     * Whether the button is enabled or not. If disabled, the button will not process any
     * touch events.
     */
    var isEnabled = true

    /**
     * Whether the button is selected or not, e.g. the chosen option of a select. Selected buttons
     * are highlighted regardless of their [variant].
     */
    var isSelected = false
        set(value) {
            if (field != value) {
                field = value
                style()
            }
        }


    //endregion


    init {
        scaleOrigin = Anchor.Center

        style = Style { theme ->
            backgroundColor = when (variant) {
                Variant.Solid -> theme.palette.buttonBackgroundSolid
                Variant.Soft -> theme.palette.buttonBackgroundSoft
                Variant.Surface -> theme.palette.buttonBackgroundSurface
                Variant.Outline, Variant.Ghost -> Color4.Transparent
            }

            border = if (variant == Variant.Outline) Border(1.5f.dip, theme.palette.buttonBackgroundSolid) else null

            if (isSelected) {
                backgroundColor = theme.palette.controlActiveBackground
            }

            // Only filled buttons look raised, a shadow would show through transparent ones.
            val isFilled = variant == Variant.Solid || variant == Variant.Soft || variant == Variant.Surface
            applyElevation(if (isFilled) theme.buttonStyle.elevation else null)

            val size = theme.buttonStyle[density]
            height = size.height
            padding = horizontalPadding(size.horizontalPadding)
            radius = size.radius

            alpha = if (isEnabled) 1f else 0.5f
        }
    }


    override fun onUpdate(clock: FrameClock) {

        val targetScale = if (isPressed) 0.9f else 1f
        val targetAlpha = if (isEnabled) 1f else 0.5f

        alpha = clock.interpolateFloat(0.2f, alpha, targetAlpha)
        scale = clock.interpolateFloat(0.2f, scale, targetScale)

        super.onUpdate(clock)
    }

    //region Touch

    override fun onInputEvent(event: InputEvent): Boolean {
        if (!isEnabled) {
            // Swallow presses but let up and cancel events reach other pressed components.
            return event is PointerEvent && event.action == PointerEvent.Action.Down
        }
        return super.onInputEvent(event)
    }


    /**
     * The variants of the button.
     */
    enum class Variant {
        Solid,
        Soft,
        Surface,
        Outline,
        Ghost
    }

    /**
     * The sizes of the button.
     */
    enum class Density {
        Small,
        Medium,
        Large
    }
}


