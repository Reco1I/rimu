package com.reco1l.framework.animation

import android.animation.TimeInterpolator
import com.reco1l.framework.FMath
import org.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


fun interface TimeEasing : TimeInterpolator, IEaseFunction
{

    operator fun invoke(input: Float) = getInterpolation(input)

    // AndEngine integration.
    override fun getPercentage(elapsed: Float, duration: Float) = getInterpolation(elapsed / duration)
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

    val BOUNCE_IN = TimeEasing { 1f - BOUNCE_OUT.getInterpolation(1f - it) }

    // Sine

    val SINE_IN = TimeEasing { -cos(it * FMath.PI_HALF) + 1f }

    val SINE_OUT = TimeEasing { sin(it * FMath.PI_HALF) }


    // Acceleration

    val DECELERATE = TimeEasing { 1.0f - (1.0f - it).pow(2) }

    val ACCELERATE = TimeEasing { it.pow(2) }

}