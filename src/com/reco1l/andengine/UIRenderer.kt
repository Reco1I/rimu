package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10


object UIRenderer {

    var activeBuffer = VertexBuffer(16)
        private set

    var activeCache: DrawCache? = null
        private set


    //region Settings

    var activeTexture: ITexture? = DrawCommand.DEFAULT_TEXTURE
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activePrimitiveType: Int = DrawCommand.DEFAULT_PRIMITIVE_TYPE
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeBlendFunctionSource: Int = DrawCommand.DEFAULT_BLEND_FUNCTION_SOURCE
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeBlendFunctionDestination: Int = DrawCommand.DEFAULT_BLEND_FUNCTION_DESTINATION
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeDepthTestingEnabled: Boolean = DrawCommand.DEFAULT_DEPTH_TESTING_ENABLED
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeDepthMask: Boolean = DrawCommand.DEFAULT_DEPTH_MASK
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeDepthFunction: Int = DrawCommand.DEFAULT_DEPTH_FUNCTION
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeLineWidth: Float = DrawCommand.DEFAULT_LINE_WIDTH
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }

    var activeScissor: Vec4? = DrawCommand.DEFAULT_SCISSOR
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }
    //endregion

    //region Metrics
    var drawCallsOnLastFrame: Int = 0
        private set
    //endregion


    private val bufferPool = ArrayDeque<VertexBuffer>()
    private val commandBatch = ArrayDeque<DrawCommand>()

    fun cached(cache: DrawCache, block: () -> Unit) {
        flush()

        if (cache.isDirty) {
            cache.isDirty = false
            cache.commands.forEach { command ->
                command.buffer.clear()
                bufferPool.addLast(command.buffer)
            }
            cache.commands.clear()

            activeCache = cache
            block()
            activeCache = null
        }

        commandBatch.addAll(cache.commands)
        activeBuffer = bufferPool.removeLastOrNull() ?: VertexBuffer(16)
    }

    fun begin(gl: GL10) {
        drawCallsOnLastFrame = 0

        activeBuffer = bufferPool.removeLastOrNull() ?: VertexBuffer(16)
        activeTexture = DrawCommand.DEFAULT_TEXTURE
        activePrimitiveType = DrawCommand.DEFAULT_PRIMITIVE_TYPE
        activeBlendFunctionSource = DrawCommand.DEFAULT_BLEND_FUNCTION_SOURCE
        activeBlendFunctionDestination = DrawCommand.DEFAULT_BLEND_FUNCTION_DESTINATION
        activeDepthTestingEnabled = DrawCommand.DEFAULT_DEPTH_TESTING_ENABLED
        activeDepthMask = DrawCommand.DEFAULT_DEPTH_MASK
        activeDepthFunction = DrawCommand.DEFAULT_DEPTH_FUNCTION
        activeLineWidth = DrawCommand.DEFAULT_LINE_WIDTH
        activeScissor = DrawCommand.DEFAULT_SCISSOR
    }

    private fun flush() {
        if (activeBuffer.vertexCount == 0) return

        val cache = activeCache
        val batch = cache?.commands ?: commandBatch

        val lastCommand = batch.lastOrNull()

        // Try to batch with the last command if it has the same settings and cache, otherwise
        // create a new command.
        if (lastCommand == null || !lastCommand.equals(
            cache,
            activeTexture,
            activePrimitiveType,
            activeBlendFunctionSource,
            activeBlendFunctionDestination,
            activeDepthTestingEnabled,
            activeDepthMask,
            activeDepthFunction,
            activeLineWidth,
            activeScissor
        )) {
            val command = DrawCommand(
                cache = cache,
                buffer = activeBuffer,
                texture = activeTexture,
                primitiveType = activePrimitiveType,
                blendFunctionSource = activeBlendFunctionSource,
                blendFunctionDestination = activeBlendFunctionDestination,
                depthTestingEnabled = activeDepthTestingEnabled,
                depthMask = activeDepthMask,
                depthFunction = activeDepthFunction,
                lineWidth = activeLineWidth,
                scissor = activeScissor
            )

            batch.add(command)
            activeBuffer = bufferPool.removeLastOrNull() ?: VertexBuffer(16)
        }
    }

    fun end(gl: GL10) {
        // Flush remaining vertices in the active buffer as a command.
        flush()

        while (commandBatch.isNotEmpty()) {
            val command = commandBatch.removeFirst()
            command.draw(gl)

            // Return buffer to pool if it was not cached.
            if (command.cache == null) {
                command.buffer.clear()
                bufferPool.addLast(command.buffer)
            }
        }
    }


    fun DrawCommand.draw(gl: GL10) {
        if (buffer.vertexCount == 0) return

        // Scissor test
        scissor?.also { scissor ->
            GLHelper.enableScissorTest(gl)
            GLHelper.setScissor(
                gl,
                scissor.x.toInt(),
                scissor.y.toInt(),
                scissor.z.toInt(),
                scissor.w.toInt()
            )
        }

        GLHelper.disableCulling(gl)

        // Blend
        GLHelper.enableBlend(gl)
        GLHelper.blendFunction(gl, blendFunctionSource, blendFunctionDestination)

        // Depth testing
        GLHelper.setDepthFunction(gl, depthFunction)
        GLHelper.setDepthMask(gl, depthMask)
        GLHelper.setDepthTest(gl, depthTestingEnabled)

        // Line width
        GLHelper.lineWidth(gl, lineWidth)

        GLHelper.enableColorArray(gl)
        GLHelper.enableVertexArray(gl)

        val texture = texture
        val useTextures = texture != null

        if (useTextures) {
            GLHelper.enableTextures(gl)
            GLHelper.enableTexCoordArray(gl)

            texture.bind(gl)
        } else {
            GLHelper.disableTextures(gl)
            GLHelper.disableTexCoordArray(gl)
            GLHelper.bindTexture(gl, 0)
        }

        gl.glVertexPointer(
            VertexBuffer.POSITION_COMPONENTS,
            GL10.GL_FLOAT,
            VertexBuffer.VERTEX_STRIDE,
            buffer.forPosition()
        )
        gl.glColorPointer(
            VertexBuffer.COLOR_COMPONENTS,
            GL10.GL_UNSIGNED_BYTE,
            VertexBuffer.VERTEX_STRIDE,
            buffer.forColor()
        )
        if (useTextures) gl.glTexCoordPointer(
            VertexBuffer.TEXTURE_COMPONENTS,
            GL10.GL_FLOAT,
            VertexBuffer.VERTEX_STRIDE,
            buffer.forTexture()
        )

        gl.glDrawArrays(primitiveType, 0, buffer.vertexCount)
        drawCallsOnLastFrame++

        // We reset driver states because legacy components already setup them on its own pipeline.
        GLHelper.disableColorArray(gl)
        GLHelper.disableScissorTest(gl)
        GLHelper.disableTexCoordArray(gl)
    }

}
