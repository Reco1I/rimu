package com.reco1l.verktex.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

        val None = Easing { x -> x }

        val InQuad = Easing { x ->
            var x = x
            x * x
        }

        val OutQuad = Easing { x ->
            var x = x
            x * (2f - x)
        }

        val InOutQuad = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * 2f else --x * x * -2f + 1f
        }

        val InCubic = Easing { x ->
            var x = x
            x * x * x
        }

        val OutCubic = Easing { x ->
            var x = x
            --x * x * x + 1f
        }

        val InOutCubic = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * x * 4f else --x * x * x * 4f + 1f
        }

        val InQuart = Easing { x ->
            var x = x
            x * x * x * x
        }

        val OutQuart = Easing { x ->
            var x = x
            1f - --x * x * x * x
        }

        val InOutQuart = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * x * x * 8 else --x * x * x * x * -8f + 1f
        }

        val InQuint = Easing { x ->
            var x = x
            x * x * x * x * x
        }

        val OutQuint = Easing { x ->
            var x = x
            --x * x * x * x * x + 1
        }

        val InOutQuint = Easing { x ->
            var x = x
            if (x < 0.5f) x * x * x * x * x * 16f else --x * x * x * x * x * 16f + 1f
        }

        val InSine = Easing { x ->
            var x = x
            1f - cos(x * PI.toFloat() * 0.5f)
        }

        val OutSine = Easing { x ->
            var x = x
            sin(x * PI.toFloat() * 0.5f)
        }

        val InOutSine = Easing { x ->
            var x = x
            0.5f - 0.5f * cos(PI.toFloat() * x)
        }

        val InExpo = Easing { x ->
            var x = x
            2f.pow(10f * (x - 1f))
        }

        val OutExpo = Easing { x ->
            var x = x
            -(2f.pow(-10f * x)) + 1f
        }

        val InOutExpo = Easing { x ->
            var x = x
            if (x < 0.5f) 0.5f * 2f.pow(20f * x - 10f) else 1f - 0.5f * 2f.pow(-20f * x + 10f)
        }

        val InCirc = Easing { x ->
            var x = x
            1f - sqrt(1f - x * x)
        }

        val OutCirc = Easing { x ->
            var x = x
            sqrt(1f - --x * x)
        }

        val InOutCirc = Easing { x ->
            var x = x
            if (let { x *= 2f; x } < 1f) {
                0.5f - 0.5f * sqrt(1f - x * x)
            } else {
                0.5f * sqrt(1f - let { x -= 2f; x } * x) + 0.5f
            }
        }

        val InElastic = Easing { x ->
            var x = x
            -(2f.pow(-10f + 10f * x)) * sin((1f - 0.3f / 4f - x) * (2f * PI.toFloat() / 0.3f))
        }

        val OutElastic = Easing { x ->
            var x = x
            2f.pow(-10f * x) * sin((x - 0.3f / 4f) * (2f * PI.toFloat() / 0.3f)) + 1f
        }

        val OutElasticHalf = Easing { x ->
            var x = x
            2f.pow(-10 * x) * sin((0.5f * x - 0.3f / 4f) * (2f * PI.toFloat() / 0.3f)) + 1f
        }

        val OutElasticQuarter = Easing { x ->
            var x = x
            2f.pow(-10f * x) * sin((0.25f * x - 0.3f / 4f) * (2f * PI.toFloat() / 0.3f)) + 1f
        }

        val InOutElastic = Easing { x ->
            var x = x
            if (let { x *= 2f; x } < 1f) {
                -0.5f * 2f.pow(-10f + 10f * x) * sin((1f - 0.3f / 4f * 1.5f - x) * (2f * PI.toFloat() / 0.3f) / 1.5f)
            } else {
                0.5f * 2f.pow(-10f * --x) * sin((x - 0.3f / 4f * 1.5f) * (2f * PI.toFloat() / 0.3f) / 1.5f) + 1f
            }
        }

        val InBack = Easing { x ->
            var x = x
            x * x * ((1.70158f + 1) * x - 1.70158f)
        }

        val OutBack = Easing { x ->
            var x = x
            --x * x * ((1.70158f + 1f) * x + 1.70158f) + 1f
        }

        val InOutBack = Easing { x ->
            var x = x
            if (let { x *= 2f; x } < 1f) {
                0.5f * x * x * ((1.70158f * 1.525f + 1f) * x - 1.70158f * 1.525f)
            } else {
                0.5f * (let { x -= 2f; x } * x * ((1.70158f * 1.525f + 1f) * x + 1.70158f * 1.525f) + 2f)
            }
        }

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

        val OutBounce = Easing { x ->
            var x = x
            when {
                x < 1f / 2.75f -> 7.5625f * x * x

                x < 2 * (1f / 2.75f) -> 7.5625f * let { x -= 1.5f * (1f / 2.75f); x } * x + 0.75f

                x < 2.5 * (1f / 2.75f) -> 7.5625f * let { x -= 2.25f * (1f / 2.75f); x } * x + 0.9375f

                else -> 7.5625f * let { x -= 2.625f * (1f / 2.75f); x } * x + 0.984375f
            }
        }

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


