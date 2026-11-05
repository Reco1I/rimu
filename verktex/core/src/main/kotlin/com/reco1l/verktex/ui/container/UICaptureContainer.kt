package com.reco1l.verktex.ui.container

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.FramebufferOrigin
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsResource
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.input.ScrollEvent
import kotlin.math.pow
import kotlin.math.sqrt


fun UIContainer.captureContainer(block: UICaptureContainer.() -> Unit): UICaptureContainer {
    return UICaptureContainer().apply(block).also(::plusAssign)
}

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
 * container, unless it blurs: what is behind it is still drawn then, and the blurred capture is drawn
 * over it within its bounds. It is captured again when its bounds in the window change.
 *
 * Like a [UIIgnoreParentPaddingContainer] it covers its parent including the padding, e.g. the
 * safe area, so a blurred capture reaches the edges, while its children keep to that padding.
 *
 * The capture can be blurred, see [blurRadius]. The blur is applied once when capturing, so showing
 * it costs the same as showing the plain capture. Its own background is drawn before the capture,
 * a tint over the blur must be the background of a child.
 */
open class UICaptureContainer : UIIgnoreParentPaddingContainer() {

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


    /**
     * How much what is behind is blurred, roughly the standard deviation of a Gaussian blur. Zero to
     * not blur it.
     */
    var blurRadius: Dimension.Fixed = 0f.px
        set(value) {
            if (field == value) return
            field = value
            invalidateCapture()
        }

    /**
     * Whether presses and scrolls that no child handled are consumed, so they don't reach what is
     * behind, which is covered.
     */
    var isBlockingInput = true


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
        val isOverlayRoot = Verktex.isOverlayRoot(this)
        val isBlurring = blurRadius.value > 0f

        if (isOverlayRoot || isBlurring) {
            updateSceneRect()

            // Everything behind was drawn this frame, it is captured before drawing the content.
            if (!isShowingCapture) {
                val texture = ensureTexture()
                renderer.captureFramebuffer(texture)

                if (isBlurring) {
                    // Elsewhere what is outside this container stays as it is, the blur is kept within it.
                    if (!isOverlayRoot) renderer.pushScissor(0f, 0f, bounds.width, bounds.height)
                    blur(renderer, texture)
                    if (!isOverlayRoot) renderer.popScissor()
                }
                isCaptured = true
            }

            if (!isOverlayRoot && isShowingCapture) {
                drawBackdrop(renderer)
            }
        }

        super.onDraw(renderer)
    }

    // The bounds in the window the capture was taken for, as left, top, right and bottom.
    private val capturedSceneRect = FloatArray(4)
    private val sceneRect = FloatArray(4)

    private fun updateSceneRect() {
        localToSceneMatrix.transform(0f, 0f, sceneRect, 0)
        localToSceneMatrix.transform(bounds.width, bounds.height, sceneRect, 2)

        if (!sceneRect.contentEquals(capturedSceneRect)) {
            sceneRect.copyInto(capturedSceneRect)
            isCaptured = false
        }
    }

    /**
     * Draws the part of the capture behind this container over it.
     */
    private fun drawBackdrop(renderer: GraphicsRenderer) {
        val texture = texture ?: return
        val region = region ?: return

        renderer.pushBlendMode(BlendMode.Opaque)
        renderer.drawTexture(
            texture = texture,
            region = region,
            x = 0f,
            y = 0f,
            width = bounds.width,
            height = bounds.height,
            uv = Vec4(
                sceneRect[0] / texture.width,
                renderer.captureV(sceneRect[1] / texture.height),
                sceneRect[2] / texture.width,
                renderer.captureV(sceneRect[3] / texture.height)
            )
        )
        renderer.popBlendMode()
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
            uv = Vec4(0f, renderer.captureV(0f), 1f, renderer.captureV(1f))
        )
        renderer.popBlendMode()
    }

    /**
     * Blurs the captured [texture] by drawing it over the window with the blur shader and capturing
     * it again, one axis at a time. Each pass doubles the spacing of the taps of the last one, so a
     * large radius takes a few passes instead of many taps.
     */
    private fun blur(renderer: GraphicsRenderer, texture: GraphicsTexture) {
        val radius = blurRadius.toPixels()

        // A pass has a deviation of two steps, the variances of all passes add up to the radius.
        // Steps longer than 1.5 pixels skip over details in the first pass, more passes are used then.
        fun firstStep(passes: Int) = radius / (2f * sqrt((4f.pow(passes) - 1f) / 3f))

        var passes = 1
        while (passes < MAX_BLUR_PASSES && firstStep(passes) > 1.5f) passes++

        var step = firstStep(passes)

        renderer.pushBlendMode(BlendMode.Opaque)
        repeat(passes) {
            drawBlurPass(renderer, texture, Vec2(step / texture.width, 0f))
            renderer.captureFramebuffer(texture)
            drawBlurPass(renderer, texture, Vec2(0f, step / texture.height))
            renderer.captureFramebuffer(texture)
            step *= 2f
        }
        renderer.popBlendMode()
    }

    private fun drawBlurPass(renderer: GraphicsRenderer, texture: GraphicsTexture, direction: Vec2) = with(renderer) {
        setState(
            shader = BuiltIn.Shaders.Blur,
            texture = texture,
            uniformSet = GraphicsShader.UniformSet().set("uDirection", direction)
        )

        val width = texture.width
        val height = texture.height

        val top = captureV(0f)
        val bottom = captureV(1f)

        // The window.
        drawGeometry { _, _ ->
            addVertex(0f, 0f, 0f, top, Color4.White)
            addVertex(width, 0f, 1f, top, Color4.White)
            addVertex(width, height, 1f, bottom, Color4.White)

            addVertex(0f, 0f, 0f, top, Color4.White)
            addVertex(width, height, 1f, bottom, Color4.White)
            addVertex(0f, height, 0f, bottom, Color4.White)
        }
    }

    // The v of the capture for a v going down the window, the capture is upside down when the
    // framebuffer starts at the bottom.
    private fun GraphicsRenderer.captureV(v: Float) = if (framebufferOrigin == FramebufferOrigin.BottomLeft) 1f - v else v

    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        // Only presses, a pointer pressed before this was shown must still be released behind.
        return isBlockingInput && (event is PointerEvent && event.action == PointerEvent.Action.Down || event is ScrollEvent)
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


    companion object {
        private const val MAX_BLUR_PASSES = 6
    }


    // The texture is filled by copying the framebuffer, nothing is uploaded.
    private class CaptureSource(override val width: Float, override val height: Float) : GraphicsTexture.Source {
        override fun writePixels(target: GraphicsTexture.PixelTarget, x: Int, y: Int) = Unit
    }
}
