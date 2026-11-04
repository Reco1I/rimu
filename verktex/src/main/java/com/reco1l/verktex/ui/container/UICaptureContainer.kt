package com.reco1l.verktex.ui.container

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsResource
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * An overlay that captures what is drawn behind it, the scene and the overlays below it, and
 * shows that capture instead of drawing them again, until [invalidateCapture] is called. Meant for
 * overlays covering most of the screen, such as menus, which make what is behind them not worth
 * drawing every frame.
 *
 * What is behind keeps being updated, only its drawing is skipped. Changes to it are seen once the
 * capture is invalidated, or once this container is removed.
 *
 * It only skips drawing as a direct overlay, see [Verktex.addOverlay]. Anywhere else it is a plain
 * container.
 */
open class UICaptureContainer : UIContainer() {

    /**
     * Whether what is behind is shown from the capture instead of being drawn.
     */
    val isShowingCapture: Boolean
        get() {
            val texture = texture ?: return false

            // A context loss empties the texture, it is captured again then.
            return isCaptured && texture.state == GraphicsResource.State.Loaded
                && texture.width.toInt() == Verktex.graphics.window.windowWidth
                && texture.height.toInt() == Verktex.graphics.window.windowHeight
        }


    @Volatile
    private var isCaptured = false

    private var texture: GraphicsTexture? = null
    private var region: GraphicsTexture.Region? = null


    /**
     * Draws what is behind again in the next frame and captures it, e.g. after it changed.
     */
    fun invalidateCapture() {
        isCaptured = false
    }


    override fun onDraw(renderer: GraphicsRenderer) {
        // Everything behind was drawn this frame, it is captured before drawing the content.
        if (!isShowingCapture && Verktex.isOverlayRoot(this)) {
            renderer.captureFramebuffer(ensureTexture())
            isCaptured = true
        }

        super.onDraw(renderer)
    }

    /**
     * Draws the capture over the whole window, in place of what is behind.
     */
    internal fun drawCapture(renderer: GraphicsRenderer) {
        val texture = texture ?: return
        val region = region ?: return

        // Opaque, it stands for everything drawn behind including the background.
        renderer.pushBlendMode(BlendMode.Opaque)
        renderer.drawTexture(
            texture = texture,
            region = region,
            x = 0f,
            y = 0f,
            width = texture.width,
            height = texture.height,
            // The framebuffer starts at the bottom.
            uv = Vec4(0f, 1f, 1f, 0f)
        )
        renderer.popBlendMode()
    }

    private fun ensureTexture(): GraphicsTexture {
        val width = Verktex.graphics.window.windowWidth.toFloat()
        val height = Verktex.graphics.window.windowHeight.toFloat()

        val current = texture
        if (current != null && current.width == width && current.height == height) {
            return current
        }

        // A new size, e.g. after a rotation or another resolution scale. This runs while drawing,
        // on the draw worker, where the old texture can be deleted.
        if (current != null) {
            current.dispose()
            Verktex.graphics.unregisterResource(current)
        }

        val texture = Verktex.graphics.createTexture(width, height)
        this.texture = texture
        region = GraphicsTexture.Region(texture, CaptureSource(width, height), 0f, 0f, width, height)
        return texture
    }

    override fun onDetached() {
        super.onDetached()
        isCaptured = false

        val texture = texture ?: return
        this.texture = null
        region = null

        // It isn't kept while hidden, a capture of the window takes a few megabytes.
        Verktex.scheduleOnDraw {
            texture.dispose()
            Verktex.graphics.unregisterResource(texture)
        }
    }


    // The texture is filled by copying the framebuffer, nothing is uploaded.
    private class CaptureSource(override val width: Float, override val height: Float) : GraphicsTexture.Source {
        override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) = Unit
    }
}
