package com.osudroid.verktex

import com.reco1l.verktex.android.vulkan.VKCommandEncoder
import com.reco1l.verktex.android.vulkan.VKFrameRing
import com.reco1l.verktex.android.vulkan.VKPipelineState
import com.reco1l.verktex.android.vulkan.VKUniformPacker
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.VertexLayout
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator
import com.reco1l.verktex.shaders.GLSL450VulkanTranslator.Stage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/**
 * The parts of the Vulkan backend that run on the JVM: the frame ring, uniform packing, the command
 * stream and the pipeline state. The native side reads what these write.
 */
class VulkanBackendTest {

    private fun ring(size: Int = 1024) = VKFrameRing(ByteBuffer.allocateDirect(size))

    @Test
    fun ringAllocatesLinearlyAndAligned() {
        val ring = ring()

        assertEquals(0, ring.writeWords(intArrayOf(1, 2, 3), 3))
        // 12 bytes written, a 64-byte aligned block starts at 64.
        assertEquals(64, ring.allocate(256, 64))
        assertEquals(320, ring.position)
        assertEquals(320, ring.writeWords(intArrayOf(4), 1))

        assertEquals(1, ring.buffer.getInt(0))
        assertEquals(3, ring.buffer.getInt(8))
        assertEquals(4, ring.buffer.getInt(320))
    }

    @Test
    fun fullRingReturnsMinusOneAndResetStartsOver() {
        val ring = ring(16)

        assertEquals(0, ring.writeWords(intArrayOf(1, 2, 3), 3))
        assertEquals(-1, ring.writeWords(intArrayOf(4, 5), 2))
        assertEquals(-1, ring.allocate(8, 8))

        ring.reset()
        assertEquals(0, ring.allocate(16, 16))
    }

    @Test
    fun uniformsArePackedAtTheTranslatorsOffsets() {
        // The fragment block of the solid shader: uColor, uBorderColor, uBorderWidth, uRadius, uSize.
        val stage = GLSL450VulkanTranslator.translate(File("assets/verktex/shaders/quad.frag").readText(), Stage.Fragment)
        val target = ByteBuffer.allocate(256).order(java.nio.ByteOrder.nativeOrder())

        // Garbage from a previous frame must not leak into members left unset.
        for (i in 0 until 64) target.putInt(i * 4, -1)

        VKUniformPacker.pack(
            target = target,
            offset = 0,
            blockSize = stage.uniformBlockSize,
            uniforms = stage.uniforms,
            values = mapOf("uRadius" to 4f, "uSize" to Vec2(10f, 20f)),
            defaults = mapOf("uColor" to Color4(0.25f, 0.5f, 0.75f, 1f), "uRadius" to 0f),
        )

        fun offsetOf(name: String) = stage.uniforms.single { it.name == name }.offset

        // The default applies where the command has no value, the command's value wins otherwise.
        assertEquals(0.25f, target.getFloat(offsetOf("uColor")), 0f)
        assertEquals(1f, target.getFloat(offsetOf("uColor") + 12), 0f)
        assertEquals(4f, target.getFloat(offsetOf("uRadius")), 0f)
        assertEquals(10f, target.getFloat(offsetOf("uSize")), 0f)
        assertEquals(20f, target.getFloat(offsetOf("uSize") + 4), 0f)

        // Neither the command nor the defaults set these.
        assertEquals(0, target.getInt(offsetOf("uBorderColor")))
        assertEquals(0, target.getInt(offsetOf("uBorderWidth")))

        // Past the block, nothing was written.
        assertEquals(-1, target.getInt(stage.uniformBlockSize))
    }

    @Test
    fun matricesArePackedColumnMajor() {
        val stage = GLSL450VulkanTranslator.translate(File("assets/verktex/shaders/particle.vert").readText(), Stage.Vertex)
        val target = ByteBuffer.allocate(256).order(java.nio.ByteOrder.nativeOrder())
        val matrix = FloatArray(16) { it.toFloat() }

        VKUniformPacker.pack(target, 0, stage.uniformBlockSize, stage.uniforms, mapOf("uTransform" to matrix), emptyMap())

        val at = stage.uniforms.single { it.name == "uTransform" }.offset
        assertArrayEquals(matrix, FloatArray(16) { target.getFloat(at + it * 4) }, 0f)
    }

    @Test
    fun drawsAreEncodedAsTheNativeSideReadsThem() {
        val encoder = VKCommandEncoder()
        val pipeline = 0x1234_5678_9ABC_DEF0L

        encoder.draw(pipeline, 1, 2, 3, 4, 64, 6, 256, 512)

        assertArrayEquals(
            intArrayOf(VKCommandEncoder.OP_DRAW, 0x9ABC_DEF0.toInt(), 0x1234_5678, 1, 2, 3, 4, 64, 6, 256, 512),
            encoder.toIntArray()
        )
        assertEquals(VKCommandEncoder.DRAW_INTS, encoder.size)

        encoder.reset()
        assertEquals(0, encoder.size)
    }

    @Test
    fun encoderGrowsPastItsInitialSize() {
        val encoder = VKCommandEncoder()
        repeat(1000) { encoder.draw(it.toLong(), 0, 0, 0, 0, it, 3, 0, 0) }

        val ints = encoder.toIntArray()
        assertEquals(1000 * VKCommandEncoder.DRAW_INTS, ints.size)
        // The last draw survived the copies.
        assertEquals(999, ints[999 * VKCommandEncoder.DRAW_INTS + 7])
    }

    @Test
    fun vertexInputDescribesBothBindings() {
        val instance = VertexLayout(listOf(VertexLayout.Attribute(3, 2, isPerInstance = true)))
        val input = VKPipelineState.vertexInput(VertexLayout.Default, instance)

        assertArrayEquals(
            intArrayOf(
                2, 20, 0, 8, 1,
                4,
                0, 0, 103, 0,   // position, R32G32_SFLOAT
                1, 0, 103, 8,   // UV
                2, 0, 37, 16,   // color, R8G8B8A8_UNORM
                3, 1, 103, 0,   // per instance
            ),
            input
        )
    }

    @Test
    fun blendModesMapToVulkanFactors() {
        // SRC_ALPHA and ONE_MINUS_SRC_ALPHA.
        assertArrayEquals(intArrayOf(6, 7, 6, 7), VKPipelineState.blendFactors(BlendMode.Alpha))
        assertArrayEquals(intArrayOf(1, 0, 1, 0), VKPipelineState.blendFactors(BlendMode.Opaque))
    }
}
