package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.time.FrameClock

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.osudroid.ui.v2.SpriteFont
import com.reco1l.verktex.ui.text.TextAlign
import com.rian.framework.RollingFloatCounter
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import com.reco1l.verktex.localization.formatString

class HUDAccuracyCounter : HUDElement() {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix).apply {
        val digitRange = '0'..'9'
        val maxDigitWidth = digitRange.maxOfOrNull { characters[it]?.width?.toFloat() ?: 0f } ?: 0f

        fixedCharWidths = buildMap {
            for (c in digitRange) {
                put(c, maxDigitWidth)
            }

            characters['.']?.let { put('.', it.width.toFloat()) }
            characters['%']?.let { put('%', it.width.toFloat()) }
        }

        measureText = "100.00%"
        text = "100.00%"
    }

    // 0.95 is 95.00%.
    private fun format(value: Float) = formatString("%.2f%%", value * 100f)

    private var value = 0f
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

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        counter.targetValue = gameScene.stat.accuracy
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        value = counter.currentValue

        super.onUpdate(clock)
    }
}