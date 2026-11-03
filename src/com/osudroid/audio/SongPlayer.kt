package com.osudroid.audio

import com.reco1l.verktex.Platform
import com.reco1l.verktex.audio.AudioStream
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import kotlin.math.pow

/**
 * Plays the current song.
 */
class SongPlayer {

    private var stream: AudioStream? = null

    private var isLooping = false
    private var isGaming = false

    private var speed = 1f
    private var pitchRate = 1f
    private var adjustPitch = false

    /**
     * The last volume applied to the stream, or NaN if none has been applied to the current one yet.
     */
    private var lastVolume = Float.NaN

    private var cachedFFT: FloatArray? = null
    private var lastFFTUpdateTime = 0L
    private var lastFFTResolution = -1


    @Synchronized
    fun preLoad(filePath: String, speed: Float, adjustPitch: Boolean, isLoop: Boolean): Boolean {
        isLooping = isLoop
        release()

        val newStream = try {
            Platform.provider.createAudioStreamSource(filePath, true)?.createStream()
        } catch (_: IllegalStateException) {
            null
        }

        if (newStream == null) {
            this.speed = 1f
            this.adjustPitch = false
            return false
        }

        stream = newStream
        lastVolume = Float.NaN
        pitchRate = 1f

        newStream.looping = isLoop
        newStream.onEnd = {
            if (isGaming) {
                stop()
            }
        }

        setSpeed(speed)
        setAdjustPitch(adjustPitch)
        return true
    }

    @Synchronized
    fun play(): Boolean {
        val stream = stream ?: return false

        // A paused song continues from where it was, otherwise it starts over.
        stream.play(restart = !stream.isPaused)
        setVolume(Config.getBgmVolume())
        return true
    }

    @Synchronized
    fun pause(): Boolean {
        val stream = stream ?: return false
        stream.pause()
        return true
    }

    @Synchronized
    fun stop(): Boolean {
        if (stream == null) {
            return false
        }

        release()
        return true
    }

    @Synchronized
    fun jump(ms: Int): Boolean {
        val stream = stream ?: return false

        if (ms <= 0) {
            return false
        }

        stream.position = ms / 1000.0
        return true
    }

    @Synchronized
    fun getStatus(): Status {
        val stream = stream ?: return Status.STOPPED

        return when {
            stream.isPlaying -> Status.PLAYING
            stream.isPaused -> Status.PAUSED
            stream.isStalled -> Status.STALLED
            else -> Status.STOPPED
        }
    }

    @Synchronized
    fun getPosition(): Double = (stream?.position ?: 0.0) * 1000

    @Synchronized
    fun getLength(): Int = ((stream?.length ?: 0.0) * 1000).toInt()

    @Synchronized
    fun getSpectrum(): FloatArray? = stream?.readFFT()

    @Synchronized
    fun getAudioFFT(resolution: Int): FloatArray? {
        val currentTime = System.currentTimeMillis()
        val cached = cachedFFT

        if (cached != null && lastFFTResolution == resolution && currentTime - lastFFTUpdateTime < FFT_UPDATE_INTERVAL_MS) {
            return cached
        }

        val rawFFT = getSpectrum()

        if (rawFFT == null || resolution <= 0) {
            return null
        }

        val fft = FloatArray(resolution)
        val fftSize = rawFFT.size

        var l = 0
        for (i in 0 until resolution) {

            // We use logarithmic for better visual representation of audio spectrum in lower frequencies
            var r = 2.0.pow(i * 9.0 / (resolution - 1)).toInt()

            if (r <= l) {
                r = l + 1
            }
            if (r > fftSize - 1) {
                r = fftSize - 1
            }

            var peak = 0f
            for (j in l until r) {
                if (rawFFT[j] > peak) {
                    peak = rawFFT[j]
                }
            }

            fft[i] = peak
            l = r
        }

        cachedFFT = fft
        lastFFTUpdateTime = currentTime
        lastFFTResolution = resolution

        return fft
    }

    fun getSpeed() = speed

    @Synchronized
    fun setSpeed(speed: Float) {
        this.speed = speed
        stream?.speed = speed
    }

    @Synchronized
    fun setAdjustPitch(adjustPitch: Boolean) {
        this.adjustPitch = adjustPitch
        stream?.adjustPitch = adjustPitch
    }

    @Synchronized
    fun setPitchRate(pitchRate: Float) {
        this.pitchRate = pitchRate
        stream?.pitch = pitchRate
    }

    @Synchronized
    fun setFrequencyForcefully(frequency: Float) {
        stream?.frequency = frequency
    }

    @Synchronized
    fun getFrequency(): Float = stream?.frequency ?: 0f

    @Synchronized
    fun getVolume(): Float = stream?.volume ?: 0f

    @Synchronized
    fun setVolume(volume: Float) {
        val stream = stream ?: return

        // Some callers set the volume every frame, so skip the native call when it is unchanged.
        if (volume != lastVolume) {
            lastVolume = volume
            stream.volume = volume
        }
    }

    @Synchronized
    fun setGaming(isGaming: Boolean) {
        this.isGaming = isGaming
    }

    private fun release() {
        stream?.dispose()
        stream = null

        cachedFFT = null
        lastFFTUpdateTime = 0
        lastFFTResolution = -1
    }


    private companion object {
        const val FFT_UPDATE_INTERVAL_MS = 96L
    }
}
