package game.rimu.ui.layouts

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import com.reco1l.framework.android.views.backgroundColor
import com.reco1l.framework.android.views.doPost
import com.reco1l.framework.android.views.orientation
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.animation.Ease
import com.reco1l.framework.animation.animate
import com.reco1l.framework.animation.cancelAnimators
import com.reco1l.framework.animation.toAlpha
import com.reco1l.framework.animation.toTranslationX
import com.reco1l.framework.graphics.Anchor
import game.rimu.R
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.data.adapter.Adapter
import game.rimu.data.adapter.IHeldView
import game.rimu.ui.LayerOverlay
import game.rimu.ui.BaseLayer
import game.rimu.ui.LayerBackground
import game.rimu.ui.views.DummyView
import game.rimu.ui.views.ImageView
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.LinearProgressIndicator
import game.rimu.ui.views.RecyclerView
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import kotlin.reflect.KClass


class NotificationCenter(ctx: MainContext) : ModelLayout(ctx)
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class

    override val shouldRemainInMemory = true


    private val notifications = mutableListOf<Notification>()

    private var backgroundAnimator: ValueAnimator? = null


    private val body = LinearLayout {

        orientation = VERTICAL
        z = 1f

        dimensions.apply {

            height = MATCH_PARENT
            width = 250
        }

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.15f
        }

        TextView {

            setText(R.string.header_notifications)
            gravity = Gravity.CENTER

            dimensions.apply {
                width = MATCH_PARENT
                padding(0, 12)
            }

            rules.apply {
                backgroundColor = "accentColor"
                backgroundColorFactor = 0.15f
            }
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val bodyShadow = DummyView {

        dimensions.set(body.dimensions)

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.1f
        }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

    private val listView = RecyclerView(parent = body) {

        orientation = VERTICAL

        dimensions.apply {
            width = MATCH_PARENT
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
            ctx.layouts[when (notification)
            {
                is ProcessNotification -> ProcessNotificationView::class
                else -> NotificationView::class

            }].apply { onAssignData(notification, 0) }.show(true)
    }

    fun update(notification: Notification) = mainThread {

        val index = notifications.indexOf(notification)

        if (index >= 0)
            listView.adapter!!.notifyItemChanged(index)
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
    IWithContext,
    IHeldView<Notification>
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class


    protected val iconView = ImageView {

        backgroundColor = 0x27000000

        rules.imageTint = "accentColor"

        dimensions.apply {
            cornerRadius = 8f
            padding(8)
            size(32)
        }

        setConstraints(topToTarget = Anchor.TOP, leftToTarget = Anchor.LEFT)
    }


    protected val headerView = TextView {

        rules.fontColorFactor = 0.8f

        dimensions.apply {
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

        dimensions.fontSize = 10

        setConstraints(
            target = headerView,
            topToTarget = Anchor.BOTTOM,
            leftToTarget = Anchor.LEFT
        )
    }

    init
    {
        dimensions.apply {
            width = MATCH_PARENT
            cornerRadius = 8f
            marginBottom = 8
            padding(8)
        }

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.2f
        }
    }


    override fun onAssignData(data: Notification, position: Int)
    {
        headerView.text = data.header.uppercase()
        messageText.text = data.message
        iconView.rules.image = data.icon

        invalidateSkin()
    }

    override fun onAttachedToWindow()
    {
        if (isAttachedToLayer)
        {
            dimensions.apply {
                width = 240
                height = WRAP_CONTENT

                marginTop = ctx.layouts[TopBarLayout::class].dimensions.height + 8
                marginRight = 8
            }

            setConstraints(topToTarget = Anchor.TOP, rightToTarget = Anchor.RIGHT)
        }

        super.onAttachedToWindow()
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

        dimensions.marginTop = 12
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