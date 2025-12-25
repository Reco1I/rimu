package com.reco1l.verktex.graphics

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Mat2
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Logger
import com.reco1l.verktex.PerformanceStats
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.math.toRadians
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
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

    // Vertex buffers are reused across frames, creating and deleting them per command is costly.
    private val bufferPool = ArrayList<GraphicsBuffer>()
    private var buffersUsed = 0

    /**
     * The draw commands recorded for the current frame so far.
     */
    val commandCount
        get() = commandBatch.size
    private val gpuResources = ArrayDeque<GraphicsResource>()


    //region Stacks
    private val matrixStack = ArrayDeque<Mat2>()
    private val colorStack = ArrayDeque<Color4>()
    private val tintStack = ArrayDeque<Color4>()
    private val scissorStack = ArrayDeque<Vec4>()
    private val blendModeStack = ArrayDeque<BlendMode>()
    private val depthStateStack = ArrayDeque<DepthState>()
    private val shaderStack = ArrayDeque<GraphicsShader>()
    //endregion

    //region State
    private var activeShader: GraphicsShader? = null
    private var activeBuffer: GraphicsBuffer? = null
    private var activeTexture: GraphicsTexture? = null
    private var activeScissor: Vec4? = null
    private var activeUniformSet: GraphicsShader.UniformSet = EMPTY_UNIFORMS

    // The uniforms of the last drawn external texture, reused so consecutive draws of it merge.
    private var externalUniformsTexture: GraphicsTexture? = null
    private var externalUniforms: GraphicsShader.UniformSet = EMPTY_UNIFORMS

    // Transformed positions are written here instead of allocating a Vec2 per vertex. It is only
    // used within one draw helper call at a time, the renderer is driven by a single thread.
    private val points = FloatArray(8)


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
        scissor: Vec4? = scissorStack.lastOrNull(),
        blendMode: BlendMode = blendModeStack.lastOrNull() ?: BlendMode.Alpha,
        depthState: DepthState = depthStateStack.lastOrNull() ?: DepthState.Default,
        uniformSet: GraphicsShader.UniformSet = activeUniformSet
    ) {
        // A pushed shader replaces the one the draw call asked for, see pushShader.
        val usedShader = shaderStack.lastOrNull() ?: shader

        val lastCommand = commandBatch.lastOrNull()
        if (lastCommand != null &&
            lastCommand.canReuse(
                usedShader,
                texture,
                scissor,
                blendMode,
                depthState,
                uniformSet,
            )
        ) return

        val vertexBuffer = bufferPool.getOrNull(buffersUsed) ?: createVertexBuffer().also(bufferPool::add)
        buffersUsed++
        vertexBuffer.clear()

        activeTexture = texture
        activeShader = usedShader
        activeBuffer = vertexBuffer
        activeScissor = scissor
        activeUniformSet = uniformSet

        val command = DrawCommand(
            buffer = vertexBuffer,
            shader = usedShader,
            texture = texture,
            scissor = scissor,
            blendMode = blendMode,
            depthState = depthState,
            uniformSet = uniformSet
        )

        commandBatch.addLast(command)
    }

    /**
     * Clears the depth buffer at this point of the frame. Draws recorded before stay before the clear
     * and the next draw starts a new command.
     */
    fun clearDepth() {
        commandBatch.addLast(DrawCommand(clearDepth = true))

        // A geometry draw without a new setState() would otherwise write into a buffer already queued before the clear.
        activeBuffer = null
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
     * Pushes a color onto the color stack. Its alpha is multiplied into subsequent colors, its RGB
     * is not, see [pushTint] for that.
     */
    fun pushColor(color: Color4) {
        colorStack.addLast(color.copy(alpha = color.alpha * (colorStack.lastOrNull()?.alpha ?: 1f)))
    }

    /**
     * Pops a color from the color stack and returns it.
     */
    fun popColor(): Color4? {
        return colorStack.removeLastOrNull()
    }

    /**
     * Pushes a tint onto the tint stack: the RGB of [color] (its alpha is ignored) is multiplied
     * into the RGB of everything drawn until the matching [popTint], on top of the tints already pushed.
     */
    fun pushTint(color: Color4) {
        val parent = tintStack.lastOrNull() ?: Color4.White
        tintStack.addLast(Color4(parent.red * color.red, parent.green * color.green, parent.blue * color.blue))
    }

    /**
     * Pops the last tint pushed with [pushTint].
     */
    fun popTint() {
        tintStack.removeLastOrNull()
    }

    /**
     * The area drawing is currently limited to, in scene space as x, y, width and height: the
     * innermost scissor, or the window when nothing is clipped.
     */
    val clipBounds: Vec4
        get() = scissorStack.lastOrNull() ?: Vec4(0f, 0f, window.windowWidth.toFloat(), window.windowHeight.toFloat())

    /**
     * Pushes a shader onto the shader stack. It replaces the shader of subsequent drawing operations,
     * so it must use the built-in vertex layout: position at location 0, UV at 1 and color at 2.
     * Uniforms it does not declare are skipped.
     *
     * It also replaces `BuiltIn.Shaders.Video` for external textures, so a shader with a 2D sampler
     * must not be pushed while an external texture is drawn.
     */
    fun pushShader(shader: GraphicsShader) {
        shaderStack.addLast(shader)
    }

    /**
     * Pops a shader from the shader stack and returns it.
     */
    fun popShader(): GraphicsShader? {
        return shaderStack.removeLastOrNull()
    }

    /**
     * Pushes a blend mode onto the blend mode stack. It will be used for subsequent drawing operations.
     */
    fun pushBlendMode(blendMode: BlendMode) {
        blendModeStack.addLast(blendMode)
    }

    /**
     * Pops a blend mode from the blend mode stack and returns it.
     */
    fun popBlendMode(): BlendMode? {
        return blendModeStack.removeLastOrNull()
    }

    /**
     * Pushes a depth state onto the depth state stack. It will be used for subsequent drawing operations.
     */
    fun pushDepthState(depthState: DepthState) {
        depthStateStack.addLast(depthState)
    }

    /**
     * Pops a depth state from the depth state stack and returns it.
     */
    fun popDepthState(): DepthState? {
        return depthStateStack.removeLastOrNull()
    }

    /**
     * Pushes a scissor rectangle onto the scissor stack. This rectangle will be used to clip subsequent drawing operations.
     */
    fun pushScissor(x: Float, y: Float, width: Float, height: Float) {

        val currentMatrix = matrixStack.lastOrNull() ?: Mat2.Identity

        currentMatrix.transform(x, y, points, 0)
        currentMatrix.transform(x + width, y + height, points, 2)
        val x0 = points[0]
        val y0 = points[1]
        val x1 = points[2]
        val y1 = points[3]

        val x = min(x0, x1)
        val y = min(y0, y1)
        val width = abs(x1 - x0)
        val height = abs(y1 - y0)

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
            // Without overlap nothing must be drawn, a negative size would be rejected by GL.
            intersectedWidth = (maxX - minX).coerceAtLeast(0f)
            intersectedHeight = (maxY - minY).coerceAtLeast(0f)
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
     * the color stack. If any tint is pushed ([pushTint]), the RGB is also multiplied by the product of
     * those tints.
     *
     * If the color stack is empty, the original color is returned.
     */
    fun transformColor(color: Color4): Color4 {
        val tint = tintStack.lastOrNull() ?: Color4.White

        return Color4(
            color.red * tint.red,
            color.green * tint.green,
            color.blue * tint.blue,
            color.alpha * (colorStack.lastOrNull()?.alpha ?: 1f)
        )
    }

    //endregion

    //region Resource Management

    /**
     * Registers a GPU resource with the renderer. This allows the renderer to manage the resource's
     * lifecycle and handle context loss events.
     */
    fun registerResource(resource: GraphicsResource) = synchronized(gpuResources) {
        gpuResources.addLast(resource)
    }

    /**
     * Unregisters a GPU resource from the renderer.
     */
    fun unregisterResource(resource: GraphicsResource) = synchronized(gpuResources) {
        gpuResources.remove(resource)
    }

    /**
     * Marks all registered GPU resources as lost due to a context loss event and should be recreated.
     *
     * Resources are registered from the update worker while the GL thread marks them, so the list is
     * locked while it is iterated.
     */
    fun markContextLoss() = synchronized(gpuResources) {
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
     * Creates a new external texture (such as one fed with video frames) with the specified width and
     * height, registered so that it is recreated after a context loss.
     */
    fun createExternalTexture(width: Float, height: Float): GraphicsTexture {
        val texture = device.createExternalTexture(width, height)
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
        requireCapacity(VERTEX_2D_STRIDE)
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
                set("uSize", Vec2(width, height))
            },
        )

        drawGeometry { transform, baseColor ->

            val color = transformColor(color)
            val p = points

            transform.transform(x, y, p, 0)
            transform.transform(x + width, y, p, 2)
            transform.transform(x + width, y + height, p, 4)
            transform.transform(x, y + height, p, 6)

            // Triangle 1
            addVertex(p[0], p[1], 0f, 0f, color)
            addVertex(p[2], p[3], 1f, 0f, color)
            addVertex(p[4], p[5], 1f, 1f, color)

            // Triangle 2
            addVertex(p[0], p[1], 0f, 0f, color)
            addVertex(p[4], p[5], 1f, 1f, color)
            addVertex(p[6], p[7], 0f, 1f, color)
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
            uniformSet = EMPTY_UNIFORMS
        )

        // The quad shader fades out at UV edges, so every vertex samples its center to keep the
        // shape opaque up to its geometric edges.
        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val p = points

            transform.transform(x, y, p, 0)
            transform.transform(x + width, y, p, 2)
            transform.transform(x + width * middle, y + height, p, 4)

            addVertex(p[0], p[1], 0.5f, 0.5f, color)
            addVertex(p[2], p[3], 0.5f, 0.5f, color)
            addVertex(p[4], p[5], 0.5f, 0.5f, color)
        }
    }

    /**
     * Draws a triangle mesh, [vertices] holds [vertexCount] positions in local space as x, y pairs
     * and every three of them form a triangle.
     *
     * It draws with the solid shader and every vertex samples the center of the UV space (0.5) and
     * uses [color]. A shader pushed with [pushShader] replaces the solid one.
     */
    fun drawTriangles(vertices: FloatArray, vertexCount: Int, color: Color4) {
        require(vertexCount % 3 == 0) { "The vertex count must be a multiple of 3, got $vertexCount." }
        require(vertices.size >= vertexCount * 2) { "Expected ${vertexCount * 2} floats, got ${vertices.size}." }

        setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = EMPTY_UNIFORMS
        )

        drawGeometry { transform, _ ->
            val color = transformColor(color)
            val p = points

            for (i in 0 until vertexCount) {
                transform.transform(vertices[i * 2], vertices[i * 2 + 1], p, 0)
                addVertex(p[0], p[1], 0.5f, 0.5f, color)
            }
        }
    }

    /**
     * Draws a line between two points with the specified width and color.
     */
    fun drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        lineWidth: Float,
        color: Color4 = Color4.White
    ) {
        val length = hypot(x2 - x1, y2 - y1)
        if (length <= 0f || lineWidth <= 0f) return

        setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = EMPTY_UNIFORMS
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            // Perpendicular offset of half the line width.
            val nx = -(y2 - y1) / length * lineWidth / 2f
            val ny = (x2 - x1) / length * lineWidth / 2f

            val p = points

            transform.transform(x1 + nx, y1 + ny, p, 0)
            transform.transform(x2 + nx, y2 + ny, p, 2)
            transform.transform(x2 - nx, y2 - ny, p, 4)
            transform.transform(x1 - nx, y1 - ny, p, 6)

            addVertex(p[0], p[1], 0.5f, 0.5f, color)
            addVertex(p[2], p[3], 0.5f, 0.5f, color)
            addVertex(p[4], p[5], 0.5f, 0.5f, color)

            addVertex(p[0], p[1], 0.5f, 0.5f, color)
            addVertex(p[4], p[5], 0.5f, 0.5f, color)
            addVertex(p[6], p[7], 0.5f, 0.5f, color)
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
            uniformSet = EMPTY_UNIFORMS
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val cx = x + width / 2f
            val cy = y + height / 2f
            val w = width / 2f
            val h = height / 2f
            val p = points
            transform.transform(cx, cy, p, 0)
            val centerX = p[0]
            val centerY = p[1]

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
                    transform.transform(previousX, previousY, p, 2)
                    transform.transform(x, y, p, 4)

                    addVertex(p[2], p[3], 0.5f, 0.5f, color)
                    addVertex(p[4], p[5], 0.5f, 0.5f, color)
                    addVertex(centerX, centerY, 0.5f, 0.5f, color)
                }

                previousX = x
                previousY = y
            }
        }
    }

    /**
     * Draws the outline of an arc with the specified thickness, angles are in degrees starting
     * from the top and going clockwise, as in [drawArc].
     */
    fun drawArcStroke(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        thickness: Float,
        startAngle: Float = 0f,
        endAngle: Float = 360f,
        color: Color4 = Color4.White
    ) {
        val segments = calculateArcResolution(width, height, abs(endAngle - startAngle))
        if (segments <= 0 || thickness <= 0f) return

        setState(
            shader = BuiltIn.Shaders.SolidQuad,
            uniformSet = EMPTY_UNIFORMS
        )

        drawGeometry { transform, _ ->
            val color = transformColor(color)

            val cx = x + width / 2f
            val cy = y + height / 2f
            val outerX = width / 2f
            val outerY = height / 2f
            val innerX = (outerX - thickness).coerceAtLeast(0f)
            val innerY = (outerY - thickness).coerceAtLeast(0f)

            val start = (startAngle - 90f).toRadians()
            val delta = ((endAngle - 90f).toRadians() - start) / segments

            val p = points

            for (segment in 0 until segments) {
                val a0 = start + segment * delta
                val a1 = a0 + delta

                // Outer start and end, then inner start and end.
                transform.transform(cx + outerX * cos(a0), cy + outerY * sin(a0), p, 0)
                transform.transform(cx + outerX * cos(a1), cy + outerY * sin(a1), p, 2)
                transform.transform(cx + innerX * cos(a0), cy + innerY * sin(a0), p, 4)
                transform.transform(cx + innerX * cos(a1), cy + innerY * sin(a1), p, 6)

                addVertex(p[0], p[1], 0.5f, 0.5f, color)
                addVertex(p[2], p[3], 0.5f, 0.5f, color)
                addVertex(p[6], p[7], 0.5f, 0.5f, color)

                addVertex(p[0], p[1], 0.5f, 0.5f, color)
                addVertex(p[6], p[7], 0.5f, 0.5f, color)
                addVertex(p[4], p[5], 0.5f, 0.5f, color)
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
        radius: Float = 0f,
        uv: Vec4 = Vec4(region.u0, region.v0, region.u1, region.v1)
    ) {
        val externalTransform = texture.transform

        // The texture shader only reads the uniforms for rounded corners, so plain textures share
        // an empty set and consecutive ones from the same texture end up in one draw call.
        val uniformSet = if (texture.isExternal && externalTransform != null) {
            // External textures are sampled through their transform, which the draw reads by reference
            // after the bind latched the latest frame. Rounded corners are not supported for them.
            if (externalUniformsTexture !== texture) {
                externalUniformsTexture = texture
                externalUniforms = GraphicsShader.UniformSet().apply { set("uTransform", externalTransform) }
            }
            externalUniforms
        } else if (radius > 0f) {
            GraphicsShader.UniformSet().apply {
                set("uRadius", radius)
                set("uSize", Vec2(width, height))
            }
        } else {
            EMPTY_UNIFORMS
        }

        // A pushed shader replaces the video one too, see pushShader.
        setState(
            shader = if (texture.isExternal) BuiltIn.Shaders.Video else BuiltIn.Shaders.TextureQuad,
            uniformSet = uniformSet,
            texture = texture
        )

        drawGeometry { transform, baseColor ->
            val color = transformColor(color)

            val p = points

            transform.transform(x, y, p, 0)
            transform.transform(x + width, y, p, 2)
            transform.transform(x + width, y + height, p, 4)
            transform.transform(x, y + height, p, 6)

            // Triangle 1
            addVertex(p[0], p[1], uv.x, uv.y, color)
            addVertex(p[2], p[3], uv.z, uv.y, color)
            addVertex(p[4], p[5], uv.z, uv.w, color)

            // Triangle 2
            addVertex(p[0], p[1], uv.x, uv.y, color)
            addVertex(p[4], p[5], uv.z, uv.w, color)
            addVertex(p[6], p[7], uv.x, uv.w, color)
        }
    }

    /**
     * Draws text using the specified font, position, and color. The text is rendered using the
     * font's atlas pages, each run of glyphs on the same page is one draw command.
     */
    fun drawText(
        text: String,
        font: Font,
        x: Float,
        y: Float,
        color: Color4 = Color4.White
    ) {
        val transform = matrixStack.lastOrNull() ?: Mat2.Identity
        val color = transformColor(color)

        // Glyphs are laid out in local space and every corner is transformed, so rotation and
        // scale apply to text as well.
        val baseline = y - font.metrics.ascent

        var charIndex = 0
        var cursor = x
        var previousCodePoint = -1

        // The page the current draw command samples from; a glyph on another page starts a new one.
        var page: GraphicsTexture? = null
        var buffer: GraphicsBuffer? = null

        while (charIndex < text.length) {

            val codePoint = text.codePointAt(charIndex)
            val charCount = Character.charCount(codePoint)

            if (previousCodePoint != -1) {
                cursor += font.getKerning(previousCodePoint, codePoint)
            }
            previousCodePoint = codePoint

            val glyph = font[codePoint]

            // Some glyphs may not have a texture region if they not need a visible representation
            // (like a space). In such cases, we skip rendering but still advance the cursor.
            if (glyph.region != null) {
                if (glyph.region.texture !== page) {
                    page = glyph.region.texture
                    setState(
                        shader = BuiltIn.Shaders.TextureQuad,
                        texture = page,
                        uniformSet = EMPTY_UNIFORMS
                    )
                    buffer = activeBuffer
                }

                val left = cursor + glyph.bearingX
                val top = baseline + glyph.bearingY

                val right = left + glyph.width
                val bottom = top + glyph.height

                val u0 = glyph.region.u0
                val v0 = glyph.region.v0
                val u1 = glyph.region.u1
                val v1 = glyph.region.v1

                val p = points

                transform.transform(left, top, p, 0)
                transform.transform(left, bottom, p, 2)
                transform.transform(right, bottom, p, 4)
                transform.transform(right, top, p, 6)

                buffer!!.apply {
                    addVertex(p[0], p[1], u0, v0, color)
                    addVertex(p[2], p[3], u0, v1, color)
                    addVertex(p[4], p[5], u1, v1, color)

                    addVertex(p[4], p[5], u1, v1, color)
                    addVertex(p[6], p[7], u1, v0, color)
                    addVertex(p[0], p[1], u0, v0, color)
                }
            }

            cursor += glyph.advance
            charIndex += charCount
        }
    }

    //endregion

    //region Rendering Lifecycle

    /**
     * Prepares the renderer for a new frame, e.g. by making the graphics context current.
     *
     * @return `false` if the frame cannot be rendered right now and should be skipped.
     */
    open fun prepareFrame(): Boolean = true

    open fun begin() {
        activeBuffer = null
        activeShader = null
        activeTexture = null
        activeScissor = null
        activeUniformSet = EMPTY_UNIFORMS

        // Should not happen but just in case, clear the command batch to avoid rendering stale commands.
        commandBatch.clear()
        buffersUsed = 0
    }

    abstract fun drawCommand(command: DrawCommand)

    open fun end() {
        while (commandBatch.isNotEmpty()) {
            drawCommand(commandBatch.removeFirst())
        }
        buffersUsed = 0
        PerformanceStats.onFrameDrawn()
    }

    //endregion


    companion object {
        const val VERTEX_2D_STRIDE = 20 // 8 bytes for position, 8 bytes for UV, 4 bytes for color

        // Shared by every draw without uniforms of its own. Nothing writes to it, commands and
        // shaders only read their set.
        private val EMPTY_UNIFORMS = GraphicsShader.UniformSet()
    }

}