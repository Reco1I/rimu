package com.reco1l.verktex

import com.reco1l.verktex.graphics.AlphaBleeding
import org.junit.Test
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals

class AlphaBleedingTest {

    @Test
    fun transparentPixelsTakeTheColourOfTheirVisibleNeighboursAndStayTransparent() {
        val pixels = intArrayOf(
            0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0xFFC080FF.toInt(), 0x80C080FF.toInt(), 0x00000000,
        )

        AlphaBleeding.bleed(pixels, 4, 2)

        for (i in intArrayOf(0, 1, 2, 3, 4, 7)) {
            assertEquals("pixel $i", 0x00C080FF, pixels[i])
        }
        assertEquals(0xFFC080FF.toInt(), pixels[5])
        assertEquals(0x80C080FF.toInt(), pixels[6])
    }

    @Test
    fun ringsAverageOnlyThePixelsFilledBeforeThem() {
        // Red on the left, blue on the right: the middle pixel averages both, the far ones copy theirs.
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0, 0, 0, 0xFF0000FF.toInt())

        AlphaBleeding.bleed(pixels, 5, 1)

        assertArrayEquals(intArrayOf(0xFFFF0000.toInt(), 0x00FF0000, 0x007F007F, 0x000000FF, 0xFF0000FF.toInt()), pixels)
    }

    @Test
    fun opaqueAndFullyTransparentImagesAreLeftAlone() {
        val opaque = intArrayOf(0xFF102030.toInt(), 0xFF405060.toInt())
        AlphaBleeding.bleed(opaque, 2, 1)
        assertArrayEquals(intArrayOf(0xFF102030.toInt(), 0xFF405060.toInt()), opaque)

        val transparent = intArrayOf(0x00123456, 0x00000000)
        AlphaBleeding.bleed(transparent, 2, 1)
        assertArrayEquals(intArrayOf(0x00123456, 0x00000000), transparent)
    }
}
