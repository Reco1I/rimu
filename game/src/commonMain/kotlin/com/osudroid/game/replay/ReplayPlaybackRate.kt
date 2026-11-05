package com.osudroid.game.replay

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.form.FormSlider
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.toFixed

class ReplayPlaybackRate : UILinearContainer() {
    /**
     * The rate at which gameplay should progress.
     */
    var rate = 1f
        private set


    init {
        orientation = Orientation.Vertical
        width = Dimension.FillAvailable

        val slider = FormSlider(rate).apply {
            label = "Playback speed"
            control.min = 0.05f
            control.max = 2f
            valueFormatter = { it.toFixed(2) + "x" }
            onValueChanged = { rate = it }
        }

        +slider

        linearContainer {
            spacing = 10f.dip
            padding = Padding(0f.dip, 16f.dip)
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter

            fun addStepButton(step: Float) = textButton {
                text = (if (step >= 0) "+" else "") + step.toFixed(2)
                height = 42f.dip
                padding = Padding(12f.dip, 0f.dip)
                onActionUp = { slider.value += step }
            }

            addStepButton(-0.05f)
            addStepButton(-0.01f)
            addStepButton(0.01f)
            addStepButton(0.05f)
        }
    }
}