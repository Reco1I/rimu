package com.reco1l.verktex.audio

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex

/**
 * A class that manages the loading and unloading of audio resources.
 */
object Sounds {

    /**
     * The list of audio resources loaded.
     */
    val resources: Map<String, AudioResource>
        field = mutableMapOf<String, AudioResource>()


    /**
     * Loads an audio stream from the given path.
     *
     * @param key The unique identifier for the audio stream.
     * @param path The path to the audio stream file.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     */
    fun loadStream(key: String, path: String, isExternal: Boolean = true): AudioStream {

        val source = Platform.provider.createAudioStreamSource(path, isExternal)
            ?: throw IllegalArgumentException("Invalid path: $path")

        val resource = source.createStream()

        val oldResource = resources[key]
        if (oldResource != null) {
            if (oldResource is AudioSample) {
                Logger.w("SoundManager", "An audio resource that was loaded as an AudioSample with the key \"$key\" is being replaced as an AudioStream.")
            } else {
                Logger.i("SoundManager", "Overriding existing AudioStream with same key: $key")
            }
            oldResource.dispose()
        }

        resources[key] = resource
        return resource
    }

    /**
     * Loads an audio sample from the given path. Differently from [loadStream] this returns a new
     * [AudioPlayback] instance of the sample upon creation instead of the [AudioSample].
     *
     * @param key The unique identifier for the audio sample.
     * @param path The path to the audio sample file.
     * @param isExternal Whether the path is external or internal, in some platforms this may affect
     */
    fun loadSample(key: String, path: String, isExternal: Boolean = true): AudioPlayback {

        val source = Platform.provider.createAudioSampleSource(path, isExternal)
            ?: throw IllegalArgumentException("Invalid path: $path")

        val resource = source.createSample()

        val oldResource = resources[key]
        if (oldResource != null) {
            if (oldResource is AudioStream) {
                Logger.w("SoundManager", "An audio resource that was loaded as an AudioStream with the key \"$key\" is being replaced as an AudioSample.")
            } else {
                Logger.i("SoundManager", "Overriding existing AudioSample with same key: $key")
            }
            oldResource.dispose()
        }

        resources[key] = resource
        return resource.createPlayback()
    }

    /**
     * Unloads an audio resource from memory.
     */
    fun unload(key: String) {
        val resource = resources[key] ?: return
        resource.dispose()
        resources.remove(key)
    }


    operator fun get(key: String): AudioPlayback? {
        val resource = resources[key] ?: return null
        return resource.createPlayback()
    }

}