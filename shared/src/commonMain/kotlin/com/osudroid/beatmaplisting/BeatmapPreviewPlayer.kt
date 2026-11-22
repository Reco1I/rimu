package com.osudroid.beatmaplisting

import com.osudroid.OsuGame
import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.currentTimeMillis
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.audio.AudioStream
import com.reco1l.verktex.ui.toast.Toasts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Plays the audio previews of the beatmap sets of the listing, streamed from the selected mirror, one at a
 * time. The music pauses while one plays, and resumes when it stops if it was playing when the player was
 * created.
 *
 * Every function is called on the update thread.
 */
class BeatmapPreviewPlayer(private val scope: CoroutineScope) {

    private val playingSet = MutableStateFlow<Long?>(null)

    /**
     * The set whose preview plays or is being loaded, null when there's none.
     */
    val playingSetId: StateFlow<Long?> = playingSet.asStateFlow()

    private val wasMusicPlaying = OsuGame.music.isPlaying

    private var stream: AudioStream? = null
    private var job: Job? = null

    private var recentStarts = 0
    private var lastStartMillis = 0L


    /**
     * Plays the preview of [set], or stops it if it's the one playing.
     */
    fun toggle(set: BeatmapSetModel) {
        if (playingSet.value == set.id) stop() else play(set)
    }

    /**
     * Plays the preview of [set], the one of its first (easiest) beatmap, stopping the one playing.
     */
    fun play(set: BeatmapSetModel) {
        val beatmap = set.beatmaps.firstOrNull() ?: return
        val now = currentTimeMillis()

        // Mirrors limit how often previews are requested.
        if (now - lastStartMillis > RATE_LIMIT_WINDOW_MILLIS) {
            recentStarts = 0
        }

        if (recentStarts >= RATE_LIMIT_COUNT) {
            Toasts.show(Res.string.beatmap_listing_preview_wait)
            return
        }

        recentStarts++
        lastStartMillis = now

        stop(resumeMusic = false)

        playingSet.value = set.id
        OsuGame.music.pause()

        val url = BeatmapMirror.selected.preview.request(beatmap.id)

        // Opening the stream connects to the mirror, which blocks.
        job = scope.launch {
            val newStream = try {
                Verktex.audio.createUrlStreamSource(url)?.createStream() ?: error("The audio driver can't stream from a URL.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("BeatmapPreviewPlayer", "Failed to play the preview of $url", e)

                updateThread {
                    if (playingSet.value == set.id) {
                        Toasts.show(Res.string.beatmap_listing_preview_failed)
                        stop()
                    }
                }
                return@launch
            }

            updateThread {
                if (playingSet.value != set.id || stream != null) {
                    newStream.dispose()
                    return@updateThread
                }

                stream = newStream.apply {
                    volume = OsuSettings.musicVolume
                    onEnd = { updateThread { if (stream === newStream) stop() } }
                    play()
                }
            }
        }
    }

    /**
     * Stops the preview playing, resuming the music if [resumeMusic] and it was playing before.
     */
    fun stop(resumeMusic: Boolean = true) {
        job?.cancel()
        job = null

        stream?.let {
            it.onEnd = null
            it.stop()
            it.dispose()
        }
        stream = null

        if (playingSet.value != null) {
            playingSet.value = null

            if (resumeMusic && wasMusicPlaying) {
                OsuGame.music.play()
            }
        }
    }


    companion object {
        private const val RATE_LIMIT_COUNT = 2
        private const val RATE_LIMIT_WINDOW_MILLIS = 5000L
    }
}
