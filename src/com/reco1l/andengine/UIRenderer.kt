package com.reco1l.andengine

import android.util.Log
import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.util.MathUtils.nextPowerOfTwo
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11


object UIRenderer {

    val buffer = VertexBuffer(512)


    var state = RenderState()
        private set

    var drawCallsOnFrame = 0
        private set

    var verticesOnFrame = 0
        private set


    private var dynamicVbo = intArrayOf(-1)
    private var dynamicVboCapacity = 0

    private var currentScissor: Vec4? = null


    var textureChanges = 0
    var primitiveChanges = 0
    var blendChanges = 0
    var depthChanges = 0
    var lineWidthChanges = 0
    var scissorChanges = 0


    fun begin(gl: GL10) {
        drawCallsOnFrame = 0
        verticesOnFrame = 0

        textureChanges = 0
        primitiveChanges = 0
        blendChanges = 0
        depthChanges = 0
        lineWidthChanges = 0
        scissorChanges = 0

        if (GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS && dynamicVbo[0] == -1) {
            Log.i("UIRenderer", "Initializing VBO for UIRenderer...")

            gl as GL11
            gl.glGenBuffers(1, dynamicVbo, 0)

            dynamicVboCapacity = 1024 * 1024

            if (dynamicVbo[0] == 0 || dynamicVbo[0] == -1) {
                throw RuntimeException("Failed to create VBO for UIRenderer")
            }

            GLHelper.bindBuffer(gl, dynamicVbo[0])
            gl.glBufferData(GL11.GL_ARRAY_BUFFER, dynamicVboCapacity, null, GL11.GL_DYNAMIC_DRAW)
            GLHelper.bindBuffer(gl, 0)
        }

        currentScissor = null
    }

    fun setState(
        gl: GL10,
        texture: ITexture? = state.texture,
        primitiveType: Int = state.primitiveType,
        blendFunctionSource: Int = state.blendFunctionSource,
        blendFunctionDestination: Int = state.blendFunctionDestination,
        depthTestingEnabled: Boolean = state.depthTestingEnabled,
        depthMask: Boolean = state.depthMask,
        depthFunction: Int = state.depthFunction,
        lineWidth: Float = state.lineWidth,
        scissor: Vec4? = state.scissor
    ) {
        // If the new state is the same as the current state, no need to flush or change anything.
        if (state.texture == texture &&
            state.primitiveType == primitiveType &&
            state.blendFunctionSource == blendFunctionSource &&
            state.blendFunctionDestination == blendFunctionDestination &&
            state.depthTestingEnabled == depthTestingEnabled &&
            state.depthMask == depthMask &&
            state.depthFunction == depthFunction &&
            state.lineWidth == lineWidth &&
            state.scissor == scissor) return

        if (state.flush(gl)) {
            if (state.texture != texture) textureChanges++
            if (state.primitiveType != primitiveType) primitiveChanges++
            if (state.blendFunctionSource != blendFunctionSource || state.blendFunctionDestination != blendFunctionDestination) blendChanges++
            if (state.depthTestingEnabled != depthTestingEnabled || state.depthMask != depthMask || state.depthFunction != depthFunction) depthChanges++
            if (state.lineWidth != lineWidth) lineWidthChanges++
            if (state.scissor != scissor) scissorChanges++

            drawCallsOnFrame++
        }

        state = state.copy(
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

        buffer.useTextures = texture != null
    }

    fun end(gl: GL10) {
        if (state.flush(gl)) {
            drawCallsOnFrame++
        }
    }

    fun RenderState.flush(gl: GL10): Boolean {
        if (buffer.vertexCount == 0) return false

        verticesOnFrame += buffer.vertexCount

        val useVBO = GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS && dynamicVbo[0] != -1
        val useTextures = texture != null && buffer.useTextures

        val usedBytes = buffer.vertexCount * buffer.stride

        if (useVBO) {
            gl as GL11
            GLHelper.bindBuffer(gl, dynamicVbo[0])

            if (usedBytes > dynamicVboCapacity) {
                dynamicVboCapacity = nextPowerOfTwo(usedBytes)
                gl.glBufferData(GL11.GL_ARRAY_BUFFER, dynamicVboCapacity, null, GL11.GL_DYNAMIC_DRAW)
            }
        }

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

        if (useVBO) {
            gl as GL11
            gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, 0, usedBytes, buffer.fromZero())

            gl.glVertexPointer(VertexBuffer.POSITION_COMPONENTS, GL10.GL_FLOAT, buffer.stride, VertexBuffer.POSITION_OFFSET_BYTES)
            gl.glColorPointer(VertexBuffer.COLOR_COMPONENTS, GL10.GL_UNSIGNED_BYTE, buffer.stride, VertexBuffer.COLOR_OFFSET_BYTES)

            if (useTextures) gl.glTexCoordPointer(VertexBuffer.TEXTURE_COMPONENTS, GL10.GL_FLOAT, buffer.stride, VertexBuffer.TEXTURE_OFFSET_BYTES)
        } else {
            gl.glVertexPointer(VertexBuffer.POSITION_COMPONENTS, GL10.GL_FLOAT, buffer.stride, buffer.forPosition())
            gl.glColorPointer(VertexBuffer.COLOR_COMPONENTS, GL10.GL_UNSIGNED_BYTE, buffer.stride, buffer.forColor())

            if (useTextures) gl.glTexCoordPointer(VertexBuffer.TEXTURE_COMPONENTS, GL10.GL_FLOAT, buffer.stride, buffer.forTexture())
        }

        gl.glDrawArrays(primitiveType, 0, buffer.vertexCount)

        if (useVBO) GLHelper.bindBuffer(gl as GL11, 0)

        buffer.clear()

        // We reset driver states because legacy components already setup them on its own pipeline.
        GLHelper.disableColorArray(gl)
        GLHelper.disableScissorTest(gl)
        GLHelper.disableTexCoordArray(gl)
        return true
    }


}
