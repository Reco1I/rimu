package com.reco1l.verktex.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val floatPi = PI.toFloat()

private const val elasticConst = 2f * floatPi / 0.3f
private const val elasticConst2 = 0.3f / 4f

// Exponential and elastic curves never reach 0 on their own. These offsets are applied linearly
// over the curve so that it starts and ends at exactly 0 and 1.
private val expoOffset = 2f.pow(-10f)
private val elasticOffsetFull = 2f.pow(-11f)
private val elasticOffsetHalf = 2f.pow(-10f) * sin((0.5f - elasticConst2) * elasticConst)
private val elasticOffsetQuarter = 2f.pow(-10f) * sin((0.25f - elasticConst2) * elasticConst)
private val inOutElasticOffset = 2f.pow(-10f) * sin((1f - elasticConst2 * 1.5f) * elasticConst / 1.5f)

/**
 * A function that interpolates a value between 0 and 1.
 */
fun interface Easing {
    /**
     * Called to interpolate a value between 0 and 1.
     *
     * @param x The value to interpolate.
     * @return The interpolated value.
     */
    fun interpolate(x: Float): Float


    companion object {

        @JvmField

        val None = Easing { x -> x }

        @JvmField

        val InQuad = Easing { x ->
            var x = x
            x * x
        }

        @JvmField

        val OutQuad = Easing { x ->
            var x = x
            x * (2f - x)
        }

        @JvmField

        val InOutQuad = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * 2f else --x * x * -2f + 1f
        }

        @JvmField

        val InCubic = Easing { x ->
            var x = x
            x * x * x
        }

        @JvmField

        val OutCubic = Easing { x ->
            var x = x
            --x * x * x + 1f
        }

        @JvmField

        val InOutCubic = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * x * 4f else --x * x * x * 4f + 1f
        }

        @JvmField

        val InQuart = Easing { x ->
            var x = x
            x * x * x * x
        }

        @JvmField

        val OutQuart = Easing { x ->
            var x = x
            1f - --x * x * x * x
        }

        @JvmField

        val InOutQuart = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * x * x * 8 else --x * x * x * x * -8f + 1f
        }

        @JvmField

        val InQuint = Easing { x ->
            var x = x
            x * x * x * x * x
        }

        @JvmField

        val OutQuint = Easing { x ->
            var x = x
            --x * x * x * x * x + 1
        }

        @JvmField

        val InOutQuint = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * x * x * x * 16f else --x * x * x * x * x * 16f + 1f
        }

        @JvmField

        val InSine = Easing { x ->
            var x = x
            1f - cos(x * PI.toFloat() * 0.5f)
        }

        @JvmField

        val OutSine = Easing { x ->
            var x = x
            sin(x * PI.toFloat() * 0.5f)
        }

        @JvmField

        val InOutSine = Easing { x ->
            var x = x
            0.5f - 0.5f * cos(PI.toFloat() * x)
        }

        @JvmField

        val InExpo = Easing { x ->
            var x = x
            2f.pow(10f * (x - 1f)) + expoOffset * (x - 1f)
        }

        @JvmField

        val OutExpo = Easing { x ->
            var x = x
            -(2f.pow(-10f * x)) + 1f + expoOffset * x
        }

        @JvmField

        val InOutExpo = Easing { x ->
            var x = x
            if (x < 0.5f) {
                0.5f * (2f.pow(20f * x - 10f) + expoOffset * (2f * x - 1f))
            } else {
                1f - 0.5f * (2f.pow(-20f * x + 10f) + expoOffset * (-2f * x + 1f))
            }
        }

        @JvmField

        val InCirc = Easing { x ->
            var x = x
            1f - sqrt(1f - x * x)
        }

        @JvmField

        val OutCirc = Easing { x ->
            var x = x
            sqrt(1f - --x * x)
        }

        @JvmField

        val InOutCirc = Easing { x ->
            var x = x
            if (let { x *= 2f; x } < 1f) {
                0.5f - 0.5f * sqrt(1f - x * x)
            } else {
                0.5f * sqrt(1f - let { x -= 2f; x } * x) + 0.5f
            }
        }

        @JvmField

        val InElastic = Easing { x ->
            var x = x
            -(2f.pow(-10f + 10f * x)) * sin((1f - elasticConst2 - x) * elasticConst) + elasticOffsetFull * (1f - x)
        }

        @JvmField

        val OutElastic = Easing { x ->
            var x = x
            2f.pow(-10f * x) * sin((x - elasticConst2) * elasticConst) + 1f - elasticOffsetFull * x
        }

        @JvmField

        val OutElasticHalf = Easing { x ->
            var x = x
            2f.pow(-10f * x) * sin((0.5f * x - elasticConst2) * elasticConst) + 1f - elasticOffsetHalf * x
        }

        @JvmField

        val OutElasticQuarter = Easing { x ->
            var x = x
            2f.pow(-10f * x) * sin((0.25f * x - elasticConst2) * elasticConst) + 1f - elasticOffsetQuarter * x
        }

        @JvmField

        val InOutElastic = Easing { x ->
            var x = x
            if (let { x *= 2f; x } < 1f) {
                -0.5f * (2f.pow(-10f + 10f * x) * sin((1f - elasticConst2 * 1.5f - x) * elasticConst / 1.5f)
                    - inOutElasticOffset * (1f - x))
            } else {
                0.5f * (2f.pow(-10f * --x) * sin((x - elasticConst2 * 1.5f) * elasticConst / 1.5f)
                    + 2f - inOutElasticOffset * x)
            }
        }

        @JvmField

        val InBack = Easing { x ->
            var x = x
            x * x * ((1.70158f + 1) * x - 1.70158f)
        }

        @JvmField

        val OutBack = Easing { x ->
            var x = x
            --x * x * ((1.70158f + 1f) * x + 1.70158f) + 1f
        }

        @JvmField

        val InOutBack = Easing { x ->
            var x = x
            if (let { x *= 2f; x } < 1f) {
                0.5f * x * x * ((1.70158f * 1.525f + 1f) * x - 1.70158f * 1.525f)
            } else {
                0.5f * (let { x -= 2f; x } * x * ((1.70158f * 1.525f + 1f) * x + 1.70158f * 1.525f) + 2f)
            }
        }

        @JvmField

        val InBounce = Easing { x ->
            var x = x
            x = 1 - x
            when {
                x < 1f / 2.75f -> 1f - 7.5625f * x * x

                x < 2f * (1f / 2.75f) -> 1f - (7.5625f * let { x -= 1.5f * (1f / 2.75f); x } * x + 0.75f)

                x < 2.5f * (1f / 2.75f) -> 1f - (7.5625f * let { x -= 2.25f * (1f / 2.75f); x } * x + 0.9375f)

                else -> 1f - (7.5625f * let { x -= 2.625f * (1f / 2.75f); x } * x + 0.984375f)
            }
        }

        @JvmField

        val OutBounce = Easing { x ->
            var x = x
            when {
                x < 1f / 2.75f -> 7.5625f * x * x

                x < 2 * (1f / 2.75f) -> 7.5625f * let { x -= 1.5f * (1f / 2.75f); x } * x + 0.75f

                x < 2.5 * (1f / 2.75f) -> 7.5625f * let { x -= 2.25f * (1f / 2.75f); x } * x + 0.9375f

                else -> 7.5625f * let { x -= 2.625f * (1f / 2.75f); x } * x + 0.984375f
            }
        }

        @JvmField

        val InOutBounce = Easing { x ->
            var x = x
            if (x < 0.5f) {
                0.5f - 0.5f * OutBounce.interpolate(1f - x * 2f)
            } else {
                OutBounce.interpolate((x - 0.5f) * 2f) * 0.5f + 0.5f
            }
        }

    }
}


