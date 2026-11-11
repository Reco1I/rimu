package com.osudroid.math

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class ExactRoundingTest {

    private fun expected(value: Double, digits: Int) = BigDecimal(value).setScale(digits, RoundingMode.HALF_UP).toFloat()

    private fun check(value: Double, digits: Int) =
        assertEquals("$value to $digits digits", expected(value, digits).toRawBits(), roundExactHalfUp(value, digits).toRawBits())

    @Test
    fun testKnownValues() {
        check(2.675, 2)
        check(2.5, 0)
        check(-2.5, 0)
        check(0.125, 2)
        check(1.0005, 3)
        check(99.995, 2)
        check(123456789.123456789, 4)
        check(Double.MIN_VALUE, 2)
        check(1e300, 2)
        check(-0.0049, 2)
    }

    @Test
    fun testRandomValues() {
        val random = Random(42)

        repeat(20_000) {
            val digits = random.nextInt(0, 9)
            val value = when (it % 4) {
                0 -> random.nextDouble(-1000.0, 1000.0)
                1 -> random.nextDouble(-1.0, 1.0)
                // Ties and values next to them.
                2 -> (random.nextInt(-100_000, 100_000) + 0.5) / 10.0.pow(digits)
                else -> Double.fromBits(random.nextLong() and Long.MAX_VALUE).takeIf { d -> d.isFinite() } ?: 1.0
            }
            check(value, digits)
        }
    }

    private fun Double.pow(n: Int) = Math.pow(this, n.toDouble())
}
