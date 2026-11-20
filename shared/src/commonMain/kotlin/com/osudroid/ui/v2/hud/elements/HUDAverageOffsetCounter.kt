package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.data.px

import com.reco1l.verktex.ui.text.Text
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.GameplayState
import kotlin.math.roundToInt

class HUDAverageOffsetCounter : HUDElement() {

    override val name = "Average offset counter"

    private val text = Text().apply {
        fontSize = 21f.px
        text = "Avg offset: 0ms"
    }

    private var value = 0.0
        set(value) {
            if (field != value) {
                field = value
                text.text = "Avg offset: ${value.roundToInt()}ms"
            }
        }

    init {
        +text
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        value = game.stat.averageHitOffset
    }
}