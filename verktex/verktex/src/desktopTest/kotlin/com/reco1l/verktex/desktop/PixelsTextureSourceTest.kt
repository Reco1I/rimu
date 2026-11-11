package com.reco1l.verktex.desktop

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PixelsTextureSourceTest {

    @Test
    fun `Decodes a PNG with an alpha channel`() {
        // Encoded here, so the test needs no image file.
        val image = BufferedImage(37, 18, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(3, 4, 0x80FF4020.toInt())

        val bytes = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        val source = PixelsTextureSource.decode(bytes)

        assertNotNull(source)
        assertEquals(37f, source!!.width)
        assertEquals(18f, source.height)
    }
}
