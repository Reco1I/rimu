package com.reco1l.rimu.ui

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.CallSuper
import androidx.core.view.forEach
import com.reco1l.toolkt.kotlin.isLazyInitialized
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting


/**
 * Indicates that a View is scalable.
 * If it's added into a [ViewGroup] that implements this interface, [invalidateScale] will be
 * automatically called by the parent when the scale factor is changed.
 */
interface IScalable
{

    /**
     * Called when the [UI scale factor][RimuSetting.UI_SCALE] is changed.
     */
    fun onApplyScale(scale: Float)
    {
        if (this is View)
        {
            (background as? IScalable)?.onApplyScale(scale)
            (foreground as? IScalable)?.onApplyScale(scale)

            if (this is ViewGroup)
                forEach { (it as? IScalable)?.onApplyScale(scale) }
        }

        // We don't have an implementation for Entity because they're already scaled by the
        // engine, also they're not used for UI.
    }

    /**
     * Calls [onApplyScale] with the context scale, if there's a [MainContext] implementation you
     * don't need to pass the context.
     */
    fun invalidateScale(ctx: MainContext = (this as IWithContext).ctx) = onApplyScale(ctx.layouts.scale)
}


/**
 * Indicates that the view is scalable and has its own scalable dimensions.
 * @see ScalableDimensions
 */
interface IScalableWithDimensions<T : Any, D : ScalableDimensions<T>> : IScalable
{

    /**
     * The view scalable dimensions, every dimension defined here will be scaled and applied once
     * [onApplyScale] is called.
     */
    val dimensions: D


    @Suppress("UNCHECKED_CAST")
    override fun onApplyScale(scale: Float)
    {
        // Preventing unnecessary initialization.
        if (::dimensions.isLazyInitialized)
            dimensions.onApplyScale(this as T, scale)

        super.onApplyScale(scale)
    }

    /**
     * Change scalable dimensions parameters, this will be applied when the scale factor or the screen
     * size are changed.
     * You can also apply the changes with [invalidateScale].
     */
    fun setDimensions(block: D.() -> Unit) = dimensions.apply(block)
}


/**
 * The view scalable dimensions that will be applied when the scale factor or the screen size is
 * changed.
 */
open class ScalableDimensions<T : Any>(

    var width: Int = LayoutParams.WRAP_CONTENT,

    var height: Int = LayoutParams.WRAP_CONTENT
)
{

    /**
     * The current scale applied to this dimensions.
     */
    var currentScale = 1f
        private set


    @CallSuper
    open fun onApplyScale(target: T, scale: Float)
    {
        currentScale = scale
    }
}