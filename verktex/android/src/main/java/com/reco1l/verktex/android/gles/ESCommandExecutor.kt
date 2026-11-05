package com.reco1l.verktex.android.gles

import android.opengl.GLES20
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.DefaultUniforms
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.VertexLayout

/**
 * Executes a [DrawCommand] against the current OpenGL ES context. The state handling is common to
 * every version, each backend implements the draw calls and how attributes are set up.
 *
 * Commands are executed on the draw thread only.
 */
abstract class ESCommandExecutor {

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

    /**
     * The vertex attribute arrays currently enabled, by location.
     */
    protected val enabledAttributes = BooleanArray(MAX_ATTRIBUTES)

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

        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)

        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        scissorEnabled = false

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        depthEnabled = false
        depthWrite = false
        depthFunction = -1

        blendMode = null
        boundShader = null

        resetAttributes()
    }

    /**
     * Puts every vertex attribute in its default state (disabled, and anything else the backend tracks).
     */
    protected open fun resetAttributes() {
        for (location in 0 until MAX_ATTRIBUTES) {
            if (enabledAttributes[location]) {
                GLES20.glDisableVertexAttribArray(location)
                enabledAttributes[location] = false
            }
        }
    }

    private fun setScissor(enabled: Boolean, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) {
        if (enabled != scissorEnabled) {
            if (enabled) GLES20.glEnable(GLES20.GL_SCISSOR_TEST) else GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            scissorEnabled = enabled
        }

        if (enabled && (x != scissorX || y != scissorY || width != scissorWidth || height != scissorHeight)) {
            GLES20.glScissor(x, y, width, height)
            scissorX = x
            scissorY = y
            scissorWidth = width
            scissorHeight = height
        }
    }

    private fun setDepth(enabled: Boolean, function: Int, write: Boolean) {
        if (enabled != depthEnabled) {
            if (enabled) GLES20.glEnable(GLES20.GL_DEPTH_TEST) else GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            depthEnabled = enabled
        }
        if (enabled && function != depthFunction) {
            GLES20.glDepthFunc(function)
            depthFunction = function
        }
        if (write != depthWrite) {
            GLES20.glDepthMask(write)
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
                GLES20.glDepthMask(true)
                depthWrite = true
            }
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
            return
        }

        val captureTexture = command.captureTexture
        if (captureTexture != null) {
            // Copies the framebuffer drawn so far, bottom-left origin. The scissor doesn't apply to it.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            captureTexture.bind()
            GLES20.glCopyTexSubImage2D(
                GLES20.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
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
            GLES20.glBlendFuncSeparate(
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

        if (shader !== boundShader) {
            shader.bind()
            boundShader = shader
        }

        val texture = command.texture
        if (texture != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            // Always bound: binding is also what uploads the texture's pending data.
            texture.bind()
        }

        applyUniforms(shader, command.uniformSet, defaultUniforms.forViewport(viewportWidth, viewportHeight))

        val vertexCount = buffer.position / command.vertexLayout.stride

        if (command.instanceBuffer == null || command.instanceLayout == null) {
            if (command.isQuads) {
                drawQuads(command, vertexCount / 4)
            } else {
                drawVertices(command, vertexCount)
            }
        } else {
            drawInstanced(command, vertexCount)
        }
    }

    /**
     * Draws the [vertexCount] vertices of the command's buffer as triangles.
     */
    protected open fun drawVertices(command: DrawCommand, vertexCount: Int) {
        val buffer = command.buffer!!
        buffer.bind()
        buffer.uploadDataToGPU()
        applyLayout(command.vertexLayout)
        disableAttributesExcept(command.vertexLayout.locationMask)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    /**
     * Draws the [quadCount] quads of the command's buffer, see [DrawCommand.isQuads].
     */
    protected abstract fun drawQuads(command: DrawCommand, quadCount: Int)

    /**
     * Draws [DrawCommand.instanceCount] instances of the [vertexCount] vertices of the command's buffer.
     */
    protected abstract fun drawInstanced(command: DrawCommand, vertexCount: Int)

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

    /**
     * Points the attributes of [layout] into the bound array buffer, starting [baseOffset] bytes in.
     * The pointers are set for every draw, as each command has its own buffer.
     */
    protected open fun applyLayout(layout: VertexLayout, baseOffset: Int = 0) {
        layout.attributes.forEachIndexed { index, attribute ->
            val location = attribute.location

            if (!enabledAttributes[location]) {
                GLES20.glEnableVertexAttribArray(location)
                enabledAttributes[location] = true
            }

            GLES20.glVertexAttribPointer(
                location,
                attribute.size,
                when (attribute.type) {
                    VertexLayout.Type.Float -> GLES20.GL_FLOAT
                    VertexLayout.Type.NormalizedByte -> GLES20.GL_UNSIGNED_BYTE
                },
                attribute.type == VertexLayout.Type.NormalizedByte,
                layout.stride,
                baseOffset + layout.offsets[index]
            )
        }
    }

    /**
     * Disables the attributes outside [usedMask]. Attributes left enabled by another layout would
     * read past the end of this draw's buffers.
     */
    protected fun disableAttributesExcept(usedMask: Int) {
        for (location in 0 until MAX_ATTRIBUTES) {
            if (enabledAttributes[location] && usedMask and (1 shl location) == 0) {
                GLES20.glDisableVertexAttribArray(location)
                enabledAttributes[location] = false
            }
        }
    }


    protected companion object {
        const val MAX_ATTRIBUTES = 16
    }
}
