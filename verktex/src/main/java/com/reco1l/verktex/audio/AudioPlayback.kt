package com.reco1l.verktex.audio

/**
 * Represents an audio buffer playback instance.
 */
interface AudioPlayback {

    /**
     * Whether the playback is currently playing.
     */
    val isPlaying: Boolean

    /**
     * The length of this resource in seconds.
     */
    val length: Double


    //region Settings

    /**
     * The current position of the playback.
     */
    var position: Double

    /**
     * The volume of the playback.
     */
    var volume: Float

    /**
     * The pitch of the playback.
     */
    var pitch: Float

    /**
     * The speed of the playback.
     */
    var speed: Float

    /**
     * Whether the playback should adjust the pitch based on the speed.
     */
    var adjustPitch: Boolean

    /**
     * Whether the playback is looping.
     */
    var looping: Boolean

    //endregion

    //region Controls

    /**
     * Plays this playback.
     */
    fun play()

    /**
     * Pauses this playback at the current position.
     */
    fun pause()

    /**
     * Stops this playback and resets the position to 0.
     */
    fun stop()

    //endregion

}