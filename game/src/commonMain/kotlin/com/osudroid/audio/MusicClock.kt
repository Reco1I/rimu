package com.osudroid.audio

import com.reco1l.verktex.time.AdjustableClock
import ru.nsu.ccfit.zuev.audio.Status

/**
 * A clock following the song being played by a [MusicService].
 */
class MusicClock(private val music: MusicService) : AdjustableClock() {

    override val time
        get() = (music.positionPrecise / 1000.0).toFloat()

    override var rate by music::speed

    override val isRunning
        get() = music.status == Status.PLAYING

    override fun reset() {
        music.seekTo(0)
        music.stop()
    }

    override fun start() {
        music.play()
    }

    override fun pause() {
        music.pause()
    }

    override fun seek(to: Float): Boolean {
        val trackLength = music.length / 1000f

        // Avoid allocation
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        if (to < 0f || to > trackLength) {
            return false
        }

        music.seekTo((to * 1000).toInt())
        return true
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }
}
