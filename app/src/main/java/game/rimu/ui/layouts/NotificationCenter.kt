package game.rimu.ui.layouts

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.VERTICAL
import com.reco1l.framework.android.views.backgroundColor
import com.reco1l.framework.android.views.doPost
import com.reco1l.framework.android.views.orientation
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.animation.Ease
import com.reco1l.framework.animation.cancelAnimators
import com.reco1l.framework.animation.toAlpha
import com.reco1l.framework.animation.toTranslationX
import com.reco1l.framework.graphics.Anchor
import game.rimu.R
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.data.adapter.Adapter
import game.rimu.data.adapter.IHeldView
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayoutLayer
import game.rimu.ui.views.ConstraintLayout
import game.rimu.ui.views.DummyView
import game.rimu.ui.views.ImageView
import game.rimu.ui.views.LinearLayout
import game.rimu.ui.views.RecyclerView
import game.rimu.ui.views.TextView
import kotlin.reflect.KClass


class NotificationCenter(ctx: RimuContext) : ModelLayout(ctx)
{

    override var layer: KClass<out LayoutLayer> = LayerOverlay::class


    private val notifications = mutableListOf<Notification>()


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

            setText(R.string.title_notifications)
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
            onCreateView = { NotificationView(ctx) }
        )
    }


    fun add(notification: Notification)
    {
        notifications.add(0, notification)
        listView.adapter!!.notifyItemInserted(0)
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()
        setConstraints(rightToTarget = Anchor.RIGHT)

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
                toTranslationX(0f, 300, ease = Ease.EXPO_OUT)
            }
        }
    }


    override fun hide()
    {
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

class ProcessNotification(

    header: String,

    message: String,

    icon: String?,

    var progress: Int

) : Notification(header, message, icon)


// View

open class NotificationView(override val ctx: RimuContext) :
    ConstraintLayout(ctx),
    IWithContext,
    IHeldView<Notification>
{

    final override val dimensions = super.dimensions

    final override val rules = super.rules


    private val iconView = ImageView {

        backgroundColor = 0x27000000

        rules.imageTint = "accentColor"

        dimensions.apply {
            cornerRadius = 8f
            padding(8)
            size(32)
        }

        setConstraints(topToTarget = Anchor.TOP, leftToTarget = Anchor.LEFT)
    }


    private val headerView = TextView {

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

    private val messageText = TextView {

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
            padding(8)
        }

        rules.apply {
            backgroundColor = "accentColor"
            backgroundColorFactor = 0.2f
        }
    }


    override fun onAssignData(data: Notification, position: Int)
    {
        headerView.text = data.header
        messageText.text = data.message
        iconView.rules.image = data.icon

        invalidateSkin()
    }
}