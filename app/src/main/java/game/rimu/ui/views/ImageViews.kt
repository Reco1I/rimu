package game.rimu.ui.views

import android.graphics.Bitmap
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.graphics.drawable.toDrawable
import com.reco1l.framework.android.views.setImageTint
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalable
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import android.widget.ImageView as AndroidImageView


// Base

open class ImageSkinningRules<T : ImageView> : ViewSkinningRules<T>()
{

    var image: String? = null

    var imageVariant: Int = 0


    var imageTint: String? = null

    var imageTintFactor: Float = 1f


    @CallSuper
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        super.onApplySkin(target, skin)

        image?.also { target.setImageBitmap(skin.ctx.resources[it, imageVariant]) }

        imageTint?.also {

            target.setImageTint(skin.colors[it]?.factorInt(imageTintFactor) ?: TRANSPARENT)
        }
    }
}


fun IWithContext.ImageView(
    parent: ViewGroup? = this as? ViewGroup,
    init: ImageView.() -> Unit
) = ImageView(ctx).apply {
    parent?.addView(this)
    init()
}

open class ImageView(override val ctx: RimuContext) :
    AndroidImageView(ctx),
    IWithContext,
    IScalableWithDimensions<ImageView>,
    ISkinnableWithRules<ImageView>
{

    override val dimensions by lazy { ViewDimensions<ImageView>() }

    override val rules by lazy { ImageSkinningRules<ImageView>() }


    override fun onApplyScale(scale: Float)
    {
        (drawable as? IScalable)?.onApplyScale(scale)

        super.onApplyScale(scale)
    }
}



// FadeImageView

fun IWithContext.FadeImageView(
    parent: ViewGroup? = this as? ViewGroup,
    init: FadeImageView.() -> Unit
) = FadeImageView(ctx).apply {
    parent?.addView(this)
    init()
}

/**
 * ImageView that fades when the drawable is changed.
 */
class FadeImageView(ctx: RimuContext) :
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