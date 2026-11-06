package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.data.px

import com.reco1l.verktex.data.dip

import com.reco1l.verktex.time.FrameClock

import com.osudroid.ui.v2.SpriteFont
import com.reco1l.framework.*
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.text.UIText
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.*

@Suppress("LeakingThis")
sealed class HUDStatisticCounter(label: String) : HUDElement() {

    protected val labelText = UIText().apply {
        fontSize = 14f.dip
        text = label
    }

    protected val valueText = SpriteFont(OsuSkin.get().scorePrefix)

    /**
     * The integer value displayed by [valueText].
     *
     * [valueText] is only updated when this changes, so that a new [String] is not allocated every frame.
     */
    protected var displayedValue = Int.MIN_VALUE
        set(value) {
            if (field != value) {
                field = value
                valueText.text = value.toString()
            }
        }

    init {
        +labelText
        +valueText
    }

    override fun onUpdate(clock: FrameClock) {

        valueText.y = labelText.measuredSize.y.px

        val innerAnchor = when (anchor.x) {
            0f -> Anchor.TopLeft
            0.5f -> Anchor.TopCenter
            else -> Anchor.TopRight
        }

        labelText.anchor = innerAnchor
        labelText.origin = innerAnchor
        valueText.anchor = innerAnchor
        valueText.origin = innerAnchor

        super.onUpdate(clock)
    }
}

sealed class HUDHitStatisticCounter(label: String, tint: Color4) : HUDStatisticCounter(label) {

    init {
        labelText.color = tint
        valueText.color = tint
    }

    /**
     * Obtains the value to display from [statistics].
     *
     * This is an abstract function rather than a lambda so that the returned [Int] is not boxed every frame.
     */
    protected abstract fun getValue(statistics: StatisticV2): Int

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        displayedValue = getValue(game.stat)
    }
}

class HUDGreatCounter : HUDHitStatisticCounter("Great", Color4(0xFF46b4dc)) {
    override fun getValue(statistics: StatisticV2) = statistics.hit300
}

class HUDOkCounter : HUDHitStatisticCounter("Ok", Color4(0xFF64DC28)) {
    override fun getValue(statistics: StatisticV2) = statistics.hit100
}

class HUDMehCounter : HUDHitStatisticCounter("Meh", Color4(0xFFc8b46e)) {
    override fun getValue(statistics: StatisticV2) = statistics.hit50
}

class HUDMissCounter : HUDHitStatisticCounter("Miss", Color4(0xFFFF0000)) {
    override fun getValue(statistics: StatisticV2) = statistics.misses
}