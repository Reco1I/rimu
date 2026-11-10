package com.reco1l.verktex.ui

import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.time.FrameClock

fun UIContainer.button(builder: UIButton.() -> Unit) = UIButton().apply(builder).also { +it }

open class UIButton : UIClickableContainer() {

    var density = Density.Medium
        set(value) {
            if (field != value) {
                field = value
                restyle()
            }
        }

    var variant: Variant = Variant.Surface
        set(value) {
            if (field != value) {
                field = value
                restyle()
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
                restyle()
            }
        }


    //endregion


    init {
        scaleOrigin = Anchor.Center

        backgroundColor = themed {
            when {
                isSelected -> palette.controlActiveBackground
                variant == Variant.Solid -> palette.buttonBackgroundSolid
                variant == Variant.Soft -> palette.buttonBackgroundSoft
                variant == Variant.Surface -> palette.buttonBackgroundSurface
                else -> Color4.Transparent
            }
        }

        height = themed { buttonStyle[density].height }
        padding = horizontalPadding(themed { buttonStyle[density].horizontalPadding })
        radius = themed { buttonStyle[density].radius }
        color = themed { if (variant == Variant.Solid) palette.contentOnSolid else palette.accent }
        border = themed {
            when (variant) {
                Variant.Solid, Variant.Soft, Variant.Surface -> buttonStyle.elevation.border
                Variant.Outline -> Border(1.5f.dip, palette.accent)
                else -> Border.None
            }
        }
        shadow = themed {
            when (variant) {
                Variant.Solid, Variant.Soft, Variant.Surface -> buttonStyle.elevation.shadow
                else -> Shadow.None
            }
        }
    }

    override fun onUpdate(clock: FrameClock) {

        val targetScale = if (isPressed) 0.95f else 1f
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


