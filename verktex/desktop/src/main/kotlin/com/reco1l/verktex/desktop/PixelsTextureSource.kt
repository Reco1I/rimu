package com.reco1l.verktex.desktop

import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack

/**
 * A texture source over premultiplied RGBA8 pixels, rows tightly packed from the top.
 */
class PixelsTextureSource(
    private val pixelWidth: Int,
    private val pixelHeight: Int,
    private val pixels: ByteBuffer,
) : GraphicsTexture.Source {

    init {
        require(pixels.remaining() >= pixelWidth * pixelHeight * 4) { "The buffer doesn't hold $pixelWidth x $pixelHeight pixels." }
    }

    override val width = pixelWidth.toFloat()
    override val height = pixelHeight.toFloat()

    override fun writePixels(target: GraphicsTexture.PixelTarget, x: Int, y: Int) {
        // A duplicate, so the source can be written again (e.g. after a context loss).
        target.write(x, y, pixelWidth, pixelHeight, pixels.duplicate().order(ByteOrder.nativeOrder()))
    }


    companion object {

        /**
         * Decodes an image file (PNG, JPEG, BMP, TGA…) with stb_image, or null when it can't be decoded.
         */
        fun decode(encoded: ByteArray): PixelsTextureSource? = MemoryStack.stackPush().use { stack ->
            val input = ByteBuffer.allocateDirect(encoded.size).put(encoded).flip()
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            // stb_image gives straight alpha, the pixels are premultiplied below as textures expect.
            val decoded = STBImage.stbi_load_from_memory(input, width, height, channels, 4) ?: return null

            // Copied to memory the garbage collector owns, so the source needs no explicit release. The copy
            // reads a duplicate: LWJGL frees the buffer's current position, which reading moves to its end.
            val pixels = ByteBuffer.allocateDirect(decoded.remaining()).order(ByteOrder.nativeOrder())
            pixels.put(decoded.duplicate()).flip()
            STBImage.stbi_image_free(decoded)
            premultiply(pixels)

            PixelsTextureSource(width[0], height[0], pixels)
        }

        /**
         * Premultiplies the RGBA8 [pixels] between the buffer's position and limit in place.
         */
        fun premultiply(pixels: ByteBuffer) {
            for (offset in pixels.position() until pixels.limit() - 3 step 4) {
                val alpha = pixels.get(offset + 3).toInt() and 0xFF
                if (alpha == 255) continue

                for (channel in 0 until 3) {
                    val value = pixels.get(offset + channel).toInt() and 0xFF
                    pixels.put(offset + channel, ((value * alpha + 127) / 255).toByte())
                }
            }
        }
    }
}
