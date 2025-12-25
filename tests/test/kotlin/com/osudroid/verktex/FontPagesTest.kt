package com.osudroid.verktex

import com.reco1l.verktex.data.px
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsTexture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A font gets a further atlas page when the current one has no room left, so already placed
 * glyphs stay where they are.
 */
class FontPagesTest {

    private class FakeSource(override val width: Float, override val height: Float) : GraphicsTexture.Source {
        override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) = Unit
    }

    private class FakeTexture(override val width: Float, override val height: Float) : GraphicsTexture() {
        var disposed = false
        override fun onCreateTexture() = Unit
        override fun onBindTexture() = Unit
        override fun onUnload() = Unit
        override fun dispose() {
            disposed = true
            super.dispose()
        }
    }

    /** Every glyph is a [glyphSize] square; every page is a [pageSize] square. */
    private class FakeFont(private val pageSize: Float, private val glyphSize: Float) : Font() {

        val createdPages = mutableListOf<FakeTexture>()

        override val descriptor = Descriptor(FontFamily("fake", object : FontFamily.Source {
            override fun onCreateFont(descriptor: Descriptor): Font = throw UnsupportedOperationException()
        }), 10f.px)

        override val metrics = Metrics(ascent = 0f, descent = 0f, leading = 0f)
        override val isKerningSupported = false

        override fun onCreatePage(): GraphicsTexture = FakeTexture(pageSize, pageSize).also { createdPages += it }

        override fun onCreateGlyph(codePoint: Int) = Glyph(
            codePoint = codePoint,
            advance = glyphSize,
            width = glyphSize,
            height = glyphSize,
            region = addGlyphSource(FakeSource(glyphSize, glyphSize)),
        )

        override fun getKerning(first: Int, second: Int) = 0f
    }

    @Test
    fun `glyphs fill the first page before a second one is created`() {
        val font = FakeFont(pageSize = 4f, glyphSize = 2f)

        val firstFour = (0 until 4).map { font['a'.code + it] }

        assertEquals(1, font.pages.size)
        firstFour.forEach { assertSame(font.pages[0], it.region!!.texture) }
    }

    @Test
    fun `a glyph that does not fit goes to a new page and the old ones keep their place`() {
        val font = FakeFont(pageSize = 4f, glyphSize = 2f)

        val firstFour = (0 until 4).map { font['a'.code + it] }
        val placements = firstFour.map { it.region!!.x to it.region!!.y }

        val fifth = font['e'.code]

        assertEquals(2, font.pages.size)
        assertSame(font.pages[1], fifth.region!!.texture)
        assertEquals(placements, firstFour.map { it.region!!.x to it.region!!.y })
        firstFour.forEach { assertSame(font.pages[0], it.region!!.texture) }
    }

    @Test
    fun `glyphs are created once`() {
        val font = FakeFont(pageSize = 4f, glyphSize = 2f)

        assertSame(font['a'.code], font['a'.code])
        assertEquals(1, font.createdPages.size)
    }

    @Test
    fun `a glyph larger than an empty page fails instead of looping`() {
        val font = FakeFont(pageSize = 2f, glyphSize = 3f)

        assertThrows(IllegalStateException::class.java) { font['a'.code] }
        // The page it could not be placed on is not kept around.
        assertTrue(font.createdPages.all { it.disposed })
        assertEquals(0, font.pages.size)
    }

    @Test
    fun `dispose disposes every page`() {
        val font = FakeFont(pageSize = 4f, glyphSize = 2f)
        (0 until 5).forEach { font['a'.code + it] }
        assertEquals(2, font.pages.size)

        font.dispose()

        assertTrue(font.createdPages.all { it.disposed })
        assertEquals(0, font.pages.size)
    }
}
