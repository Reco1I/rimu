package game.rimu.ui

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.forEach
import com.reco1l.framework.kotlin.isLazyInit
import com.reco1l.framework.kotlin.isLazyInitialized
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.constants.RimuSetting


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


open class ScalableDimensions<T : Any>(

    var width: Int = UNKNOWN,

    var height: Int = UNKNOWN
)
{

    var currentScale = 1f
        private set

    @CallSuper
    open fun onApplyScale(target: T, scale: Float)
    {
        currentScale = scale
    }

    companion object
    {
        const val UNKNOWN = -1
    }
}


interface IScalableWithDimensions<T : Any> : IScalable
{

    val dimensions: ScalableDimensions<T>

    @Suppress("UNCHECKED_CAST")
    override fun onApplyScale(scale: Float)
    {
        // Preventing unnecessary initialization.
        if (!::dimensions.isLazyInit || ::dimensions.isLazyInitialized)
            dimensions.onApplyScale(this as T, scale)

        super.onApplyScale(scale)
    }
}