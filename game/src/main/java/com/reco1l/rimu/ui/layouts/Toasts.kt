package com.reco1l.rimu.ui.layouts

import android.graphics.Color
import android.graphics.Color.BLACK
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import com.reco1l.toolkt.android.fontColor
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.rimu.constants.Ease
import com.reco1l.toolkt.animation.cancelAnimators
import com.reco1l.toolkt.animation.toAlpha
import com.reco1l.toolkt.animation.toScale
import com.reco1l.toolkt.graphics.Anchor
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.LayerOverlay
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.view
import kotlin.reflect.KClass


class ToastView(ctx: MainContext) : ModelLayout(ctx)
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class

    override var hideTime: Long? = 5000L


    /**
     * Change the header text.
     */
    var header: String
        get() = headerText.text.toString()
        set(value)
        {
            headerText.text = value
        }

    /**
     * Change the message text.
     */
    var message: String
        get() = messageText.text.toString()
        set(value)
        {
            messageText.text = value
        }


    private val headerText = view<TextView> {

        setDimensions {
            fontSize = 10
        }

        setSkinning {
            fontColor = null
        }

        fontColor = Color.WHITE
        alpha = 0.8f

        setConstraints(
            rightToTarget = Anchor.RIGHT,
            leftToTarget = Anchor.LEFT
        )
    }

    private val messageText = view<TextView> {

        setDimensions {
            fontSize = 14
            marginTop = 6
        }

        setSkinning {
            fontColor = null
        }

        fontColor = Color.WHITE

        setConstraints(
            rightToTarget = Anchor.RIGHT,
            leftToTarget = Anchor.LEFT
        )

        setConstraints(target = headerText, topToTarget = Anchor.BOTTOM)
    }


    init
    {
        setDimensions {
            width = LayoutParams.WRAP_CONTENT
            height = LayoutParams.WRAP_CONTENT

            cornerRadius = 12f
            marginBottom = 20
            padding(14)
        }

        background = ColorDrawable(BLACK).apply {
            alpha = 190
        }
    }


    override fun show(override: Boolean): Boolean
    {
        // Since only one toast can be shown in screen we'll always override the previous if any.
        return super.show(true)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        hide()
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow()
    {
        setConstraints(
            bottomToTarget = Anchor.BOTTOM,
            rightToTarget = Anchor.RIGHT,
            leftToTarget = Anchor.LEFT
        )

        cancelAnimators()
        toAlpha(0f)
        toScale(0.8f)
        toAlpha(1f, 200)
        toScale(1f, 500, ease = Ease.BOUNCE_OUT)

        super.onAttachedToWindow()
    }

    override fun hide()
    {
        removeHideTimer()

        cancelAnimators()
        toAlpha(0f, 300, ease = Ease.EXPO_OUT)
        toScale(0.8f, 300, ease = Ease.EXPO_OUT) {
            onEnd = { super.hide() }
        }
    }
}