package com.reco1l.verktex.android

import android.graphics.Bitmap
import android.opengl.GLES32
import android.opengl.GLUtils
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.min

class AndroidTextureSource(source: Bitmap) : GraphicsTexture.Source {

    // The atlas is GL_RGBA8: RGBA_F16 (16-bit PNGs), RGB_565 and HARDWARE bitmaps fail to upload
    // (GL_INVALID_OPERATION), which leaves whatever was in the atlas before.
    val bitmap: Bitmap = if (source.config == Bitmap.Config.ARGB_8888) source
        else source.copy(Bitmap.Config.ARGB_8888, false)

    override val width = bitmap.width.toFloat()
    override val height = bitmap.height.toFloat()

    override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) {
        val x = texturePositionX.toInt()
        val y = texturePositionY.toInt()

        // Blending uses straight alpha, so premultiplied bitmaps are un-premultiplied first.
        val isPremultipliedArgb = bitmap.isPremultiplied && bitmap.hasAlpha() &&
            bitmap.config == Bitmap.Config.ARGB_8888

        if (!isPremultipliedArgb) {
            GLUtils.texSubImage2D(GLES32.GL_TEXTURE_2D, 0, x, y, bitmap)
            return
        }

        // Rows can be padded past width * 4 bytes. Skia keeps the row bytes a multiple of the
        // pixel size, so the stride is a whole number of pixels.
        val rowStride = bitmap.rowBytes / 4
        val buffer = ByteBuffer.allocateDirect(bitmap.rowBytes * bitmap.height).order(ByteOrder.LITTLE_ENDIAN)

        // Raw premultiplied pixels in RGBA byte order, getPixels() would already un-premultiply them.
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()

        unpremultiplyRows(buffer.asIntBuffer(), bitmap.width, bitmap.height, rowStride)

        buffer.rewind()

        // The padded rows are uploaded as they are, the row length makes GL skip the padding.
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ROW_LENGTH, rowStride)
        GLES32.glTexSubImage2D(GLES32.GL_TEXTURE_2D, 0, x, y, bitmap.width, bitmap.height, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, buffer)
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ROW_LENGTH, 0)
    }

    companion object {

        /**
         * Un-premultiplies [height] rows of [width] pixels in place, each row starting [rowStride]
         * pixels after the previous one. The padding past [width] in a row is left untouched.
         *
         * Read as little-endian ints the pixels are ABGR, alpha stays in the top byte as [unpremultiply]
         * expects and the colour channels are converted the same way regardless of their order.
         */
        fun unpremultiplyRows(pixels: IntBuffer, width: Int, height: Int, rowStride: Int) {
            require(rowStride >= width) { "The row stride $rowStride is smaller than the width $width." }

            val row = IntArray(width)

            for (rowIndex in 0 until height) {
                val offset = rowIndex * rowStride

                pixels.position(offset)
                pixels.get(row)

                for (i in row.indices) {
                    row[i] = unpremultiply(row[i])
                }

                pixels.position(offset)
                pixels.put(row)
            }
        }

        /**
         * Converts a premultiplied ARGB pixel to straight alpha.
         */
        fun unpremultiply(argb: Int): Int {
            val alpha = argb ushr 24

            if (alpha == 0) return 0
            if (alpha == 255) return argb

            fun channel(value: Int) = min(255, (value * 255 + alpha / 2) / alpha)

            val red = channel((argb ushr 16) and 0xFF)
            val green = channel((argb ushr 8) and 0xFF)
            val blue = channel(argb and 0xFF)

            return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
}
