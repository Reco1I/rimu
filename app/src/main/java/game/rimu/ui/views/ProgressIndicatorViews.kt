package game.rimu.ui.views

import android.app.ActionBar.LayoutParams
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.reco1l.framework.graphics.CompoundRectF
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules


open class ProgressIndicatorDimensions<T : LinearProgressIndicator> : ViewDimensions<T>(
    initialWidth = LayoutParams.MATCH_PARENT,
    initialHeight = 6
)
{

    var barRadius = 8f

}

open class ProgressIndicatorSkinningRules<T : LinearProgressIndicator> : ViewSkinningRules<T>(
)
{

    var activeColor = "accentColor"

    var activeColorFactor = 0.6f

    var inactiveColor = activeColor

    var inactiveColorFactor = 0.3f


    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        super.onApplySkin(target, skin)

        target.activeBarColor = skin.colors[activeColor]!!.toInt(factor = activeColorFactor)
        target.inactiveBarColor = skin.colors[inactiveColor]!!.toInt(factor = inactiveColorFactor)
    }
}



fun IWithContext.LinearProgressIndicator(
    parent: ViewGroup? = this as? ViewGroup,
    init: LinearProgressIndicator.() -> Unit
) = LinearProgressIndicator(ctx).apply { parent?.addView(this); init() }

class LinearProgressIndicator(ctx: MainContext) :
    View(ctx),
    IScalableWithDimensions<LinearProgressIndicator>,
    ISkinnableWithRules<LinearProgressIndicator>
{

    override val dimensions = ProgressIndicatorDimensions<LinearProgressIndicator>()

    override val rules = ProgressIndicatorSkinningRules<LinearProgressIndicator>()


    /**
     * The active progress bar color.
     */
    @ColorInt
    var activeBarColor = Color.WHITE
        set(value)
        {
            field = value
            invalidate()
        }

    /**
     * The inactive progress bar color.
     */
    @ColorInt
    var inactiveBarColor = Color.DKGRAY
        set(value)
        {
            field = value
            invalidate()
        }

    /**
     * The current progress.
     */
    var progress = 0f
        set(value)
        {
            field = value.coerceIn(min, max)
            invalidate()
        }

    /**
     * The minimum allowed value.
     */
    var min = 0f
        set(value)
        {
            if (value > max)
                throw IllegalArgumentException("Min value cannot be greater than max value.")

            field = value
            invalidate()
        }

    /**
     * The maximum allowed value.
     */
    var max = 1f
        set(value)
        {
            if (value < min)
                throw IllegalArgumentException("Max value cannot be lower than min value.")

            field = value
            invalidate()
        }

    /**
     * If `true` the bar will show the indeterminate animation rather than the actual progress.
     */
    var indeterminate = true
        set(value)
        {
            field = value
            invalidate()
        }


    private val barCompound = CompoundRectF()


    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Drawing inactive bar
        barCompound.set(0f, 0f, width, height)
        barCompound.paint.color = inactiveBarColor
        barCompound.drawTo(canvas, dimensions.barRadius)

        // Drawing active bar

        if (indeterminate)
            barCompound.set(barCompound.left + 0.01f, 0f, width * 0.50f, height)
        else
            barCompound.set(0f, 0f, width * (progress / max), height)

        barCompound.paint.color = activeBarColor
        barCompound.drawTo(canvas, dimensions.barRadius)

        if (indeterminate)
            invalidate()
    }
}