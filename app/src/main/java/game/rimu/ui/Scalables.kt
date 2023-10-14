package game.rimu.ui

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.CallSuper
import androidx.core.view.forEach
import com.reco1l.framework.android.views.cornerRadius
import com.reco1l.framework.android.views.setMargins
import com.reco1l.framework.android.views.setPaddings
import com.reco1l.framework.android.views.setSize
import com.reco1l.framework.lang.isLazyInit
import com.reco1l.framework.lang.isLazyInitialized
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
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
     * Calls [onApplyScale] with the context scale, if there's a [RimuContext] implementation you
     * don't need to pass the context.
     */
    fun invalidateScale(ctx: RimuContext? = (this as? IWithContext)?.ctx)
    {
        ctx?.also { onApplyScale(it.engine.surface.scale) }
    }

}


abstract class ScalableDimensions<T : Any>(

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


// Views

open class ViewDimensions<V : View>(

    initialWidth: Int = WRAP_CONTENT,

    initialHeight: Int = WRAP_CONTENT,

) : ScalableDimensions<V>(initialWidth, initialHeight)
{

    var paddingLeft: Int = 0

    var paddingRight: Int = 0

    var paddingTop: Int = 0

    var paddingBottom: Int = 0

    var marginLeft: Int = 0

    var marginRight: Int = 0

    var marginTop: Int = 0

    var marginBottom: Int = 0

    var cornerRadius: Float = 0f


    fun padding(value: Int)
    {
        paddingLeft = value
        paddingRight = value
        paddingTop = value
        paddingBottom = value
    }

    fun size(value: Int)
    {
        width = value
        height = value
    }


    override fun onApplyScale(target: V, scale: Float)
    {
        super.onApplyScale(target, scale)

        target.setSize(
            width = if (width >= 0) (width * scale).toInt() else width,
            height = if (height >= 0) (height * scale).toInt() else height
        )

        target.setPaddings(
            left = paddingLeft * scale,
            top = paddingTop * scale,
            right = paddingRight * scale,
            bottom = paddingBottom * scale
        )

        target.setMargins(
            left = marginLeft * scale,
            top = marginTop * scale,
            right = marginRight * scale,
            bottom = marginBottom * scale
        )

        target.cornerRadius = cornerRadius * scale
    }
}