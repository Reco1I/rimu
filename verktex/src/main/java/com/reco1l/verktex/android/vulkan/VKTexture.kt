package com.reco1l.verktex.android.vulkan

import android.view.Surface
import com.reco1l.verktex.android.AndroidExternalTexture
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An RGBA8 texture of the Vulkan backend. Uploads are staged and copied before the frame's render
 * pass, see [GraphicsTexture.flushUploads], which the renderer calls for each texture it draws.
 *
 * It may be created and released from any thread, the image is only destroyed once the GPU is done
 * with it.
 */
class VKTexture internal constructor(
    override val width: Float,
    override val height: Float,
    private val context: VKContext,
) : GraphicsTexture() {

    /**
     * The native texture, 0 until loaded.
     */
    @Volatile
    var handle = 0L
        private set

    // Uploads need a direct buffer, sources normally provide one.
    private var directCopy: ByteBuffer? = null


    override fun onCreateTexture() {
        if (handle == 0L) {
            handle = VKNative.createTexture(context.handle, width.toInt(), height.toInt())
            check(handle != 0L) { "Couldn't create a ${width.toInt()}x${height.toInt()} texture." }
        }
    }

    override fun onBindTexture() = Unit

    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: ByteBuffer) {
        val size = pixels.remaining()

        if (pixels.isDirect) {
            VKNative.uploadTexture(context.handle, handle, x, y, width, height, pixels, pixels.position(), size)
            return
        }

        val copy = directCopy?.takeIf { it.capacity() >= size }
            ?: ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).also { directCopy = it }
        copy.clear()
        copy.put(pixels.duplicate())
        VKNative.uploadTexture(context.handle, handle, x, y, width, height, copy, 0, size)
    }

    override fun onUnload() {
        if (handle != 0L) {
            VKNative.destroyTexture(context.handle, handle)
            handle = 0L
        }
    }
}

/**
 * An external texture of the Vulkan backend. Until video frames are imported (phase 6) it never has
 * a [surface], so producers don't play into it and its draws are skipped.
 */
class VKExternalTexture(
    override val width: Float,
    override val height: Float,
) : GraphicsTexture(), AndroidExternalTexture {

    override val isExternal get() = true

    override val transform = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    override val region = Region(this, EmptySource, 0f, 0f, width, height)

    override val surface: Surface?
        get() = null

    override var onSurfaceRecreated: (() -> Unit)? = null

    override fun markFrameAvailable() = Unit

    override fun fitsSource(source: Source) = false
    override fun addSource(source: Source): Region? = null

    override fun onCreateTexture() = Unit
    override fun onBindTexture() = Unit
    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: ByteBuffer) = Unit
    override fun onUnload() = Unit

    private object EmptySource : Source {
        override val width = 0f
        override val height = 0f
        override fun writePixels(target: PixelTarget, x: Int, y: Int) = Unit
    }
}
