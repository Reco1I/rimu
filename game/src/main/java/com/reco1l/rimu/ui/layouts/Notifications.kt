package com.reco1l.rimu.ui.layouts

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import androidx.core.view.marginRight
import com.reco1l.toolkt.android.backgroundColor
import com.reco1l.toolkt.android.doPost
import com.reco1l.toolkt.android.orientation
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.rimu.constants.Ease
import com.reco1l.toolkt.animation.animate
import com.reco1l.toolkt.animation.cancelAnimators
import com.reco1l.toolkt.animation.toAlpha
import com.reco1l.toolkt.animation.toScale
import com.reco1l.toolkt.animation.toTranslationX
import com.reco1l.toolkt.animation.toTranslationY
import com.reco1l.toolkt.graphics.Anchor
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.R
import com.reco1l.rimu.data.adapter.Adapter
import com.reco1l.rimu.data.adapter.IHeldView
import com.reco1l.rimu.mainThread
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.LayerBackground
import com.reco1l.rimu.ui.LayerOverlay
import com.reco1l.rimu.ui.views.DummyView
import com.reco1l.rimu.ui.views.ImageView
import com.reco1l.rimu.ui.views.LinearLayout
import com.reco1l.rimu.ui.views.LinearProgressIndicator
import com.reco1l.rimu.ui.views.RecyclerView
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.addons.setTouchHandler
import kotlin.reflect.KClass


