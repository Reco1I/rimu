package com.reco1l.verktex

import com.reco1l.verktex.android.asByteBuffer
import android.graphics.Bitmap
import com.reco1l.verktex.android.AndroidTextureSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * Textures are stored with straight alpha, so premultiplied bitmaps are un-premultiplied on upload.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidTextureSourceTest {

    private fun assertChannelsNear(expected: Int, actual: Int) {
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val e = (expected ushr shift) and 0xFF
            val a = (actual ushr shift) and 0xFF
            assertTrue("channel at $shift: expected $e, got $a", kotlin.math.abs(e - a) <= 1)
        }
    }

    @Test
    fun unpremultiplyLeavesOpaquePixelsAndClearsTransparentOnes() {
        assertEquals(0xFFFF8000.toInt(), AndroidTextureSource.unpremultiply(0xFFFF8000.toInt()))
        assertEquals(0, AndroidTextureSource.unpremultiply(0x00123456))
        assertChannelsNear(0x80FF0000.toInt(), AndroidTextureSource.unpremultiply(0x80800000.toInt()))
    }

    @Test
    fun unpremultiplyNeverExceeds255() {
        for (alpha in 1..255) {
            // Channels larger than alpha are invalid premultiplied data, they must still clamp.
            val argb = (alpha shl 24) or 0xFFFFFF
            val result = AndroidTextureSource.unpremultiply(argb)
            assertEquals(alpha, result ushr 24)
            assertEquals(0xFFFFFF, result and 0xFFFFFF)
        }
    }

    private class Write(val x: Int, val y: Int, val width: Int, val height: Int, val pixels: IntArray)

    // Copies what the source writes, the buffer is only valid during the call.
    private fun writeOf(source: AndroidTextureSource, x: Int, y: Int): Write {
        var write: Write? = null

        source.writePixels({ wx, wy, width, height, pixels ->
            val ints = IntArray(pixels.remaining() / 4)
            pixels.asByteBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints)
            write = Write(wx, wy, width, height, ints)
        }, x, y)

        return write!!
    }

    @Test
    fun writeDoesNotModifyTheBitmap() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val pixels = intArrayOf(0xFFFF8000.toInt(), 0x80800000.toInt(), 0x00000000, 0x40202020)
        bitmap.setPixels(pixels, 0, 2, 0, 0, 2, 2)

        val before = IntArray(4)
        bitmap.getPixels(before, 0, 2, 0, 0, 2, 2)

        writeOf(AndroidTextureSource(bitmap), 0, 0)

        val after = IntArray(4)
        bitmap.getPixels(after, 0, 2, 0, 0, 2, 2)
        assertArrayEquals(before, after)
    }

    @Test
    fun writeGivesTightlyPackedRgbaRowsAtThePosition() {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(IntArray(6) { 0xFFFF8000.toInt() }, 0, 3, 0, 0, 3, 2)

        val write = writeOf(AndroidTextureSource(bitmap), 5, 7)

        assertEquals(5, write.x)
        assertEquals(7, write.y)
        assertEquals(3, write.width)
        assertEquals(2, write.height)
        // The byte order isn't checked, Robolectric's bitmaps copy ARGB ints instead of RGBA bytes.
        assertEquals(6, write.pixels.size)
    }

    @Test
    fun nonArgb8888BitmapsAreConvertedForTheRgba8Atlas() {
        for (config in listOf(Bitmap.Config.RGBA_F16, Bitmap.Config.RGB_565)) {
            val bitmap = Bitmap.createBitmap(3, 2, config)
            val source = AndroidTextureSource(bitmap)
            assertEquals(Bitmap.Config.ARGB_8888, source.bitmap.config)
            assertEquals(3f, source.width)
            assertEquals(2f, source.height)
        }
    }

    @Test
    fun argb8888BitmapsAreKeptWithoutACopy() {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        assertSame(bitmap, AndroidTextureSource(bitmap).bitmap)
    }

    @Test
    fun unpremultiplyRowsSkipsTheRowPadding() {
        // Two rows of two pixels, padded to a stride of three, the padding holds a sentinel.
        val padding = 0x40FFFFFF
        val pixels = IntBuffer.wrap(intArrayOf(
            0x80800000.toInt(), 0xFF102030.toInt(), padding,
            0x00123456, 0x80008000.toInt(), padding,
        ))

        AndroidTextureSource.unpremultiplyRows(pixels, 2, 2, 3)

        val result = pixels.array()
        assertChannelsNear(0x80FF0000.toInt(), result[0])
        assertEquals(0xFF102030.toInt(), result[1])
        assertEquals(padding, result[2])
        assertEquals(0, result[3])
        assertChannelsNear(0x8000FF00.toInt(), result[4])
        assertEquals(padding, result[5])
    }

    @Test
    fun packRowsDropsTheRowPadding() {
        val padding = 0x40FFFFFF
        val source = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.LITTLE_ENDIAN)
        source.asIntBuffer().put(intArrayOf(1, 2, padding, 3, 4, padding))

        val packed = AndroidTextureSource.packRows(source, 2, 2, 3)

        val result = IntArray(4)
        packed.asIntBuffer().get(result)
        assertArrayEquals(intArrayOf(1, 2, 3, 4), result)
        assertEquals(ByteOrder.LITTLE_ENDIAN, packed.order())
    }
}
