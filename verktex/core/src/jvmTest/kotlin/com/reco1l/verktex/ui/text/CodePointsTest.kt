package com.reco1l.verktex.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The common code point helpers match Java's String and Character.
 */
class CodePointsTest {

    // ASCII, a BMP char, an emoji (surrogate pair) and lone surrogates at the edges.
    private val text = "a€😀b\uD83D" + "x\uDE00"

    @Test
    fun codePointAtMatchesJava() {
        for (i in text.indices) {
            assertEquals("at $i", Character.codePointAt(text, i), (text as CharSequence).codePointAt(i))
        }
    }

    @Test
    fun codePointCountMatchesJava() {
        for (begin in 0..text.length) {
            for (end in begin..text.length) {
                assertEquals("$begin..$end", Character.codePointCount(text, begin, end), (text as CharSequence).codePointCount(begin, end))
            }
        }
    }

    @Test
    fun charCountAndStringMatchJava() {
        for (codePoint in intArrayOf(0x41, 0x20AC, 0xFFFF, 0x10000, 0x1F600, 0x10FFFF)) {
            assertEquals(Character.charCount(codePoint), charCountOf(codePoint))
            assertEquals(String(Character.toChars(codePoint)), codePointToString(codePoint))
        }
    }
}
