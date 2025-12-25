package com.reco1l.verktex.ui

import com.reco1l.verktex.ui.container.UIClickableContainer
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
            borderWidth = if (variant == Variant.Outline) 1.5f.dip else Dimension.Zero

            when (variant) {
                Variant.Solid -> {
                    backgroundColor = theme.palette.buttonBackgroundSolid
                    borderColor = Color4.Transparent
                }
                Variant.Soft -> {
                    backgroundColor = theme.palette.buttonBackgroundSoft
                    borderColor = Color4.Transparent
                }
                Variant.Surface -> {
                    backgroundColor = theme.palette.buttonBackgroundSurface
                    borderColor = Color4.Transparent
                }
                Variant.Outline -> {
                    backgroundColor = Color4.Transparent
                    borderColor = theme.palette.buttonBackgroundSolid
                }
                Variant.Ghost -> {
                    backgroundColor = Color4.Transparent
                    borderColor = Color4.Transparent
                }
            }

            if (isSelected) {
                backgroundColor = theme.palette.controlActiveBackground
            }

            when (density) {
                Density.Small -> {
                    height = theme.controlStyle.minimumHeight
                    padding = horizontalPadding(theme.gap.sm)
                    radius = theme.radius.sm
                }
                Density.Medium -> {
                    height = theme.controlStyle.minimumHeight
                    padding = horizontalPadding(theme.gap.md)
                    radius = theme.radius.md
                }
                Density.Large -> {
                    height = theme.controlStyle.minimumHeight
                    padding = horizontalPadding(theme.gap.lg)
                    radius = theme.radius.lg
                }
            }

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


