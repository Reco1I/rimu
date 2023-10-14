package game.rimu.ui.views

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toDrawable
import com.reco1l.framework.android.views.setImageTint
import com.reco1l.skindecoder.data.SkinDataColours
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalable
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.ViewDimensions
import game.rimu.ui.ViewSkinningRules


// Base

open class ImageSkinningRules<T : ImageView> : ViewSkinningRules<T>()
{

    /**
     * The asset key.
     */
    var texture: Pair<String, Int>? = null

    /**
     * The tint name and factor that should be set to the drawable.
     */
    var tint: Pair<String, Float>? = null


    @CallSuper
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        super.onApplySkin(target, skin)

        texture?.also { (key, variant) ->

            target.setImageBitmap(skin.ctx.resources[key, variant])
        }

        tint?.also { (key, factor) ->

            target.setImageTint(skin.data.colours.map[key]?.factorInt(factor) ?: TRANSPARENT)
        }
    }
}


fun <T> T.ImageView(
    attach: Boolean = true,
    block: ImageView.() -> Unit
) where T : ViewGroup, T : IWithContext = ImageView(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

open class ImageView(override val ctx: RimuContext) :
    AppCompatImageView(ctx),
    IWithContext,
    IScalableWithDimensions<ImageView>,
    ISkinnableWithRules<ImageView>
{

    override val dimensions by lazy { ViewDimensions<ImageView>() }

    override val skinningRules by lazy { ImageSkinningRules<ImageView>() }


    override fun onApplyScale(scale: Float)
    {
        (drawable as? IScalable)?.onApplyScale(scale)

        super.onApplyScale(scale)
    }
}



// FadeImageView

fun <T> T.FadeImageView(
    attach: Boolean = true,
    block: FadeImageView.() -> Unit
) where T : ViewGroup, T : IWithContext = FadeImageView(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

/**
 * ImageView that fades when the drawable is changed.
 */
class FadeImageView(override val ctx: RimuContext) :
    ImageView(ctx),
    IWithContext
{

    private val blankDrawable = ColorDrawable(TRANSPARENT)

    /**
     * The transition animation duration.
     */
    var duration: Int = 300


    // The transition drawable that will be used for the animation, initially it will contain two
    // transparent ColorDrawables.
    private val transition = TransitionDrawable(
        arrayOf(
            ColorDrawable(TRANSPARENT),
            blankDrawable
        )
    )


    init
    {
        // Calling super to set the TransitionDrawable properly.
        super.setImageDrawable(transition)
    }


    override fun setImageBitmap(bitmap: Bitmap?) = setImageDrawable(bitmap?.toDrawable(resources))

    /**
     * Sets a drawable as the content of this ImageView with a transition animation.
     *
     * @see duration
     */
    override fun setImageDrawable(drawable: Drawable?) = playTransition(drawable)


    private fun playTransition(newDrawable: Drawable?)
    {
        transition.setDrawable(0, transition.getDrawable(1))
        transition.setDrawable(1, newDrawable ?: blankDrawable)
        transition.startTransition(duration)

        requestLayout()
    }

    /**
     * Returns `true` if it has an image showing, use this instead of `drawable == null`.
     */
    fun hasImage() = transition.getDrawable(1) != blankDrawable
}