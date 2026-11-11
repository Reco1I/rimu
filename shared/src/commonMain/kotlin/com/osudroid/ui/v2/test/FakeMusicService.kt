package com.osudroid.ui.v2.test

import com.osudroid.audio.MusicService
import kotlin.math.abs
import kotlin.math.sin
import ru.nsu.ccfit.zuev.audio.Status
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * A music service that plays nothing but keeps the position as if it did, so the music controls,
 * the visualizers and the beat of the menus work in the UI test browser without audio files.
 *
 * Any song "loads", and every song is [SONG_LENGTH] long.
 */
class FakeMusicService : MusicService() {

    private var currentStatus = Status.STOPPED

    // Position in milliseconds at the last change, and the time it was taken at.
    private var basePosition = 0.0
    private var baseMark = TimeSource.Monotonic.markNow()


    override fun preLoad(filePath: String?, speed: Float, adjustPitch: Boolean, isLoop: Boolean): Boolean {
        currentStatus = Status.STOPPED
        this.speed = speed
        setPosition(0.0)
        return true
    }

    override fun startStream() {
        setPosition(positionPrecise)
        currentStatus = Status.PLAYING
    }

    override fun pauseStream() {
        if (currentStatus == Status.PLAYING) {
            setPosition(positionPrecise)
            currentStatus = Status.PAUSED
        }
    }

    override fun stopStream() {
        currentStatus = Status.STOPPED
        setPosition(0.0)
    }

    override fun seekTo(time: Int) = setPosition(time.toDouble())

    override val status: Status
        get() {
            // Stops at the end, as a song would.
            if (currentStatus == Status.PLAYING && elapsedPosition() >= SONG_LENGTH) {
                stopStream()
            }
            return currentStatus
        }

    override val positionPrecise: Double
        get() = if (currentStatus == Status.PLAYING) elapsedPosition().coerceAtMost(SONG_LENGTH.toDouble()) else basePosition

    override val length get() = SONG_LENGTH

    override val spectrum get() = FloatArray(512) { level(it, 512) }

    override fun getAudioFFT(resolution: Int) = FloatArray(resolution) { level(it, resolution) }

    override var isGaming = false

    override var volume = 1f

    override var speed = 1f
        set(speed) {
            setPosition(positionPrecise)
            field = speed
        }

    override fun setAdjustPitch(adjustPitch: Boolean) = Unit

    override fun setPitchRate(pitchRate: Float) = Unit

    override fun setFrequencyForcefully(frequency: Float) = Unit

    override val frequency get() = DEFAULT_FREQUENCY.toFloat()


    private fun setPosition(position: Double) {
        basePosition = position.coerceIn(0.0, SONG_LENGTH.toDouble())
        baseMark = TimeSource.Monotonic.markNow()
    }

    private fun elapsedPosition() = basePosition + baseMark.elapsedNow().toDouble(DurationUnit.MILLISECONDS) * speed

    // A made up spectrum moving with the position, louder in the bass as music usually is.
    private fun level(index: Int, count: Int): Float {
        if (currentStatus != Status.PLAYING) {
            return 0f
        }
        val time = positionPrecise / 1000.0
        val wave = abs(sin(time * 4.0 + index * 0.35) * sin(time * 1.3 + index * 0.11)).toFloat()
        return wave * (1f - index.toFloat() / count) * 0.6f
    }


    companion object {
        const val SONG_LENGTH = 120_000
    }
}
