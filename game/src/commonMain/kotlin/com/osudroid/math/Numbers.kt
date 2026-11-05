@file:JvmName("NumberExt")

package com.osudroid.math

import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.jvm.JvmName

/**
 * Rounds this [Float] to the specified number of decimal places.
 *
 * Unlike [round], this method rounds the decimal representation of the [Float], which allows for more precise rounding, **but is
 * slower than [round]. Only use when precision is important**.
 *
 * @param precision The number of decimal places to round to.
 * @return The rounded [Float], or:
 * - [Float.POSITIVE_INFINITY] if this [Float] is greater than or equal to [Float.MAX_VALUE].
 * - [Float.NEGATIVE_INFINITY] if this [Float] is less than or equal to -[Float.MAX_VALUE].
 * - This [Float] itself if it is NaN or infinite.
 */
fun Float.preciseRoundBy(precision: Int) = when {
    precision < 0 -> throw IllegalArgumentException("Precision must be non-negative")
    isNaN() || isInfinite() -> this
    absoluteValue >= Float.MAX_VALUE -> if (compareTo(0) >= 0) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
    else -> toString().roundHalfUp(precision).toFloat()
}

/**
 * Rounds this [Double] to the specified number of decimal places.
 *
 * Unlike [round], this method rounds the decimal representation of the [Double], which allows for more precise rounding, **but is
 * slower than [round]. Only use when precision is important**.
 *
 * @param precision The number of decimal places to round to.
 * @return The rounded [Double], or:
 * - [Double.POSITIVE_INFINITY] if this [Double] is greater than or equal to [Double.MAX_VALUE].
 * - [Double.NEGATIVE_INFINITY] if this [Double] is less than or equal to -[Double.MAX_VALUE].
 * - This [Double] itself if it is NaN or infinite.
 */
fun Double.preciseRoundBy(precision: Int) = when {
    precision < 0 -> throw IllegalArgumentException("Precision must be non-negative")
    isNaN() || isInfinite() -> this
    absoluteValue >= Double.MAX_VALUE -> if (compareTo(0) >= 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
    else -> toString().roundHalfUp(precision).toDouble()
}

/**
 * Rounds the decimal number in this [String], as [Float.toString] and [Double.toString] write it, to
 * [precision] decimal places, with ties going away from zero. Being done on the decimal digits, the
 * result is the same as rounding a `BigDecimal` made from the [String] with `RoundingMode.HALF_UP`.
 */
private fun String.roundHalfUp(precision: Int): String {
    var number = this
    var negative = number.startsWith('-')
    if (negative) {
        number = number.substring(1)
    }

    var exponent = 0
    val exponentIndex = number.indexOfFirst { it == 'E' || it == 'e' }
    if (exponentIndex >= 0) {
        exponent = number.substring(exponentIndex + 1).toInt()
        number = number.substring(0, exponentIndex)
    }

    val pointIndex = number.indexOf('.')
    var digits = if (pointIndex >= 0) number.removeRange(pointIndex, pointIndex + 1) else number
    // The amount of digits before the decimal point.
    var integerLength = (if (pointIndex >= 0) pointIndex else number.length) + exponent
    if (integerLength < 0) {
        digits = "0".repeat(-integerLength) + digits
        integerLength = 0
    }

    val kept = integerLength + precision
    if (kept >= digits.length) {
        return this
    }

    var result = digits.substring(0, kept)
    if (digits[kept] >= '5') {
        val keptDigits = result.toCharArray()
        var i = keptDigits.size - 1
        while (i >= 0 && keptDigits[i] == '9') {
            keptDigits[i] = '0'
            i--
        }
        if (i >= 0) {
            keptDigits[i]++
            result = keptDigits.concatToString()
        } else {
            result = "1" + keptDigits.concatToString()
            integerLength++
        }
    }

    if (result.all { it == '0' }) {
        negative = false
    }

    val integerPart = result.substring(0, integerLength).ifEmpty { "0" }
    val fractionPart = result.substring(integerLength)

    return (if (negative) "-" else "") + integerPart + (if (fractionPart.isNotEmpty()) ".$fractionPart" else "")
}
