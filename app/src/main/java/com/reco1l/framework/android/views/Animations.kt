package com.reco1l.framework.android.views

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.view.View
import android.view.ViewPropertyAnimator


inline fun View.animate(
    cancelCurrent: Boolean = true,
    withStart: Boolean = true,
    block: ViewPropertyAnimator.() -> Unit

) = animate().apply {

    if (cancelCurrent)
    {
        cancel()
        setListener(null)
    }

    block()

    if (withStart)
        start()
}!!

fun ViewPropertyAnimator.scale(value: Float) = apply {

    scaleX(value)
    scaleY(value)
}


fun ViewPropertyAnimator.setListeners(

    onStart: ((Animator) -> Unit)? = null,
    onEnd: ((Animator) -> Unit)? = null,
    onCancel: ((Animator) -> Unit)? = null,
    onRepeat: ((Animator) -> Unit)? = null

) = setListener(object : AnimatorListener
{
    override fun onAnimationCancel(a: Animator) = onCancel?.invoke(a) ?: Unit
    override fun onAnimationRepeat(a: Animator) = onRepeat?.invoke(a) ?: Unit
    override fun onAnimationStart(a: Animator) = onStart?.invoke(a) ?: Unit
    override fun onAnimationEnd(a: Animator) = onEnd?.invoke(a) ?: Unit
})
