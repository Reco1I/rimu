package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

const val POSITION_SIZE = 2
const val POSITION_OFFSET = 0
const val COLOR_SIZE = 4
const val COLOR_OFFSET = POSITION_SIZE
const val TEXTURE_SIZE = 2
const val TEXTURE_OFFSET = POSITION_SIZE + COLOR_SIZE
const val VERTEX_STRIDE = POSITION_SIZE + COLOR_SIZE + TEXTURE_SIZE


object UIRenderer {

    /**
     * A counter for the number of draw calls made in the current frame. This can be used
     * for performance monitoring and optimization purposes.
     */
    var drawCallsOnFrame = 0

    /**
     * The current rendering state, which includes the texture atlas, primitive type, blending information,
     * depth testing information, and scissor rectangle.
     */
    var state = RenderState()
        private set


    /**
     * Begins a new rendering session. This function should be called at the start of each frame
     * before any rendering operations are performed.
     */
    fun begin(gl: GL10) {
        drawCallsOnFrame = 0
    }

    /**
     * Sets the current rendering state.
     */
    fun setState(
        gl: GL10,
        texture: ITexture? = state.texture,
        primitiveType: Int = state.primitiveType,
        blendFunctionSource: Int = state.blendFunctionSource,
        blendFunctionDestination: Int = state.blendFunctionDestination,
        depthTestingEnabled: Boolean = state.depthTestingEnabled,
        depthMask: Boolean = state.depthMask,
        depthFunction: Int = state.depthFunction,
        scissor: Vec4? = state.scissor
    ) {
        if (
            state.texture != texture ||
            state.primitiveType != primitiveType ||
            state.blendFunctionSource != blendFunctionSource ||
            state.blendFunctionDestination != blendFunctionDestination ||
            state.depthTestingEnabled != depthTestingEnabled ||
            state.depthMask != depthMask ||
            state.depthFunction != depthFunction ||
            state.scissor != scissor
        ) {
            if (state.flush(gl)) {
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
                scissor = scissor
            )

        }
    }

    /**
     * Ends the current rendering session flushing any remaining vertex data to the GPU.
     */
    fun end(gl: GL10) {
        if (state.flush(gl)) {
            drawCallsOnFrame++
        }
    }

    fun RenderState.flush(gl: GL10): Boolean {
        if (buffer.stored == 0) return false

        GLHelper.disableCulling(gl)

        val scissor = scissor
        if (scissor != null) {
            GLHelper.enableScissorTest(gl)

            gl.glScissor(
                scissor.x.toInt(),
                scissor.y.toInt(),
                scissor.z.toInt(),
                scissor.w.toInt()
            )
        }

        GLHelper.setDepthTest(gl, depthTestingEnabled)
        gl.glDepthFunc(depthFunction)
        gl.glDepthMask(depthMask)

        GLHelper.enableBlend(gl)
        GLHelper.blendFunction(gl, blendFunctionSource, blendFunctionDestination)

        buffer.offsetToPosition()
        GLHelper.enableVertexArray(gl)
        gl.glVertexPointer(POSITION_SIZE, GL10.GL_FLOAT, VERTEX_STRIDE * Float.SIZE_BYTES, buffer.getInternalBuffer())

        buffer.offsetToColor()
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(COLOR_SIZE, GL10.GL_FLOAT, VERTEX_STRIDE * Float.SIZE_BYTES, buffer.getInternalBuffer())

        val texture = texture
        if (texture != null) {
            GLHelper.enableTextures(gl)
            texture.bind(gl)

            buffer.offsetToTexture()
            GLHelper.enableTexCoordArray(gl)
            gl.glTexCoordPointer(TEXTURE_SIZE, GL10.GL_FLOAT, VERTEX_STRIDE * Float.SIZE_BYTES, buffer.getInternalBuffer())
        } else {
            GLHelper.disableTextures(gl)
            GLHelper.disableTexCoordArray(gl)
        }

        gl.glDrawArrays(primitiveType, 0, buffer.stored)

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        GLHelper.disableScissorTest(gl)
        buffer.clear()
        return true
    }

}
