package com.osudroid.game.objects

import com.osudroid.game.GameplayContext
import com.reco1l.verktex.data.Color4
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The colors circles flash in on each beat of a kiai section, brightened towards white. Kept for each combo color
 * and step of brightness, as colors are immutable: building one for each circle on every frame fills the heap.
 */
internal object KiaiColors {

    // Steps between the combo color and the brightest flash, half way to white.
    private const val STEPS = 32

    private val bases = ArrayList<Color4>()
    private val colors = ArrayList<Array<Color4?>>()

    /**
     * The color of a circle of [comboColor] now, in the beat of [context]'s timing.
     */
    fun at(context: GameplayContext, comboColor: Color4): Color4 {
        val timing = context.timing
        val modifier = max(0.0, 1 - timing.currentBeatTime / timing.beatLength).toFloat() * 0.5f
        val step = (modifier / 0.5f * STEPS).roundToInt().coerceIn(0, STEPS)

        if (step == 0) {
            return comboColor
        }

        val steps = stepsOf(comboColor)

        return steps[step] ?: run {
            val amount = step.toFloat() / STEPS * 0.5f
            Color4(
                min(1f, comboColor.red + (1 - comboColor.red) * amount),
                min(1f, comboColor.green + (1 - comboColor.green) * amount),
                min(1f, comboColor.blue + (1 - comboColor.blue) * amount)
            ).also { steps[step] = it }
        }
    }

    private fun stepsOf(comboColor: Color4): Array<Color4?> {
        // A few combo colors are used by a play, the same instances.
        for (i in bases.indices) {
            if (bases[i] === comboColor) {
                return colors[i]
            }
        }

        // Synesthesia colors and the ones of earlier plays.
        if (bases.size >= 64) {
            bases.clear()
            colors.clear()
        }

        val steps = arrayOfNulls<Color4>(STEPS + 1)
        bases += comboColor
        colors += steps
        return steps
    }
}
