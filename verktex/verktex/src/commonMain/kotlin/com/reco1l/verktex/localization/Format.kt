package com.reco1l.verktex.localization

import com.reco1l.verktex.math.toFixed
import kotlin.math.abs

/**
 * Formats [args] as [pattern] says, the way `java.util.Formatter` does for the conversions strings
 * use, on every platform:
 *
 * - `%s`: the argument's `toString()` (`null` for null), cut to the precision if there's one.
 * - `%d`: an integer, `%x` and `%X` in hexadecimal.
 * - `%f`: a decimal number with the precision's digits after the point, 6 by default, e.g. `%.2f`.
 * - `%%`: a percent sign, `%n`: a line break.
 *
 * An argument can be picked by position as `%2$s`. The flags `-` (justify left), `0` (pad with
 * zeros), `+` (always a sign) and `,` (group thousands) and a width are supported. The decimal
 * point is always `.`.
 *
 * @throws IllegalArgumentException If the pattern has an unknown conversion or lacks an argument.
 */
fun formatString(pattern: String, vararg args: Any?): String {
    if ('%' !in pattern) {
        return pattern
    }

    val result = StringBuilder(pattern.length + 16)
    var nextArgument = 0
    var i = 0

    while (i < pattern.length) {
        val c = pattern[i]

        if (c != '%') {
            result.append(c)
            i++
            continue
        }

        // %[argument$][flags][width][.precision]conversion
        var j = i + 1

        var position = -1
        var digitsEnd = j
        while (digitsEnd < pattern.length && pattern[digitsEnd].isDigit()) digitsEnd++
        if (digitsEnd > j && digitsEnd < pattern.length && pattern[digitsEnd] == '$') {
            position = pattern.substring(j, digitsEnd).toInt() - 1
            j = digitsEnd + 1
        }

        var leftJustify = false
        var zeroPad = false
        var plusSign = false
        var grouping = false
        flags@ while (j < pattern.length) {
            when (pattern[j]) {
                '-' -> leftJustify = true
                '0' -> zeroPad = true
                '+' -> plusSign = true
                ',' -> grouping = true
                else -> break@flags
            }
            j++
        }

        var width = 0
        while (j < pattern.length && pattern[j].isDigit()) {
            width = width * 10 + (pattern[j] - '0')
            j++
        }

        var precision = -1
        if (j < pattern.length && pattern[j] == '.') {
            j++
            precision = 0
            while (j < pattern.length && pattern[j].isDigit()) {
                precision = precision * 10 + (pattern[j] - '0')
                j++
            }
        }

        require(j < pattern.length) { "The pattern \"$pattern\" ends with an incomplete conversion" }
        val conversion = pattern[j]
        i = j + 1

        when (conversion) {
            '%' -> {
                result.append(pad("%", width, leftJustify, false))
                continue
            }
            'n' -> {
                result.append('\n')
                continue
            }
        }

        val index = if (position >= 0) position else nextArgument++
        require(index < args.size) { "The pattern \"$pattern\" needs more than ${args.size} arguments" }
        val argument = args[index]

        val text = when (conversion) {
            's', 'S' -> {
                var value = argument.toString()
                if (precision >= 0 && precision < value.length) {
                    value = value.substring(0, precision)
                }
                if (conversion == 'S') value.uppercase() else value
            }

            'd' -> {
                val value = (argument as? Number)?.toLong()
                    ?: throw IllegalArgumentException("%d can't format ${argument?.let { it::class.simpleName }}")
                signed(if (grouping) group(abs(value).toULong().toString()) else abs(value).toULong().toString(), value < 0, plusSign)
            }

            'x', 'X' -> {
                val value = (argument as? Number)?.toLong()
                    ?: throw IllegalArgumentException("%x can't format ${argument?.let { it::class.simpleName }}")
                val hex = value.toULong().toString(16)
                if (conversion == 'X') hex.uppercase() else hex
            }

            'f' -> {
                val value = (argument as? Number)?.toDouble()
                    ?: throw IllegalArgumentException("%f can't format ${argument?.let { it::class.simpleName }}")

                if (value.isNaN() || value.isInfinite()) {
                    if (value.isNaN()) "NaN" else signed("Infinity", value < 0, plusSign)
                } else {
                    val digits = abs(value).toFixed(if (precision >= 0) precision else 6)
                    val integerEnd = digits.indexOf('.').let { if (it < 0) digits.length else it }
                    val integer = digits.substring(0, integerEnd)
                    val number = (if (grouping) group(integer) else integer) + digits.substring(integerEnd)
                    signed(number, value < 0 || (value == 0.0 && 1.0 / value < 0), plusSign)
                }
            }

            else -> throw IllegalArgumentException("Unknown conversion %$conversion in \"$pattern\"")
        }

        val numeric = conversion == 'd' || conversion == 'f' || conversion == 'x' || conversion == 'X'
        result.append(pad(text, width, leftJustify, zeroPad && numeric))
    }

    return result.toString()
}

private fun group(digits: String): String {
    if (digits.length <= 3) return digits

    val result = StringBuilder(digits.length + digits.length / 3)
    val first = digits.length % 3
    for (k in digits.indices) {
        if (k > 0 && (k - first) % 3 == 0) result.append(',')
        result.append(digits[k])
    }
    return result.toString()
}

private fun signed(number: String, isNegative: Boolean, plusSign: Boolean) = when {
    isNegative -> "-$number"
    plusSign -> "+$number"
    else -> number
}

private fun pad(text: String, width: Int, leftJustify: Boolean, zeroPad: Boolean): String {
    if (text.length >= width) return text

    return when {
        leftJustify -> text.padEnd(width)
        zeroPad -> {
            // Zeros go after the sign.
            val signLength = if (text.startsWith('-') || text.startsWith('+')) 1 else 0
            text.substring(0, signLength) + "0".repeat(width - text.length) + text.substring(signLength)
        }
        else -> text.padStart(width)
    }
}
