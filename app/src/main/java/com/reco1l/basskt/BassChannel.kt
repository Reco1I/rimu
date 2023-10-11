package com.reco1l.basskt

enum class AudioChannel
{
    LEFT,
    RIGHT,
    BOTH
}

/**
 * Indicates the channel state.
 */
enum class AudioState
{
    /**The channel is currently playing a stream*/
    PLAYING,

    /**The channel is stopped*/
    STOPPED,

    /**The channel is paused*/
    PAUSED,

    STALLED
}