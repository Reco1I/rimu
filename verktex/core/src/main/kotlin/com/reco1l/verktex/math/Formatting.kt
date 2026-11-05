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

    val rounded = floor(abs(this) * factor + 0.5).toLong()
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
