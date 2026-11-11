package com.reco1l.verktex.desktop

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PixelsTextureSourceTest {

    @Test
    fun `Decodes a PNG with an alpha channel`() {
        val file = File("../../assets/gfx/welcome.png")
        val source = PixelsTextureSource.decode(file.readBytes())

        assertNotNull(source)
        assertEquals(375f, source!!.width)
        assertEquals(78f, source.height)
    }
}
