package com.reco1l.verktex.audio

/**
 * Represents an audio resource that can be played.
 *
 * It is a chunk of audio data that depending on its type it can be played from memory or streamed
 * from a file.
 *
 * @see AudioSample
 * @see AudioStream
 */
sealed interface AudioResource {

    /**
     * Disposes this resource.
     */
    fun dispose()

    /**
     * Creates and returns a new [AudioPlayback] instance from this resource.
     */
    fun createPlayback(): AudioPlayback

}

/**
 * Represents an audio buffer that can be played from memory.
 */
abstract class AudioSample : AudioResource {

    /**
     * Represents the source of an audio sample.
     */
    interface Source {

        /**
         * Creates a new instance of [AudioSample] based on this source.
         */
        fun createSample(): AudioSample
    }
}

/**
 * Represents an audio buffer that can be streamed from a file.
 *
 * An AudioStream also acts as a [AudioPlayback] since streams can only have one playback instance.
 */
abstract class AudioStream : AudioResource, AudioPlayback {

    override fun createPlayback(): AudioPlayback {
        return this
    }

    /**
     * Represents the source of an audio stream.
     */
    interface Source {
        /**
         * Creates a new instance of [AudioStream] based on this source.
         */
        fun createStream(): AudioStream
    }
}




