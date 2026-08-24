package com.reco1l.verktex.time

import com.reco1l.verktex.audio.AudioPlayback

/**
 * A clock that is linked to an audio playback.
 */
class AudioPlaybackClock(private val audioPlayback: AudioPlayback) : Clock() {

    override val time: Float
        get() = audioPlayback.position.toFloat()

    override val isRunning: Boolean
        get() = audioPlayback.isPlaying

}
