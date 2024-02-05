package com.reco1l.rimu.ui.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.ViewGroup
import com.reco1l.toolkt.graphics.drawRoundRect

import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin



fun ViewGroup.SeekBar(block: SeekBar.() -> Unit) = SeekBar(context as MainContext).also {
    addView(it)
    it.block()
}

open class SeekBar(ctx: MainContext) : ProgressIndicator(ctx)
{

    override val dimensions = super.dimensions.apply {
        height = 14
        barRadius = 14f
        barPadding = 4f
    }

    override var indeterminate: Boolean
        get() = false
        set(_) = throw IllegalArgumentException("SeekBar cannot be indeterminate.")


    var onSeek: ((Float) -> Unit)? = null

    var onStartSeek: (() -> Unit)? = null

    var onEndSeek: ((Float) -> Unit)? = null


    private val thumbRect = RectF()

    private val thumbPaint = Paint()


    init
    {
        isClickable = true
    }


    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = dimensions.let { it.barRadius * it.currentScale }

        // Thumb size will always equal to view height without accounting for padding.
        val left = (activeBarRect.right - height / 2f).coerceIn(0f, width - height)

        thumbRect.set(left, 0f, left + height, height)

        canvas.drawRoundRect(thumbRect, radius, thumbPaint)
    }


    private fun setProgressFromTouch(x: Float)
    {
        val padding = dimensions.let { it.barPadding * it.currentScale }

        // Computing max X position.
        val fX = inactiveBarRect.right - padding

        // Coercing delta X to bar bounds rather than view bounds because of the padding.
        val dX = x.coerceIn(padding, fX) - padding

        // Setting new progress accounting for range (min to max values).
        progress = min + ((max - min) * (dX / fX))

        onSeek?.invoke(progress)
    }


    override fun onTouchEvent(event: MotionEvent) = when (event.action)
    {
        ACTION_DOWN ->
        {
            // Disabling ACTION_MOVE interception for parent views so we can handle seeking outside
            // the view bounds, useful for ScrollViews.
            parent.requestDisallowInterceptTouchEvent(true)

            onStartSeek?.invoke()
            setProgressFromTouch(event.x)
            true
        }

        ACTION_MOVE ->
        {
            setProgressFromTouch(event.x)
            true
        }

        ACTION_UP ->
        {
            // Re-enabling interception.
            parent.requestDisallowInterceptTouchEvent(false)

            onEndSeek?.invoke(progress)
            true
        }

        else -> super.onTouchEvent(event)
    }


    override fun onApplySkin(skin: WorkingSkin)
    {
        thumbPaint.color = skin.data.colours.accentColor.toInt()

        super.onApplySkin(skin)
    }
}