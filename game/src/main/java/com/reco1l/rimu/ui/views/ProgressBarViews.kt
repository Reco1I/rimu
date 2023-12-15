package com.reco1l.rimu.ui.views

import android.app.ActionBar.LayoutParams
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction.CW
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.reco1l.framework.graphics.addRoundRect
import com.reco1l.framework.graphics.drawRoundRect
import com.reco1l.framework.graphics.toInt
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.IScalableWithDimensions
import com.reco1l.rimu.ui.ISkinnableWithRules


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

open class LinearProgressIndicator(override val ctx: MainContext) :
    View(ctx),
    IWithContext,
    IScalableWithDimensions<LinearProgressIndicator, ProgressIndicatorDimensions<LinearProgressIndicator>>,
    ISkinnableWithRules<LinearProgressIndicator, ProgressIndicatorSkinningRules<LinearProgressIndicator>>
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


    protected val inactiveBarRect = RectF()

    protected val activeBarRect = RectF()


    private var indeterminateX = 0f


    private val barPaint = Paint()

    private val barPath = Path()


    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = dimensions.let { it.barRadius * it.currentScale }
        val padding = dimensions.let { it.barPadding * it.currentScale }

        // Inactive bar
        barPaint.color = inactiveBarColor
        inactiveBarRect.set(padding, padding, width - padding, height - padding)

        // The clip path is a workaround to avoid flat corners when the active bar is small.
        barPath.reset()
        barPath.addRoundRect(inactiveBarRect, radius, CW)

        canvas.save()
        canvas.clipPath(barPath)
        canvas.drawPath(barPath, barPaint)

        // Active bar
        barPaint.color = activeBarColor

        if (indeterminate)
        {
            // Computing the bar width as the 4th part of the view width for indeterminate bar.
            val barWidth = (width - padding * 2) / 4

            indeterminateX += 2f

            if (indeterminateX > width - padding)
                indeterminateX = padding - barWidth

            activeBarRect.apply {

                top = padding
                bottom = height - padding

                // Coercing both left and right to the view bounds less the padding accordingly to
                // the indeterminate X.

                left = indeterminateX.coerceAtLeast(padding)
                right = (indeterminateX + barWidth).coerceAtMost(width - padding)
            }

            canvas.drawRoundRect(activeBarRect, radius, barPaint)
            canvas.restore()

            // Since is indeterminate it should be invalidated afterwards because of the animation.
            invalidate()
            return
        }

        // Computing barWidth with the view width as base and padding both left and right sides
        // accounting for progress range.
        val barWidth = (width - padding * 2) * ((progress - min) / (max - min))

        activeBarRect.set(padding, padding, padding + barWidth, height - padding)

        canvas.drawRoundRect(activeBarRect, radius, barPaint)
        canvas.restore()
    }
}