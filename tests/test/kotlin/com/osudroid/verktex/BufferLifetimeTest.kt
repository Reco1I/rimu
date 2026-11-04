package com.osudroid.verktex

import com.reco1l.verktex.Logger
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsBuffer
import com.reco1l.verktex.graphics.GraphicsContext
import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.graphics.GraphicsWindow
import com.reco1l.verktex.graphics.VertexLayout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * A buffer used by a recorded draw must keep its data until the frame is executed, after which it
 * can be rewritten. See the lifetime in [GraphicsBuffer].
 */
class BufferLifetimeTest {

    private class FakeBuffer : GraphicsBuffer() {
        override fun uploadDataToGPU() = Unit
        override fun onBind() = Unit
        override fun onLoad() = Unit
        override fun onUnload() = Unit
    }

    private class FakeShader : GraphicsShader() {
        override val source = Source("", "")
        override fun applyUniforms(uniformSet: UniformSet) = Unit
        override fun onBind() = Unit
        override fun onLoad() = Unit
        override fun onUnload() = Unit
    }

    private object FakeDevice : GraphicsDevice {
        override val capabilities = GraphicsDevice.Capabilities(maxTextureSize = 4096f)
        override fun createBuffer(): GraphicsBuffer = FakeBuffer()
        override fun createShader(source: GraphicsShader.Source): GraphicsShader = FakeShader()
        override fun createTexture(width: Float, height: Float): GraphicsTexture = throw UnsupportedOperationException()
        override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = throw UnsupportedOperationException()
    }

    private class RecordingRenderer : GraphicsRenderer() {
        override val device = FakeDevice
        override val context: GraphicsContext get() = throw UnsupportedOperationException()
        override val window: GraphicsWindow get() = throw UnsupportedOperationException()
        override var version = "fake"

        // What each executed command's buffer held when it was executed.
        val executed = mutableListOf<IntArray>()

        override fun drawCommand(command: DrawCommand) {
            val buffer = command.instanceBuffer ?: command.buffer ?: return
            executed += buffer.words.copyOf(buffer.wordCount)
        }
    }

    private val warnings = mutableListOf<String>()

    @Before
    fun setUp() {
        Logger.implementation = object : Logger.Implementation {
            override fun i(tag: String, message: String) = Unit
            override fun w(tag: String, message: String) {
                warnings += message
            }
            override fun e(tag: String, message: String, throwable: Throwable?) = Unit
        }
    }

    private val shader = FakeShader()

    private fun RecordingRenderer.drawParticles(vertices: GraphicsBuffer, instances: GraphicsBuffer, value: Int) {
        instances.clear()
        instances.putPackedBytes(value)

        drawInstanced(
            shader = shader,
            texture = null,
            vertexBuffer = vertices,
            vertexLayout = VertexLayout.Default,
            vertexCount = 1,
            instanceBuffer = instances,
            instanceLayout = VertexLayout.Default,
            instanceCount = 1,
            uniformSet = GraphicsShader.UniformSet()
        )
    }

    private fun quad() = FakeBuffer().apply { putVertex(0f, 0f, 0f, 0f, 0) }

    @Test
    fun rewritingABufferInTheNextFrameIsAllowed() {
        val renderer = RecordingRenderer()
        val vertices = quad()
        val instances = FakeBuffer()

        renderer.begin()
        renderer.drawParticles(vertices, instances, 1)
        renderer.end()

        renderer.begin()
        renderer.drawParticles(vertices, instances, 2)
        renderer.end()

        assertEquals(emptyList<String>(), warnings)
        assertArrayEquals(intArrayOf(1), renderer.executed[0])
        assertArrayEquals(intArrayOf(2), renderer.executed[1])
    }

    @Test
    fun rewritingABufferUsedByThisFrameWarns() {
        val renderer = RecordingRenderer()
        val vertices = quad()
        val instances = FakeBuffer()

        renderer.begin()
        renderer.drawParticles(vertices, instances, 1)
        renderer.drawParticles(vertices, instances, 2)
        renderer.end()

        assertEquals(1, warnings.size)
    }

    @Test
    fun buffersOfADroppedFrameCanBeRewritten() {
        val renderer = RecordingRenderer()
        val vertices = quad()
        val instances = FakeBuffer()

        // Recorded but never executed, the next frame starts over.
        renderer.begin()
        renderer.drawParticles(vertices, instances, 1)

        renderer.begin()
        renderer.drawParticles(vertices, instances, 2)
        renderer.end()

        assertEquals(emptyList<String>(), warnings)
        assertArrayEquals(intArrayOf(2), renderer.executed.single())
    }

    @Test
    fun pooledBuffersAreNotSharedWithinAFrame() {
        val renderer = RecordingRenderer()
        val first = FakeShader()
        val second = FakeShader()

        renderer.begin()
        renderer.setState(shader = first)
        renderer.drawGeometry { _, _ -> putPackedBytes(1) }
        renderer.setState(shader = second)
        renderer.drawGeometry { _, _ -> putPackedBytes(2) }
        renderer.end()

        assertEquals(emptyList<String>(), warnings)
        assertArrayEquals(intArrayOf(1), renderer.executed[0])
        assertArrayEquals(intArrayOf(2), renderer.executed[1])
    }
}
