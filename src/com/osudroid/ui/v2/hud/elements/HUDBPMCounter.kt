package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.time.FrameClock

import com.rian.framework.RollingIntCounter
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDBPMCounter : HUDStatisticCounter("BPM") {

    override val name = "BPM counter"

    private val counter = RollingIntCounter(0).apply { rollingDuration = 0.375f }

    init {
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        val beatmap = gameScene.playableBeatmap ?: return
        val replaySettingsPanel = gameScene.replaySettingsPanel

        val timingPoint = beatmap.controlPoints.timing.controlPointAt(gameScene.elapsedTime * 1000.0)

        // GameHelper.getSpeedMultiplier accounts for rate set by ReplaySettingsPanel, so it is not a representative of
        // the actual playback rate. We need to divide by the replay playback rate to get the correct BPM.
        val replayPlaybackRate = (replaySettingsPanel?.playbackControl?.rateControl?.rate ?: 1f)
        val bpm = timingPoint.bpm * GameHelper.getSpeedMultiplier() / replayPlaybackRate

        counter.targetValue = bpm.roundToInt()
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        displayedValue = counter.currentValue

        super.onUpdate(clock)
    }
}