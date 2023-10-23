package com.reco1l.framework.animation

import android.animation.TimeInterpolator
import org.andengine.util.modifier.ease.IEaseFunction
import kotlin.math.pow

object Ease
{

    val LINEAR = TimeInterpolator { it }


    // Exponential

    val EXPO_OUT = TimeInterpolator { if (it == 1f) it else -(2f.pow(-10f * it)) + 1f }

    val EXPO_IN = TimeInterpolator { if (it == 0f) it else 2f.pow(10f * (it - 1)) - 0.001f }


    // Bounce

    val BOUNCE_OUT = TimeInterpolator {
        // org/andengine/util/modifier/ease/EaseBounceOut.java:54
        when
        {
            it < 1f / 2.75f -> 7.5625f * it.pow(2)

            it < 2f / 2.75f -> 7.5625f * (it - 1.5f / 2.75f).pow(2) + 0.75f

            it < 2.5f / 2.75f -> 7.5625f * (it - 2.25f / 2.75f).pow(2) + 0.9375f

            else -> 7.5625f * (it - 2.625f / 2.75f).pow(2) + 0.984375f
        }
    }

    val BOUNCE_IN = TimeInterpolator { 1f - BOUNCE_OUT.getInterpolation(1f - it) }


    // Acceleration

    val DECELERATE = TimeInterpolator { 1.0f - (1.0f - it) * (1.0f - it) }

    val ACCELERATE = TimeInterpolator { it.pow(2) }

}


fun TimeInterpolator.toEaseFunction() = IEaseFunction { sElapsed, sDuration ->
    getInterpolation(sElapsed / sDuration)
}