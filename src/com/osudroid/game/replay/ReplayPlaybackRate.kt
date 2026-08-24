package com.osudroid.game.replay

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.linearContainer
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.form.FormSlider
import com.reco1l.verktex.data.Vec4
import java.text.DecimalFormat

class ReplayPlaybackRate : UILinearContainer() {
    /**
     * The rate at which gameplay should progress.
     */
    var rate = 1f
        private set

    private val rateFormatter = DecimalFormat("0.00x")

    init {
        orientation = Orientation.Vertical
        width = Dimension.FillAvailable

        val slider = FormSlider(rate).apply {
            label = "Playback speed"
            control.min = 0.05f
            control.max = 2f
            valueFormatter = { rateFormatter.format(it) }
            onValueChanged = { rate = it }
        }

        +slider

        linearContainer {
            spacing = 10f
            padding = Vec4(0f, 16f)
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter

            fun addStepButton(step: Float) = textButton {
                text = "%+.2f".format(step)
                height = 42f
                padding = Vec4(12f, 0f)
                onActionUp = { slider.value += step }
            }

            addStepButton(-0.05f)
            addStepButton(-0.01f)
            addStepButton(0.01f)
            addStepButton(0.05f)
        }
    }
}