package com.reco1l.verktex.ui

import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.text.text
import com.rian.andengine.timing.IFrameBasedClock

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
     * Whether the button is selected or not.
     */
    @Deprecated("Use colorVariant instead", ReplaceWith("variant"))
    var isSelected = false


    //endregion


    init {
        scaleOrigin = Anchor.Center

        style = Style { theme ->
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


    override fun onUpdate(clock: IFrameBasedClock) {

        val targetScale = if (isPressed) 1f else 0.9f
        val targetAlpha = if (isEnabled) 1f else 0.5f

        alpha = clock.interpolateFloat(0.2f, alpha, targetAlpha, Easing.OutExpo)
        scale = clock.interpolateFloat(0.2f, scale, targetScale, Easing.OutExpo)

        super.onUpdate(clock)
    }

    //region Touch

    override fun onInputEvent(event: InputEvent): Boolean {
        if (!isEnabled) {
            return true
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


