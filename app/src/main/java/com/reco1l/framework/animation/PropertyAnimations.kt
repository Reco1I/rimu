package com.reco1l.framework.animation

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import kotlin.reflect.KFunction1
import kotlin.reflect.KMutableProperty0


// Floats

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
    start()
}

fun KMutableProperty0<Float>.animateTo(
    to: Float,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator? = null

): ValueAnimator = animateFloat(this::set, get(), to, end, delay, ease)


// Integers

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
    start()
}

fun KMutableProperty0<Int>.animateTo(
    target: Int,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator? = null

): ValueAnimator = animateInt(this::set, get(), target, end, delay, ease)


// Functions

fun KFunction1<Int, Any>.animate(
    from: Int = 0,
    to: Int,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator? = null

) = animateInt(this::invoke, from, to, end, delay, ease)

fun KFunction1<Float, Any>.animate(
    from: Float = 0f,
    to: Float,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator? = null

) = animateFloat(this::invoke, from, to, end, delay, ease)
