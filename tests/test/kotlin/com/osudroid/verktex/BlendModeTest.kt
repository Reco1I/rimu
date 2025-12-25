package com.osudroid.verktex

import android.opengl.GLES32
import com.reco1l.verktex.android.gles.toGLES
import com.reco1l.verktex.graphics.BlendFactor
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsShader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlendModeTest {

    private fun assertFactors(
        mode: BlendMode,
        sourceRGB: BlendFactor,
        destinationRGB: BlendFactor,
        sourceAlpha: BlendFactor = sourceRGB,
        destinationAlpha: BlendFactor = destinationRGB
    ) {
        assertEquals(sourceRGB, mode.sourceRGB)
        assertEquals(destinationRGB, mode.destinationRGB)
        assertEquals(sourceAlpha, mode.sourceAlpha)
        assertEquals(destinationAlpha, mode.destinationAlpha)
    }

    @Test
    fun existingModesKeepTheirFactors() {
        assertFactors(BlendMode.Opaque, BlendFactor.One, BlendFactor.Zero)
        assertFactors(BlendMode.Alpha, BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha)
        assertFactors(BlendMode.Additive, BlendFactor.SrcAlpha, BlendFactor.One)
        assertFactors(BlendMode.Multiply, BlendFactor.DstColor, BlendFactor.Zero)
    }

    @Test
    fun premultipliedAlphaIsOneOneMinusSrcAlpha() {
        assertFactors(BlendMode.PremultipliedAlpha, BlendFactor.One, BlendFactor.OneMinusSrcAlpha)
    }

    @Test
    fun customBlendModesBatchOnlyWhenEqual() {
        val uniforms = GraphicsShader.UniformSet()
        val command = DrawCommand(
            blendMode = BlendMode.Custom(BlendFactor.One, BlendFactor.One, BlendFactor.Zero, BlendFactor.One),
            uniformSet = uniforms
        )

        fun reuses(mode: BlendMode) = command.canReuse(null, null, null, mode, DepthState.Default, uniforms)

        assertTrue(reuses(BlendMode.Custom(BlendFactor.One, BlendFactor.One, BlendFactor.Zero, BlendFactor.One)))
        assertFalse(reuses(BlendMode.Custom(BlendFactor.One, BlendFactor.One)))
        assertFalse(reuses(BlendMode.Additive))
        assertTrue(DrawCommand(blendMode = BlendMode.Alpha, uniformSet = uniforms).canReuse(null, null, null, BlendMode.Alpha, DepthState.Default, uniforms))
    }

    @Test
    fun factorsMapToGLConstants() {
        val expected = mapOf(
            BlendFactor.Zero to GLES32.GL_ZERO,
            BlendFactor.One to GLES32.GL_ONE,
            BlendFactor.SrcAlpha to GLES32.GL_SRC_ALPHA,
            BlendFactor.OneMinusSrcAlpha to GLES32.GL_ONE_MINUS_SRC_ALPHA,
            BlendFactor.DstAlpha to GLES32.GL_DST_ALPHA,
            BlendFactor.OneMinusDstAlpha to GLES32.GL_ONE_MINUS_DST_ALPHA,
            BlendFactor.DstColor to GLES32.GL_DST_COLOR,
            BlendFactor.SrcColor to GLES32.GL_SRC_COLOR,
            BlendFactor.OneMinusSrcColor to GLES32.GL_ONE_MINUS_SRC_COLOR,
            BlendFactor.OneMinusDstColor to GLES32.GL_ONE_MINUS_DST_COLOR
        )

        assertEquals(BlendFactor.entries.toSet(), expected.keys)
        expected.forEach { (factor, constant) -> assertEquals(factor.name, constant, factor.toGLES()) }
    }
}
