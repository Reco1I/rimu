package com.reco1l.verktex.android.gles

import android.opengl.GLES32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.VertexLayout
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.BlendMode

/**
 * Executes a [DrawCommand] against the current OpenGL ES context, shared by every renderer drawing through GLES.
 */
object ESCommandExecutor {

    private val IDENTITY_MAT4 = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    // The defaults every draw starts from, rebuilt only when the viewport size changes. Commands are
    // executed on the draw thread only, so one set is shared.
    private var defaultUniformsWidth = -1
    private var defaultUniformsHeight = -1
    private var defaultUniforms = GraphicsShader.UniformSet()

    private fun defaultUniforms(viewportWidth: Int, viewportHeight: Int): GraphicsShader.UniformSet {
        if (viewportWidth != defaultUniformsWidth || viewportHeight != defaultUniformsHeight) {
            defaultUniformsWidth = viewportWidth
            defaultUniformsHeight = viewportHeight

            val viewportSize = Vec2(viewportWidth.toFloat(), viewportHeight.toFloat())

            defaultUniforms = GraphicsShader.UniformSet().apply {
                set("uViewportSize", viewportSize)
                set("uColor", Color4.White)
                set("uTexture", 0)
                set("uRadius", 0f)
                set("uBorderWidth", 0f)
                set("uBorderColor", Color4.Transparent)
                set("uSize", viewportSize)
                set("uTransform", IDENTITY_MAT4)
            }
        }

        return defaultUniforms
    }

    //region State cache

    // The GL state the last command left, so calls changing nothing are skipped. Only valid while
    // commands are executed, see resetState.
    private var isStateKnown = false
    private var scissorEnabled = false
    private var scissorX = 0
    private var scissorY = 0
    private var scissorWidth = 0
    private var scissorHeight = 0
    private var blendMode: BlendMode? = null
    private var depthEnabled = false
    private var depthFunction = -1
    private var depthWrite = false
    private var boundShader: GraphicsShader? = null

    // The index buffer shared by every quad command, see DrawCommand.isQuads: two triangles for
    // each quad of four vertices. It grows to the largest command and is made again for a new
    // context.
    private var quadIndexBuffer = 0
    private var quadIndexCapacity = 0
    private val bufferId = IntArray(1)

    private fun bindQuadIndices(quadCount: Int) {
        if (quadIndexBuffer == 0 || !GLES32.glIsBuffer(quadIndexBuffer)) {
            GLES32.glGenBuffers(1, bufferId, 0)
            quadIndexBuffer = bufferId[0]
            quadIndexCapacity = 0
        }

        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer)

        if (quadIndexCapacity >= quadCount) {
            return
        }

