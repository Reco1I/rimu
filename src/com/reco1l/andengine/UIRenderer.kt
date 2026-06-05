package com.reco1l.andengine

import android.util.Log
import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10


object UIRenderer {

    var activeBatch: Batch? = null
        private set


    private val batchPool = ArrayDeque<Batch>()


    //region Metrics
    var drawCallsOnFrame = 0
    var verticesOnFrame = 0
    var textureChanges = 0
    var primitiveChanges = 0
    var blendChanges = 0
    var depthChanges = 0
    var lineWidthChanges = 0
    var scissorChanges = 0

    var quadsRendered = 0
    var trianglesRendered = 0
    var circlesRendered = 0
    var linesRendered = 0
    var spriteRendered = 0
    var textsRendered = 0
    var charactersRendered = 0
    //endregion


    fun begin(gl: GL10) {
        drawCallsOnFrame = 0
        verticesOnFrame = 0
        textureChanges = 0
        primitiveChanges = 0
        blendChanges = 0
        depthChanges = 0
        lineWidthChanges = 0
        scissorChanges = 0

        quadsRendered = 0
        trianglesRendered = 0
        circlesRendered = 0
        linesRendered = 0
        spriteRendered = 0
        textsRendered = 0
        charactersRendered = 0
    }

    fun setState(
        gl: GL10,
        texture: ITexture? = activeBatch?.texture ?: Batch.DEFAULT_TEXTURE,
        primitiveType: Int = activeBatch?.primitiveType ?: Batch.DEFAULT_PRIMITIVE_TYPE,
        blendFunctionSource: Int = activeBatch?.blendFunctionSource ?: Batch.DEFAULT_BLEND_FUNCTION_SOURCE,
        blendFunctionDestination: Int = activeBatch?.blendFunctionDestination ?: Batch.DEFAULT_BLEND_FUNCTION_DESTINATION,
        depthTestingEnabled: Boolean = activeBatch?.depthTestingEnabled ?: Batch.DEFAULT_DEPTH_TESTING_ENABLED,
        depthMask: Boolean = activeBatch?.depthMask ?: Batch.DEFAULT_DEPTH_MASK,
        depthFunction: Int = activeBatch?.depthFunction ?: Batch.DEFAULT_DEPTH_FUNCTION,
        lineWidth: Float = activeBatch?.lineWidth ?: Batch.DEFAULT_LINE_WIDTH,
        scissor: Vec4? = activeBatch?.scissor ?: Batch.DEFAULT_SCISSOR,
    ) {
        if (activeBatch?.equals(
                texture = texture,
                primitiveType = primitiveType,
                blendFunctionSource = blendFunctionSource,
                blendFunctionDestination = blendFunctionDestination,
                depthTestingEnabled = depthTestingEnabled,
                depthMask = depthMask,
                depthFunction = depthFunction,
                lineWidth = lineWidth,
                scissor = scissor
            ) == true
        ) return

        //region Metrics
        if ((activeBatch?.buffer?.vertexCount ?: 0) > 0) {
            if (texture != activeBatch?.texture) textureChanges++
            if (primitiveType != activeBatch?.primitiveType) primitiveChanges++
            if (blendFunctionSource != activeBatch?.blendFunctionSource || blendFunctionDestination != activeBatch?.blendFunctionDestination) blendChanges++
            if (depthTestingEnabled != activeBatch?.depthTestingEnabled || depthMask != activeBatch?.depthMask || depthFunction != activeBatch?.depthFunction) depthChanges++
            if (lineWidth != activeBatch?.lineWidth) lineWidthChanges++
            if (scissor != activeBatch?.scissor) scissorChanges++
        }
        //endregion

        activeBatch?.also { batch ->
            batch.flush(gl)
            batch.setToDefault()
            batchPool.addLast(batch)
        }

        val newBatch = batchPool.removeLastOrNull() ?: run {
            Log.w("UIRenderer", "Batch pool exhausted, creating a new batch...")
            Batch()
        }
        newBatch.set(
            texture = texture,
            primitiveType = primitiveType,
            blendFunctionSource = blendFunctionSource,
            blendFunctionDestination = blendFunctionDestination,
            depthTestingEnabled = depthTestingEnabled,
            depthMask = depthMask,
            depthFunction = depthFunction,
            lineWidth = lineWidth,
            scissor = scissor
        )

        activeBatch = newBatch
    }


    fun end(gl: GL10) {
        activeBatch?.also { batch ->
            batch.flush(gl)
            batch.setToDefault()
            batchPool.addLast(batch)
        }
        activeBatch = null
    }


    fun Batch.flush(gl: GL10) {
        if (buffer.vertexCount == 0) return

        verticesOnFrame += buffer.vertexCount

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
        val useTextures = texture != null && buffer.useTextures

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
        drawCallsOnFrame++

        buffer.clear()

        // We reset driver states because legacy components already setup them on its own pipeline.
        GLHelper.disableColorArray(gl)
        GLHelper.disableScissorTest(gl)
        GLHelper.disableTexCoordArray(gl)
    }

}
