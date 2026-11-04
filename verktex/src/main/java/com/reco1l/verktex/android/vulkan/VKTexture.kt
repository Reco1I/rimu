package com.reco1l.verktex.android.vulkan

import android.view.Surface
import com.reco1l.verktex.android.AndroidExternalTexture
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer

/**
 * A texture of the Vulkan backend. Phase 4 of the roadmap has no images yet: sources are packed as on
 * any backend but nothing is uploaded, and draws sampling a texture are skipped.
 */
class VKTexture(
    override val width: Float,
    override val height: Float,
) : GraphicsTexture() {

    override fun onCreateTexture() = Unit
    override fun onBindTexture() = Unit
    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: ByteBuffer) = Unit
    override fun onUnload() = Unit
}

/**
 * An external texture of the Vulkan backend. Until video frames are imported (phase 6) it never has
 * a [surface], so producers don't play into it.
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
