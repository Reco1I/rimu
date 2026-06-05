package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.util.GLHelper
import java.util.TreeMap
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max


object UIRenderer {

    var batch: Batch? = null
        private set

    private val batchLayers = TreeMap<Int, HashMap<BatchKey, Batch>>()
    private val batchPool = ArrayDeque<Batch>()

    private var currentLayer = 0
    private var maxLayerOnFrame = 0
    private var currentScissor: Vec4? = null

    var drawCallsOnFrame = 0
    var verticesOnFrame = 0
    var textureChanges = 0
    var primitiveChanges = 0
    var blendChanges = 0
    var depthChanges = 0
    var lineWidthChanges = 0
    var scissorChanges = 0
    var layerChanges = 0


    fun begin(gl: GL10) {
        drawCallsOnFrame = 0
        verticesOnFrame = 0

        currentLayer = 0
        maxLayerOnFrame = 0
        currentScissor = null
    }


    fun pushLayer(gl: GL10) {
        currentLayer++
        maxLayerOnFrame = max(maxLayerOnFrame, currentLayer)

        batch = findOrCreateBatchForLayer(batch?.getKey() ?: DefaultBatchKey, currentLayer)
    }

    fun pushMaxLayer(gl: GL10) {
        currentLayer = maxLayerOnFrame + 1
        maxLayerOnFrame = max(maxLayerOnFrame, currentLayer)

        batch = findOrCreateBatchForLayer(batch?.getKey() ?: DefaultBatchKey, currentLayer)
    }

    fun popLayer(gl: GL10) {
        if (currentLayer > 0) currentLayer--

        batch = findOrCreateBatchForLayer(batch?.getKey() ?: DefaultBatchKey, currentLayer)
    }

    fun setBatchOptions(
        gl: GL10,
        texture: ITexture? = batch?.texture ?: DefaultBatchKey.texture,
        primitiveType: Int = batch?.primitiveType ?: DefaultBatchKey.primitiveType,
        blendFunctionSource: Int = batch?.blendFunctionSource ?: DefaultBatchKey.blendFunctionSource,
        blendFunctionDestination: Int = batch?.blendFunctionDestination ?: DefaultBatchKey.blendFunctionDestination,
        depthTestingEnabled: Boolean = batch?.depthTestingEnabled ?: DefaultBatchKey.depthTestingEnabled,
        depthMask: Boolean = batch?.depthMask ?: DefaultBatchKey.depthMask,
        depthFunction: Int = batch?.depthFunction ?: DefaultBatchKey.depthFunction,
        lineWidth: Float = batch?.lineWidth ?: DefaultBatchKey.lineWidth,
        scissor: Vec4? = batch?.scissor ?: DefaultBatchKey.scissor
    ) {
        if (batch?.equals(
                texture,
                primitiveType,
                blendFunctionSource,
                blendFunctionDestination,
                depthTestingEnabled,
                depthMask,
                depthFunction,
                lineWidth,
                scissor
            ) == true
        ) {
            return
        }

        val key = BatchKey(
            texture,
            primitiveType,
            blendFunctionSource,
            blendFunctionDestination,
            depthTestingEnabled,
            depthMask,
            depthFunction,
            lineWidth,
            scissor
        )

        batch = findOrCreateBatchForLayer(key, currentLayer)
    }

    fun findOrCreateBatchForLayer(key: BatchKey, layer: Int): Batch {
        return batchLayers
            .getOrPut(layer) { HashMap() }
            .getOrPut(key) {
                (batchPool.removeLastOrNull() ?: Batch()).apply {
                    set(
                        texture = key.texture,
                        primitiveType = key.primitiveType,
                        blendFunctionSource = key.blendFunctionSource,
                        blendFunctionDestination = key.blendFunctionDestination,
                        depthTestingEnabled = key.depthTestingEnabled,
                        depthMask = key.depthMask,
                        depthFunction = key.depthFunction,
                        lineWidth = key.lineWidth,
                        scissor = key.scissor
                    )
                }
            }
    }


    fun end(gl: GL10) {
        for ((_, layerMap) in batchLayers.descendingMap()) {

            for (batch in layerMap.values) {
                if (batch.flush(gl)) {
                    drawCallsOnFrame++
                }

                batch.setToDefault()
                batchPool.addLast(batch)
            }

            layerMap.clear()
        }

        batchLayers.clear()
    }


    fun Batch.flush(gl: GL10): Boolean {
        if (buffer.vertexCount == 0) return false

        verticesOnFrame += buffer.vertexCount

        val texture = texture
        val useTextures = texture != null && buffer.useTextures

        val scissor = scissor
        if (scissor != null) {
            GLHelper.enableScissorTest(gl)

            if (scissor != currentScissor) {
                currentScissor = scissor

                gl.glScissor(
                    scissor.x.toInt(),
                    scissor.y.toInt(),
                    scissor.z.toInt(),
                    scissor.w.toInt()
                )
            }
        }

        GLHelper.disableCulling(gl)

        // Blend
        GLHelper.enableBlend(gl)
        GLHelper.blendFunction(gl, blendFunctionSource, blendFunctionDestination)

        // Depth testing
        GLHelper.setDepthTest(gl, depthTestingEnabled)
        if (depthTestingEnabled) {
            gl.glDepthFunc(depthFunction)
            gl.glDepthMask(depthMask)
        }

        GLHelper.enableColorArray(gl)
        GLHelper.enableVertexArray(gl)

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
            buffer.stride,
            buffer.forPosition()
        )
        gl.glColorPointer(
            VertexBuffer.COLOR_COMPONENTS,
            GL10.GL_UNSIGNED_BYTE,
            buffer.stride,
            buffer.forColor()
        )
        if (useTextures) gl.glTexCoordPointer(
            VertexBuffer.TEXTURE_COMPONENTS,
            GL10.GL_FLOAT,
            buffer.stride,
            buffer.forTexture()
        )

        gl.glDrawArrays(primitiveType, 0, buffer.vertexCount)

        buffer.clear()

        // We reset driver states because legacy components already setup them on its own pipeline.
        GLHelper.disableColorArray(gl)
        GLHelper.disableScissorTest(gl)
        GLHelper.disableTexCoordArray(gl)
        return true
    }


}
