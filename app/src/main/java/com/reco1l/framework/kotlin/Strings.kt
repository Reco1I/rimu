package com.reco1l.framework.kotlin

import kotlin.math.abs
import kotlin.math.min


/**
 * Returns a copy of this string having its first letter in uppercase.
 */
fun String.capitalize() = replaceFirstChar { it.uppercase() }

/**
 * Returns a copy of this string having its first letter in lowercase.
 */
fun String.decapitalize() = replaceFirstChar { it.lowercase() }


// Ranges

infix fun String.isBetween(range: CharRange) = startsWith(range.first) && endsWith(range.last)

/**
 * Returns a substring cropped between the specified characters or `null` if it doesn't contains
 * those characters or the part between them is empty.
 */
fun String.between(first: Char, last: Char): String?
{
    val firstIndex = indexOf(first)
    val secondIndex = lastIndexOf(last)

    if (firstIndex == -1 || firstIndex + 1 >= secondIndex)
        return null

    return substring(firstIndex + 1, secondIndex).takeUnless { it.isEmpty() }
}

/**
 * Multiply the characters and create a sequence
 */
operator fun Char.times(times: Int): CharSequence
{
    var result = ""
    for (i in 0 until abs(times))
        result += this
    return result
}

// Regex

fun String.takeIfMatches(regex: Regex) = takeIf { regex.matches(it) }


/**
 * Pack of constants with defaults regular expressions.
 */
object Regexs
{

    /**
     * Regex for integer numbers.
     *
     * Valid cases:
     * * `123`
     * * `-123`
     */
    val INTEGER = Regex("^-?\\d+$")

    /**
     * Regex for numbers with decimal (floating points).
     *
     * Valid cases:
     * * `123.123`
     * * `-123.123`
     * * `123`
     * * `-123`
     */
    val DECIMAL = Regex("^NaN|Infinity|-?\\d+(\\.\\d+$)?$")

    /**
     * Regex for an array of integer numbers.
     *
     * The valid cases are the same as [INTEGER] regex allowing one or more arguments delimited
     * by a comma.
     */
    val INTEGER_ARRAY = Regex("^-?\\d+(?:\\s*,\\s*-?\\d+)*\$")


    /**
     * Regex for an array of numbers with decimal.
     *
     * Same as [INTEGER_ARRAY].
     */
    val DECIMAL_ARRAY = Regex("^-?\\d+(\\.\\d+)?(?:\\s*,\\s*-?\\d+(\\.\\d+)?)*\$")

    /**
     * Regex for alphanumeric strings.
     *
     * Valid cases:
     * * `abc123`
     * * `abc`
     * * `123`
     */
    val ALPHANUMERIC = Regex("^[a-zA-Z0-9]+\$")
}


// Escapes

fun String.withTranslatedEscapes(ignoreInvalidSequences: Boolean = true): String
{
    if (isEmpty())
        return ""

    val sequence = toCharArray()
    val length = sequence.size

    var from = 0
    var to = 0

    while (from < length)
    {
        var ch = sequence[from++]

        if (ch == '\\')
        {
            ch = if (from < length) sequence[from++] else '\u0000'

            when (ch)
            {
                'b' -> ch = '\b'
                'f' -> ch = '\u000c'
                'n' -> ch = '\n'
                'r' -> ch = '\r'
                's' -> ch = ' '
                't' -> ch = '\t'
                '\'', '\"', '\\' -> Unit

                '0', '1', '2', '3', '4', '5', '6', '7' ->
                {
                    val limit = min(from + if (ch <= '3') 2 else 1, length)
                    var code = ch.code - '0'.code

                    while (from < limit)
                    {
                        ch = sequence[from]

                        if (ch < '0' || '7' < ch)
                            break

                        from++
                        code = code shl 3 or ch.code - '0'.code
                    }
                    ch = code.toChar()
                }

                '\n' -> continue
                '\r' ->
                {
                    if (from < length && sequence[from] == '\n')
                        from++

                    continue
                }

                else ->
                {
                    if (!ignoreInvalidSequences)
                        throw IllegalArgumentException("Invalid escape sequence: \\%c \\\\u%04X".format(ch, ch.code))
                }
            }
        }

        sequence[to++] = ch
    }
    return sequence.concatToString(endIndex = to)
}