package com.reco1l.verktex.time

import com.reco1l.verktex.audio.AudioPlayback

/**
 * A clock that is linked to an audio playback.
 */
class AudioPlaybackClock(private val audioPlayback: AudioPlayback) : Clock(), IAdjustableClock {

    override val time: Float
        get() = audioPlayback.position.toFloat()

    override val isRunning: Boolean
        get() = audioPlayback.isPlaying

    override var rate
        get() = audioPlayback.speed
        set(value) {
            audioPlayback.speed = value
        }

    override fun start() = audioPlayback.play()

    override fun pause() = audioPlayback.pause()

    override fun reset() {
        audioPlayback.position = 0.0
        audioPlayback.stop()
    }

    override fun seek(to: Float): Boolean {
        if (to < 0f || to > audioPlayback.length) {
            return false
        }

        audioPlayback.position = to.toDouble()
        return true
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }
}
