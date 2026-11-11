package com.reco1l.verktex.android

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.reco1l.verktex.Haptics

/**
 * The device's [Vibrator] as [Haptics]. Before Android 8 amplitudes can't be set, so
 * vibrations are at full strength.
 */
internal class AndroidHaptics private constructor(private val vibrator: Vibrator) : Haptics {

    override fun vibrate(milliseconds: Long, amplitude: Int) {
        vibrator.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    override fun vibrate(timings: LongArray, amplitudes: IntArray) {
        vibrator.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, IntArray(timings.size) { amplitudes.getOrElse(it) { 0 }.coerceIn(0, 255) }, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    override fun cancel() = vibrator.cancel()


    companion object {

        /**
         * The haptics of the device, or null if it can't vibrate.
         */
        fun create(context: Context): AndroidHaptics? {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            }
            return vibrator?.takeIf { it.hasVibrator() }?.let(::AndroidHaptics)
        }
    }
}
