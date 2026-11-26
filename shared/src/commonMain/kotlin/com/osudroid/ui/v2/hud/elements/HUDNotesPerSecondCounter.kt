package com.osudroid.ui.v2.hud.elements

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.ui.v2.hud.GameplayState

class HUDNotesPerSecondCounter : HUDStatisticCounter("Notes/sec") {

    override val name = "Notes per second counter"

    private val objects = ArrayDeque<HitObject>()

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        objects.add(obj)
    }

    override fun onSeek() {
        objects.clear()
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        val elapsedTimeMs = game.elapsedTime * 1000

        while (objects.isNotEmpty()) {
            val obj = objects.first()

            if (obj.startTime - obj.timePreempt + 1000 * game.context.timing.speedMultiplier >= elapsedTimeMs) {
                break
            }

            objects.removeFirst()
        }

        displayedValue = objects.size
    }
}