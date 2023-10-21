package com.reco1l.framework.animation

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.ofFloat
import android.animation.ValueAnimator.ofInt
import kotlin.reflect.KMutableProperty0


fun KMutableProperty0<Float>.animateTo(
    target: Float,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator? = null

): ValueAnimator = ofFloat(get(), target).apply {

    duration = end
    startDelay = delay
    interpolator = ease ?: Ease.LINEAR

    addUpdateListener { set(it.animatedValue as Float) }
    start()
}

fun KMutableProperty0<Int>.animateTo(
    target: Int,
    end: Long,
    delay: Long = 0,
    ease: TimeInterpolator? = null

): ValueAnimator = ofInt(get(), target).apply {

    duration = end
    startDelay = delay
    interpolator = ease ?: Ease.LINEAR

    addUpdateListener { set(it.animatedValue as Int) }
    start()
}