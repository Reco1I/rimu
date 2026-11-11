package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.time.FrameClock

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import com.rian.framework.RollingLongCounter
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import com.reco1l.verktex.localization.formatString

class HUDScoreCounter : HUDElement() {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)
    // Eight digits, padded with zeros.
    private fun format(value: Long) = formatString("%08d", value)

    private var value = 0L
        set(value) {
            if (field != value) {
                field = value
                sprite.text = format(value)
            }
        }

    private val counter = RollingLongCounter(0L).apply { rollingDuration = 1f }

    init {
        sprite.spacing = -OsuSkin.get().scoreOverlap

        val digitRange = '0'..'9'
        val maxDigitWidth = digitRange.maxOfOrNull { sprite.characters[it]?.width?.toFloat() ?: 0f } ?: 0f

        sprite.fixedCharWidths = digitRange.associateWith { maxDigitWidth }
        sprite.text = format(0)
        +sprite
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        counter.targetValue = game.stat.totalScoreWithMultiplier
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        value = counter.currentValue

        super.onUpdate(clock)
    }
}