package ru.nsu.ccfit.zuev.audio.effect

import com.osudroid.beatmaps.timings.TimingControlPoint

import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import com.osudroid.ui.v2.SkinSounds

class Metronome {


    private val kickSound: BassSoundProvider = SkinSounds["nightcore-kick"]
    private val finishSound: BassSoundProvider = SkinSounds["nightcore-finish"]
    private val clapSound: BassSoundProvider = SkinSounds["nightcore-clap"]
    private val hatSound: BassSoundProvider = SkinSounds["nightcore-hat"]

    private var lastBeatIndex = -1

    fun update(elapsedTime: Float, activeTimingPoint: TimingControlPoint) {
        val timingPointTime = activeTimingPoint.time.toFloat() / 1000

        if (elapsedTime - timingPointTime <= 0) {
            return
        }

        val playSeconds = elapsedTime - timingPointTime
        val beatIndex = (playSeconds * 2 / (activeTimingPoint.msPerBeat / 1000)).toInt()

        if (beatIndex < 0) {
            return
        }
        if (beatIndex == lastBeatIndex) {
            return
        }
        lastBeatIndex = beatIndex

        val beatInBar = beatIndex % activeTimingPoint.timeSignature

        // 每隔8小节在第4拍kick+finish
        if (beatIndex % (8 * activeTimingPoint.timeSignature) == 0) {
            kickSound.play()
            if (beatIndex > 0) {
                finishSound.play()
            }
            return
        }
        // 每小节第4拍kick
        if (beatInBar % 4 == 0) {
            kickSound.play()
            return
        }
        // 每小节第2拍clap
        if (beatInBar % 4 == 2) {
            clapSound.play()
            return
        }
        // 每小节奇数拍hat
        hatSound.play()
    }

    fun setVolume(volume: Float) {
        kickSound.setVolume(volume)
        finishSound.setVolume(volume)
        clapSound.setVolume(volume)
        hatSound.setVolume(volume)
    }
}
