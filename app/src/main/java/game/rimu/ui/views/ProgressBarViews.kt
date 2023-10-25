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

    var barPadding = 0f

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

open class LinearProgressIndicator(ctx: MainContext) :
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
    open var indeterminate = true
        set(value)
        {
            field = value
            invalidate()
        }


    protected val barCompound = CompoundRectF()

    protected var indeterminateX = 0f


    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = dimensions.barPadding
        val cornerRadius = dimensions.barRadius

        // Drawing inactive bar
        barCompound.set(padding, padding, width - padding, height - padding)
        barCompound.paint.color = inactiveBarColor
        barCompound.drawTo(canvas, cornerRadius)

        // Drawing active bar

        barCompound.paint.color = activeBarColor

        if (indeterminate)
        {
            // Computing the bar width as the 4th part of the view width for indeterminate bar.
            val barWidth = (width - padding * 2) / 4

            indeterminateX += 2f

            if (indeterminateX > width - padding)
                indeterminateX = padding - barWidth

            barCompound.apply {

                top = padding
                bottom = height - padding

                // Coercing both left and right to the view bounds less the padding accordingly to
                // the indeterminate X.

                left = indeterminateX.coerceAtLeast(padding)
                right = (indeterminateX + barWidth).coerceAtMost(width - padding)
            }

            barCompound.drawTo(canvas, cornerRadius)

            // Since is indeterminate it should be invalidated afterwards because of the animation.
            invalidate()
        }

        // Computing barWidth with the view width as base and padding both left and right sides.
        val barWidth = (width - padding * 2) * (progress / max)

        barCompound.set(padding, padding, padding + barWidth, height - padding)
        barCompound.drawTo(canvas, cornerRadius)
    }
}