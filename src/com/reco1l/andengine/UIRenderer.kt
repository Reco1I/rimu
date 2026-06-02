package com.reco1l.andengine

import android.util.Log
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.framework.math.Vec4
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.opengl.texture.atlas.TextureAtlas
import org.anddev.andengine.opengl.texture.source.ITextureAtlasSource
import org.anddev.andengine.opengl.util.GLHelper
import java.util.LinkedList
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


    private val stateQueue = LinkedList<RenderState>()


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
        texture: TextureAtlas<out ITextureAtlasSource>? = state.texture,
        primitiveType: Int = state.primitiveType,
        blendInfo: BlendInfo = state.blendInfo,
        depthInfo: DepthInfo = state.depthInfo,
        scissor: Vec4? = state.scissor
    ) {
        val queueState = stateQueue.firstOrNull {
            it.texture == texture &&
                    it.primitiveType == primitiveType &&
                    it.blendInfo == blendInfo &&
                    it.depthInfo == depthInfo &&
                    it.scissor == scissor
        }

        if (queueState == null) {
            Log.v("UIRenderer", "Creating new RenderState: texture=$texture, primitiveType=$primitiveType, blendInfo=$blendInfo, depthInfo=$depthInfo)")
            val newState = RenderState(texture, primitiveType, blendInfo, depthInfo, scissor)
            stateQueue.add(newState)
            state = newState
        } else {
            state = queueState
        }
    }

    /**
     * Ends the current rendering session. This function should be called at the end of each frame
     * after all rendering operations have been performed.
     * It will flush all the states in the state queue and clear it for the next frame.
     */
    fun end(gl: GL10) {
        stateQueue.fastForEach { state ->
            if (state.flush(gl))
                drawCallsOnFrame++
        }
    }

    fun RenderState.flush(gl: GL10): Boolean {
        val limit = buffer.position()
        if (limit == 0) return false

        buffer.limit(limit)

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
        } else {
            GLHelper.disableScissorTest(gl)
        }

        GLHelper.setDepthTest(gl, depthInfo.test)
        if (depthInfo.test) {
            gl.glDepthFunc(depthInfo.function)
            gl.glDepthMask(depthInfo.mask)
        }

        if (blendInfo != BlendInfo.None) {
            GLHelper.enableBlend(gl)
            GLHelper.blendFunction(gl, blendInfo.sourceFactor, blendInfo.destinationFactor)
        } else {
            GLHelper.disableBlend(gl)
        }

        buffer.position(POSITION_OFFSET)
        GLHelper.enableVertexArray(gl)
        gl.glVertexPointer(POSITION_SIZE, GL10.GL_FLOAT, VERTEX_STRIDE * Float.SIZE_BYTES, buffer)

        buffer.position(COLOR_OFFSET)
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(COLOR_SIZE, GL10.GL_FLOAT, VERTEX_STRIDE * Float.SIZE_BYTES, buffer)

        val texture = texture
        if (texture != null) {
            GLHelper.enableTextures(gl)
            texture.bind(gl)

            buffer.position(TEXTURE_OFFSET)
            GLHelper.enableTexCoordArray(gl)
            gl.glTexCoordPointer(
                TEXTURE_SIZE,
                GL10.GL_FLOAT,
                VERTEX_STRIDE * Float.SIZE_BYTES,
                buffer
            )
        } else {
            GLHelper.disableTextures(gl)
            GLHelper.disableTexCoordArray(gl)
        }

        gl.glDrawArrays(primitiveType, 0, limit / VERTEX_STRIDE)

        // Disable color array after drawing for compatibility with AndEngine's default rendering pipeline.
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)

        buffer.clear()
        return true
    }

}
