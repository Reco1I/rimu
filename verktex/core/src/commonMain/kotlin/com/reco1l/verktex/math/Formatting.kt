package com.reco1l.verktex.math

import kotlin.math.abs
import kotlin.math.floor

/**
 * The number with [decimals] digits after the point, rounded half up, like `"%.2f"` formats it but
 * always with a `.` as the separator, whatever the locale.
 */
fun Double.toFixed(decimals: Int): String {
    require(decimals in 0..18) { "Can't format with $decimals decimals." }

    if (isNaN() || isInfinite()) {
        return toString()
    }

    var factor = 1L
    repeat(decimals) { factor *= 10 }

    val scaled = abs(this) * factor

    // Beyond what a Long holds exactly the decimals don't matter any more.
    if (scaled >= 9.0E15) {
        val whole = floor(abs(this)).toLong()
        val sign = if (this < 0) "-" else ""
        return if (decimals == 0) "$sign$whole" else "$sign$whole." + "0".repeat(decimals)
    }

    // The nudge makes halves that the binary representation stores slightly below, like 1.005,
    // round up as written.
    val rounded = floor(scaled + 0.5 + scaled * 1e-15).toLong()
    val sign = if (this < 0 && rounded != 0L) "-" else ""
    val whole = rounded / factor

    if (decimals == 0) {
        return "$sign$whole"
    }

    return "$sign$whole." + (rounded % factor).toString().padStart(decimals, '0')
}

/**
 * The number with [decimals] digits after the point, see [Double.toFixed].
 */
fun Float.toFixed(decimals: Int): String = toDouble().toFixed(decimals)
