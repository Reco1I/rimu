package com.osudroid.audio

import com.osudroid.settings.OsuSettings
import com.osudroid.BeatmapSelection
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.audio.AudioStream
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.math.pow
import kotlin.time.TimeSource
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.audio.Status

/**
 * Plays the music of the game, and knows which beatmap it belongs to.
 *
 * The song is an [AudioStream] that any thread can control. The stream primitives ([startStream],
 * [pauseStream], [stopStream] and the properties) are open so a test can stand in for the audio, and
 * [onPlaybackStateChanged] lets a platform follow the playback, e.g. with a media session.
 */
open class MusicService {

    /**
     * Where [next] and [previous] take beatmap sets from.
     */
    interface Playlist {
        fun nextBeatmapSet(): BeatmapSetInfo?
        fun previousBeatmapSet(): BeatmapSetInfo?
    }


    /**
     * The beatmap whose song is played: the [BeatmapSelection], which [load] sets. Setting it alone
     * doesn't load its song, e.g. when the song is already playing.
     */
    var currentBeatmap: BeatmapInfo?
        get() = BeatmapSelection.beatmap
        set(value) {
            BeatmapSelection.beatmap = value
        }

    /**
     * Where [next] and [previous] take beatmap sets from, nothing is played without it.
     */
    var playlist: Playlist? = null

    /**
     * Whether the song is played by gameplay, which stops it at its end instead of playing the next.
     */
    open var isGaming = false
        set(value) {
            synchronized(lock) { field = value }
            Logger.w("MusicService", "Gaming mode: $value")
        }

    /**
     * The status of the song.
     */
    open val status: Status
        get() = synchronized(lock) {
            val stream = stream ?: return Status.STOPPED

            when {
                stream.isPlaying -> Status.PLAYING
                stream.isPaused -> Status.PAUSED
                stream.isStalled -> Status.STALLED
                else -> Status.STOPPED
            }
        }

    /**
     * Whether the song is playing.
     */
    val isPlaying
        get() = status == Status.PLAYING

    /**
     * The position of the song in milliseconds.
     */
    val position: Int
        get() = positionPrecise.toInt()

    /**
     * The position of the song in milliseconds, with its fraction.
     */
    open val positionPrecise: Double
        get() = synchronized(lock) { (stream?.position ?: 0.0) * 1000 }

    /**
     * The length of the song in milliseconds.
     */
    open val length: Int
        get() = synchronized(lock) { ((stream?.length ?: 0.0) * 1000).toInt() }

    /**
     * The raw FFT of what is playing now.
     */
    open val spectrum: FloatArray?
        get() = synchronized(lock) { stream?.readFFT() }

    open var volume: Float
        get() = synchronized(lock) { stream?.volume ?: 0f }
        set(value) {
            synchronized(lock) {
                val stream = stream ?: return

                // Some callers set the volume every frame, so skip the native call when it is unchanged.
                if (value != lastVolume) {
                    lastVolume = value
                    stream.volume = value
                }
            }
        }

    open var speed: Float
        get() = synchronized(lock) { speedValue }
        set(value) {
            synchronized(lock) {
                speedValue = value
                stream?.speed = value
            }
        }

    open val frequency: Float
        get() = synchronized(lock) { stream?.frequency ?: 0f }

    /**
     * The cutoff of the song's low-pass filter in Hz, none at or above [AudioStream.MAX_LOW_PASS_CUTOFF].
     */
    open var lowPassCutoff: Float
        get() = synchronized(lock) { stream?.lowPassCutoff ?: AudioStream.MAX_LOW_PASS_CUTOFF }
        set(value) {
            synchronized(lock) { stream?.lowPassCutoff = value }
        }

    /**
     * The cutoff of the song's high-pass filter in Hz, none at 0.
     */
    open var highPassCutoff: Float
        get() = synchronized(lock) { stream?.highPassCutoff ?: 0f }
        set(value) {
            synchronized(lock) { stream?.highPassCutoff = value }
        }

    /**
     * Where the song starts over, in milliseconds, once it reaches its end, as song select's preview
     * does: [update] doesn't play the next song, nor does gameplay stop it, and [consumePreviewEnd]
     * tells it ended. Null by default, loading another song clears it.
     */
    var previewTime: Int?
        get() = synchronized(lock) { previewTimeValue }
        set(value) {
            synchronized(lock) { previewTimeValue = value }
        }


