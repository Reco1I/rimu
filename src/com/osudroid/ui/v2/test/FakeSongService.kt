package com.osudroid.ui.v2.test

import kotlin.math.abs
import kotlin.math.sin
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService

/**
 * A song service that plays nothing but keeps the position as if it did, so the music controls,
 * the visualizers and the beat of the menus work in the UI test browser without audio files.
 *
 * Any song "loads", and every song is [SONG_LENGTH] long.
 */
class FakeSongService : SongService() {

    private var status = Status.STOPPED
    private var isGaming = false
    private var speed = 1f
    private var volume = 1f

    // Position in milliseconds at the last change, and the time it was taken at.
    private var basePosition = 0.0
    private var baseNanos = System.nanoTime()


    override fun preLoad(filePath: String?, speed: Float, adjustPitch: Boolean, isLoop: Boolean): Boolean {
        status = Status.STOPPED
        this.speed = speed
        setPosition(0.0)
        return true
    }

    override fun play() {
        setPosition(positionPrecise)
        status = Status.PLAYING
    }

    override fun pause() {
        if (status == Status.PLAYING) {
            setPosition(positionPrecise)
            status = Status.PAUSED
        }
    }

    override fun stop(): Boolean {
        status = Status.STOPPED
        setPosition(0.0)
        return true
    }

    override fun seekTo(time: Int) = setPosition(time.toDouble())

    override fun getStatus(): Status {
        // Stops at the end, as a song would.
        if (status == Status.PLAYING && elapsedPosition() >= SONG_LENGTH) {
            stop()
        }
        return status
    }

    override fun getPositionPrecise(): Double =
        if (status == Status.PLAYING) elapsedPosition().coerceAtMost(SONG_LENGTH.toDouble()) else basePosition

    override fun getLength() = SONG_LENGTH

    override fun getSpectrum() = FloatArray(512) { level(it, 512) }

    override fun getAudioFFT(resolution: Int) = FloatArray(resolution) { level(it, resolution) }

    override fun isGaming() = isGaming

    override fun setGaming(isGaming: Boolean) {
        this.isGaming = isGaming
    }

    override fun getVolume() = volume

    override fun setVolume(volume: Float) {
        this.volume = volume
    }

    override fun getSpeed() = speed

    override fun setSpeed(speed: Float) {
        setPosition(positionPrecise)
        this.speed = speed
    }

    override fun setAdjustPitch(adjustPitch: Boolean) = Unit

    override fun setPitchRate(pitchRate: Float) = Unit

    override fun setFrequencyForcefully(frequency: Float) = Unit

    override fun getFrequency() = DEFAULT_FREQUENCY

    override fun checkFileExist(path: String?) = true

    override fun isRunningForeground() = true

    override fun exit(): Boolean {
        stop()
        return true
    }


    private fun setPosition(position: Double) {
        basePosition = position.coerceIn(0.0, SONG_LENGTH.toDouble())
        baseNanos = System.nanoTime()
    }

    private fun elapsedPosition() = basePosition + (System.nanoTime() - baseNanos) / 1_000_000.0 * speed

    // A made up spectrum moving with the position, louder in the bass as music usually is.
    private fun level(index: Int, count: Int): Float {
        if (status != Status.PLAYING) {
            return 0f
        }
        val time = positionPrecise / 1000.0
        val wave = abs(sin(time * 4.0 + index * 0.35) * sin(time * 1.3 + index * 0.11)).toFloat()
        return wave * (1f - index.toFloat() / count) * 0.6f
    }


    companion object {
        const val SONG_LENGTH = 120_000
        private const val DEFAULT_FREQUENCY = defaultFrequency.toFloat()
    }
}
