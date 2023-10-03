package game.rimu.ui.views.addons

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.forEach
import com.reco1l.framework.android.views.radius
import com.reco1l.framework.android.views.setMargins
import com.reco1l.framework.android.views.setPaddings
import com.reco1l.framework.android.views.setSize
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logI
import game.rimu.android.IWithContext
import game.rimu.constants.RimuSetting
import kotlin.math.max


open class ViewDimensions
{

    var width: Int = LayoutParams.WRAP_CONTENT

    var height: Int = LayoutParams.WRAP_CONTENT

    var paddingLeft: Int = 0

    var paddingRight: Int = 0

    var paddingTop: Int = 0

    var paddingBottom: Int = 0

    var marginLeft: Int = 0

    var marginRight: Int = 0

    var marginTop: Int = 0

    var marginBottom: Int = 0

    var radius: Float = 0f


    var size
        get() = max(width, height)
        set(value)
        {
            width = value
            height = value
        }


    open fun onApplyScale(view: View, scale: Float)
    {
        view.setSize(
            width = if (width >= 0) (width * scale).toInt() else width,
            height = if (height >= 0) (height * scale).toInt() else height
        )

        view.setPaddings(
            left = paddingLeft * scale,
            top = paddingTop * scale,
            right = paddingRight * scale,
            bottom = paddingBottom * scale
        )

        view.setMargins(
            left = marginLeft * scale,
            top = marginTop * scale,
            right = marginRight * scale,
            bottom = marginBottom * scale
        )

        view.radius = radius * scale
    }
}


/**
 * Indicates that a View is scalable.
 * If it's added into a [ViewGroup] that implements this interface, [onApplyScale] will be
 * automatically called by the parent when the scale factor is changed.
 */
interface IScalable
{

    /**
     * Called when [RimuSetting.UI_SCALE] option was changed.
     */
    fun onApplyScale(scale: Float)
    {
        if (this is ViewGroup)
        {
            "onApplyScale::${className} - ViewGroup child count: $childCount".logI(className)

            forEach {

                // Applying to nested views
                (it as? IScalable)?.onApplyScale(scale)
            }
        }
    }

    /**
     * Apply initially the scale based on the UI scale and surface ratio.
     */
    fun IWithContext.onApplyScale() = onApplyScale(ctx.engine.surface.scale)


    /**
     * Apply engine scale ratio and [RimuSetting.UI_SCALE] factor.
     */
    fun Float.toScale() = when (this@IScalable)
    {
        is IWithContext -> this * ctx.engine.surface.scale

        else ->
            throw NotImplementedError("This method is only usable if there's a RimuContext.")
    }

    /**
     * Apply engine scale ratio and [RimuSetting.UI_SCALE] factor.
     */
    fun Int.toScale() = when (this@IScalable)
    {
        is IWithContext -> (this * ctx.engine.surface.scale).toInt()

        else ->
            throw NotImplementedError("This method is only usable if there's a RimuContext.")
    }

}


interface IScalableWithDimensions<T : ViewDimensions> : IScalable
{

    val dimensions: T

    override fun onApplyScale(scale: Float)
    {
        if (this is View)
            dimensions.onApplyScale(this, scale)

        super.onApplyScale(scale)
    }

}

inline fun <T : ViewDimensions> IScalableWithDimensions<T>.dimensions(block: T.() -> Unit) = dimensions.block()

