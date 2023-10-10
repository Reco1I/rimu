package game.rimu.ui.layouts

import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.CallSuper
import com.reco1l.framework.android.views.removeSelf
import game.rimu.android.RimuContext
import game.rimu.ui.LayoutLayer
import game.rimu.ui.dimensions
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.views.ConstraintLayout
import kotlin.reflect.KClass

abstract class AttachableLayout(final override val ctx: RimuContext) : ConstraintLayout(ctx)
{

    /**
     * Determine the layer where this layout should be added.
     */
    abstract var layer: KClass<out LayoutLayer>

    /**
     * The parents scenes where the layouts should be show/hide automatically.
     */
    open var parents: Array<KClass<out RimuScene>>? = null


    /**
     * Determine the time in ms that the layout will take to automatically hide.
     */
    open var hideTime: Long? = null
        set(value)
        {
            field = value
            invalidateHideTimer()
        }


    private val hideTask = { hide() }


    init
    {
        dimensions {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        // Invalidating the timer
        invalidateHideTimer()
    }


    @CallSuper
    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        invalidateSkin()
        invalidateScale()
    }

    @CallSuper
    override fun onTouchEvent(event: MotionEvent?): Boolean
    {
        // Invalidating the timer at every event.
        invalidateHideTimer()

        return super.onTouchEvent(event)
    }


    // Actions

    /**
     * Invalidate the timer to automatically hide the layout, specified by [hideTime].
     */
    fun invalidateHideTimer()
    {
        removeCallbacks(hideTask)

        hideTime?.also { postDelayed(hideTask, it) }
    }


    // Attachment

    /**
     * Alternate between [show] and [hide].
     */
    fun alternate()
    {
        if (isAttachedToWindow)
            hide()
        else
            show()
    }

    /**
     * Show the layout.
     */
    @CallSuper
    open fun show() = ctx.layouts.show(this)

    /**
     * Remove the layout.
     */
    @CallSuper
    open fun hide() = removeSelf()

}
