package com.reco1l.rimu.ui.views

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.reco1l.toolkt.android.backgroundColor
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.setMargins
import com.reco1l.toolkt.android.setPaddings
import com.reco1l.toolkt.android.setSize

import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.IScalableWithDimensions
import com.reco1l.rimu.ui.ISkinnableWithRules
import com.reco1l.rimu.ui.ScalableDimensions
import com.reco1l.rimu.ui.SkinningRules
import com.reco1l.toolkt.kotlin.createInstance


/**
 * Creates a view from this context.
 *
 * @param parent The parent from where this new view will be attached. If `null` the view will not
 * be attached to anything.
 * @param block The block that will be called when the view is created and after attachment if [parent]
 * was specified.
 */
inline fun <reified T> IWithContext.view(

    parent: ViewGroup? = this as? ViewGroup,

    block: T.() -> Unit

): T where T : View, T : IWithContext
{
    val view = T::class.createInstance(ctx)
    parent?.addView(view)
    view.block()
    return view
}


fun ViewGroup.DummyView(block: DummyView.() -> Unit) = DummyView(context as MainContext).also {
    addView(it)
    it.block()
}

/**
 * A dummy view that has no special functionality.
 */
class DummyView(override val ctx: MainContext) :
    View(ctx),
    IWithContext,
    IScalableWithDimensions<View, ViewDimensions<View>>,
    ISkinnableWithRules<View, ViewSkinningRules<View>>
{
    override var dimensions = ViewDimensions<View>()

    override var skinningRules = ViewSkinningRules<View>()
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

    var fadeEdgeLength: Int = 0


    fun padding(value: Int) = padding(value, value)

    fun padding(horizontal: Int, vertical: Int)
    {
        paddingLeft = horizontal
        paddingRight = horizontal
        paddingTop = vertical
        paddingBottom = vertical
    }

    fun margin(value: Int) = margin(value, value)

    fun margin(horizontal: Int, vertical: Int)
    {
        marginLeft = horizontal
        marginRight = horizontal
        marginTop = vertical
        marginBottom = vertical
    }

    fun size(value: Int)
    {
        width = value
        height = value
    }


    open fun <T : View> clone(other: ViewDimensions<T>)
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
        target.setFadingEdgeLength((fadeEdgeLength * scale).toInt())
    }
}


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