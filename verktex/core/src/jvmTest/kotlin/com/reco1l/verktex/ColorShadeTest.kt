package com.reco1l.verktex

import com.reco1l.verktex.data.Color4
import kotlin.math.atan2
import kotlin.math.cbrt
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorShadeTest {

    private val color = Color4(0.9f, 0.4f, 0.2f, 0.7f)

    @Test
    fun fullFactorKeepsTheColor() {
        val shaded = color.shade(1f)
        assertEquals(color.red, shaded.red, 1e-4f)
        assertEquals(color.green, shaded.green, 1e-4f)
        assertEquals(color.blue, shaded.blue, 1e-4f)
        assertEquals(color.alpha, shaded.alpha, 0f)
    }

    @Test
    fun zeroFactorIsBlackKeepingAlpha() {
        assertEquals(Color4(0f, 0f, 0f, color.alpha), color.shade(0f))
    }

    @Test
    fun scalesOklabLightnessAndKeepsHue() {
        val (l, a, b) = oklab(color)
        for (factor in listOf(0.9f, 0.5f, 0.1f)) {
            val (sl, sa, sb) = oklab(color.shade(factor))
            assertEquals(l * factor, sl, 1e-3f)
            assertEquals(atan2(b, a), atan2(sb, sa), 1e-3f)
        }
    }

    private fun oklab(color: Color4): Triple<Float, Float, Float> {
        val c = color.toLinear()
        val l = cbrt(0.4122214708f * c.red + 0.5363325363f * c.green + 0.0514459929f * c.blue)
        val m = cbrt(0.2119034982f * c.red + 0.6806995451f * c.green + 0.1073969566f * c.blue)
        val s = cbrt(0.0883024619f * c.red + 0.2817188376f * c.green + 0.6299787005f * c.blue)
        return Triple(
            0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
            1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
            0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
        )
    }
}
