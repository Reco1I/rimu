package com.reco1l.verktex.graphics

import com.osudroid.math.toRadians
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Mat2
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Logger
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.math.toRadians
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The main renderer class that handles drawing commands and manages the rendering state.
 */
abstract class GraphicsRenderer {

    /**
     * The device associated with the renderer, which provides access to graphics hardware capabilities
     * and resource management.
     */
    abstract val device: GraphicsDevice

    /**
     * The context in which the renderer operates. This could represent the rendering context or environment.
     */
    abstract val context: GraphicsContext

    /**
     * The window associated with the renderer, which represents the display area for rendering.
     */
    abstract val window: GraphicsWindow


    /**
     * The version of the graphics API being used by the renderer.
     */
    abstract var version: String


    private val commandBatch = ArrayDeque<DrawCommand>()
    private val gpuResources = ArrayDeque<GraphicsResource>()


    //region Stacks
    private val matrixStack = ArrayDeque<Mat2>()
    private val colorStack = ArrayDeque<Color4>()
    private val scissorStack = ArrayDeque<Vec4>()
    //endregion

    //region State
    private var activeShader: GraphicsShader? = null
    private var activeBuffer: GraphicsBuffer? = null
    private var activeTexture: GraphicsTexture? = null
    private var activeScissor: Vec4? = null
    private var activeBlendMode: BlendMode = BlendMode.Additive
    private var activeDepthState: DepthState = DepthState.Default
    private var activeUniformSet: GraphicsShader.UniformSet = GraphicsShader.UniformSet()


    private fun DrawCommand.canReuse(
        shader: GraphicsShader?,
        texture: GraphicsTexture?,
        scissor: Vec4?,
        blendMode: BlendMode,
        depthState: DepthState,
        uniformSet: GraphicsShader.UniformSet,
    ): Boolean {
        return this.shader == shader &&
                this.uniformSet == uniformSet &&
                this.texture == texture &&
                this.blendMode == blendMode &&
                this.depthState == depthState &&
                this.scissor == scissor
    }


    /**
     * Sets the current rendering state, including shader, texture, scissor rectangle, blend mode,
     * depth state, and uniform set.
     *
     * If the last command in the batch can be reused with the new state, it will not create a new
     * command.
     */
    fun setState(
        shader: GraphicsShader? = activeShader,
        texture: GraphicsTexture? = activeTexture,
        scissor: Vec4? = activeScissor,
        blendMode: BlendMode = activeBlendMode,
        depthState: DepthState = activeDepthState,
        uniformSet: GraphicsShader.UniformSet = activeUniformSet
    ) {
        val lastCommand = commandBatch.lastOrNull()
        if (lastCommand != null &&
            lastCommand.canReuse(
                shader,
                texture,
                scissor,
                blendMode,
                depthState,
                uniformSet,
            )
        ) return

        val vertexBuffer = createVertexBuffer()

        activeTexture = texture
        activeShader = shader
        activeBuffer = vertexBuffer
        activeScissor = scissor
        activeBlendMode = blendMode
        activeDepthState = depthState
        activeUniformSet = uniformSet

        val command = DrawCommand(
            buffer = vertexBuffer,
            shader = shader,
            texture = texture,
            scissor = scissor,
            blendMode = blendMode,
            depthState = depthState,
            uniformSet = uniformSet
        )

        commandBatch.addLast(command)
    }

    /**
     * Pushes a transformation matrix onto the matrix stack. This matrix will be applied to subsequent drawing operations.
     */
    fun pushMatrix(matrix: Mat2) {
        matrixStack.addLast(matrix)
    }

    /**
     * Pops a transformation matrix from the matrix stack.
     */
    fun popMatrix() {
        matrixStack.removeLastOrNull()
    }

    /**
     * Pushes a color onto the color stack. This color will be used to transform subsequent drawing operations.
     */
    fun pushColor(color: Color4) {
        colorStack.addLast(transformColor(color))
    }

    /**
     * Pops a color from the color stack and returns it.
     */
    fun popColor(): Color4? {
        return colorStack.removeLastOrNull()
    }

    /**
     * Pushes a scissor rectangle onto the scissor stack. This rectangle will be used to clip subsequent drawing operations.
     */
    fun pushScissor(x: Float, y: Float, width: Float, height: Float) {

        val currentMatrix = matrixStack.lastOrNull() ?: Mat2.Identity

        val (x, y) = currentMatrix.transform(x, y)
        val (width, height) = currentMatrix.transform(width, height)

        val intersectedX: Float
        val intersectedY: Float
        val intersectedWidth: Float
        val intersectedHeight: Float

        if (scissorStack.isEmpty()) {
            intersectedX = x
            intersectedY = y
            intersectedWidth = width
            intersectedHeight = height
        } else {
            val current = scissorStack.last()

            val minX = max(current.x, x)
            val minY = max(current.y, y)
            val maxX = min(current.x + current.z, x + width)
            val maxY = min(current.y + current.w, y + height)

            intersectedX = minX
            intersectedY = minY
            intersectedWidth = (maxX - minX)
            intersectedHeight = (maxY - minY)
        }

        val rect = Vec4(intersectedX, intersectedY, intersectedWidth, intersectedHeight)
        scissorStack.addLast(rect)
    }

