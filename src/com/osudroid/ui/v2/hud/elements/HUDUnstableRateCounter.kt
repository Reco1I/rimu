package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.ui.text.UIText
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.verktex.theme.FontSize
import java.text.DecimalFormat
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDUnstableRateCounter : HUDElement() {

    override val name = "Unstable rate counter"

    private val format = DecimalFormat("UR: 0.00")

    private val text = UIText().apply {
        fontSize = FontSize.SM
        text = format.format(0.0)
    }

    private var value = 0.0
        set(value) {
            if (field != value) {
                field = value
                text.text = format.format(value)
            }
        }

    init {
        attachChild(text)
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        value = game.stat.unstableRate
    }
}