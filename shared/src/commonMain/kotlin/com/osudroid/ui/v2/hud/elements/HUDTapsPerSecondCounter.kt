package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.GameplayState

class HUDTapsPerSecondCounter : HUDStatisticCounter("Taps/sec") {

    override val name = "Taps per second counter"

    private val timestamps = ArrayDeque<Float>(50)

    override fun onGameplayTouchDown(time: Float) {
        timestamps.add(time)
    }

    override fun onSeek() {
        timestamps.clear()
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        val earliestElapsedTime = game.elapsedTime - game.context.timing.speedMultiplier

        while (timestamps.isNotEmpty() && timestamps.first() < earliestElapsedTime) {
            timestamps.removeFirst()
        }

        displayedValue = timestamps.size
    }
}