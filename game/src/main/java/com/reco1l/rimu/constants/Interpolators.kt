package com.reco1l.rimu.constants

import android.animation.TimeInterpolator
import com.badlogic.gdx.math.Interpolation
import com.reco1l.toolkt.MathF
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private fun TimeEasing(function: (Float) -> Float) = object : TimeEasing()
{
    override fun getInterpolation(input: Float) = function(input)
}

abstract class TimeEasing : Interpolation(), TimeInterpolator
{
    operator fun invoke(input: Float) = getInterpolation(input)

    override fun apply(a: Float) = getInterpolation(a)
}


object Ease
{

    val LINEAR = TimeEasing { it }


    // Exponential

    val EXPO_OUT = TimeEasing { if (it == 1f) it else -(2f.pow(-10f * it)) + 1f }

    val EXPO_IN = TimeEasing { if (it == 0f) it else 2f.pow(10f * (it - 1)) - 0.001f }


    // Bounce

    val BOUNCE_OUT = TimeEasing {
        // org/andengine/util/modifier/ease/EaseBounceOut.java:54
        when
        {
            it < 1f / 2.75f -> 7.5625f * it.pow(2)

            it < 2f / 2.75f -> 7.5625f * (it - 1.5f / 2.75f).pow(2) + 0.75f

            it < 2.5f / 2.75f -> 7.5625f * (it - 2.25f / 2.75f).pow(2) + 0.9375f

            else -> 7.5625f * (it - 2.625f / 2.75f).pow(2) + 0.984375f
        }
    }

    // Acceleration

    val DECELERATE = TimeEasing { 1.0f - (1.0f - it).pow(2) }

}