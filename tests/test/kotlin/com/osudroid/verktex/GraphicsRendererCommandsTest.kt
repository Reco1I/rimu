package com.osudroid.verktex

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Mat2
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsBuffer
import com.reco1l.verktex.graphics.GraphicsContext
import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.graphics.GraphicsWindow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * A depth clear is recorded as its own command between draws, so the batch is never merged across it.
 */
class GraphicsRendererCommandsTest {

    private class FakeBuffer : GraphicsBuffer() {
        override val capacity get() = Int.MAX_VALUE
        override var position = 0
        val floats = mutableListOf<Float>()
        override fun clear() { position = 0; floats.clear() }
        override fun requireCapacity(requiredBytes: Int) = Unit
        override fun putFloat(float: Float) { position += 4; floats += float }
        override fun putByte(byte: Byte) { position += 1 }
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

    private class FakeTexture(override val width: Float, override val height: Float) : GraphicsTexture() {
        override fun onCreateTexture() = Unit
        override fun onBindTexture() = Unit
        override fun onUnload() = Unit
    }

    private class FakeExternalTexture : GraphicsTexture() {
        override val width = 16f
        override val height = 16f
        override val isExternal get() = true
        override val transform = FloatArray(16) { if (it % 5 == 0) 1f else 0f }
        override fun onCreateTexture() = Unit
        override fun onBindTexture() = Unit
        override fun onUnload() = Unit
    }

    private object FakeDevice : GraphicsDevice {
        override val capabilities = GraphicsDevice.Capabilities(maxTextureSize = 4096f)
        override fun createBuffer(): GraphicsBuffer = FakeBuffer()
        override fun createShader(source: GraphicsShader.Source): GraphicsShader = FakeShader()
        override fun createTexture(width: Float, height: Float): GraphicsTexture = FakeTexture(width, height)
        override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = FakeTexture(width, height)
    }

    private class RecordingRenderer : GraphicsRenderer() {
        override val device: GraphicsDevice = FakeDevice
        override val context: GraphicsContext get() = throw UnsupportedOperationException()
        override val window: GraphicsWindow get() = throw UnsupportedOperationException()
        override var version = "fake"

        val commands = mutableListOf<DrawCommand>()

        override fun drawCommand(command: DrawCommand) {
            commands += command
        }
    }

    private val solidQuad = FakeShader()
    private val video = FakeShader()
    private var previousSolidQuad: GraphicsShader? = null
    private var previousVideo: GraphicsShader? = null

    @Before
    fun setUp() {
        // The built-in shaders are lateinit, reading one that was never set throws.
        previousSolidQuad = try { BuiltIn.Shaders.SolidQuad } catch (_: UninitializedPropertyAccessException) { null }
        BuiltIn.Shaders.SolidQuad = solidQuad
        previousVideo = try { BuiltIn.Shaders.Video } catch (_: UninitializedPropertyAccessException) { null }
        BuiltIn.Shaders.Video = video
    }

    @After
    fun tearDown() {
        previousSolidQuad?.let { BuiltIn.Shaders.SolidQuad = it }
        previousVideo?.let { BuiltIn.Shaders.Video = it }
    }

    @Test
    fun clearDepthSplitsTheBatchAndKeepsOrder() {
        val renderer = RecordingRenderer()

        renderer.begin()
        renderer.drawRect(0f, 0f, 10f, 10f)
        renderer.clearDepth()
        renderer.drawRect(0f, 0f, 10f, 10f)
        renderer.end()

        val commands = renderer.commands
        assertEquals(3, commands.size)

        assertFalse(commands[0].clearDepth)
        assertTrue(commands[1].clearDepth)
        assertNull(commands[1].buffer)
        assertNull(commands[1].shader)
        assertFalse(commands[2].clearDepth)

        // The draw after the clear was not merged back into the first command.
        assertNotSame(commands[0].buffer, commands[2].buffer)
        assertEquals(VERTICES_PER_RECT * GraphicsRenderer.VERTEX_2D_STRIDE, commands[0].buffer!!.position)
        assertEquals(VERTICES_PER_RECT * GraphicsRenderer.VERTEX_2D_STRIDE, commands[2].buffer!!.position)
    }

    @Test
    fun drawsWithTheSameStateMergeWithoutAClear() {
        val renderer = RecordingRenderer()

        renderer.begin()
        renderer.drawRect(0f, 0f, 10f, 10f)
        renderer.drawRect(0f, 0f, 10f, 10f)
        renderer.end()

        assertEquals(1, renderer.commands.size)
    }

    @Test
    fun clearDepthWithNoDrawsStillRecords() {
        val renderer = RecordingRenderer()

        renderer.begin()
        renderer.clearDepth()
        renderer.end()

        assertEquals(1, renderer.commands.size)
        assertTrue(renderer.commands[0].clearDepth)
        assertNull(renderer.commands[0].buffer)
    }

    @Test
    fun drawTrianglesEmitsTheGivenVertexCount() {
        val renderer = RecordingRenderer()
        val vertices = floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f)

        renderer.begin()
        renderer.pushMatrix(Mat2(a = 2f, d = 2f, tx = 5f, ty = 7f))
        renderer.setState(shader = solidQuad)
        renderer.drawTriangles(vertices, 3, Color4.White)
        renderer.popMatrix()
        renderer.end()

        assertEquals(1, renderer.commands.size)

        val buffer = renderer.commands[0].buffer as FakeBuffer
        assertEquals(3 * GraphicsRenderer.VERTEX_2D_STRIDE, buffer.position)

        // Every vertex writes x, y, u and v as floats, the pushed matrix is applied to the positions.
        val positions = buffer.floats.chunked(4).map { (x, y) -> x to y }
        assertEquals(listOf(5f to 7f, 25f to 7f, 5f to 27f), positions)
        assertTrue(buffer.floats.chunked(4).all { it[2] == 0.5f && it[3] == 0.5f })
    }

    @Test
    fun drawTrianglesUsesTheSolidStateAfterARect() {
        val renderer = RecordingRenderer()

        renderer.begin()
        renderer.drawRect(0f, 0f, 10f, 10f, radius = 4f)
        renderer.drawTriangles(floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f), 3, Color4.White)
        renderer.end()

        // The mesh did not merge into the rect's command and does not inherit its uniforms.
        assertEquals(2, renderer.commands.size)

        val command = renderer.commands.last()
        assertSame(BuiltIn.Shaders.SolidQuad, command.shader)
        assertEquals(GraphicsShader.UniformSet(), command.uniformSet)
        assertNull(command.texture)
    }

    @Test
    fun externalTexturesDrawWithTheVideoShaderAndMerge() {
        val renderer = RecordingRenderer()
        val texture = FakeExternalTexture()
        val region = GraphicsTexture.Region(texture, FakeSource, 0f, 0f, 16f, 16f)

        renderer.begin()
        renderer.drawTexture(texture, region, 0f, 0f, 16f, 16f)
        renderer.drawTexture(texture, region, 16f, 0f, 16f, 16f)
        renderer.end()

        assertEquals(1, renderer.commands.size)

        val command = renderer.commands[0]
        assertSame(video, command.shader)
        assertSame(texture.transform, command.uniformSet.entries["uTransform"])
        assertEquals(2 * VERTICES_PER_RECT * GraphicsRenderer.VERTEX_2D_STRIDE, command.buffer!!.position)
    }

    private object FakeSource : GraphicsTexture.Source {
        override val width = 16f
        override val height = 16f
        override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) = Unit
    }

    private companion object {
        const val VERTICES_PER_RECT = 6
    }
}
