package com.reco1l.framework.extensions

import java.util.Locale


/**
 * Returns a copy of this string having its first letter in lowercase.
 */
fun String.decapitalize(locale: Locale = Locale.getDefault()) =
    replaceFirstChar { it.lowercase(locale) }

/**
 * Returns a copy of this string trimmed or `null` if it's blank.
 */
fun String.sanitize() = trim().takeUnless { it.isBlank() }

/**
 * Convert the string to nullable if it's empty.
 */
fun String.dropIfEmpty() = takeUnless { it.isEmpty() }

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
 * Returns a copy of this string excluding the part between the specified characters (inclusive) or
 * the same string if no range was found.
 */
fun String.removeBetween(first: Char, last: Char): String
{
    val left = substringBefore(first, missingDelimiterValue = "")
    val right = substringAfterLast(last, missingDelimiterValue = "")

    return when
    {
        left.isEmpty() && right.isNotEmpty() -> right

        right.isEmpty() && left.isNotEmpty() -> left

        left.isNotEmpty() && right.isNotEmpty() -> left + right

        else -> this
    }
}

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
    val DECIMAL = Regex("^-?\\d+(\\.\\d+$)?$")

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