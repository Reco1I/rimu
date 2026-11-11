package com.osudroid.math

import kotlin.math.abs

/**
 * Rounds the exact binary value of [value] to [digits] decimal places, with ties going away from zero,
 * as a `BigDecimal` made from the [Double] itself rounds with `RoundingMode.HALF_UP`. Unlike
 * [preciseRoundBy], which rounds the shortest decimal representation of the number, a value such as
 * 2.675, which is slightly below that in binary, rounds down.
 *
 * The result is the [Float] nearest to the rounded decimal.
 */
fun roundExactHalfUp(value: Double, digits: Int): Float {
    require(digits >= 0) { "digits must not be negative: $digits" }

    if (!value.isFinite() || value == 0.0) {
        return value.toFloat()
    }

    // value = mantissa * 2^exponent, exactly.
    val bits = abs(value).toRawBits()
    val biasedExponent = ((bits ushr 52) and 0x7FF).toInt()
    val fraction = bits and ((1L shl 52) - 1)

    val mantissa = if (biasedExponent == 0) fraction else fraction or (1L shl 52)
    val exponent = if (biasedExponent == 0) -1074 else biasedExponent - 1075

    // value * 10^digits = mantissa * 5^digits * 2^(exponent + digits).
    val scaled = ExactInteger(mantissa)
    repeat(digits) { scaled.multiply(5) }

    val shift = exponent + digits
    if (shift >= 0) {
        scaled.shiftLeft(shift)
    } else {
        // The first bit shifted out is the half: set means the rest is at least one half, a tie
        // included, which rounds up.
        val roundsUp = scaled.testBit(-shift - 1)
        scaled.shiftRight(-shift)
        if (roundsUp) {
            scaled.add(1)
        }
    }

    // The rounded decimal is scaled / 10^digits, which the string conversion turns into the
    // nearest Float, as BigDecimal.toFloat does.
    val digitsString = scaled.toDecimalString()
    val integer = digitsString.padStart(digits + 1, '0')
    val decimal = if (digits == 0) integer else integer.dropLast(digits) + "." + integer.takeLast(digits)
    // A BigDecimal has no negative zero.
    val sign = if (value < 0 && digitsString != "0") "-" else ""

    return (sign + decimal).toFloat()
}

/**
 * A non-negative integer of any size, as little-endian 32-bit words, with only what
 * [roundExactHalfUp] needs.
 */
private class ExactInteger(value: Long) {

    private var words = LongArray(4).also {
        it[0] = value and WORD_MASK
        it[1] = value ushr 32
    }

    private var length = 2

    fun multiply(factor: Int) {
        var carry = 0L
        for (i in 0 until length) {
            val product = words[i] * factor + carry
            words[i] = product and WORD_MASK
            carry = product ushr 32
        }
        if (carry != 0L) {
            ensureLength(length + 1)
            words[length - 1] = carry
        }
    }

    fun add(addend: Int) {
        var carry = addend.toLong()
        var i = 0
        while (carry != 0L) {
            if (i == length) {
                ensureLength(length + 1)
            }
            val sum = words[i] + carry
            words[i] = sum and WORD_MASK
            carry = sum ushr 32
            i++
        }
    }

    fun shiftLeft(count: Int) {
        val wordShift = count / 32
        val bitShift = count % 32
        val oldLength = length
        ensureLength(oldLength + wordShift + 1)

        for (i in length - 1 downTo 0) {
            val source = i - wordShift
            val high = if (source in 0 until oldLength) words[source] shl bitShift else 0L
            val low = if (bitShift != 0 && source - 1 in 0 until oldLength) words[source - 1] ushr (32 - bitShift) else 0L
            words[i] = (high or low) and WORD_MASK
        }
    }

    fun shiftRight(count: Int) {
        val wordShift = count / 32
        val bitShift = count % 32

        for (i in 0 until length) {
            val source = i + wordShift
            val low = if (source < length) words[source] ushr bitShift else 0L
            val high = if (bitShift != 0 && source + 1 < length) words[source + 1] shl (32 - bitShift) else 0L
            words[i] = (low or high) and WORD_MASK
        }
    }

    fun testBit(index: Int): Boolean {
        val word = index / 32
        return word < length && (words[word] ushr (index % 32)) and 1L != 0L
    }

    fun toDecimalString(): String {
        val remaining = words.copyOf(length)
        val digits = StringBuilder()

        while (remaining.any { it != 0L }) {
            var remainder = 0L
            for (i in remaining.indices.reversed()) {
                val current = (remainder shl 32) or remaining[i]
                remaining[i] = current / 10
                remainder = current % 10
            }
            digits.append(('0' + remainder.toInt()))
        }

        return if (digits.isEmpty()) "0" else digits.reverse().toString()
    }

    private fun ensureLength(newLength: Int) {
        if (newLength > words.size) {
            words = words.copyOf(maxOf(newLength, words.size * 2))
        }
        length = maxOf(length, newLength)
    }

    private companion object {
        const val WORD_MASK = 0xFFFFFFFFL
    }
}
