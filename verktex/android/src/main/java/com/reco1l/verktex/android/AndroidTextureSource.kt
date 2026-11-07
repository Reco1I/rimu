package com.reco1l.verktex.android

import android.graphics.Bitmap
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

class AndroidTextureSource(source: Bitmap) : GraphicsTexture.Source {

    // The atlas is GL_RGBA8: RGBA_F16 (16-bit PNGs), RGB_565 and HARDWARE bitmaps fail to upload
    // (GL_INVALID_OPERATION), which leaves whatever was in the atlas before.
    val bitmap: Bitmap = if (source.config == Bitmap.Config.ARGB_8888) source
        else source.copy(Bitmap.Config.ARGB_8888, false)

    override val width = bitmap.width.toFloat()
    override val height = bitmap.height.toFloat()

    override fun writePixels(target: GraphicsTexture.PixelTarget, x: Int, y: Int) {
        val width = bitmap.width
        val height = bitmap.height

        // Rows can be padded past width * 4 bytes. Skia keeps the row bytes a multiple of the
        // pixel size, so the stride is a whole number of pixels.
        val rowStride = bitmap.rowBytes / 4
        val buffer = scratchBuffer(bitmap.rowBytes * height)

        // Raw pixels in RGBA byte order, getPixels() would un-premultiply them as ARGB ints.
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()

        // Textures are premultiplied, as bitmaps are unless told otherwise.
        if (!bitmap.isPremultiplied && bitmap.hasAlpha()) {
            premultiplyRows(buffer.asIntBuffer(), width, height, rowStride)
        }

        // Targets take tightly packed rows, OpenGL ES 2.0 has no GL_UNPACK_ROW_LENGTH to skip the padding.
        val pixels = if (rowStride == width) buffer else packRows(buffer, width, height, rowStride)

        target.write(x, y, width, height, pixels)
    }

    companion object {

        // The largest scratch buffer kept between uploads, 1024 x 1024 pixels. Larger ones (e.g.
        // backgrounds) are rare enough to allocate each time.
        private const val MAX_KEPT_SCRATCH_BYTES = 4 shl 20

        private val scratch = ThreadLocal<ByteBuffer>()

        /**
         * A direct little-endian buffer of [size] bytes between its position and limit. Small ones are
         * reused, so the content is only valid until the next call on the same thread.
         */
        private fun scratchBuffer(size: Int): ByteBuffer {
            val kept = scratch.get()
            if (kept != null && kept.capacity() >= size) {
                kept.clear().limit(size)
                return kept
            }

            val buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
            if (size <= MAX_KEPT_SCRATCH_BYTES) {
                scratch.set(buffer)
            }
            return buffer
        }

        /**
         * Premultiplies [height] rows of [width] pixels in place, each row starting [rowStride]
         * pixels after the previous one. The padding past [width] in a row is left untouched.
         *
         * Read as little-endian ints the pixels are ABGR, alpha stays in the top byte as [premultiply]
         * expects and the colour channels are converted the same way regardless of their order.
         */
        fun premultiplyRows(pixels: IntBuffer, width: Int, height: Int, rowStride: Int) {
            require(rowStride >= width) { "The row stride $rowStride is smaller than the width $width." }

            val row = IntArray(width)

            for (rowIndex in 0 until height) {
                val offset = rowIndex * rowStride

                pixels.position(offset)
                pixels.get(row)

                for (i in row.indices) {
                    row[i] = premultiply(row[i])
                }

                pixels.position(offset)
                pixels.put(row)
            }
        }

        /**
         * Copies [height] rows of [width] pixels, each starting [rowStride] pixels after the previous
         * one, into a new buffer without the padding.
         */
        fun packRows(source: ByteBuffer, width: Int, height: Int, rowStride: Int): ByteBuffer {
            require(rowStride >= width) { "The row stride $rowStride is smaller than the width $width." }

            val packed = ByteBuffer.allocateDirect(width * height * 4).order(source.order())
            val sourcePixels = source.duplicate().order(source.order()).asIntBuffer()
            val packedPixels = packed.asIntBuffer()
            val row = IntArray(width)

            for (rowIndex in 0 until height) {
                sourcePixels.position(rowIndex * rowStride)
                sourcePixels.get(row)
                packedPixels.put(row)
            }

            return packed
        }

        /**
         * Converts an ARGB pixel with straight alpha to premultiplied alpha.
         */
        fun premultiply(argb: Int): Int {
            val alpha = argb ushr 24

            if (alpha == 0) return 0
            if (alpha == 255) return argb

            fun channel(value: Int) = (value * alpha + 127) / 255

            val red = channel((argb ushr 16) and 0xFF)
            val green = channel((argb ushr 8) and 0xFF)
            val blue = channel(argb and 0xFF)

            return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
}
