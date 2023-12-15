package com.reco1l.framework.animation

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import kotlin.reflect.KFunction1
import kotlin.reflect.KMutableProperty0


// Base

private fun animateFloat(
    onApply: (Float) -> Unit,
    from: Float,
    to: Float,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator?

) = ValueAnimator.ofFloat(from, to).apply {

    duration = end
    startDelay = delay
    interpolator = ease ?: Ease.LINEAR

    addUpdateListener { onApply(it.animatedValue as Float) }

    if (from != to || end == 0L)
        start()
}

private fun animateInt(
    onApply: (Int) -> Unit,
    from: Int,
    to: Int,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator?

) = ValueAnimator.ofInt(from, to).apply {

    duration = end
    startDelay = delay
    interpolator = ease ?: Ease.LINEAR

    addUpdateListener { onApply(it.animatedValue as Int) }

    if (from != to || end == 0L)
        start()
}


// Float

/**
 * Animate the property from the current value to the [target][to] value.
 *
 * If no arguments are passed then it creates a [ValueAnimator] based on the property.
 */
fun KMutableProperty0<Float>.animateTo(
    to: Float = 0f,
    end: Long = 0,
    delay: Long = 0,
    ease: TimeInterpolator? = null

): ValueAnimator = animateFloat(this::set, get(), to, end, delay, ease)

/**
 * Animate calling the function from the [given][from] value to the [target][to] value.
 *
 * If no arguments are passed then it creates a [ValueAnimator] based on the function.
 */
fun KFunction1<Float, Any>.animate(
    from: Float = 0f,
    to: Float = 0f,
    end: Long = 0,
    delay: Long = 0,
    ease: TimeInterpolator? = null

) = animateFloat(this::invoke, from, to, end, delay, ease)


// Integers

/**
 * Animate the property from the current value to the [target][to] value.
 *
 * If no arguments are passed then it creates a [ValueAnimator] based on the property.
 */
fun KMutableProperty0<Int>.animateTo(
    to: Int = 0,
    end: Long = 0,
    delay: Long = 0,
    ease: TimeInterpolator? = null

): ValueAnimator = animateInt(this::set, get(), to, end, delay, ease)

/**
 * Animate calling the function from the [given][from] value to the [target][to] value.
 *
 * If no arguments are passed then it creates a [ValueAnimator] based on the function.
 */
fun KFunction1<Int, Any>.animate(
    from: Int = 0,
    to: Int = 0,
    end: Long = 0,
    delay: Long = 0,
    ease: TimeInterpolator? = null

) = animateInt(this::invoke, from, to, end, delay, ease)


// Extensions

fun ValueAnimator.doOnUpdate(listener: ValueAnimator.AnimatorUpdateListener): ValueAnimator
{
    addUpdateListener(listener)
    return this
}