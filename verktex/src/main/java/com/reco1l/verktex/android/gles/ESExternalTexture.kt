package com.reco1l.verktex.android.gles

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES32
import android.view.Surface
import com.reco1l.verktex.graphics.GraphicsTexture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A texture backed by a [SurfaceTexture] (`GL_TEXTURE_EXTERNAL_OES`), fed by a producer such as a
 * `MediaPlayer` drawing into [surface]. It has no storage or sources of its own, only [region]
 * covering it whole, and is sampled with `BuiltIn.Shaders.Video` through [transform].
 *
 * Create it with `GraphicsRenderer.createExternalTexture` so it is registered with the renderer;
 * constructing it directly skips registration, so it is never told about a context loss.
 */
open class ESExternalTexture(
    override val width: Float,
    override val height: Float,
) : GraphicsTexture() {

    override val isExternal get() = true

    /**
     * The column-major texture-coordinate transform of the latest frame, with the vertical flip
     * from the [SurfaceTexture]'s bottom-left origin to the engine's top-left one folded in.
     */
    override val transform = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    /**
     * The region covering the whole texture.
     */
    val region = Region(this, EmptySource, 0f, 0f, width, height)

    /**
     * The [SurfaceTexture] receiving the frames, null until the texture is loaded.
     */
    @Volatile
    var surfaceTexture: SurfaceTexture? = null
        private set

    /**
     * The [Surface] producers draw into, null until the texture is loaded.
     */
    @Volatile
    var surface: Surface? = null
        private set

    /**
     * Called every time the surface is (re)created, i.e. on the first load and after a context loss,
     * so the producer can attach to the new [surface]. It runs on the draw thread in the middle of a
     * draw (outside the texture's lock), so it must stay cheap. A handler that touches the component
     * tree must hop to the update thread first.
     *
     * After a context loss it fires late, see [onContextLost].
     */
    var onSurfaceRecreated: (() -> Unit)? = null


    private var id = 0

    // The frame listener fires on an arbitrary thread while the draw thread latches the frame.
    private val frameAvailable = AtomicBoolean(false)


    /**
     * Marks that a new frame is waiting, the next bind latches it.
     */
    fun markFrameAvailable() {
        frameAvailable.set(true)
    }


    override fun onCreateTexture() {
        id = GLES32Helper.glGenTexture()
        bindExternalTexture()

        GLES32.glTexParameteri(TARGET, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR)
        GLES32.glTexParameteri(TARGET, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR)
        GLES32.glTexParameteri(TARGET, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE)
        GLES32.glTexParameteri(TARGET, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE)

        val surfaceTexture = SurfaceTexture(id)
        surfaceTexture.setDefaultBufferSize(width.toInt(), height.toInt())
        surfaceTexture.setOnFrameAvailableListener { markFrameAvailable() }

        this.surfaceTexture = surfaceTexture
        surface = Surface(surfaceTexture)
    }

    override fun onTextureCreated() {
        onSurfaceRecreated?.invoke()
    }

    /**
     * Always false, an external texture is filled by its [surface] and holds no sources.
     */
    override fun fitsSource(source: Source) = false

    /**
     * Always null, an upload into the external texture's name would be invalid.
     */
    override fun addSource(source: Source): Region? = null

    override fun onBindTexture() {
        bindExternalTexture()

        if (frameAvailable.getAndSet(false)) {
            updateTexImage()
        }
    }

    /**
     * Binds the GL name to the external target.
     */
    protected open fun bindExternalTexture() {
        GLES32.glBindTexture(TARGET, id)
    }

    /**
     * Latches the latest frame into the texture and refreshes [transform].
     */
    protected open fun updateTexImage() {
        val surfaceTexture = surfaceTexture ?: return

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(transform)
        foldVerticalFlip(transform)
    }

    /**
     * Releases the [surface] right away, but the new one is only created, and [onSurfaceRecreated]
     * only fired (on the draw thread), on the next bind, i.e. when the sprite draws again. Until then
     * the producer keeps writing into the abandoned surface and its frames are lost.
     */
    override fun onContextLost() {
        // The GL name died with the context, the surface is recreated on the next load.
        releaseSurface()
        id = 0
        super.onContextLost()
    }

    override fun onUnload() {
        releaseSurface()

        if (id != 0) {
            GLES32Helper.glDeleteTexture(id)
            id = 0
        }
    }

    private fun releaseSurface() {
        surface?.release()
        surface = null
        surfaceTexture?.setOnFrameAvailableListener(null)
        surfaceTexture?.release()
        surfaceTexture = null
        frameAvailable.set(false)
    }


    private object EmptySource : Source {
        override val width = 0f
        override val height = 0f
        override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) = Unit
    }

    companion object {
        private const val TARGET = GLES11Ext.GL_TEXTURE_EXTERNAL_OES

        /**
         * Replaces the column-major [matrix] with `matrix * flip`, where flip maps v to 1 - v, turning
         * the [SurfaceTexture]'s bottom-left origin into the engine's top-left one.
         */
        fun foldVerticalFlip(matrix: FloatArray) {
            // Column 3 gains column 1, which is negated.
            for (row in 0 until 4) {
                matrix[12 + row] += matrix[4 + row]
                matrix[4 + row] = -matrix[4 + row]
            }
        }
    }
}