        val capacity = maxOf(quadCount, quadIndexCapacity * 2, MIN_QUAD_INDEX_CAPACITY)
        val indices = ByteBuffer.allocateDirect(capacity * 6 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()

        for (quad in 0 until capacity) {
            val first = quad * 4
            indices.put(first).put(first + 1).put(first + 2)
            indices.put(first).put(first + 2).put(first + 3)
        }
        indices.flip()

        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, capacity * 6 * 4, indices, GLES32.GL_STATIC_DRAW)
        quadIndexCapacity = capacity
    }

    private const val MIN_QUAD_INDEX_CAPACITY = 1024

    private const val MAX_ATTRIBUTES = 16
    private val enabledAttributes = BooleanArray(MAX_ATTRIBUTES)
    private val attributeDivisors = IntArray(MAX_ATTRIBUTES)

    /**
     * Forgets the cached GL state. Called before executing a frame's commands, as other code (e.g.
     * texture uploads) may change it in between.
     */
    fun resetState() {
        isStateKnown = false
    }

    // Puts GL in a known state once per frame, the cache then follows every change.
    private fun ensureStateKnown() {
        if (isStateKnown) return
        isStateKnown = true

        GLES32.glDisable(GLES32.GL_CULL_FACE)
        GLES32.glEnable(GLES32.GL_BLEND)

        GLES32.glDisable(GLES32.GL_SCISSOR_TEST)
        scissorEnabled = false

        GLES32.glDisable(GLES32.GL_DEPTH_TEST)
        GLES32.glDepthMask(false)
        depthEnabled = false
        depthWrite = false
        depthFunction = -1

        blendMode = null
        boundShader = null

        for (location in 0 until MAX_ATTRIBUTES) {
            if (enabledAttributes[location]) {
                GLES32.glDisableVertexAttribArray(location)
                enabledAttributes[location] = false
            }
            if (attributeDivisors[location] != 0) {
                GLES32.glVertexAttribDivisor(location, 0)
                attributeDivisors[location] = 0
            }
        }
    }

    private fun setScissor(enabled: Boolean, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) {
        if (enabled != scissorEnabled) {
            if (enabled) GLES32.glEnable(GLES32.GL_SCISSOR_TEST) else GLES32.glDisable(GLES32.GL_SCISSOR_TEST)
            scissorEnabled = enabled
        }

        if (enabled && (x != scissorX || y != scissorY || width != scissorWidth || height != scissorHeight)) {
            GLES32.glScissor(x, y, width, height)
            scissorX = x
            scissorY = y
            scissorWidth = width
            scissorHeight = height
        }
    }

    private fun setDepth(enabled: Boolean, function: Int, write: Boolean) {
        if (enabled != depthEnabled) {
            if (enabled) GLES32.glEnable(GLES32.GL_DEPTH_TEST) else GLES32.glDisable(GLES32.GL_DEPTH_TEST)
            depthEnabled = enabled
        }
        if (enabled && function != depthFunction) {
            GLES32.glDepthFunc(function)
            depthFunction = function
        }
        if (write != depthWrite) {
            GLES32.glDepthMask(write)
            depthWrite = write
        }
    }

    //endregion

    fun execute(command: DrawCommand, viewportWidth: Int, viewportHeight: Int) {
        ensureStateKnown()

        if (command.clearDepth) {
            // glClear honours the scissor test and the depth mask, either may be left over from the previous command.
            setScissor(false)
            if (!depthWrite) {
                GLES32.glDepthMask(true)
                depthWrite = true
            }
            GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT)
            return
        }

        val captureTexture = command.captureTexture
        if (captureTexture != null) {
            // Copies the framebuffer drawn so far, bottom-left origin. The scissor doesn't apply to it.
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0)
            captureTexture.bind()
            GLES32.glCopyTexSubImage2D(
                GLES32.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
                minOf(captureTexture.width.toInt(), viewportWidth),
                minOf(captureTexture.height.toInt(), viewportHeight)
            )
            return
        }

        if (command.buffer == null || command.shader == null || command.buffer.isEmpty()) return

        // Scissor test, the scissor is in window space (top-left origin) while GL expects bottom-left.
        val scissor = command.scissor
        if (scissor != null) {
            setScissor(
                true,
                scissor.x.toInt(),
                (viewportHeight - scissor.y - scissor.w).toInt(),
                scissor.z.toInt().coerceAtLeast(0),
                scissor.w.toInt().coerceAtLeast(0)
            )
        } else {
            setScissor(false)
        }

        val blendMode = command.blendMode
        if (blendMode != this.blendMode) {
            GLES32.glBlendFuncSeparate(
                blendMode.sourceRGB.toGLES(),
                blendMode.destinationRGB.toGLES(),
                blendMode.sourceAlpha.toGLES(),
                blendMode.destinationAlpha.toGLES()
            )
            this.blendMode = blendMode
        }

        val depthState = command.depthState
        if (depthState.enabled) {
            setDepth(true, depthState.function.toGLES(), depthState.writeEnabled)
        } else {
            setDepth(false, depthFunction, false)
        }

        val shader = command.shader
        if (shader !== boundShader) {
            shader.bind()
            boundShader = shader
        }

        if (command.texture != null) {
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0)
            // Always bound: binding is also what uploads the texture's pending data.
            command.texture.bind()
        }

        applyUniforms(shader, command.uniformSet, defaultUniforms(viewportWidth, viewportHeight))

        command.buffer.bind()
        command.buffer.uploadDataToGPU()

        val instanceBuffer = command.instanceBuffer
        val instanceLayout = command.instanceLayout

        applyLayout(command.vertexLayout)

        val vertexCount = command.buffer.position / command.vertexLayout.stride

        if (instanceBuffer == null || instanceLayout == null) {
            disableAttributesExcept(command.vertexLayout, null)

            if (command.isQuads) {
                val quadCount = vertexCount / 4
                bindQuadIndices(quadCount)
                GLES32.glDrawElements(GLES32.GL_TRIANGLES, quadCount * 6, GLES32.GL_UNSIGNED_INT, 0)
            } else {
                GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, vertexCount)
            }
        } else {
            instanceBuffer.bind()
            instanceBuffer.uploadDataToGPU()
            applyLayout(instanceLayout)
            disableAttributesExcept(command.vertexLayout, instanceLayout)

            GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLES, 0, vertexCount, command.instanceCount)
        }
    }

    // Every uniform gets either the command's value or its default, uniform values persist per
    // program, so a value left by a previous draw would otherwise apply.
    private fun applyUniforms(shader: GraphicsShader, uniformSet: GraphicsShader.UniformSet, defaults: GraphicsShader.UniformSet) {
        val shader = shader as ESShader
        val values = uniformSet.entries

        for ((name, default) in defaults.entries) {
            shader.applyUniform(name, values[name] ?: default)
        }

        if (values.isNotEmpty()) {
            for ((name, value) in values) {
                if (name !in defaults.entries) {
                    shader.applyUniform(name, value)
                }
            }
        }
    }

    // The pointers are set for every draw, as each command has its own buffer.
    private fun applyLayout(layout: VertexLayout) {
        layout.attributes.forEachIndexed { index, attribute ->
            val location = attribute.location

            if (!enabledAttributes[location]) {
                GLES32.glEnableVertexAttribArray(location)
                enabledAttributes[location] = true
            }

            GLES32.glVertexAttribPointer(
                location,
                attribute.size,
                when (attribute.type) {
                    VertexLayout.Type.Float -> GLES32.GL_FLOAT
                    VertexLayout.Type.NormalizedByte -> GLES32.GL_UNSIGNED_BYTE
                },
                attribute.type == VertexLayout.Type.NormalizedByte,
                layout.stride,
                layout.offsets[index]
            )

            val divisor = if (attribute.isPerInstance) 1 else 0
            if (attributeDivisors[location] != divisor) {
                GLES32.glVertexAttribDivisor(location, divisor)
                attributeDivisors[location] = divisor
            }
        }
    }

    // Attributes left enabled by another layout would read past the end of this draw's buffers.
    private fun disableAttributesExcept(layout: VertexLayout, instanceLayout: VertexLayout?) {
        val usedMask = layout.locationMask or (instanceLayout?.locationMask ?: 0)

        for (location in 0 until MAX_ATTRIBUTES) {
            if (enabledAttributes[location] && usedMask and (1 shl location) == 0) {
                GLES32.glDisableVertexAttribArray(location)
                enabledAttributes[location] = false
            }
        }
    }
}
