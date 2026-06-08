package com.reco1l.andengine

import android.util.Log
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
                flushCommand()
                field = value
                //activeBuffer.useTextures = value != null
            }
        }

    var activePrimitiveType: Int = DrawCommand.DEFAULT_PRIMITIVE_TYPE
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeBlendFunctionSource: Int = DrawCommand.DEFAULT_BLEND_FUNCTION_SOURCE
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeBlendFunctionDestination: Int = DrawCommand.DEFAULT_BLEND_FUNCTION_DESTINATION
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeDepthTestingEnabled: Boolean = DrawCommand.DEFAULT_DEPTH_TESTING_ENABLED
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeDepthMask: Boolean = DrawCommand.DEFAULT_DEPTH_MASK
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeDepthFunction: Int = DrawCommand.DEFAULT_DEPTH_FUNCTION
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeLineWidth: Float = DrawCommand.DEFAULT_LINE_WIDTH
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }

    var activeScissor: Vec4? = DrawCommand.DEFAULT_SCISSOR
        set(value) {
            if (field != value) {
                flushCommand()
                field = value
            }
        }
    //endregion

    //region Metrics
    var drawCallsOnLastFrame: Int = 0
        private set
    //endregion


    private val bufferPool = ArrayDeque<VertexBuffer>()
    private val layerStack = ArrayDeque<RenderLayer>().apply { addLast(RenderLayer()) }

    private var activeLayer = layerStack.first()


    private fun MutableCollection<RenderLayer>.findOrCreateLayer(zIndex: Int): RenderLayer {
        return find { it.zIndex == zIndex } ?: RenderLayer(zIndex).also { add(it) }
    }


    //region Cache
    fun startCache(cache: DrawCache) {
        flushCommand()
        activeCache = cache

        if (cache.isDirty) {
            cache.layers.forEach { layer ->
                layer.commands.forEach { command ->
                    // Pooling used buffers.
                    bufferPool.addLast(command.buffer)
                }
            }
            cache.layers.clear()
        }
    }

    fun endCache() {
        val cache = activeCache
        activeCache = null

        if (cache != null) {
            cache.isDirty = false
            cache.layers.forEach { layer ->
                layerStack.findOrCreateLayer(layer.zIndex).commands.addAll(layer.commands)
            }
        }
    }
    //endregion

    //region Layers
    fun pushLayer() {
        flushCommand()

        val zIndex = activeLayer.zIndex + 1
        activeLayer = layerStack.find { it.zIndex == zIndex } ?: RenderLayer(zIndex).also {
            layerStack.addLast(it)
        }
    }

    fun popLayer() {
        flushCommand()

        val zIndex = activeLayer.zIndex - 1
        if (zIndex < 0) {
            Log.w("UIRenderer", "Cannot pop layer: already at the bottom layer.")
            return
        }
        activeLayer = layerStack.findOrCreateLayer(zIndex)
    }
    //endregion


    fun begin(gl: GL10) {
        drawCallsOnLastFrame = 0
    }

    fun flushCommand() {
        // Nothing to draw.
        if (activeBuffer.vertexCount == 0) return

        val cache = activeCache
        val zIndex = activeLayer.zIndex

        // When the draw command is identical we can reuse the same buffer without issuing a new draw call.
        if ((cache?.layers?.find { it.zIndex == zIndex }?.commands ?: activeLayer.commands).lastOrNull { command ->
            command.texture == activeTexture &&
            command.primitiveType == activePrimitiveType &&
            command.blendFunctionSource == activeBlendFunctionSource &&
            command.blendFunctionDestination == activeBlendFunctionDestination &&
            command.depthTestingEnabled == activeDepthTestingEnabled &&
            command.depthMask == activeDepthMask &&
            command.depthFunction == activeDepthFunction &&
            command.lineWidth == activeLineWidth &&
            command.scissor == activeScissor
        } != null) return

        val command = DrawCommand(
            isCached = cache != null,
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

        if (cache != null) {
            cache.layers.findOrCreateLayer(zIndex).commands.add(command)
        } else {
            activeLayer.commands.add(command)
        }

        activeBuffer = bufferPool.removeLastOrNull()?.also { it.clear() } ?: run {
            Log.w("UIRenderer", "Buffer pool exhausted, allocating new buffer.")
            VertexBuffer(16)
        }
    }

    fun end(gl: GL10) {
        while (layerStack.isNotEmpty()) {
            val layer = layerStack.removeFirstOrNull() ?: continue

            while (layer.commands.isNotEmpty()) {
                val command = layer.commands.removeFirstOrNull() ?: continue
                command.draw(gl)

                if (!command.isCached) {
                    bufferPool.addLast(command.buffer)
                }
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
