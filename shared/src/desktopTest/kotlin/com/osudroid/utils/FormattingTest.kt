package com.osudroid.utils

import com.reco1l.verktex.localization.formatString
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The game's formats, which used Java's String.format, give the same text with formatString.
 */
class FormattingTest {

    private fun check(pattern: String, vararg args: Any?) =
        assertEquals("$pattern with ${args.toList()}", String.format(Locale.US, pattern, *args), formatString(pattern, *args))

    private val decimals = listOf(0.0, 0.004, 0.125, 1.0, 9.995, 12.3456, 99.99, 100.0, 1234.5678, -0.5, -12.345)

    @Test
    fun testDecimalPatterns() {
        for (pattern in listOf("%.2f%%", "%2.2f%%", "%.1f%%", "%.2fx", "%.2f", "%.0fpp", "UR: %.2f", "AR%.1f,", "FLD%.2f,")) {
            for (value in decimals) {
                check(pattern, value)
                check(pattern, value.toFloat())
            }
        }

        check("%.2f%%\n(+%.2f%%)", 98.76f, 0.12f)
        check("%.0fpp\n(+%.0fpp)", 345.0, 12.0)
        check("Error: %.2fms - %.2fms avg", -12.3456, 8.5)
    }

    @Test
    fun testIntegerPatterns() {
        for (value in listOf(0L, 7L, 1234L, 12_345_678L, 123_456_789L)) {
            check("%08d", value)
            check("%,d", value)
        }

        check("#%,d · %,dpp · %.2f%%", 12345, 6789, 98.765f)
        check("%d:%02d:%02d", 1, 5, 9)
        check("%02d:%02d", 3, 7)
        check("  %s(%s)", "1.2.3", "release")
    }

    @Test
    fun testHelpers() {
        assertEquals("1,234,567", formatNumber(1234567))
        assertEquals("03:07", formatDuration(187_000))
        assertEquals("59:59", formatDuration(3_599_999))
        assertEquals("01:00:01", formatDuration(3_601_000))
    }
}
