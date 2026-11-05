package com.reco1l.verktex.math

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun roundsHalfUpToTheDecimals() {
        assertEquals("2", 1.5.toFixed(0))
        assertEquals("0.3", 0.25.toFixed(1))
        assertEquals("0.13", 0.125.toFixed(2))
        assertEquals("16.67", (50.0 / 3.0).toFixed(2))
        assertEquals("60", 59.96f.toFixed(0))
    }

    @Test
    fun padsTheDecimals() {
        assertEquals("3.00", 3.0.toFixed(2))
        assertEquals("0.05", 0.05.toFixed(2))
        assertEquals("1.0", 1f.toFixed(1))
    }

    @Test
    fun keepsTheSign() {
        assertEquals("-1.25", (-1.25).toFixed(2))
        assertEquals("0.0", (-0.01).toFixed(1))
    }

    @Test
    fun usesAPointWhateverTheLocale() {
        assertEquals("1234.5", 1234.5.toFixed(1))
    }
}
