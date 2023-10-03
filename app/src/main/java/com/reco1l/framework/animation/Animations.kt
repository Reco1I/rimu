package com.reco1l.framework.animation

import android.animation.TimeInterpolator
import android.animation.ValueAnimator.ofFloat
import android.animation.ValueAnimator.ofInt
import kotlin.reflect.KMutableProperty0


fun Pair<Int, Int>.animate(
    duration: Long,
    delay: Long = 0,
    interpolator: TimeInterpolator? = null,
    onUpdate: ((Float) -> Unit)? = null

) = ofInt(first, second).apply {

    startDelay = delay

    this.duration = duration
    this.interpolator = interpolator ?: this.interpolator

    if (onUpdate != null)
        addUpdateListener { onUpdate(it.animatedValue as Float) }

    start()
}!!

fun Pair<Float, Float>.animateTo(
    duration: Long,
    delay: Long = 0,
    interpolator: TimeInterpolator? = null,
    onUpdate: ((Float) -> Unit)? = null

) = ofFloat(first, second).apply {

    startDelay = delay

    this.duration = duration
    this.interpolator = interpolator ?: this.interpolator

    if (onUpdate != null)
        addUpdateListener { onUpdate(it.animatedValue as Float) }

    start()
}!!


fun KMutableProperty0<Float>.animateTo(
    target: Float,
    duration: Long,
    delay: Long = 0,
    interpolator: TimeInterpolator? = null

) = ofFloat(get(), target).apply {

    startDelay = delay

    this.duration = duration
    this.interpolator = interpolator ?: this.interpolator

    addUpdateListener { set(it.animatedValue as Float) }

    start()
}

fun KMutableProperty0<Int>.animateTo(
    target: Int,
    duration: Long,
    delay: Long = 0,
    interpolator: TimeInterpolator? = null

) = ofInt(get(), target).apply {

    startDelay = delay

    this.duration = duration
    this.interpolator = interpolator ?: this.interpolator

    addUpdateListener { set(it.animatedValue as Int) }

    start()
}