    private val lock = SynchronizedObject()

    private var stream: AudioStream? = null

    private var speedValue = 1f

    // The last volume applied to the stream, or NaN if none has been applied to the current one yet.
    private var lastVolume = Float.NaN

    private var cachedFFT: FloatArray? = null
    private var lastFFTUpdateTime = 0L
    private var lastFFTResolution = -1
    private val timeOrigin = TimeSource.Monotonic.markNow()

    // Whether the song is meant to be playing, set by play() and cleared by pause() and stop().
    private var isPlaybackWanted = false

    private var previewTimeValue: Int? = null

    // Whether the song reached its end with a preview time, set from the audio thread.
    private val previewEnded = AtomicBoolean(false)


    //region Beatmap

    /**
     * Makes [beatmap] the [currentBeatmap] and loads its song.
     *
     * @return Whether the song was loaded.
     */
    fun load(beatmap: BeatmapInfo): Boolean {
        currentBeatmap = beatmap
        return preLoad(beatmap.audioPath)
    }

    /**
     * Loads the song of [beatmap] and plays it.
     */
    fun play(beatmap: BeatmapInfo) {
        load(beatmap)
        play()
    }

    /**
     * Plays a random beatmap of the next beatmap set of the [playlist].
     */
    fun next() = playBeatmapSet(playlist?.nextBeatmapSet())

    /**
     * Plays a random beatmap of the previous beatmap set of the [playlist].
     */
    fun previous() = playBeatmapSet(playlist?.previousBeatmapSet())

    private fun playBeatmapSet(beatmapSet: BeatmapSetInfo?) {
        // Null when the library is empty.
        val beatmap = beatmapSet?.beatmaps?.randomOrNull() ?: return
        play(beatmap)
    }

    /**
     * Plays the next song when the one meant to be playing reached its end outside of gameplay, as
     * the old menu did. Called every frame by the scenes that play songs one after another.
     */
    fun update() {
        if (isPlaybackWanted && !isGaming && previewTime == null && status == Status.STOPPED) {
            isPlaybackWanted = false
            next()
        }
    }

    //endregion

    //region Playback

    fun preLoad(filePath: String?) = preLoad(filePath, 1f, false, false)

    fun preLoad(filePath: String?, speed: Float, adjustPitch: Boolean) = preLoad(filePath, speed, adjustPitch, false)

    /**
     * Loads the song at [filePath], replacing the current one, stopped.
     *
     * @return Whether the song was loaded.
     */
    open fun preLoad(filePath: String?, speed: Float, adjustPitch: Boolean, isLoop: Boolean): Boolean {
        if (filePath.isNullOrBlank() || !Platform.fileSystem.exists(filePath.toPath())) {
            return false
        }

        val loaded = synchronized(lock) {
            release()

            val newStream = try {
                Verktex.audio.createStreamSource(filePath, true)?.createStream()
            } catch (_: IllegalStateException) {
                null
            }

            if (newStream == null) {
                speedValue = 1f
                false
            } else {
                stream = newStream
                lastVolume = Float.NaN

                newStream.looping = isLoop
                newStream.onEnd = { onStreamEnd(newStream) }
                previewTimeValue = null
                previewEnded.store(false)

                this.speed = speed
                setAdjustPitch(adjustPitch)
                true
            }
        }

        onPlaybackStateChanged(status)
        return loaded
    }

    /**
     * Plays the song, from where it was paused or else from the start, at [volume]. Nothing changes if
     * it's already playing.
     */
    fun play(volume: Float = OsuSettings.musicVolume) {
        if (status != Status.PLAYING) {
            startStream(volume)
        }
        // A song that can't be played is not waited for, see update().
        isPlaybackWanted = status == Status.PLAYING
        onPlaybackStateChanged(status)
    }

    /**
     * Plays the song from [time], in milliseconds, at [volume] (silent by default, for the caller to
     * fade it in), and starts it over at [loopTime] whenever it ends (see [previewTime]).
     */
    fun playPreview(time: Int, loopTime: Int = time, volume: Float = 0f) {
        previewTime = loopTime
        previewEnded.store(false)
        play(volume)
        seekTo(time)
    }

    /**
     * The preview point of [beatmap] in milliseconds, once its song is loaded: the one it gives, or else
     * 40% of the song.
     */
    fun previewPoint(beatmap: BeatmapInfo) =
        if (beatmap.previewTime >= 0) beatmap.previewTime else (length * 0.4f).toInt()

