package com.reco1l.verktex.ui.text

// Unicode code points over UTF-16 text, as Java's String and Character handle them.

private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000

/**
 * The code point starting at [index]: a surrogate pair is combined, any other char is returned as is.
 */
fun CharSequence.codePointAt(index: Int): Int {
    val high = this[index]

    if (high.isHighSurrogate() && index + 1 < length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + MIN_SUPPLEMENTARY_CODE_POINT
        }
    }

    return high.code
}

/**
 * The number of code points between [beginIndex] and [endIndex], a surrogate pair counting as one.
 */
fun CharSequence.codePointCount(beginIndex: Int = 0, endIndex: Int = length): Int {
    var count = 0
    var i = beginIndex

    while (i < endIndex) {
        i += if (this[i].isHighSurrogate() && i + 1 < endIndex && this[i + 1].isLowSurrogate()) 2 else 1
        count++
    }

    return count
}

/**
 * The number of chars the code point takes in UTF-16, 2 for a surrogate pair.
 */
fun charCountOf(codePoint: Int): Int = if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

/**
 * The code point as a string of one or two chars.
 */
fun codePointToString(codePoint: Int): String {
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
        return Char(codePoint).toString()
    }

    val offset = codePoint - MIN_SUPPLEMENTARY_CODE_POINT
    return charArrayOf(Char(0xD800 + (offset shr 10)), Char(0xDC00 + (offset and 0x3FF))).concatToString()
}