    /**
     * Pops a scissor rectangle from the scissor stack and returns it.
     */
    fun popScissor(): Vec4? {
        return scissorStack.removeLastOrNull()
    }

    /**
     * Transforms the given color by multiplying its alpha value with the alpha of the last color in
     * the color stack.
     *
     * If the color stack is empty, the original color is returned.
     */
    fun transformColor(color: Color4): Color4 {
        return color.copy(alpha = color.alpha * (colorStack.lastOrNull()?.alpha ?: 1f))
    }

    //endregion

    //region Resource Management

    /**
     * Registers a GPU resource with the renderer. This allows the renderer to manage the resource's
     * lifecycle and handle context loss events.
     */
    fun registerResource(resource: GraphicsResource) {
        gpuResources.addLast(resource)
    }

    /**
     * Unregisters a GPU resource from the renderer.
     */
    fun unregisterResource(resource: GraphicsResource) {
        gpuResources.remove(resource)
    }

    /**
     * Marks all registered GPU resources as lost due to a context loss event and should be recreated.
     */
    fun markContextLoss() {
        for (resource in gpuResources) {
            resource.markContextLost()
        }
    }

    /**
     * Creates a new shader instance with the specified source.
     */
    fun createShader(shaderSource: GraphicsShader.Source): GraphicsShader {
        val shader = device.createShader(shaderSource)
        registerResource(shader)
        return shader
    }

    /**
     * Creates a new texture with the specified width and height.
     */
    fun createTexture(width: Float, height: Float): GraphicsTexture {
        val texture = device.createTexture(width, height)
        registerResource(texture)
        return texture
    }

    /**
     * Creates a new vertex buffer for storing vertex data.
     */
    fun createVertexBuffer(): GraphicsBuffer {
        val buffer = device.createBuffer()
        registerResource(buffer)
        return buffer
    }

    //endregion

    //region Shape

    fun GraphicsBuffer.addVertex(x: Float, y: Float, u: Float, v: Float, color: Color4) {
        requireCapacity(position + VERTEX_2D_STRIDE)
        putFloat(x)
        putFloat(y)
        putFloat(u)
        putFloat(v)
        putByte(color.red8.toByte())
        putByte(color.green8.toByte())
        putByte(color.blue8.toByte())
        putByte(color.alpha8.toByte())
    }


    /**
     * Allows to draw custom geometry by providing a block that operates on the active vertex buffer.
     */
    fun drawGeometry(block: GraphicsBuffer.(transform: Mat2, baseColor: Color4) -> Unit) {
        activeBuffer?.apply {
            val transform = matrixStack.lastOrNull() ?: Mat2.Identity
            val baseColor = colorStack.lastOrNull() ?: Color4.White

            block(transform, baseColor)
        } ?: throw IllegalStateException("No active vertex buffer to draw geometry. Make sure to call setState() before drawing.")
    }

