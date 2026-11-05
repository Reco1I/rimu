package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.android.asByteBuffer
import com.ditchoom.buffer.ReadBuffer
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.reco1l.verktex.android.AndroidExternalTexture
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

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

    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: ReadBuffer) {
        val bytes = pixels.asByteBuffer()
        val size = bytes.remaining()

        if (bytes.isDirect) {
            VKNative.uploadTexture(context.handle, handle, x, y, width, height, bytes, bytes.position(), size)
            return
        }

        val copy = directCopy?.takeIf { it.capacity() >= size }
            ?: ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).also { directCopy = it }
        copy.clear()
        copy.put(bytes.duplicate())
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
 * An external texture of the Vulkan backend, fed with video frames through an [ImageReader]'s
 * [surface]. Each new frame's hardware buffer is imported as an image of its own and sampled
 * through a YCbCr conversion, see `Context::importHardwareBuffer`.
 *
 * A frame's [Image] is only closed, giving its buffer back to the producer, once the GPU is done
 * with the frames that sampled it, so the decoder never writes a buffer being read.
 *
 * Needs Android 10 (GPU sampled image readers) and the device's support, see [isSupported]. Without
 * it there's no [surface], producers don't play into it and its draws are skipped.
 */
class VKExternalTexture internal constructor(
    override val width: Float,
    override val height: Float,
    private val context: VKContext,
) : GraphicsTexture(), AndroidExternalTexture {

    override val isExternal get() = true

    /**
     * The column-major texture-coordinate transform of the latest frame, mapping the region to the
     * frame's crop rectangle (decoders pad buffers, e.g. 1088 rows for 1080p).
     */
    override val transform = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    override val region = Region(this, EmptySource, 0f, 0f, width, height)

    @Volatile
    override var surface: Surface? = null
        private set

    override var onSurfaceRecreated: (() -> Unit)? = null

    /**
     * Whether frames can be fed, i.e. the device supports it and the texture is loaded.
     */
    val hasSurface: Boolean
        get() = surface != null

    /**
     * The imported texture of the latest frame, 0 until there's one.
     */
    var handle = 0L
        private set

    /**
     * The YCbCr sampler of the latest frame, see `VKNative.textureYcbcrSampler`.
     */
    var ycbcrSampler = 0L
        private set

    private var reader: ImageReader? = null
    private var currentImage: Image? = null

    // Frames replaced while the GPU may still sample them, closed once the submission is complete.
    private val retiredImages = ArrayDeque<Pair<Image, Long>>()

    // The listener fires on the main thread while the draw thread latches the frame.
    private val frameAvailable = AtomicBoolean(false)


    override fun markFrameAvailable() {
        frameAvailable.set(true)
    }

    override fun onCreateTexture() {
        if (reader != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !isSupported(context)) {
            return
        }

        // The producer sets the size of its buffers, the nominal size only applies until then.
        val reader = ImageReader.newInstance(
            max(1, width.toInt()),
            max(1, height.toInt()),
            ImageFormat.PRIVATE,
            MAX_IMAGES,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
        )
        reader.setOnImageAvailableListener({ markFrameAvailable() }, listenerHandler)

        this.reader = reader
        surface = reader.surface
    }

    override fun onTextureCreated() {
        onSurfaceRecreated?.invoke()
    }

    /**
     * Imports the latest frame if a new one arrived, called by the renderer on the draw thread
     * before it draws the texture.
     */
    fun latch() {
        closeRetiredImages()

        // The reader only exists from Android 10, the check is for lint.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val reader = reader ?: return
        if (!frameAvailable.getAndSet(false)) {
            return
        }

        val image = try {
            reader.acquireLatestImage()
        } catch (_: IllegalStateException) {
            // Every image is held (the GPU is behind), it's tried again on the next draw.
            frameAvailable.set(true)
            null
        } ?: return

        val buffer = image.hardwareBuffer
        val imported = if (buffer != null) VKNative.importHardwareBuffer(context.handle, buffer) else 0L
        buffer?.close()

        if (imported == 0L) {
            image.close()
            return
        }

        retireCurrentFrame()

        currentImage = image
        handle = imported
        ycbcrSampler = VKNative.textureYcbcrSampler(context.handle, imported)

        val crop = image.cropRect
        val imageWidth = image.width.toFloat()
        val imageHeight = image.height.toFloat()
        transform[0] = crop.width() / imageWidth
        transform[5] = crop.height() / imageHeight
        transform[12] = crop.left / imageWidth
        transform[13] = crop.top / imageHeight
    }

    private fun retireCurrentFrame() {
        if (handle != 0L) {
            // Destroyed once the submissions that may sample it are complete.
            VKNative.destroyTexture(context.handle, handle)
            handle = 0L
            ycbcrSampler = 0L
        }

        currentImage?.let { image ->
            // The frame being recorded may sample it as well.
            retiredImages.addLast(image to VKNative.submissionCount(context.handle) + 1)
        }
        currentImage = null
    }

    private fun closeRetiredImages() {
        if (retiredImages.isEmpty()) return

        val completed = VKNative.completedSubmissionCount(context.handle)
        while (retiredImages.isNotEmpty() && retiredImages.first().second <= completed) {
            retiredImages.removeFirst().first.close()
        }
    }

    /**
     * Always false, an external texture is filled by its [surface] and holds no sources.
     */
    override fun fitsSource(source: Source) = false

    override fun addSource(source: Source): Region? = null

    override fun onBindTexture() = Unit
    override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: ReadBuffer) = Unit

    override fun onUnload() {
        retireCurrentFrame()

        // The imports keep their buffers alive until the GPU is done, the reader can go right away.
        for ((image, _) in retiredImages) image.close()
        retiredImages.clear()

        surface = null
        reader?.setOnImageAvailableListener(null, null)
        reader?.close()
        reader = null
        frameAvailable.set(false)
    }


    private object EmptySource : Source {
        override val width = 0f
        override val height = 0f
        override fun writePixels(target: PixelTarget, x: Int, y: Int) = Unit
    }

    companion object {

        // The current frame, the ones the GPU may still sample (frames in flight) and one more
        // for acquireLatestImage.
        private const val MAX_IMAGES = 6

        private val listenerHandler by lazy { Handler(Looper.getMainLooper()) }

        /**
         * Whether video frames can be shown: GPU sampled image readers need Android 10.
         */
        internal fun isSupported(context: VKContext) =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && VKNative.supportsExternalTextures(context.handle)
    }
}
