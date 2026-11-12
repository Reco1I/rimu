package com.osudroid.ui.v2

import com.osudroid.OsuGame
import com.reco1l.verktex.data.Background
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.data.WinCondition.Accuracy
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.text.UIText
import kotlin.math.hypot
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import com.reco1l.verktex.localization.formatString
import com.osudroid.utils.formatNumber

/**
 * The list of scores of a multiplayer match shown in the results screen.
 */
class StatisticSelector(
    private val scoring: ScoringScene,
    stats: Array<StatisticV2>?
) : UIScrollableContainer() {

    private val selected: StatisticV2? = scoring.currentStatistic


    init {
        scrollAxes = Axis.Y
        width = 140f.px
        height = Dimension.FillAvailable
        x = 570f.px
        showVerticalIndicator = false

        +UILinearContainer().apply {
            orientation = UILinearContainer.Orientation.Vertical
            width = Dimension.FillAvailable

            stats?.forEachIndexed { index, stat -> +BoardItem(index, stat) }
        }
    }


    private fun select(stat: StatisticV2) {
        if (stat == selected) {
            return
        }


        scoring.load(stat, scoring.beatmapInfo, OsuGame.music, null, null, null)
    }


    private inner class BoardItem(index: Int, private val stat: StatisticV2) : UIContainer() {

        private var pressX = 0f
        private var pressY = 0f
        private var isPressed = false


        init {
            val isSelected = stat.playerName == selected?.playerName

            width = Dimension.FillAvailable
            height = 100f.px
            padding = Padding(10f.dip, 4f.dip)

            background = Background.Color(when {
                stat.isAlive && isSelected -> Color4(0.5f, 0.5f, 1f, 0.9f)
                stat.isAlive -> Color4(0.5f, 0.5f, 0.5f, 0.9f)
                isSelected -> Color4(1f, 0.4f, 0.4f, 0.9f)
                else -> Color4(1f, 0.6f, 0.6f, 0.9f)
            })

            +UIText().apply {
                color = if (stat.isAlive) Color4(0.6f, 0.6f, 0.6f, 0.9f) else Color4(1f, 0.6f, 0.6f, 0.9f)
                text = "#${index + 1}"
                anchor = com.reco1l.verktex.data.Anchor.CenterRight
                origin = com.reco1l.verktex.data.Anchor.CenterRight
            }

            +UIText().apply {
                color = if (stat.isAlive) Color4(0.9f, 0.9f, 0.9f) else Color4(1f, 0.8f, 0.8f)
                text = listOf(
                    stat.playerName,
                    formatNumber(stat.totalScoreWithMultiplier),
                    when (Multiplayer.room?.winCondition) {
                        Accuracy -> formatString("%2.2f%%", stat.accuracy * 100f)
                        else -> "${formatNumber(stat.scoreMaxCombo)}x"
                    }
                ).joinToString("\n")
            }
        }


        override fun onInputEvent(event: InputEvent): Boolean {
            if (event !is PointerEvent) {
                return false
            }

            when (event.action) {
                PointerEvent.Action.Down -> {
                    isPressed = containsScenePoint(event.x, event.y)
                    pressX = event.x
                    pressY = event.y
                }

                PointerEvent.Action.Move -> {
                    if (hypot(event.x - pressX, event.y - pressY) > 50f) {
                        isPressed = false
                    }
                }

                PointerEvent.Action.Up -> {
                    if (isPressed && containsScenePoint(event.x, event.y)) {
                        select(stat)
                    }

                    isPressed = false
                }

                else -> isPressed = false
            }

            // Moves must reach the scrollable container.
            return false
        }
    }
}
