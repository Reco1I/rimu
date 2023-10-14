package game.rimu.ui.views.addons

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Vibrator
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration.getLongPressTimeout
import android.view.ViewPropertyAnimator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import com.reco1l.basskt.stream.SampleStream
import com.reco1l.framework.android.getSystemService
import com.reco1l.framework.android.views.animate
import com.reco1l.framework.android.views.scale
import com.reco1l.framework.graphics.setRadius
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalable
import game.rimu.ui.ISkinnable


class TouchHandler(init: TouchHandler.() -> Unit) : OnTouchListener
{

    var onActionDown: (() -> Unit)? = null

    var onActionUp: (() -> Unit)? = null

    var onActionLong: (() -> Unit)? = null


    var soundActionDown: (() -> SampleStream?)? = null

    var soundActionUp: (() -> SampleStream?)? = null

    var soundActionLong: (() -> SampleStream?)? = null


    var touchEffectDrawable: (() -> Drawable)? = { TouchEffectDrawable() }

    var touchEffectAnimation: (ViewPropertyAnimator.(isPressed: Boolean) -> Unit)? = block@{

        if (it)
        {
            scale(0.85f)
            interpolator = DecelerateInterpolator()
            duration = 100
            return@block
        }
        scale(1f)
        interpolator = BounceInterpolator()
        duration = 200
    }


    private lateinit var view: View


    private val longPresCallback = {

        ignoreActionUp = true

        onActionLong?.invoke()
        soundActionLong?.invoke()?.play()

        // Newer method requires higher min API.
        view.context.getSystemService<Vibrator>().vibrate(50)
    }

    private var ignoreActionUp = false


    init
    {
        init()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean
    {
        this.view = view

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

            val context = view.context as? RimuContext

            it.invalidateSkin(context)
            it.invalidateScale(context)
        }

        return when (event.action)
        {
            ACTION_DOWN ->
            {
                view.isPressed = true
                touchEffectAnimation?.also { view.animate { it(true) } }

                // Queuing the long press callback, it'll be executed according to the timeout set by
                // the user in its device settings.
                if (onActionLong != null)
                    view.handler.postDelayed(longPresCallback, getLongPressTimeout().toLong())

                onActionDown?.invoke()
                soundActionDown?.invoke()?.play()

                true
            }

            ACTION_UP ->
            {
                view.isPressed = false
                touchEffectAnimation?.also { view.animate { it(false) } }

                // This means the long press callback was executed so we should ignore action UP.
                if (ignoreActionUp)
                {
                    ignoreActionUp = false
                    return true
                }

                // Removing any queued callback if the action UP was produced before the long
                // press callback was executed.
                view.handler.removeCallbacks(longPresCallback)

                onActionUp?.invoke()
                soundActionUp?.invoke()?.play()
                true
            }

            ACTION_MOVE, ACTION_CANCEL ->
            {
                view.handler.removeCallbacks(longPresCallback)
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
    val animator = ValueAnimator.ofInt().apply {

        duration = 100
        addUpdateListener { alpha = it.animatedValue as Int }
    }

    init
    {
        alpha = 0
    }


    override fun isStateful() = true


    override fun onStateChange(stateSet: IntArray): Boolean
    {
        if (android.R.attr.state_pressed in stateSet)
            fadeIn()
        else
            fadeOut()

        return super.onStateChange(stateSet)
    }

    override fun onApplyScale(scale: Float) = setRadius(8f * scale)

    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preserving previous alpha when changing color.
        setColor(skin.data.colours.accentColor.toInt())
    }


    private fun fadeIn()
    {
        if (alpha == 20)
            return

        animator.cancel()
        animator.setIntValues(alpha, 20)
        animator.start()
    }

    private fun fadeOut()
    {
        if (alpha == 0)
            return

        animator.cancel()
        animator.setIntValues(alpha, 0)
        animator.start()
    }
}