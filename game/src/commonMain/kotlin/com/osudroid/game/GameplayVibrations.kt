package com.osudroid.game

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings

/**
 * The vibrations of gameplay, which the settings turn on for each kind of object, at the strength of
 * [OsuSettings.SeekBarVibrateIntensity]. They need [Platform.haptics].
 */
object GameplayVibrations {

    // Off and on durations, in milliseconds.
    private val SLIDER_PATTERN = longArrayOf(0, 30, 20, 50)
    private val SPINNER_PATTERN = longArrayOf(0, 20, 10, 30)

    /**
     * Whether the device can vibrate.
     */
    val isAvailable: Boolean
        get() = Platform.haptics != null

    fun circle() {
        if (Settings[OsuSettings.VibrationCircle]) {
            Platform.haptics?.vibrate(30, intensity)
        }
    }

    fun slider() {
        if (Settings[OsuSettings.VibrationSlider]) {
            Platform.haptics?.vibrate(SLIDER_PATTERN, patternAmplitudes())
        }
    }

    fun spinner() {
        if (Settings[OsuSettings.VibrationSpinner]) {
            Platform.haptics?.vibrate(SPINNER_PATTERN, patternAmplitudes())
        }
    }

    private val intensity
        get() = Settings[OsuSettings.SeekBarVibrateIntensity]

    // Silent while off, at the intensity while on.
    private fun patternAmplitudes() = intensity.let { intArrayOf(0, it, 0, it) }
}
