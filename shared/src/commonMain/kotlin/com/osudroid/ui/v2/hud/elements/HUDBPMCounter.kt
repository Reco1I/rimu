package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.time.FrameClock

import com.rian.framework.RollingIntCounter
import kotlin.math.roundToInt
import com.osudroid.ui.v2.hud.GameplayState

class HUDBPMCounter : HUDStatisticCounter("BPM") {

    override val name = "BPM counter"

    private val counter = RollingIntCounter(0).apply { rollingDuration = 0.375f }

    init {
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        val beatmap = game.playableBeatmap ?: return

        val timingPoint = beatmap.controlPoints.timing.controlPointAt(game.elapsedTime * 1000.0)

        // The speed multiplier accounts for rate set by the replay controls, so it is not a representative of
        // the actual playback rate. We need to divide by the replay playback rate to get the correct BPM.
        val replayPlaybackRate = game.replayControls?.rate ?: 1f
        val bpm = timingPoint.bpm * game.context.timing.speedMultiplier / replayPlaybackRate

        counter.targetValue = bpm.roundToInt()
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        displayedValue = counter.currentValue

        super.onUpdate(clock)
    }
}