package game.rimu.ui.views

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.reco1l.framework.android.views.backgroundColor
import com.reco1l.framework.android.views.cornerRadius
import com.reco1l.framework.android.views.setMargins
import com.reco1l.framework.android.views.setPaddings
import com.reco1l.framework.android.views.setSize
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.ScalableDimensions
import game.rimu.ui.SkinningRules


fun IWithContext.DummyView(
    parent: ViewGroup? = this as? ViewGroup,
    init: DummyView.() -> Unit
) = DummyView(ctx).apply {
    parent?.addView(this)
    init()
}

class DummyView(override val ctx: MainContext) :
    View(ctx),
    IWithContext,
    IScalableWithDimensions<View>,
    ISkinnableWithRules<View>
{
    override var dimensions = ViewDimensions<View>()

    override var rules = ViewSkinningRules<View>()
}


open class ViewDimensions<V : View>(

    initialWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT,

    initialHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

) : ScalableDimensions<V>(initialWidth, initialHeight)
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


    fun padding(horizontal: Int, vertical: Int)
    {
        paddingLeft = horizontal
        paddingRight = horizontal
        paddingTop = vertical
        paddingBottom = vertical
    }

    fun padding(value: Int)
    {
        paddingLeft = value
        paddingRight = value
        paddingTop = value
        paddingBottom = value
    }

    fun margin(horizontal: Int, vertical: Int)
    {
        marginLeft = horizontal
        marginRight = horizontal
        marginTop = vertical
        marginBottom = vertical
    }

    fun margin(value: Int)
    {
        marginLeft = value
        marginRight = value
        marginTop = value
        marginBottom = value
    }

    fun size(value: Int)
    {
        width = value
        height = value
    }


    open fun <T : View> set(other: ViewDimensions<T>)
    {
        width = other.width
        height = other.height
        paddingLeft = other.paddingLeft
        paddingRight = other.paddingRight
        paddingTop = other.paddingTop
        paddingBottom = other.paddingBottom
        marginLeft = other.marginLeft
        marginRight = other.marginRight
        marginTop = other.marginTop
        marginBottom = other.marginBottom
        cornerRadius = other.cornerRadius
    }


    override fun onApplyScale(target: V, scale: Float)
    {
        super.onApplyScale(target, scale)

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

        target.cornerRadius = cornerRadius * scale
    }
}


/**
 * Defines the rules that the view should follow when the skin is changed.
 */
open class ViewSkinningRules<T : View> : SkinningRules<T>()
{

    /**
     * Define the drawable that should be set as background.
     */
    var background: String? = null

    var backgroundVariant: Int = 0


    /**
     * Define the background color that should be set.
     *
     * Note: This overrides the value set in [background] property.
     */
    var backgroundColor: String? = null

    var backgroundColorFactor: Float = 1f


    @CallSuper
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        backgroundColor?.also {

            target.backgroundColor = skin.colors[it]?.toInt(factor = backgroundColorFactor) ?: Color.TRANSPARENT

        } ?: background?.also { target.background = skin.ctx.resources[it, backgroundVariant] }
    }
}