    /**
     * Whether the song reached its end since the last call, with a [previewTime] to start over from.
     */
    fun consumePreviewEnd() = previewEnded.exchange(false)

    /**
     * Takes the song out of the service as it plays, for the caller to dispose of: loading another song
     * then doesn't stop it, e.g. so a preview keeps playing while gameplay loads its own. Null without
     * a song.
     */
    open fun detach(): AudioStream? {
        val detached = synchronized(lock) {
            val detached = stream ?: return null
            detached.onEnd = null
            stream = null
            previewTimeValue = null
            previewEnded.store(false)
            release()
            detached
        }
        isPlaybackWanted = false
        onPlaybackStateChanged(status)
        return detached
    }

    /**
     * Makes a song [detach]ed before the song again, replacing the one loaded since.
     */
    open fun attach(stream: AudioStream) {
        synchronized(lock) {
            release()
            this.stream = stream
            lastVolume = Float.NaN
            stream.onEnd = { onStreamEnd(stream) }
        }
        isPlaybackWanted = status == Status.PLAYING
        onPlaybackStateChanged(status)
    }

    fun pause() {
        isPlaybackWanted = false
        pauseStream()
        onPlaybackStateChanged(status)
    }

    /**
     * Stops the song and releases it, it must be loaded again to be played.
     */
    fun stop() {
        isPlaybackWanted = false
        stopStream()
        onPlaybackStateChanged(status)
    }

    open fun seekTo(time: Int) {
        val success = synchronized(lock) {
            val stream = stream

            if (stream == null || time <= 0) {
                false
            } else {
                stream.position = time / 1000.0
                true
            }
        }
        Logger.i("MusicService", "Seeking to $time ms: " + if (success) "Success" else "Failed")
    }

    open fun setAdjustPitch(adjustPitch: Boolean) {
        synchronized(lock) { stream?.adjustPitch = adjustPitch }
    }

    open fun setPitchRate(pitchRate: Float) {
        synchronized(lock) { stream?.pitch = pitchRate }
    }

    open fun setFrequencyForcefully(frequency: Float) {
        synchronized(lock) { stream?.frequency = frequency }
    }

    /**
     * The FFT of what is playing now in [resolution] bands, logarithmic so the lower frequencies get
     * more of them. It's computed at most every [FFT_UPDATE_INTERVAL_MS].
     */
    open fun getAudioFFT(resolution: Int): FloatArray? {
        return synchronized(lock) {
            val currentTime = timeOrigin.elapsedNow().inWholeMilliseconds
            val cached = cachedFFT

            if (cached != null && lastFFTResolution == resolution && currentTime - lastFFTUpdateTime < FFT_UPDATE_INTERVAL_MS) {
                return cached
            }

            val rawFFT = spectrum

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

            fft
        }
    }

    //endregion

    //region Stream

    /**
     * Starts the stream, or continues it if it's paused.
     */
    protected open fun startStream(volume: Float) {
        synchronized(lock) {
            val stream = stream ?: return

            // Before playing, a preview fading in mustn't be heard at full volume first.
            lastVolume = volume
            stream.volume = volume

            // A paused song continues from where it was, otherwise it starts over.
            stream.play(restart = !stream.isPaused)
        }
    }

    protected open fun pauseStream() {
        synchronized(lock) { stream?.pause() }
    }

    protected open fun stopStream() {
        synchronized(lock) { release() }
    }

    /**
     * Called after the song was loaded, played, paused or stopped. Does nothing by default.
     */
    protected open fun onPlaybackStateChanged(status: Status) = Unit

    // From the audio thread: a song with a preview time is started over by its scene, otherwise
    // gameplay stops it.
    private fun onStreamEnd(ended: AudioStream) {
        synchronized(lock) {
            if (ended !== stream) return

            if (previewTimeValue != null) {
                previewEnded.store(true)
                return
            }
        }

        if (isGaming) {
            stopStream()
        }
    }

    private fun release() {
        stream?.dispose()
        stream = null

        cachedFFT = null
        lastFFTUpdateTime = 0
        lastFFTResolution = -1
    }

    //endregion


    companion object {
        const val DEFAULT_FREQUENCY = 44100

        private const val FFT_UPDATE_INTERVAL_MS = 96L
    }
}
