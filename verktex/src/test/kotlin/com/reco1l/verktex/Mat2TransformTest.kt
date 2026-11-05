package com.osudroid.verktex

import com.reco1l.verktex.data.Mat2
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class Mat2TransformTest {

    @Test
    fun inPlaceTransformMatchesTheAllocatingOne() {
        val random = Random(42)
        val out = FloatArray(4)

        repeat(200) {
            val matrix = Mat2(
                a = random.nextFloat() * 4f - 2f,
                b = random.nextFloat() * 4f - 2f,
                c = random.nextFloat() * 4f - 2f,
                d = random.nextFloat() * 4f - 2f,
                tx = random.nextFloat() * 200f - 100f,
                ty = random.nextFloat() * 200f - 100f,
            )
            val x = random.nextFloat() * 1000f - 500f
            val y = random.nextFloat() * 1000f - 500f

            val expected = matrix.transform(x, y)
            matrix.transform(x, y, out, offset = 2)

            assertEquals(expected.x, out[2], 0f)
            assertEquals(expected.y, out[3], 0f)
        }
    }

    @Test
    fun inPlaceTransformDefaultsToTheStartOfTheArray() {
        val out = floatArrayOf(-1f, -1f, -1f)

        Mat2(tx = 5f, ty = 7f).transform(1f, 2f, out)

        assertEquals(6f, out[0], 0f)
        assertEquals(9f, out[1], 0f)
        assertEquals(-1f, out[2], 0f)
    }
}
