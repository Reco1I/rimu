package game.rimu.ui

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.forEach
import com.reco1l.framework.android.views.radius
import com.reco1l.framework.android.views.setMargins
import com.reco1l.framework.android.views.setPaddings
import com.reco1l.framework.android.views.setSize
import com.reco1l.framework.lang.isLazyInit
import com.reco1l.framework.lang.isLazyInitialized
import game.rimu.android.IWithContext
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

    fun IWithContext.invalidateScale() = onApplyScale(ctx.engine.surface.scale)

}


abstract class ScalableDimensions<T : Any>(
    var width: Int = UNKNOWN,
    var height: Int = UNKNOWN
)
{

    abstract fun onApplyScale(target: T, scale: Float)

    companion object
    {
        const val UNKNOWN = -1
    }
}


interface IScalableWithDimensions<T : Any, D : ScalableDimensions<T>> : IScalable
{

    val dimensions: D

    @Suppress("UNCHECKED_CAST")
    override fun onApplyScale(scale: Float)
    {
        // Preventing unnecessary initialization.
        if (!::dimensions.isLazyInit || ::dimensions.isLazyInitialized)
            dimensions.onApplyScale(this as T, scale)

        super.onApplyScale(scale)
    }
}

inline fun <T : Any, D : ScalableDimensions<T>> IScalableWithDimensions<T, D>.dimensions(
    block: D.() -> Unit
) = dimensions.block()


// Views

open class ViewDimensions<V : View> : ScalableDimensions<V>(WRAP_CONTENT, WRAP_CONTENT)
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


    override fun onApplyScale(target: V, scale: Float)
    {
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

        target.radius = cornerRadius * scale
    }
}


// Drawables

open class DrawableDimensions<T : Drawable> : ScalableDimensions<T>(UNKNOWN, UNKNOWN)
{

    override fun onApplyScale(target: T, scale: Float)
    {
        val bounds = target.bounds

        val width = if (width >= 0) width else target.intrinsicWidth
        val height = if (height >= 0) height else target.intrinsicHeight

        target.setBounds(
            bounds.left,
            bounds.top,
            bounds.left + width,
            bounds.top + height
        )
    }
}
