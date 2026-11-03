package ru.nsu.ccfit.zuev.audio

import com.reco1l.verktex.Platform
import com.reco1l.verktex.audio.AudioPlayback
import com.reco1l.verktex.audio.AudioSample
import ru.nsu.ccfit.zuev.osu.Config

/**
 * A sound effect. Playing it again restarts it instead of overlapping with the previous playback.
 */
class BassSoundProvider {

    private var sample: AudioSample? = null
    private var playback: AudioPlayback? = null

    private var volume = 1f
    private var looping = false

    /**
     * The rate at which the sound is played back (affects pitch). 1 is 100% playback speed, or default frequency.
     */
    private var frequency = 1f


    /**
     * Loads a sound from a file.
     */
    fun prepare(fileName: String?) = load(fileName, isExternal = true)

    /**
     * Loads a sound from the game's assets.
     */
    fun prepareAsset(assetName: String?) = load(assetName, isExternal = false)

    private fun load(path: String?, isExternal: Boolean): Boolean {
        free()

        if (path.isNullOrEmpty()) {
            return false
        }

        sample = try {
            Platform.provider.createAudioSampleSource(path, isExternal)?.createSample()
        } catch (_: IllegalStateException) {
            null
        }

        return sample != null
    }

    @JvmOverloads
    fun play(volume: Float = 1f) {
        val sample = sample ?: return

        this.volume = volume
        val finalVolume = volume * Config.getSoundVolume()

        if (finalVolume == 0f) {
            stop()
            return
        }

        val playback = playback ?: sample.createPlayback().also {
            playback = it
            applyEffects()
        }

        playback.play(restart = true)
        playback.volume = finalVolume
    }

    fun stop() {
        playback?.stop()
    }

    fun free() {
        sample?.dispose()
        sample = null
        playback = null
    }

    fun setLooping(looping: Boolean) {
        if (this.looping != looping) {
            this.looping = looping
            applyEffects()
        }
    }

    fun setFrequency(frequency: Float) {
        if (this.frequency != frequency) {
            this.frequency = frequency
            applyEffects()
        }
    }

    fun setVolume(volume: Float) {
        if (this.volume != volume) {
            this.volume = volume
            applyEffects()
        }
    }

    private fun applyEffects() {
        val playback = playback ?: return

        playback.looping = looping
        playback.volume = volume * Config.getSoundVolume()
        playback.pitch = frequency
    }


    companion object {
        @JvmField
        val EMPTY = BassSoundProvider()
    }
}
