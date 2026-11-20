package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.time.FrameClock

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.rian.framework.RollingLongCounter
import com.osudroid.ui.v2.hud.GameplayState
import ru.nsu.ccfit.zuev.skins.OsuSkin
import com.reco1l.verktex.localization.formatString

class HUDScoreCounter : HUDElement() {

    private val sprite = SkinPieces.current.create(SkinComponent.ScoreFont)
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

        sprite.useFixedDigitWidth()
        sprite.text = format(0)
        +sprite
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        counter.targetValue = game.stat.totalScoreWithMultiplier
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        value = counter.currentValue

        super.onUpdate(clock)
    }
}