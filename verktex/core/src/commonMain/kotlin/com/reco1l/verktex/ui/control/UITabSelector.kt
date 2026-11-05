package com.reco1l.verktex.ui.control

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.themed

/**
 * A row of buttons where only one is selected, marked by an indicator moving between them.
 */
class UITabSelector : UIContainer() {

    /**
     * The index of the selected tab, if any.
     */
    var selectedTab: Int?
        get() = selectedButton?.let { buttonsContainer.indexOfFirst { child -> child === it } }?.takeIf { it >= 0 }
        set(value) {
            selectedButton = if (value == null) null else buttonsContainer[value] as UITextButton
        }


    private val selectionIndicator: UIBox
    private val buttonsContainer: UILinearContainer

    private var selectedButton: UITextButton? = null

    private var indicatorX = 0f
    private var indicatorWidth = 0f


    init {
        padding = themed { Padding(gap.xs) }
        backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.25f) }
        radius = themed { radius.lg }

        selectionIndicator = box {
            alpha = 0f
            height = Dimension.FillAvailable
            backgroundColor = themed { palette.accent.shade(0.2f) }
            radius = themed { radius.lg }
        }

        buttonsContainer = linearContainer {
            width = Dimension.FillAvailable
            spacing = themed { gap.xs }
        }
    }


    /**
     * Adds a tab, calling [onSelect] when the user selects it.
     */
    fun addButton(name: String, onSelect: () -> Unit) {
        buttonsContainer += UITextButton().apply {
            width = Dimension.FillAvailable
            text = name
            alignment = Anchor.Center
            density = UIButton.Density.Small
            variant = UIButton.Variant.Ghost
            onActionUp = {
                selectedButton = this
                onSelect()
            }
        }
    }


    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime
        val button = selectedButton

        selectionIndicator.apply {
            if (button != null) {
                alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), alpha, 1f, 0f, 0.1f, Easing.None)

                indicatorWidth = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), indicatorWidth, button.bounds.width, 0f, 0.1f, Easing.None)
                indicatorX = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), indicatorX, button.bounds.x, 0f, 0.2f, Easing.None)

                width = indicatorWidth.px
                x = indicatorX.px
            } else {
                alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), alpha, 0f, 0f, 0.1f, Easing.None)
            }
        }

        super.onUpdate(clock)
    }
}
