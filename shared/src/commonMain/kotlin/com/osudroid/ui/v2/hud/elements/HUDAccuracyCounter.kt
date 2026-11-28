package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.time.FrameClock

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.reco1l.verktex.ui.text.TextAlign
import com.rian.framework.RollingFloatCounter
import com.osudroid.ui.v2.hud.GameplayState
import com.reco1l.verktex.localization.formatString
import kotlin.math.roundToInt

class HUDAccuracyCounter : HUDElement() {

    private val sprite = SkinPieces.current.create(SkinComponent.ScoreFont).apply {
        useFixedDigitWidth()

        measureText = "100.00%"
        text = "100.00%"
    }

    // In hundredths of a percent, as shown: 0.95 is 9500, 95.00%.
    private fun format(hundredths: Int) = formatString("%.2f%%", hundredths / 100f)

    // Formatted only when what's shown changes, not on every frame the counter rolls.
    private var value = 10000
        set(value) {
            if (field != value) {
                field = value
                sprite.text = format(value)
            }
        }

    private val counter = RollingFloatCounter(1f).apply { rollingDuration = 1f }

    init {
        +sprite
    }

    override fun setSkinData(data: HUDElementSkinData?) {
        super.setSkinData(data)

        sprite.textAlign = when {
            anchor.x < 0.5f -> TextAlign.Left
            anchor.x > 0.5f -> TextAlign.Right
            else -> TextAlign.Center
        }
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        counter.targetValue = game.stat.accuracy
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        value = (counter.currentValue * 10000f).roundToInt()

        super.onUpdate(clock)
    }
}