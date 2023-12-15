package com.reco1l.rimu.ui.views.addons

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Vibrator
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration.getLongPressTimeout
import com.reco1l.basskt.stream.SampleStream
import com.reco1l.toolkt.animation.Ease
import com.reco1l.toolkt.animation.animate
import com.reco1l.toolkt.animation.toScale
import com.reco1l.toolkt.graphics.setRadius
import com.reco1l.toolkt.graphics.toInt
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.IScalable
import com.reco1l.rimu.ui.ISkinnable


class TouchHandler(init: TouchHandler.() -> Unit) : OnTouchListener
{

    var onActionDown: (() -> Unit)? = null

    var onActionUp: (() -> Unit)? = null

    var onActionLong: (() -> Unit)? = null


    var soundActionDown: String? = null

    var soundActionUp: String? = null

    var soundActionLong: String? = null


    var touchEffectDrawable: (() -> Drawable)? = { TouchEffectDrawable() }

    var touchEffectAnimation: (View.(isPressed: Boolean) -> Unit)? = {

        if (it)
            toScale(0.85f, 100, ease = Ease.DECELERATE)
        else
            toScale(1f, 200, ease = Ease.BOUNCE_OUT)
    }


    private lateinit var ctx: MainContext


    private val longPresCallback = {

        ignoreActionUp = true

        onActionLong?.invoke()
        soundActionLong?.also { ctx.resources.get<SampleStream>(it, 0)?.play() }

        // Newer method requires higher min API.
        ctx.getSystemService(Vibrator::class.java).vibrate(50)
    }

    private var ignoreActionUp = false


    init
    {
        init()
    }


    fun noEffect()
    {
        touchEffectDrawable = null
        touchEffectAnimation = null
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean
    {
        ctx = view.context as MainContext

        // Setting the touch effect if hasn't been set yet.
        touchEffectDrawable?.also {

            if (view.foreground !is TouchEffectDrawable)
            {
                view.foreground = it()
                touchEffectDrawable = null
            }
        }

        // If it's an scalable view then we invalidate it to apply scale to the recently added drawable
        // as well for skin.
        if (event.action == ACTION_DOWN || event.action == ACTION_UP) (view.foreground as? TouchEffectDrawable)?.also {

            it.invalidateScale(ctx)
            it.invalidateSkin(ctx)
        }


        fun onHandleTouchEffect(isPressed: Boolean)
        {
            view.isPressed = isPressed
            (view.foreground as? TouchEffectDrawable)?.animate(isPressed)
            touchEffectAnimation?.also { view.it(isPressed) }
        }

        when (event.action)
        {
            ACTION_DOWN -> onHandleTouchEffect(true)
            ACTION_UP, ACTION_OUTSIDE, ACTION_CANCEL -> onHandleTouchEffect(false)
        }

        return when (event.action)
        {
            ACTION_DOWN ->
            {
                // Queuing the long press callback, it'll be executed according to the timeout set by
                // the user in its device settings.
                if (onActionLong != null)
                    view.handler.postDelayed(longPresCallback, getLongPressTimeout().toLong())

                onActionDown?.invoke()
                soundActionDown?.also { ctx.resources.get<SampleStream>(it, 0)?.play() }
                true
            }

            ACTION_MOVE, ACTION_CANCEL ->
            {
                view.handler.removeCallbacks(longPresCallback)
                true
            }

            ACTION_UP ->
            {
                // This means the long press callback was executed so we should ignore.
                if (ignoreActionUp)
                {
                    ignoreActionUp = false
                    return true
                }

                // Removing any queued callback if the action UP was produced before the long
                // press callback was executed.
                view.handler.removeCallbacks(longPresCallback)

                onActionUp?.invoke()
                soundActionUp?.also { ctx.resources.get<SampleStream>(it, 0)?.play() }
                true
            }

            else -> false
        }
    }
}

fun View.setTouchHandler(block: TouchHandler.() -> Unit) = setOnTouchListener(TouchHandler(block))


class TouchEffectDrawable :
    GradientDrawable(),
    ISkinnable,
    IScalable
{

    // Initial values for animator is irrelevant because it's updated before starting the animation.
    private val animator = this::setAlpha.animate(end = 100)

    init
    {
        alpha = 0
    }


    fun animate(isPressed: Boolean)
    {
        val newAlpha = if (isPressed) 20 else 0

        animator.cancel()
        animator.setIntValues(alpha, newAlpha)
        animator.start()
    }

    override fun onApplyScale(scale: Float) = setRadius(8f * scale)

    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preserving previous alpha when changing color.
        setColor(skin.data.colours.accentColor.toInt())
    }
}