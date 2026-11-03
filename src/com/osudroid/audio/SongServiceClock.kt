package com.osudroid.audio

import com.reco1l.verktex.time.AdjustableClock
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService

/**
 * A clock following the song being played by a [SongService].
 */
class SongServiceClock(private val songService: SongService) : AdjustableClock() {

    override val time
        get() = (songService.getPositionPrecise() / 1000.0).toFloat()

    override var rate by songService::speed

    override val isRunning
        get() = songService.getStatus() == Status.PLAYING

    override fun reset() {
        songService.seekTo(0)
        songService.stop()
    }

    override fun start() {
        songService.play()
    }

    override fun pause() {
        songService.pause()
    }

    override fun seek(to: Float): Boolean {
        val trackLength = songService.getLength() / 1000f

        // Avoid allocation
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        if (to < 0f || to > trackLength) {
            return false
        }

        songService.seekTo((to * 1000).toInt())
        return true
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }
}
