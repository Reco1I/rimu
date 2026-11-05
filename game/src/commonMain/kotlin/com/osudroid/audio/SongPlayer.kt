package com.osudroid.audio

import com.reco1l.verktex.Platform
import com.reco1l.verktex.audio.AudioStream
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.pow
import kotlin.time.TimeSource

/**
 * Plays the current song.
 */
class SongPlayer {

    private val lock = SynchronizedObject()

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
    private val timeOrigin = TimeSource.Monotonic.markNow()
    private var lastFFTResolution = -1


    fun preLoad(filePath: String, speed: Float, adjustPitch: Boolean, isLoop: Boolean): Boolean {
        return synchronized(lock) {
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
    }

    fun play(): Boolean {
        return synchronized(lock) {
            val stream = stream ?: return false

            // A paused song continues from where it was, otherwise it starts over.
            stream.play(restart = !stream.isPaused)
            setVolume(Config.getBgmVolume())
            return true
        }
    }

    fun pause(): Boolean {
        return synchronized(lock) {
            val stream = stream ?: return false
            stream.pause()
            return true
        }
    }

    fun stop(): Boolean {
        return synchronized(lock) {
            if (stream == null) {
                return false
            }

            release()
            return true
        }
    }

    fun jump(ms: Int): Boolean {
        return synchronized(lock) {
            val stream = stream ?: return false

            if (ms <= 0) {
                return false
            }

            stream.position = ms / 1000.0
            return true
        }
    }

    fun getStatus(): Status {
        return synchronized(lock) {
            val stream = stream ?: return Status.STOPPED

            return when {
                stream.isPlaying -> Status.PLAYING
                stream.isPaused -> Status.PAUSED
                stream.isStalled -> Status.STALLED
                else -> Status.STOPPED
            }
        }
    }

    fun getPosition(): Double = synchronized(lock) { (stream?.position ?: 0.0) * 1000 }

    fun getLength(): Int = synchronized(lock) { ((stream?.length ?: 0.0) * 1000).toInt() }

    fun getSpectrum(): FloatArray? = synchronized(lock) { stream?.readFFT() }

    fun getAudioFFT(resolution: Int): FloatArray? {
        return synchronized(lock) {
            val currentTime = timeOrigin.elapsedNow().inWholeMilliseconds
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
    }

    fun getSpeed() = speed

    fun setSpeed(speed: Float) {
        synchronized(lock) {
            this.speed = speed
            stream?.speed = speed
        }
    }

    fun setAdjustPitch(adjustPitch: Boolean) {
        synchronized(lock) {
            this.adjustPitch = adjustPitch
            stream?.adjustPitch = adjustPitch
        }
    }

    fun setPitchRate(pitchRate: Float) {
        synchronized(lock) {
            this.pitchRate = pitchRate
            stream?.pitch = pitchRate
        }
    }

    fun setFrequencyForcefully(frequency: Float) {
        synchronized(lock) {
            stream?.frequency = frequency
        }
    }

    fun getFrequency(): Float = synchronized(lock) { stream?.frequency ?: 0f }

    fun getVolume(): Float = synchronized(lock) { stream?.volume ?: 0f }

    fun setVolume(volume: Float) {
        synchronized(lock) {
            val stream = stream ?: return

            // Some callers set the volume every frame, so skip the native call when it is unchanged.
            if (volume != lastVolume) {
                lastVolume = volume
                stream.volume = volume
            }
        }
    }

    fun setGaming(isGaming: Boolean) {
        synchronized(lock) {
            this.isGaming = isGaming
        }
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
