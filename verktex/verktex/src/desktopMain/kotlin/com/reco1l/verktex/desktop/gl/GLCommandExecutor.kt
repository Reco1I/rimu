package com.reco1l.verktex.desktop.gl

import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.DefaultUniforms
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.VertexLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.lwjgl.opengl.GL33.*

/**
 * Executes a [DrawCommand] against the current OpenGL context: a port of the OpenGL ES backend's
 * ESCommandExecutor and ES3CommandExecutor in one. Keep them in step, see docs/desktop-platform.md.
 *
 * Commands are executed on the draw thread only.
 */
class GLCommandExecutor {

    private val defaultUniforms = DefaultUniforms()

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

    private val enabledAttributes = BooleanArray(MAX_ATTRIBUTES)
    private val attributeDivisors = IntArray(MAX_ATTRIBUTES)

    // The index buffer shared by every quad command, see DrawCommand.isQuads: two triangles for
    // each quad of four vertices. It grows to the largest command.
    private var quadIndexBuffer = 0
    private var quadIndexCapacity = 0

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

        glDisable(GL_CULL_FACE)
        glEnable(GL_BLEND)

        glDisable(GL_SCISSOR_TEST)
        scissorEnabled = false

        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        depthEnabled = false
        depthWrite = false
        depthFunction = -1

        blendMode = null
        boundShader = null

        for (location in 0 until MAX_ATTRIBUTES) {
            if (enabledAttributes[location]) {
                glDisableVertexAttribArray(location)
                enabledAttributes[location] = false
            }
            if (attributeDivisors[location] != 0) {
                glVertexAttribDivisor(location, 0)
                attributeDivisors[location] = 0
            }
        }
    }

    private fun setScissor(enabled: Boolean, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) {
        if (enabled != scissorEnabled) {
            if (enabled) glEnable(GL_SCISSOR_TEST) else glDisable(GL_SCISSOR_TEST)
            scissorEnabled = enabled
        }

        if (enabled && (x != scissorX || y != scissorY || width != scissorWidth || height != scissorHeight)) {
            glScissor(x, y, width, height)
            scissorX = x
            scissorY = y
            scissorWidth = width
            scissorHeight = height
        }
    }

    private fun setDepth(enabled: Boolean, function: Int, write: Boolean) {
        if (enabled != depthEnabled) {
            if (enabled) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
            depthEnabled = enabled
        }
        if (enabled && function != depthFunction) {
            glDepthFunc(function)
            depthFunction = function
        }
        if (write != depthWrite) {
            glDepthMask(write)
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
                glDepthMask(true)
                depthWrite = true
            }
            glClear(GL_DEPTH_BUFFER_BIT)
            return
        }

        val captureTexture = command.captureTexture
        if (captureTexture != null) {
            // Copies the framebuffer drawn so far, bottom-left origin. The scissor doesn't apply to it.
            glActiveTexture(GL_TEXTURE0)
            captureTexture.bind()
            glCopyTexSubImage2D(
                GL_TEXTURE_2D, 0, 0, 0, 0, 0,
                minOf(captureTexture.width.toInt(), viewportWidth),
                minOf(captureTexture.height.toInt(), viewportHeight)
            )
            return
        }

        val buffer = command.buffer ?: return
        val shader = command.shader ?: return
        if (buffer.isEmpty()) return

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
            glBlendFuncSeparate(
                blendMode.sourceRGB.toGL(),
                blendMode.destinationRGB.toGL(),
                blendMode.sourceAlpha.toGL(),
                blendMode.destinationAlpha.toGL()
            )
            this.blendMode = blendMode
        }

        val depthState = command.depthState
        if (depthState.enabled) {
            setDepth(true, depthState.function.toGL(), depthState.writeEnabled)
        } else {
            setDepth(false, depthFunction, false)
        }

        if (shader !== boundShader) {
            shader.bind()
            boundShader = shader
        }

        val texture = command.texture
        if (texture != null) {
            glActiveTexture(GL_TEXTURE0)
            // Always bound: binding is also what uploads the texture's pending data.
            texture.bind()
        }

        applyUniforms(shader as GLShader, command.uniformSet, defaultUniforms.forViewport(viewportWidth, viewportHeight))

        val vertexCount = command.drawnVertexCount()

        val instanceBuffer = command.instanceBuffer
        val instanceLayout = command.instanceLayout

        buffer.bind()
        buffer.uploadDataToGPU()
        applyLayout(command.vertexLayout)

        if (instanceBuffer == null || instanceLayout == null) {
            disableAttributesExcept(command.vertexLayout.locationMask)

            if (command.isQuads) {
                val quadCount = vertexCount / 4
                bindQuadIndices(quadCount)
                glDrawElements(GL_TRIANGLES, quadCount * 6, GL_UNSIGNED_INT, 0L)
            } else {
                glDrawArrays(GL_TRIANGLES, command.firstVertex, vertexCount)
            }
        } else {
            instanceBuffer.bind()
            instanceBuffer.uploadDataToGPU()
            applyLayout(instanceLayout)

            disableAttributesExcept(command.vertexLayout.locationMask or instanceLayout.locationMask)

            glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, command.instanceCount)
        }
    }

    // Every uniform gets either the command's value or its default, uniform values persist per
    // program, so a value left by a previous draw would otherwise apply.
    private fun applyUniforms(shader: GLShader, uniformSet: GraphicsShader.UniformSet, defaults: GraphicsShader.UniformSet) {
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

    /**
     * Points the attributes of [layout] into the bound array buffer. The pointers are set for every
     * draw, as each command has its own buffer.
     */
    private fun applyLayout(layout: VertexLayout) {
        layout.attributes.forEachIndexed { index, attribute ->
            val location = attribute.location

            if (!enabledAttributes[location]) {
                glEnableVertexAttribArray(location)
                enabledAttributes[location] = true
            }

            glVertexAttribPointer(
                location,
                attribute.size,
                when (attribute.type) {
                    VertexLayout.Type.Float -> GL_FLOAT
                    VertexLayout.Type.NormalizedByte -> GL_UNSIGNED_BYTE
                },
                attribute.type == VertexLayout.Type.NormalizedByte,
                layout.stride,
                layout.offsets[index].toLong()
            )

            val divisor = if (attribute.isPerInstance) 1 else 0
            if (attributeDivisors[location] != divisor) {
                glVertexAttribDivisor(location, divisor)
                attributeDivisors[location] = divisor
            }
        }
    }

    /**
     * Disables the attributes outside [usedMask]. Attributes left enabled by another layout would
     * read past the end of this draw's buffers.
     */
    private fun disableAttributesExcept(usedMask: Int) {
        for (location in 0 until MAX_ATTRIBUTES) {
            if (enabledAttributes[location] && usedMask and (1 shl location) == 0) {
                glDisableVertexAttribArray(location)
                enabledAttributes[location] = false
            }
        }
    }

    private fun bindQuadIndices(quadCount: Int) {
        if (quadIndexBuffer == 0 || !glIsBuffer(quadIndexBuffer)) {
            quadIndexBuffer = glGenBuffers()
            quadIndexCapacity = 0
        }

        // The element array binding is part of the vertex array, which is the same for every draw.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadIndexBuffer)

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

        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
        quadIndexCapacity = capacity
    }

    private companion object {
        const val MAX_ATTRIBUTES = 16
        const val MIN_QUAD_INDEX_CAPACITY = 1024
    }
}
