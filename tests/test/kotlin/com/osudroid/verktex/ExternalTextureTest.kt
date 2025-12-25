package com.osudroid.verktex

import com.reco1l.verktex.android.gles.ESExternalTexture
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsBuffer
import com.reco1l.verktex.graphics.GraphicsContext
import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsResource
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.graphics.GraphicsWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The CPU side of external (video) textures: the mat4 uniform and the frame latch. The GL and
 * SurfaceTexture calls are replaced through the texture's seams.
 */
class ExternalTextureTest {

    private class FakeExternalTexture : ESExternalTexture(320f, 180f) {
        var updates = 0
        var contextLosses = 0

        override fun onContextLost() {
            contextLosses++
            super.onContextLost()
        }

        override fun onCreateTexture() = Unit
        override fun bindExternalTexture() = Unit
        override fun onUnload() = Unit

        override fun updateTexImage() {
            updates++
        }
    }

    @Test
    fun mat4UniformIsAcceptedByTheUniformSet() {
        val matrix = FloatArray(16) { it.toFloat() }
        val uniforms = GraphicsShader.UniformSet().apply { set("uTransform", matrix) }

        assertSame(matrix, uniforms.entries["uTransform"])
        assertEquals(16, (uniforms.entries["uTransform"] as FloatArray).size)
    }

    @Test
    fun externalTextureStartsWithIdentityTransform() {
        val texture = FakeExternalTexture()

        val identity = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        assertArrayEquals(identity, texture.transform, 0f)
        assertTrue(texture.isExternal)

        // One region covering the whole texture, so a sprite can draw it.
        val region = texture.region
        assertSame(texture, region.texture)
        assertEquals(0f, region.u0, 0f)
        assertEquals(0f, region.v0, 0f)
        assertEquals(1f, region.u1, 0f)
        assertEquals(1f, region.v1, 0f)
    }

    @Test
    fun markFrameAvailableIsLatchedOnce() {
        val texture = FakeExternalTexture()

        texture.bind()
        assertEquals(0, texture.updates)

        texture.markFrameAvailable()
        texture.markFrameAvailable()
        texture.bind()
        assertEquals(1, texture.updates)

        // The flag was cleared by the first bind.
        texture.bind()
        assertEquals(1, texture.updates)

        texture.markFrameAvailable()
        texture.bind()
        assertEquals(2, texture.updates)
    }

    @Test
    fun externalTextureIsRegisteredWithTheRenderer() {
        val device = object : GraphicsDevice {
            override val capabilities = GraphicsDevice.Capabilities(maxTextureSize = 4096f)
            override fun createBuffer(): GraphicsBuffer = throw UnsupportedOperationException()
            override fun createShader(source: GraphicsShader.Source): GraphicsShader = throw UnsupportedOperationException()
            override fun createTexture(width: Float, height: Float): GraphicsTexture = throw UnsupportedOperationException()
            override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = FakeExternalTexture()
        }
        val renderer = object : GraphicsRenderer() {
            override val device = device
            override val context: GraphicsContext get() = throw UnsupportedOperationException()
            override val window: GraphicsWindow get() = throw UnsupportedOperationException()
            override var version = "fake"
            override fun drawCommand(command: DrawCommand) = Unit
        }

        val texture = renderer.createExternalTexture(320f, 180f) as FakeExternalTexture
        texture.load()

        renderer.markContextLoss()

        assertEquals(1, texture.contextLosses)
        assertEquals(GraphicsResource.State.Unloaded, texture.state)
    }

    @Test
    fun externalTextureRejectsSources() {
        val texture = FakeExternalTexture()
        val source = object : GraphicsTexture.Source {
            override val width = 1f
            override val height = 1f
            override fun uploadToHardware(texturePositionX: Float, texturePositionY: Float) = Unit
        }

        assertFalse(texture.fitsSource(source))
        assertNull(texture.addSource(source))
    }

    @Test
    fun verticalFlipFoldMatchesMatrixMultiplication() {
        val random = java.util.Random(7)
        val matrix = FloatArray(16) { random.nextFloat() * 4f - 2f }

        // flip maps v to 1 - v, column-major: column 1 is (0, -1, 0, 0) and column 3 is (0, 1, 0, 1).
        val flip = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, -1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 1f, 0f, 1f
        )
        val expected = FloatArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += matrix[k * 4 + row] * flip[column * 4 + k]
                }
                expected[column * 4 + row] = sum
            }
        }

        val folded = matrix.copyOf()
        ESExternalTexture.foldVerticalFlip(folded)

        assertArrayEquals(expected, folded, 1e-6f)
    }
}
