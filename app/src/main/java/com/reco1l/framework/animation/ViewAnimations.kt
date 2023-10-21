package com.reco1l.framework.animation

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.TimeInterpolator
import android.view.View
import android.view.ViewPropertyAnimator


class AnimatorListenerImpl(init: (AnimatorListenerImpl.() -> Unit)? = null) : AnimatorListener
{
    var onStart: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    var onRepeat: (() -> Unit)? = null
    var onEnd: (() -> Unit)? = null

    init
    {
        init?.invoke(this)
    }

    override fun onAnimationStart(animation: Animator) = onStart?.invoke() ?: Unit
    override fun onAnimationCancel(animation: Animator) = onCancel?.invoke() ?: Unit
    override fun onAnimationRepeat(animation: Animator) = onRepeat?.invoke() ?: Unit
    override fun onAnimationEnd(animation: Animator) = onEnd?.invoke() ?: Unit
}


private fun <T> View.animateTo(
    setProperty: ViewPropertyAnimator.(T) -> ViewPropertyAnimator,
    value: T,
    end: Long,
    delay: Long,
    ease: TimeInterpolator?,
    listener: (AnimatorListenerImpl.() -> Unit)?
): View
{
    animate().apply {

        setProperty(value)
        setListener(listener?.let { AnimatorListenerImpl(it) })

        duration = end
        startDelay = delay
        interpolator = ease

        start()
    }
    return this
}


fun View.cancelAnimators(): View
{
    animate().cancel()
    return this
}


fun View.toAlpha(
    value: Float,
    end: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    listener: (AnimatorListenerImpl.() -> Unit)? = null
) = animateTo(
    ViewPropertyAnimator::alpha,
    value, end, delay, ease, listener
)

fun View.toScale(
    value: Float,
    end: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    listener: (AnimatorListenerImpl.() -> Unit)? = null
) = animateTo(
    { scaleX(value); scaleY(value) },
    value, end, delay, ease, listener
)

fun View.toTranslationX(
    value: Float,
    end: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    listener: (AnimatorListenerImpl.() -> Unit)? = null
) = animateTo(
    ViewPropertyAnimator::translationX,
    value, end, delay, ease, listener
)

fun View.toTranslationY(
    value: Float,
    end: Long = 0L,
    delay: Long = 0L,
    ease: TimeInterpolator? = null,
    listener: (AnimatorListenerImpl.() -> Unit)? = null
) = animateTo(
    ViewPropertyAnimator::translationY,
    value, end, delay, ease, listener
)