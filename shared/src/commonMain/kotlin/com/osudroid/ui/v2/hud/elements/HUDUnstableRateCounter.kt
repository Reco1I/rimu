package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.data.px

import com.reco1l.verktex.ui.text.Text
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.GameplayState
import com.reco1l.verktex.localization.formatString

class HUDUnstableRateCounter : HUDElement() {

    override val name = "Unstable rate counter"

    private fun format(value: Double) = formatString("UR: %.2f", value)

    private val text = Text().apply {
        fontSize = 21f.px
        text = format(0.0)
    }

    private var value = 0.0
        set(value) {
            if (field != value) {
                field = value
                text.text = format(value)
            }
        }

    init {
        +text
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        value = game.stat.unstableRate
    }
}