class NotificationCenter(ctx: MainContext) : ModelLayout(ctx)
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class

    override val shouldRemainInMemory = true


    private val notifications = mutableListOf<Notification>()

    private val body = LinearLayout {

        orientation = VERTICAL
        z = 1f

        setDimensions {

            height = MATCH_PARENT
            width = 250
        }

        setSkinning {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.15f
        }

        TextView {

            setText(R.string.header_notifications)
            gravity = Gravity.CENTER

            setDimensions {
                width = MATCH_PARENT
                padding(0, 12)
            }

            setSkinning {
                backgroundColor = "accentColor"
                backgroundColorFactor = 0.15f
            }
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val bodyShadow = DummyView {

        dimensions.set(body.dimensions)

        setSkinning {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.1f
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val listView = RecyclerView(parent = body) {

        orientation = VERTICAL
        isVerticalFadingEdgeEnabled = true

        setDimensions {
            width = MATCH_PARENT
            fadeEdgeLength = 30
            padding(14)
        }

        adapter = Adapter(
            data = notifications,
            onDistinctViews = {
                when (notifications[it]::class)
                {
                    ProcessNotification::class -> 1
                    else -> 0
                }
            },
            onCreateView = {
                when (it)
                {
                    1 -> ProcessNotificationView(ctx)
                    else -> NotificationView(ctx)
                }
            }
        )
    }

    private var backgroundAnimator: ValueAnimator? = null

    private var currentPopup: NotificationView? = null


    init
    {
        backgroundColor = Color.BLACK
        background.alpha = 0

        setTouchHandler {
            noEffect()
            onActionUp = { hide() }
        }
    }


    fun add(notification: Notification) = mainThread {

        notifications.add(0, notification)
        listView.adapter!!.notifyItemInserted(0)

        if (!isAttachedToLayer)
        {
            currentPopup = when (notification)
            {
                is ProcessNotification -> ProcessNotificationView(ctx)
                else -> NotificationView(ctx)
            }
            currentPopup!!.onAssignData(notification, 0)
            currentPopup!!.show(true)
        }
    }

    fun update(notification: Notification) = mainThread {

        val index = notifications.indexOf(notification)

        if (index >= 0)
            listView.adapter!!.notifyItemChanged(index)

        currentPopup?.apply {

            if (associatedData == notification)
                onAssignData(notification, 0)

        }
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()
        setConstraints(rightToTarget = Anchor.RIGHT)

        backgroundAnimator?.cancel()
        backgroundAnimator = background::setAlpha.animate(background.alpha, 65, 200)

        ctx.layouts[LayerBackground::class].apply {

            cancelAnimators()
            toTranslationX(-50f, 400, ease = Ease.EXPO_OUT)
        }

        body.apply {
            cancelAnimators()
            toAlpha(0f)

            doPost {
                toTranslationX(width.toFloat())
                toAlpha(1f)
                toTranslationX(0f, 400, 50, Ease.EXPO_OUT)
            }
        }

        bodyShadow.apply {
            cancelAnimators()
            toAlpha(0f)

            doPost {
                toTranslationX(width.toFloat())
                toAlpha(1f)
                toTranslationX(0f, 400, ease = Ease.EXPO_OUT)
            }
        }
    }


    override fun hide()
    {
        backgroundAnimator?.cancel()
        backgroundAnimator = background::setAlpha.animate(background.alpha, 0, 200)

        ctx.layouts[LayerBackground::class].apply {

            cancelAnimators()
            toTranslationX(0f, 350, ease = Ease.EXPO_IN)
        }

        body.apply {
            cancelAnimators()
            toTranslationX(width.toFloat(), 350, ease = Ease.EXPO_IN)
        }

        bodyShadow.apply {
            cancelAnimators()
            toTranslationX(width.toFloat(), 350, 50, Ease.EXPO_IN, listener = {
                onEnd = { super.hide() }
            })
        }
    }
}


// Notifications

open class Notification(

    var header: String,

    var message: String,

    var icon: String?

)
{
    fun update(ctx: MainContext) = ctx.layouts[NotificationCenter::class].update(this)

    fun show(ctx: MainContext) = ctx.layouts[NotificationCenter::class].add(this)
}

class ProcessNotification(

    header: String,

    message: String,

    var progress: Float = 0f,

    var minProgress: Float = 0f,

    var maxProgress: Float = 1f,

    var indeterminate: Boolean = true,

    var showIndicator: Boolean = true,

    icon: String? = null

) : Notification(header, message, icon)


// View

open class NotificationView(ctx: MainContext) :
    ModelLayout(ctx),
    IHeldView<Notification>
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class

    lateinit var associatedData: Notification


    protected val iconView = ImageView {

        backgroundColor = 0x27000000

        rules.imageTint = "accentColor"

        setDimensions {
            cornerRadius = 8f
            padding(8)
            size(32)
        }

        setConstraints(topToTarget = Anchor.TOP, leftToTarget = Anchor.LEFT)
    }


    protected val headerText = TextView {

        rules.fontColorFactor = 0.8f

        setDimensions {
            marginLeft = 8
            fontSize = 9
        }

        setConstraints(
            target = iconView,
            topToTarget = Anchor.TOP,
            leftToTarget = Anchor.RIGHT
        )
    }

    protected val messageText = TextView {

        setDimensions {
            fontSize = 10
        }

        setConstraints(
            target = headerText,
            topToTarget = Anchor.BOTTOM,
            leftToTarget = Anchor.LEFT
        )
    }

    init
    {
        setDimensions {
            width = MATCH_PARENT
            height = WRAP_CONTENT
            cornerRadius = 8f
            marginBottom = 8
            padding(8)
        }

        setSkinning {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.2f
        }
    }


    override fun onAssignData(data: Notification, position: Int)
    {
        associatedData = data

        headerText.text = data.header.uppercase()
        messageText.text = data.message
        iconView.rules.image = data.icon

        invalidateSkin()
        invalidateHideTimer()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        invalidateHideTimer()

        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow()
    {
        if (isAttachedToLayer)
        {
            hideTime = 5000

            setDimensions {
                width = 240

                marginTop = ctx.layouts[TopBarLayout::class].dimensions.height + 8
                marginRight = 8
            }

            setConstraints(topToTarget = Anchor.TOP, rightToTarget = Anchor.RIGHT)

            toAlpha(0f)

            post {
                cancelAnimators()
                toTranslationX((width + marginRight).toFloat())

                toAlpha(1f, 300)
                toTranslationX(0f, 400, ease = Ease.BOUNCE_OUT)
            }
        }

        super.onAttachedToWindow()
    }

    override fun hide()
    {
        cancelAnimators()
        toAlpha(0f, 300, ease = Ease.EXPO_OUT)
        toScale(0.8f, 300, ease = Ease.EXPO_OUT)
        toTranslationY(30f, 300, ease = Ease.EXPO_OUT, listener = { onEnd = { super.hide() } })
    }
}


class ProcessNotificationView(ctx: MainContext) : NotificationView(ctx)
{

    private val indicator = LinearProgressIndicator {

        setConstraints(
            target = messageText,
            topToTarget = Anchor.BOTTOM,
            leftToTarget = Anchor.LEFT,
            rightToTarget = Anchor.RIGHT
        )

        setDimensions {
            marginTop = 12
        }
    }

    override fun onAssignData(data: Notification, position: Int)
    {
        data as ProcessNotification

        indicator.apply {

            progress = data.progress
            min = data.minProgress
            max = data.maxProgress
            indeterminate = data.indeterminate

            visibility = if (data.showIndicator) VISIBLE else GONE
        }

        super.onAssignData(data, position)
    }
}