    /**
     * Draws a rectangle with the specified position, size, color, border width, border color, and corner radius.
     */
    fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color4 = Color4.Transparent,
        borderWidth: Float = 0f,
        borderColor: Color4 = Color4.Transparent,
        radius: Float = 0f
    ) {
        setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = GraphicsShader.UniformSet().apply {
                set("uRadius", radius)
                set("uBorderWidth", borderWidth)
                set("uBorderColor", borderColor)
            },
        )

        drawGeometry { transform, baseColor ->

            val color = transformColor(color)

            val (x, y) = transform.transform(x, y)
            val (w, h) = transform.transform(width, height)

            // Triangle 1
            addVertex(x, y, 0f, 0f, color)
            addVertex(x + w, y, 1f, 0f, color)
            addVertex(x + w, y + h, 1f, 1f, color)

            // Triangle 2
            addVertex(x, y, 0f, 0f, color)
            addVertex(x + w, y + h, 1f, 1f, color)
            addVertex(x, y + h, 0f, 1f, color)
        }
    }

    /**
     * Draws a triangle with the specified position, size, and color.
     */
    fun drawTriangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        middle: Float = 0.5f,
        color: Color4 = Color4.White
    ) {
        setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = GraphicsShader.UniformSet()
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val (x, y) = transform.transform(x, y)
            val (w, h) = transform.transform(width, height)

            addVertex(x, y, 0f, 0f, color)
            addVertex(x + w, y, 1f, 0f, color)
            addVertex(x + w * middle, y + h, 0.5f, 1f, color)
        }
    }

    private fun calculateArcResolution(width: Float, height: Float, angle: Float = 360f): Int {
        if (angle <= 0f || width <= 0f || height <= 0f) return 0

        val averageRadius = (width + height) / 4f
        val angleBetweenSegments = min(5f, 360f / averageRadius)
        val segments = abs(angle) / angleBetweenSegments

        return max(3, segments.toInt())
    }

    fun drawArc(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        startAngle: Float = 0f,
        endAngle: Float = 360f,
        color: Color4 = Color4.White
    ) {
        val segments = calculateArcResolution(width, height, abs(endAngle - startAngle)).coerceAtLeast(1)

        setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = GraphicsShader.UniformSet()
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val (cx, cy) = transform.transform(x + width / 2f, y + height / 2f)
            val (w, h) = transform.transform(width / 2f, height / 2f)

            if (segments <= 0) {
                Logger.w(
                    "GraphicsRenderer",
                    "drawArc: Invalid number of segments ($segments) for arc. Skipping drawing."
                )
                return@drawGeometry
            }

            val start = (startAngle - 90f).toRadians()
            val end = (endAngle - 90f).toRadians()

            val delta = (end - start) / segments

            var previousX = cx + w * cos(start)
            var previousY = cy + h * sin(start)

            for (j in 0..segments) {

                val angle = start + j * delta
                val x = cx + w * cos(angle)
                val y = cy + h * sin(angle)

                if (j > 0) {
                    addVertex(previousX, previousY, 0f, 0f, color)
                    addVertex(x, y, 1f, 1f, color)
                    addVertex(cx, cy, 0.5f, 0.5f, color)
                }

                previousX = x
                previousY = y
            }
        }
    }

    /**
     * Draws a texture with the specified position, size, color, border width, border color, and corner radius.
     */
    fun drawTexture(
        texture: GraphicsTexture,
        region: GraphicsTexture.Region,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color4 = Color4.White,
        borderWidth: Float = 0f,
        borderColor: Color4 = Color4.Transparent,
        radius: Float = 0f
    ) {
        setState(
            shader = BuiltIn.Shaders.TextureQuad,
            uniformSet = GraphicsShader.UniformSet().apply {
                set("uRadius", radius)
                set("uBorderWidth", borderWidth)
                set("uBorderColor", borderColor)
                set("uSize", Vec2(width, height))
            },
            texture = texture
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val (x, y) = transform.transform(x, y)
            val (w, h) = transform.transform(width, height)

            // Triangle 1
            addVertex(x, y, region.u0, region.v0, color)
            addVertex(x + w, y, region.u1, region.v0, color)
            addVertex(x + w, y + h, region.u1, region.v1, color)

            // Triangle 2
            addVertex(x, y, region.u0, region.v0, color)
            addVertex(x + w, y + h, region.u1, region.v1, color)
            addVertex(x, y + h, region.u0, region.v1, color)
        }
    }

    /**
     * Draws text using the specified font, position, and color. The text is rendered using the
     * font's texture atlas.
     */
    fun drawText(
        text: String,
        font: Font,
        x: Float,
        y: Float,
        color: Color4 = Color4.White
    ) {
        setState(
            shader = BuiltIn.Shaders.TextureQuad,
            texture = font.texture,
            uniformSet = GraphicsShader.UniformSet()
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val (x, y) = transform.transform(x, y)
            val baseline = y - font.metrics.ascent

            var charIndex = 0
            var cursor = x

            while (charIndex < text.length) {

                val codePoint = text.codePointAt(charIndex)
                val charCount = Character.charCount(codePoint)

                if (charIndex > 0) {
                    val previousCodePoint = text.codePointAt(charIndex - 1)
                    cursor += font.getKerning(previousCodePoint, codePoint)
                }

                val glyph = font[codePoint]

                // Some glyphs may not have a texture region if they not need a visible representation
                // (like a space). In such cases, we skip rendering but still advance the cursor.
                if (glyph.region != null) {
                    val left = cursor + glyph.bearingX
                    val top = baseline + glyph.bearingY

                    val right = left + glyph.width
                    val bottom = top + glyph.height

                    val u0 = glyph.region.u0
                    val v0 = glyph.region.v0
                    val u1 = glyph.region.u1
                    val v1 = glyph.region.v1

                    addVertex(left, top, u0, v0, color)
                    addVertex(left, bottom, u0, v1, color)
                    addVertex(right, bottom, u1, v1, color)

                    addVertex(right, bottom, u1, v1, color)
                    addVertex(right, top, u1, v0, color)
                    addVertex(left, top, u0, v0, color)
                }

                cursor += glyph.advance
                charIndex += charCount
            }

        }
    }

    //endregion

    //region Rendering Lifecycle

    open fun begin() {
        activeBuffer = null
        activeShader = null
        activeTexture = null
        activeScissor = null
        activeBlendMode = BlendMode.Additive
        activeDepthState = DepthState.Default
        activeUniformSet = GraphicsShader.UniformSet()

        // Should not happen but just in case, clear the command batch to avoid rendering stale commands.
        commandBatch.clear()
    }

    abstract fun drawCommand(command: DrawCommand)

    open fun end() {
        while (commandBatch.isNotEmpty()) {
            val command = commandBatch.removeFirst()
            drawCommand(command)
        }
    }

    //endregion


    companion object {
        const val VERTEX_2D_STRIDE = 20 // 8 bytes for position, 8 bytes for UV, 4 bytes for color
    }

}